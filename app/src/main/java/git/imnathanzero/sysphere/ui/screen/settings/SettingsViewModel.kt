package git.imnathanzero.sysphere.ui.screen.settings

import android.app.Application
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import git.imnathanzero.sysphere.core.BaseViewModel
import git.imnathanzero.sysphere.model.Session
import git.imnathanzero.sysphere.preferences.AppPrefs
import git.imnathanzero.sysphere.ui.theme.ThemeMode
import git.imnathanzero.sysphere.util.OperationMode
import git.imnathanzero.sysphere.util.OperationModeUtils
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    override val dataStore: DataStore<Preferences>,
    private val session: Session,
    val application: Application,
) : BaseViewModel(dataStore) {

    private val tag = this.javaClass.simpleName

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun reloadPreferences() {
        uiState.value.preferences.forEach { entry ->
            viewModelScope.launch {
                val defaultVal = entry.key == AppPrefs.USE_MONET
                val isEnabled = readBoolPref(entry.key, defaultVal)
                togglePreference(entry.key, isEnabled)
            }
        }

        viewModelScope.launch {
            val themeModeStr = readStringPref(AppPrefs.THEME_MODE, "system")
            val themeMode = ThemeMode.fromString(themeModeStr)
            val useMonet = readBoolPref(AppPrefs.USE_MONET, true)
            _uiState.update {
                it.copy(
                    themeMode = themeMode,
                    useMonet = useMonet,
                )
            }
        }

        if (session.isRoot()) {
            _uiState.update { it.copy(isRoot = true) }
        }
    }

    init {
        reloadPreferences()
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            updateStringPref(AppPrefs.THEME_MODE, themeMode.name.lowercase()) {
                _uiState.update { it.copy(themeMode = themeMode) }
            }
        }
    }

    fun togglePreference(preference: String, value: Boolean) {
        viewModelScope.launch {
            updateBoolPref(preference, value) {
                _uiState.update {
                    val cloneMap = hashMapOf<String, Boolean>()
                    cloneMap.putAll(it.preferences)
                    cloneMap[preference] = value
                    if (preference == AppPrefs.USE_MONET) {
                        it.copy(preferences = cloneMap, useMonet = value)
                    } else {
                        it.copy(preferences = cloneMap)
                    }
                }
            }
        }
    }

    fun isAndroidQ(): Boolean = Build.VERSION.SDK_INT == 29
    fun isMonetSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun updateSheetDisplay(sheet: DialogSheetState) {
        _uiState.update { it.copy(dialogSheetDisplay = sheet) }
    }

    fun checkOperationMode(): String {
        return OperationModeUtils.getOperationModeAsString(session.getOperationMode())
    }

    fun getOperationMode(): OperationMode {
        return session.getOperationMode()
    }

    fun checkDevOpt() {
        viewModelScope.launch {
            val isDevOptEnabled = readBoolPref(AppPrefs.DEVELOPER_OPTIONS)
            _uiState.update { it.copy(isDevOptEnabled = isDevOptEnabled) }
            if (isDevOptEnabled) {
                reloadPreferences()
            }
        }
    }
}
