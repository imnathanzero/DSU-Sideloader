package git.imnathanzero.sysphere

import git.imnathanzero.sysphere.installer.InstallationLiveUpdateNotifier
import git.imnathanzero.sysphere.installer.LiveInstallationState
import git.imnathanzero.sysphere.installer.LiveNotificationActionReceiver
import git.imnathanzero.sysphere.preparation.InstallationStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallationNotifierUnitTest {

    @Test
    fun testNotificationChannelsAndIdentifiers() {
        assertEquals("dsu_live_installation", InstallationLiveUpdateNotifier.CHANNEL_INSTALLATION)
        assertEquals("dsu_alerts", InstallationLiveUpdateNotifier.CHANNEL_ALERTS)
        assertEquals(2001, InstallationLiveUpdateNotifier.NOTIFICATION_ID)
        assertEquals(2002, InstallationLiveUpdateNotifier.ALERT_NOTIFICATION_ID)
        assertEquals(
            "git.imnathanzero.sysphere.ACTION_CANCEL_INSTALLATION",
            InstallationLiveUpdateNotifier.ACTION_CANCEL_INSTALLATION,
        )
        assertEquals(
            "git.imnathanzero.sysphere.ACTION_REBOOT_DSU",
            InstallationLiveUpdateNotifier.ACTION_REBOOT_DSU,
        )
    }

    @Test
    fun testLiveInstallationStateDefaultValues() {
        val state = LiveInstallationState()
        assertFalse(state.isInstalling)
        assertEquals(InstallationStep.NOT_INSTALLING, state.step)
        assertEquals(0F, state.progress, 0.001F)
        assertFalse(state.isIndeterminate)
        assertEquals("", state.title)
        assertEquals("", state.stepText)
        assertEquals("", state.partition)
        assertFalse(state.canReboot)
        assertFalse(state.isSuccess)
        assertFalse(state.isError)
        assertEquals("", state.errorText)
    }

    @Test
    fun testLiveInstallationStateSuccessTransition() {
        val installingState = LiveInstallationState(
            isInstalling = true,
            step = InstallationStep.INSTALLING,
            progress = 0.85F,
            partition = "system",
        )
        assertTrue(installingState.isInstalling)
        assertEquals(0.85F, installingState.progress, 0.001F)
        assertEquals("system", installingState.partition)

        val successState = installingState.copy(
            isInstalling = false,
            isSuccess = true,
            canReboot = true,
            progress = 1.0F,
            title = "Installation Complete",
            stepText = "Reboot to dynamic system",
        )

        assertFalse(successState.isInstalling)
        assertTrue(successState.isSuccess)
        assertTrue(successState.canReboot)
        assertEquals(1.0F, successState.progress, 0.001F)
        assertEquals("Installation Complete", successState.title)
    }

    @Test
    fun testLiveInstallationStateErrorTransition() {
        val errorState = LiveInstallationState(
            isInstalling = false,
            isError = true,
            step = InstallationStep.ERROR_CREATE_PARTITION,
            errorText = "Failed to allocate space for system",
            title = "Installation Failed",
        )

        assertFalse(errorState.isInstalling)
        assertTrue(errorState.isError)
        assertEquals(InstallationStep.ERROR_CREATE_PARTITION, errorState.step)
        assertEquals("Failed to allocate space for system", errorState.errorText)
    }

    @Test
    fun testReceiverConstantsMatchNotifier() {
        assertEquals(
            InstallationLiveUpdateNotifier.ACTION_CANCEL_INSTALLATION,
            LiveNotificationActionReceiver.ACTION_CANCEL_INSTALLATION,
        )
        assertEquals(
            InstallationLiveUpdateNotifier.ACTION_REBOOT_DSU,
            LiveNotificationActionReceiver.ACTION_REBOOT_DSU,
        )
    }
}
