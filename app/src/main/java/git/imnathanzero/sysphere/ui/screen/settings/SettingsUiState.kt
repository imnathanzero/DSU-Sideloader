package git.imnathanzero.sysphere.ui.screen.settings

import git.imnathanzero.sysphere.preferences.AppPrefs
import git.imnathanzero.sysphere.ui.theme.ThemeMode

enum class DialogSheetState {
    NONE,
    BUILT_IN_INSTALLER,
    DISABLE_STORAGE_CHECK,
    THEME_SELECTION,
}

data class SettingsUiState(
    val preferences: HashMap<String, Boolean> = hashMapOf(
        AppPrefs.USE_BUILTIN_INSTALLER to false,
        AppPrefs.KEEP_SCREEN_ON to false,
        AppPrefs.UMOUNT_SD to false,
        AppPrefs.DISABLE_STORAGE_CHECK to false,
        AppPrefs.FULL_LOGCAT_LOGGING to false,
        AppPrefs.USE_MONET to true,
    ),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useMonet: Boolean = true,
    val dialogSheetDisplay: DialogSheetState = DialogSheetState.NONE,
    val isRoot: Boolean = false,
    val isDevOptEnabled: Boolean = false,
)
