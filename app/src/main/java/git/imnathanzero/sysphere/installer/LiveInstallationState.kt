package git.imnathanzero.sysphere.installer

import git.imnathanzero.sysphere.preparation.InstallationStep

data class LiveInstallationState(
    val isInstalling: Boolean = false,
    val step: InstallationStep = InstallationStep.NOT_INSTALLING,
    val progress: Float = 0F,
    val partition: String = "",
    val isIndeterminate: Boolean = false,
    val title: String = "",
    val stepText: String = "",
    val errorText: String = "",
    val isSuccess: Boolean = false,
    val canReboot: Boolean = false,
    val isError: Boolean = false,
)
