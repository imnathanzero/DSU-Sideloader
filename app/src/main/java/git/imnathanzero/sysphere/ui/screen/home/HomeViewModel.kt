package git.imnathanzero.sysphere.ui.screen.home

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.lifecycle.HiltViewModel
import git.imnathanzero.sysphere.BuildConfig
import git.imnathanzero.sysphere.R
import git.imnathanzero.sysphere.core.BaseViewModel
import git.imnathanzero.sysphere.core.StorageManager
import git.imnathanzero.sysphere.installer.LiveInstallationManager
import git.imnathanzero.sysphere.installer.adb.AdbInstallationHandler
import git.imnathanzero.sysphere.installer.privileged.DsuInstallationHandler
import git.imnathanzero.sysphere.installer.privileged.LogcatDiagnostic
import git.imnathanzero.sysphere.installer.root.DSUInstaller
import git.imnathanzero.sysphere.model.DSUInstallationSource
import git.imnathanzero.sysphere.model.SelectedImageFile
import git.imnathanzero.sysphere.model.Session
import git.imnathanzero.sysphere.preferences.AppPrefs
import git.imnathanzero.sysphere.preparation.InstallationStep
import git.imnathanzero.sysphere.preparation.Preparation
import git.imnathanzero.sysphere.service.PrivilegedProvider
import git.imnathanzero.sysphere.util.DevicePropUtils
import git.imnathanzero.sysphere.util.FilenameUtils
import git.imnathanzero.sysphere.util.OperationMode
import git.imnathanzero.sysphere.util.OperationModeUtils
import git.imnathanzero.sysphere.util.StorageUtils
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    val application: Application,
    override val dataStore: DataStore<Preferences>,
    private val storageManager: StorageManager,
    var session: Session,
    val liveInstallationManager: LiveInstallationManager,
) : BaseViewModel(dataStore) {

    private val tag = this.javaClass.simpleName

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    var checkDynamicPartitions = true
    var checkUnavaiableStorage = true
    var checkReadLogsPermission = true
    var disabledStorageCheck = false

    var installationJob: Job = Job()
    var logger: LogcatDiagnostic? = null

    private val allocPercentage = DevicePropUtils.getGsidBinaryAllowedPerc()
    val allocPercentageInt = String.format("%.0f", allocPercentage * 100).toInt()

    private val storageStats = StorageUtils.getAllocInfo(allocPercentage)
    private val hasAvailableStorage = storageStats.first
    private val maximumAllowedForAllocation = storageStats.second

    //
    // Helper methods used for controlling UI State
    //

    private fun updateAdditionalCardState(additionalCard: AdditionalCardState) =
        _uiState.update { it.copy(additionalCard = additionalCard) }

    private fun updateUserdataCard(update: (UserDataCardState) -> UserDataCardState) =
        _uiState.update { it.copy(userDataCard = update(it.userDataCard.copy())) }

    private fun updateInstallationCard(update: (InstallationCardState) -> InstallationCardState) =
        _uiState.update { it.copy(installationCard = update(it.installationCard.copy())) }

    private fun updateSheetState(sheetDisplay: SheetDisplayState) =
        _uiState.update { it.copy(sheetDisplay = sheetDisplay) }

    fun resetInstallationCard() {
        session.userSelection.selectedFiles.clear()
        session.userSelection.selectedFileName = ""
        session.userSelection.selectedFileUri = Uri.EMPTY
        liveInstallationManager.cancel()
        _uiState.update {
            val installationStep = if (it.isDsuInstalled) {
                InstallationStep.DSU_ALREADY_INSTALLED
            } else {
                InstallationStep.NOT_INSTALLING
            }
            it.copy(
                installationCard = InstallationCardState(installationStep = installationStep),
                sheetDisplay = SheetDisplayState.NONE,
            )
        }
    }

    fun dismissSheet() = updateSheetState(SheetDisplayState.NONE)

    //
    // Home startup and checks
    //

    init {
        liveInstallationManager.onCancelRequested = {
            onClickCancelInstallationButton()
        }
        liveInstallationManager.onRebootRequested = {
            onClickRebootToDynOS()
        }

        // Check if a DSU is already installed
        // Root-only because MANAGE_DYNAMIC_SYSTEM is required
        if (session.isRoot()) {
            PrivilegedProvider.run {
                if (isInUse) {
                    updateInstallationCard { it.copy(installationStep = InstallationStep.DSU_ALREADY_RUNNING_DYN_OS) }
                    return@run
                }
                if (isInstalled) {
                    _uiState.update {
                        it.copy(
                            isDsuInstalled = true,
                            userDataCard = it.userDataCard.copy(preserveSelected = true),
                        )
                    }
                    session.preferences.preserveUserdata = true
                    updateInstallationCard { it.copy(installationStep = InstallationStep.DSU_ALREADY_INSTALLED) }
                    return@run
                }
            }
        }
    }

    fun initialChecks() {
        if (checkDynamicPartitions && !DevicePropUtils.hasDynamicPartitions()) {
            updateAdditionalCardState(AdditionalCardState.NO_DYNAMIC_PARTITIONS)
            return
        }

        if (checkUnavaiableStorage && !hasAvailableStorage) {
            updateAdditionalCardState(AdditionalCardState.UNAVAIABLE_STORAGE)
            return
        }

        viewModelScope.launch {
            val result = readStringPref(AppPrefs.SAF_PATH)
            if (!storageManager.arePermissionsGrantedToFolder(result)) {
                updateAdditionalCardState(AdditionalCardState.SETUP_STORAGE)
                return@launch
            }

            if (session.getOperationMode() == OperationMode.SHIZUKU && checkReadLogsPermission &&
                !OperationModeUtils.isReadLogsPermissionGranted(application)
            ) {
                _uiState.update { it.copy(passedInitialChecks = false) }
                updateAdditionalCardState(AdditionalCardState.MISSING_READ_LOGS_PERMISSION)
                return@launch
            }

            val seenUnlockedBootloaderWarning = readBoolPref(AppPrefs.BOOTLOADER_UNLOCKED_WARNING)
            if (!seenUnlockedBootloaderWarning) {
                _uiState.update { it.copy(passedInitialChecks = false) }
                updateAdditionalCardState(AdditionalCardState.BOOTLOADER_UNLOCKED_WARNING)
                return@launch
            }

            updateAdditionalCardState(AdditionalCardState.NONE)
            _uiState.update { it.copy(passedInitialChecks = true) }
        }
    }

    fun setupUserPreferences() {
        viewModelScope.launch {
            val shouldKeepScreenOn = readBoolPref(AppPrefs.KEEP_SCREEN_ON)
            Log.d(tag, "shouldKeepScreenOn: $shouldKeepScreenOn")
            _uiState.update { it.copy(shouldKeepScreenOn = shouldKeepScreenOn) }

            disabledStorageCheck = readBoolPref(AppPrefs.DISABLE_STORAGE_CHECK)
            Log.d(tag, "disabledStorageCheck: $shouldKeepScreenOn")
        }
    }

    fun overrideDynamicPartitionCheck() {
        checkDynamicPartitions = false
        Log.d(tag, "checkDynamicPartitions: $checkDynamicPartitions")
        initialChecks()
    }

    fun overrideUnavaiableStorage() {
        checkUnavaiableStorage = false
        Log.d(tag, "checkUnavaiableStorage: $checkUnavaiableStorage")
        initialChecks()
    }

    fun onClickBootloaderUnlockedWarning() {
        viewModelScope.launch {
            updateBoolPref(AppPrefs.BOOTLOADER_UNLOCKED_WARNING, true)
            initialChecks()
        }
    }

    //
    // Installation
    //

    fun obtainSelectedFilename(): String = session.userSelection.selectedFileName

    fun onClickCancel() {
        if (uiState.value.isInstalling()) {
            updateSheetState(SheetDisplayState.CANCEL_INSTALLATION)
        }
    }

    fun onClickInstall() {
        session.userSelection.setUserDataSize(uiState.value.userDataCard.text)
        updateSheetState(SheetDisplayState.CONFIRM_INSTALLATION)
    }

    fun onConfirmInstallationSheet() {
        dismissSheet()
        updateInstallationCard { it.copy(installationStep = InstallationStep.PROCESSING) }
        liveInstallationManager.startInstallation(
            step = InstallationStep.PROCESSING,
            title = application.getString(R.string.notification_preparing_title),
            stepText = application.getString(R.string.processing),
            isIndeterminate = true,
        )
        installationJob = Job()
        viewModelScope.launch(Dispatchers.IO + installationJob) {
            session.preferences.isUnmountSdCard = readBoolPref(AppPrefs.UMOUNT_SD)
            session.preferences.useBuiltinInstaller = readBoolPref(AppPrefs.USE_BUILTIN_INSTALLER)
            Preparation(
                storageManager = storageManager,
                session = session,
                job = installationJob,
                onStepUpdate = this@HomeViewModel::onStepUpdate,
                onPreparationProgressUpdate = this@HomeViewModel::onPreparationProgressUpdate,
                onCanceled = this@HomeViewModel::onClickCancelInstallationButton,
                onPreparationFinished = this@HomeViewModel::onPreparationFinished,
            ).invoke()
        }
    }

    private fun onPreparationFinished(dsuInstallation: DSUInstallationSource) {
        Log.d(tag, "DSU preparation finished, result: $dsuInstallation")
        session.dsuInstallation = dsuInstallation
        startInstallation()
    }

    private fun startInstallation() {
        Log.d(tag, "startInstallation(), session: \n$session")
        updateInstallationCard { it.copy(installationStep = InstallationStep.PROCESSING) }

        if (session.getOperationMode() == OperationMode.ADB) {
            setupAdbInstallation()
            return
        }

        if (session.preferences.useBuiltinInstaller && session.isRoot()) {
            startDSUInstallation()
            return
        }

        startPrivilegedInstallation()
    }

    private fun setupAdbInstallation() {
        AdbInstallationHandler(storageManager, session).generate { scriptPath ->
            Log.d(tag, "Installation script generated: $scriptPath")
            session.installationScriptPath = scriptPath
            resetInstallationCard()
            updateInstallationCard { it.copy(installationStep = InstallationStep.REQUIRES_ADB_CMD_TO_CONTINUE) }
        }
    }

    private fun startDSUInstallation() {
        DSUInstaller(
            application = application,
            userdataSize = session.userSelection.userSelectedUserdata,
            dsuInstallation = session.dsuInstallation,
            installationJob = installationJob,
            onInstallationError = this::onInstallationError,
            onInstallationProgressUpdate = this::onInstallationProgressUpdate,
            onCreatePartition = this::onCreatePartition,
            onInstallationStepUpdate = this::onStepUpdate,
            onInstallationSuccess = this::onRootInstallationSuccess,
            preserveUserdata = session.preferences.preserveUserdata,
        ).invoke()
    }

    private fun startPrivilegedInstallation() {
        updateInstallationCard { it.copy(installationStep = InstallationStep.WAITING_USER_CONFIRMATION) }
        DsuInstallationHandler(session).startInstallation()
        if (session.isRoot() || OperationModeUtils.isReadLogsPermissionGranted(application)) {
            startLogging()
        } else {
            updateInstallationCard { it.copy(installationStep = InstallationStep.INSTALL_SUCCESS) }
        }
    }

    // Track and diagnose installation by reading logcat
    private fun startLogging() {
        if (logger == null) {
            logger = LogcatDiagnostic(
                onInstallationError = this::onInstallationError,
                onStepUpdate = this::onStepUpdate,
                onInstallationProgressUpdate = this::onInstallationProgressUpdate,
                onInstallationSuccess = this::onInstallationSuccess,
                onLogLineReceived = this::onLogLineReceived,
            )
        }
        viewModelScope.launch(Dispatchers.IO + installationJob) {
            logger!!.shouldLogEverything = readBoolPref(AppPrefs.FULL_LOGCAT_LOGGING)
            logger!!.startLogging(generateUsefulLogInfo())
        }
    }

    private fun generateUsefulLogInfo(): String {
        return "Device: ${Build.MODEL}\n" +
            "SDK: Android ${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})\n" +
            "$session\n" +
            "Package: ${BuildConfig.APPLICATION_ID}\n" +
            "Version: ${BuildConfig.VERSION_NAME} - ${BuildConfig.VERSION_CODE} (${BuildConfig.BUILD_TYPE}})\n" +
            "checkDynamicPartitions: $checkDynamicPartitions\n" +
            "checkUnavaiableStorage: $checkUnavaiableStorage\n" +
            "checkReadLogsPermission: $checkReadLogsPermission\n" +
            "allocPercentage: $allocPercentage\n" +
            "hasAvailableStorage: $hasAvailableStorage\n" +
            "maximumAllowedForAllocation: $maximumAllowedForAllocation\n"
    }

    fun saveLogs(uriToSaveLogs: Uri) {
        Log.d(tag, "Writing logs to: $uriToSaveLogs")
        storageManager.writeStringToUri(uiState.value.installationLogs, uriToSaveLogs)
    }

    fun onClickCancelInstallationButton() {
        liveInstallationManager.cancel()
        resetInstallationCard()
        if (session.getOperationMode() != OperationMode.ADB &&
            logger != null && logger!!.isLogging.get()
        ) {
            logger!!.destroy()
            // Since stopping installation requires MANAGE_DYNAMIC_SYSTEM
            // then, we stop installation using other way, not so polite, but works :)))
            PrivilegedProvider.run { forceStopPackage("com.android.dynsystem") }
        }

        if (installationJob.isActive) {
            installationJob.cancel()
        }
        session.dsuInstallation = DSUInstallationSource()
    }

    //
    // Installation Card actions
    //

    fun onClickRebootToDynOS() {
        updateInstallationCard { it.copy(installationStep = InstallationStep.PROCESSING) }
        PrivilegedProvider.run {
            setEnable(true, true)
            Shell.cmd("reboot").exec()
        }
    }

    fun onClickDiscardGsiAndStartInstallation() {
        updateInstallationCard { it.copy(installationStep = InstallationStep.PROCESSING) }
        PrivilegedProvider.run {
            remove()
            forceStopPackage("com.android.dynsystem")
            startDSUInstallation()
        }
    }

    fun onClickDiscardGsi() {
        liveInstallationManager.cancel()
        updateInstallationCard { it.copy(installationStep = InstallationStep.PROCESSING) }
        PrivilegedProvider.run {
            remove()
            forceStopPackage("com.android.dynsystem")
            dismissSheet()
            _uiState.update {
                it.copy(
                    installationCard = InstallationCardState(),
                    sheetDisplay = SheetDisplayState.NONE,
                    isDsuInstalled = false,
                )
            }
        }
    }

    fun onClickRetryInstallation() {
        updateInstallationCard { it.copy(installationStep = InstallationStep.PROCESSING) }
        startInstallation()
    }

    fun onClickUnmountSdCardAndRetry() {
        updateInstallationCard { it.copy(installationStep = InstallationStep.PROCESSING) }
        session.preferences.isUnmountSdCard = true
        startInstallation()
    }

    fun onClickSetSeLinuxPermissive() {
        updateInstallationCard { it.copy(installationStep = InstallationStep.PROCESSING) }
        viewModelScope.launch {
            Shell.cmd("setenforce 0").exec()
            delay(5000)
            startInstallation()
        }
    }

    fun showDiscardSheet() = updateSheetState(SheetDisplayState.DISCARD_DSU)

    //
    // Userdata card
    //

    fun onCheckUserdataCard() =
        updateUserdataCard { it.copy(isSelected = !it.isSelected, text = "") }

    fun updateUserdataSize(input: String) {
        val selectedSize = FilenameUtils.getDigits(input)
        val sizeWithSuffix = FilenameUtils.appendToDigitsToString(input, "GB")
        Log.d(
            tag,
            "disabledStorageCheck: $disabledStorageCheck, selectedSize: $selectedSize, maximumAllowedForAllocation: $maximumAllowedForAllocation",
        )

        if (!disabledStorageCheck && selectedSize.isNotEmpty() && selectedSize.toInt() > maximumAllowedForAllocation) {
            val fixedSize =
                FilenameUtils.appendToDigitsToString("$maximumAllowedForAllocation", "GB")
            updateUserdataCard {
                it.copy(
                    text = fixedSize,
                    isError = true,
                    maximumAllowed = maximumAllowedForAllocation,
                )
            }
            viewModelScope.launch {
                delay(5000)
                updateUserdataCard { it.copy(isError = false) }
            }
            return
        }

        updateUserdataCard { it.copy(text = sizeWithSuffix) }
    }

    fun onCheckPreserveUserdata(checked: Boolean) {
        updateUserdataCard { it.copy(preserveSelected = checked) }
        session.preferences.preserveUserdata = checked
    }

    //
    // File selection
    //

    fun takeUriPermission(uri: Uri) {
        application.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        viewModelScope.launch {
            if (storageManager.arePermissionsGrantedToFolder(uri.toString())) {
                updateStringPref(AppPrefs.SAF_PATH, uri.toString()) { initialChecks() }
            }
        }
    }

    fun onFileSelectionResult(uri: Uri) {
        onFilesSelectionResult(listOf(uri))
    }

    fun onFilesSelectionResult(uris: List<Uri>) {
        if (uris.isEmpty()) return

        val supportedFiles = arrayListOf("gz", "xz", "img", "gzip")
        if (Build.VERSION.SDK_INT > 29) {
            supportedFiles.add("zip")
        }

        val validFiles = mutableListOf<SelectedImageFile>()
        for (uri in uris) {
            val filename = FilenameUtils.queryName(application.contentResolver, uri)
            val extension = filename.substringAfterLast(".", "").lowercase()
            if (!supportedFiles.contains(extension)) {
                Log.d(tag, "Unsupported extension: $extension for file: $filename")
                continue
            }
            val partitionName = FilenameUtils.extractPartitionName(filename)
            val fileSize = storageManager.getFilesizeFromUri(uri)
            validFiles.add(
                SelectedImageFile(
                    uri = uri,
                    name = filename,
                    partitionName = partitionName,
                    fileSize = fileSize,
                ),
            )
        }

        if (validFiles.isEmpty()) {
            viewModelScope.launch {
                updateInstallationCard { it.copy(isError = true, isTextFieldEnabled = false) }
                delay(2000)
                updateInstallationCard { it.copy(isError = false, isTextFieldEnabled = true) }
            }
            return
        }

        session.userSelection.selectedFiles = validFiles
        session.userSelection.selectedFileUri = validFiles.first().uri
        if (validFiles.size == 1) {
            session.userSelection.selectedFileName = validFiles.first().name
            updateInstallationCard {
                it.copy(
                    text = validFiles.first().name,
                    isTextFieldEnabled = false,
                    isInstallable = true,
                    selectedFiles = validFiles,
                )
            }
        } else {
            val summary = "${validFiles.size} images: " + validFiles.joinToString(", ") { it.name }
            session.userSelection.selectedFileName = summary
            updateInstallationCard {
                it.copy(
                    text = "${validFiles.size} images (${validFiles.joinToString(", ") { it.partitionName }})",
                    isTextFieldEnabled = false,
                    isInstallable = true,
                    selectedFiles = validFiles,
                )
            }
        }
    }

    //
    // Read logs permission warning
    //

    fun grantReadLogs() {
        updateAdditionalCardState(AdditionalCardState.GRANTING_READ_LOGS_PERMISSION)
        val intent = Intent()
        intent.setClassName(
            BuildConfig.APPLICATION_ID,
            "${BuildConfig.APPLICATION_ID}.MainActivity",
        )
        intent.flags += Intent.FLAG_ACTIVITY_NEW_TASK
        PrivilegedProvider.run {
            grantPermission("android.permission.READ_LOGS")
            if (Build.VERSION.SDK_INT <= 30) {
                forceStopPackage(BuildConfig.APPLICATION_ID)
            }
            startActivity(intent)
        }
    }

    fun refuseReadLogs() {
        checkReadLogsPermission = false
        Log.d(tag, "checkReadLogsPermission: $checkReadLogsPermission")
        initialChecks()
    }

    fun showLogsWarning() {
        updateSheetState(SheetDisplayState.VIEW_LOGS)
    }

    //
    // Progress tracking
    //

    private fun onRootInstallationSuccess() {
        updateInstallationCard { it.copy(installationStep = InstallationStep.INSTALL_SUCCESS_REBOOT_DYN_OS) }
        liveInstallationManager.notifySuccess(
            canReboot = true,
            title = application.getString(R.string.notification_completed_title),
            text = application.getString(R.string.installation_finished),
        )
    }

    private fun onInstallationSuccess() {
        updateInstallationCard { it.copy(installationStep = InstallationStep.INSTALL_SUCCESS) }
        liveInstallationManager.notifySuccess(
            canReboot = session.isRoot(),
            title = application.getString(R.string.notification_completed_title),
            text = application.getString(R.string.installation_finished_rootless),
        )
    }

    private fun onLogLineReceived() {
        _uiState.update { it.copy(installationLogs = logger!!.logs) }
    }

    private fun onStepUpdate(step: InstallationStep) {
        updateInstallationCard { it.copy(installationStep = step) }
        val stepString = getStepDescription(step)
        val isIndeterminate = step == InstallationStep.PROCESSING ||
            step == InstallationStep.PROCESSING_LOG_READABLE ||
            step == InstallationStep.WAITING_USER_CONFIRMATION
        liveInstallationManager.updateStep(
            step = step,
            stepText = stepString,
            isIndeterminate = isIndeterminate,
        )
    }

    private fun onPreparationProgressUpdate(progress: Float) {
        updateInstallationCard { it.copy(installationProgress = progress) }
        liveInstallationManager.updateProgress(
            progress = progress,
            partition = "",
            stepText = getStepDescription(uiState.value.installationCard.installationStep),
        )
    }

    private fun onInstallationError(error: InstallationStep, errorContent: String) {
        val finalStep = if (error == InstallationStep.ERROR_SELINUX && !session.isRoot()) {
            InstallationStep.ERROR_SELINUX_ROOTLESS
        } else {
            error
        }
        updateInstallationCard {
            it.copy(installationStep = finalStep, errorText = errorContent)
        }
        val errorDescription = getErrorDescription(finalStep, errorContent)
        liveInstallationManager.notifyError(
            step = finalStep,
            errorContent = errorDescription,
            title = application.getString(R.string.notification_failed_title),
        )
    }

    private fun onInstallationProgressUpdate(progress: Float, partition: String) {
        updateInstallationCard {
            it.copy(currentPartitionText = partition, installationProgress = progress)
        }
        liveInstallationManager.updateProgress(
            progress = progress,
            partition = partition,
            stepText = application.getString(R.string.installing_partition, partition),
        )
    }

    private fun onCreatePartition(partition: String) {
        updateInstallationCard {
            it.copy(
                installationStep = InstallationStep.CREATING_PARTITION,
                currentPartitionText = partition,
            )
        }
        liveInstallationManager.updateStep(
            step = InstallationStep.CREATING_PARTITION,
            stepText = application.getString(R.string.creating_partition, partition),
            partition = partition,
            isIndeterminate = true,
        )
    }

    private fun getStepDescription(step: InstallationStep): String {
        return when (step) {
            InstallationStep.PROCESSING -> application.getString(R.string.processing)
            InstallationStep.COPYING_FILE -> application.getString(R.string.copying_file)
            InstallationStep.DECOMPRESSING_XZ -> application.getString(R.string.decompressing_xz)
            InstallationStep.COMPRESSING_TO_GZ -> application.getString(R.string.compressing_to_gz)
            InstallationStep.DECOMPRESSING_GZIP, InstallationStep.EXTRACTING_FILE ->
                application.getString(R.string.extracting_file)
            InstallationStep.DISCARD_CURRENT_GSI -> application.getString(R.string.discarding)
            InstallationStep.WAITING_USER_CONFIRMATION -> application.getString(R.string.installation_prompt)
            InstallationStep.PROCESSING_LOG_READABLE -> application.getString(R.string.installing)
            InstallationStep.INSTALLING, InstallationStep.INSTALLING_ROOTED ->
                application.getString(R.string.installing)
            InstallationStep.CREATING_PARTITION -> application.getString(R.string.processing)
            InstallationStep.REQUIRES_ADB_CMD_TO_CONTINUE ->
                application.getString(R.string.require_adb_cmd_to_continue)
            InstallationStep.INSTALL_SUCCESS ->
                application.getString(R.string.installation_finished_rootless)
            InstallationStep.INSTALL_SUCCESS_REBOOT_DYN_OS ->
                application.getString(R.string.installation_finished)
            else -> ""
        }
    }

    private fun getErrorDescription(step: InstallationStep, errorText: String): String {
        return when (step) {
            InstallationStep.ERROR ->
                application.getString(R.string.unknown_error, errorText)
            InstallationStep.ERROR_CANCELED ->
                application.getString(R.string.installation_canceled)
            InstallationStep.ERROR_REQUIRES_DISCARD_DSU ->
                application.getString(R.string.discard_dsu_otg)
            InstallationStep.ERROR_ALREADY_RUNNING_DYN_OS ->
                application.getString(R.string.already_running_dsu)
            InstallationStep.ERROR_CREATE_PARTITION ->
                application.getString(R.string.failed_create_partition, errorText)
            InstallationStep.ERROR_EXTERNAL_SDCARD_ALLOC ->
                application.getString(R.string.allocation_error)
            InstallationStep.ERROR_NO_AVAIL_STORAGE ->
                application.getString(R.string.storage_error)
            InstallationStep.ERROR_F2FS_WRONG_PATH ->
                application.getString(R.string.fs_features_error)
            InstallationStep.ERROR_EXTENTS ->
                application.getString(R.string.extents_error_title)
            InstallationStep.ERROR_SELINUX, InstallationStep.ERROR_SELINUX_ROOTLESS ->
                application.getString(R.string.selinux_error)
            else -> errorText.ifEmpty { application.getString(R.string.unknown_error) }
        }
    }
}
