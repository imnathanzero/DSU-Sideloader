package me.nathan.dsusideloader.ui.screen.home

import me.nathan.dsusideloader.model.SelectedImageFile
import me.nathan.dsusideloader.preparation.InstallationStep

data class InstallationCardState(
    val installationStep: InstallationStep = InstallationStep.NOT_INSTALLING,
    val isTextFieldEnabled: Boolean = true,
    val isInstallable: Boolean = false,
    val isError: Boolean = false,
    val text: String = "",
    val errorText: String = "",
    val isProgressBarIndeterminate: Boolean = false,
    val installationProgress: Float = 0F,
    val currentPartitionText: String = "",
    val selectedFiles: List<SelectedImageFile> = emptyList(),
)

data class UserDataCardState(
    val isSelected: Boolean = false,
    val isError: Boolean = false,
    val text: String = "",
    val maximumAllowed: Int = 0,
    val preserveSelected: Boolean = false,
)

enum class AdditionalCardState {
    NONE,
    SETUP_STORAGE,
    UNAVAIABLE_STORAGE,
    NO_DYNAMIC_PARTITIONS,
    MISSING_READ_LOGS_PERMISSION,
    GRANTING_READ_LOGS_PERMISSION,
    BOOTLOADER_UNLOCKED_WARNING,
}

enum class SheetDisplayState {
    NONE,
    CONFIRM_INSTALLATION,
    CANCEL_INSTALLATION,
    DISCARD_DSU,
    VIEW_LOGS,
}

data class HomeUiState(
    val installationCard: InstallationCardState = InstallationCardState(),
    val userDataCard: UserDataCardState = UserDataCardState(),
    val additionalCard: AdditionalCardState = AdditionalCardState.NONE,
    val sheetDisplay: SheetDisplayState = SheetDisplayState.NONE,
    val installationLogs: String = "",
    val passedInitialChecks: Boolean = false,
    val shouldKeepScreenOn: Boolean = false,
    val isDsuInstalled: Boolean = false,
) {
    fun isInstalling(): Boolean {
        return installationCard.installationStep != InstallationStep.NOT_INSTALLING &&
            installationCard.installationStep != InstallationStep.DSU_ALREADY_INSTALLED
    }
}
