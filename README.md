# Sysphere

> *Explore beyond your system.*

Sysphere (*pronounced Sis-feer*) is a modern, minimal, and intuitive Android application designed to install and manage Generic System Images (GSIs) through Android's native Dynamic System Updates (DSU) feature — completely isolated from your host partitions.

Originally created as **DSU Sideloader** by [VegaBobo](https://github.com/VegaBobo).

## Requirements
- Android 10 (API 29) or higher
- Unlocked Bootloader
- Device with Dynamic Partitions
- Compatible GSI image (`.img`, `.gz`, `.xz`, or DSU `.zip`)

Community GSIs: https://github.com/phhusson/treble_experimentations/wiki/Generic-System-Image-%28GSI%29-list

Google GSIs: https://developer.android.com/topic/generic-system-image/releases

*\*Ensure selected GSIs match your device architecture (ARM64) and VNDK requirements.*

## Downloads

Download the latest release from the [GitHub Releases](https://github.com/imnathanzero/DSU-Sideloader/releases).

## How to use Sysphere?
1. **Setup Storage**: Allow Sysphere access to a directory via Storage Access Framework (SAF) for temporary decompression.
2. **Select GSI**: Pick a `.img`, `.gz`, `.xz`, or DSU `.zip` image.
3. **Configure Userdata**: Choose allocation size for the secondary system userdata.
4. **Install**: Tap Install and follow the guided wizard.
5. **Boot & Test**: Once finished, reboot directly into your Dynamic System.

## Operation Modes

Sysphere supports multiple operation modes, automatically selecting the most capable mode available on your device:

1. **ADB**: Default unprivileged mode; prepares images and provides exact commands for execution via PC or on-device local ADB tools (LADB, WebADB, Shizuku Rish).
2. **Shizuku**: Privilege escalation via Shizuku API; triggers installation without manual commands and enables live logcat diagnostics.
3. **Root**: Direct integration via `libsu`; supports the fast built-in installer (`IGsiService` / Ashmem) and direct one-click reboot/discard actions.
4. **System Mode**: Running as a privileged system application in `/system/priv-app`.
5. **System/Root Mode**: Combines system privilege and root execution for maximum compatibility across strict OEM SELinux policies.

¹ Requires READ_LOGS permission.
<br>
² Partital support on Android 10 and 11.
<br>
³ Android 13 requires "One-time log access".
<br>
⁴ Feature not supported on Android 10.
<br>
⁵ Experimental feature, built-in installer code is [here](https://github.com/VegaBobo/DSU-Sideloader/blob/master/app/src/main/java/vegabobo/dsusideloader/installer/root/DSUInstaller.kt).

#### Recomendations

- For non-rooted devices, Shizuku is a pretty nice operation mode, it support most features with no hassle, however, you need to install and setup [Shizuku](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api) app in your device.
- For rooted devices, Root operation mode is more than ok for most people.
- If you're having issues with DSU feature, go with System/Root.
- Rooted devices via Magisk, should be running Magisk v24 or higher, older versions may break DSU feature.
- We highly recommend using this app with Stock ROM, some Custom ROM builds may also work fine.

## Common questions

1. DSU installation finishes with no errors, but device doesn't boot into installed DSU, what should i do?
- It is likely that AVB is preventing device from booting installed images, try flashing disabled vbmeta, check [this](https://developer.android.com/topic/generic-system-image#flash-gsi) for more info.
2. Why isn't possible to set a high userdata value?
- The more storage you have free, the more you can use to be your userdata, some Android versions limit the maximum allowed for allocation (this limitation is 40%, and is not our app limitation, it is a thing from Android itself).
3. Why "Unmount SD" option exists?
- If available, DSU priorizes allocation in sdcard, but allocating in sdcard is not supported in some cases (it may depends on filesystem present on sd, and if the allocation in SD is allowed by OS itself), since allocating in SD may cause installation errors in some devices, that options is here to enforce allocation in device storage.
4. Why built-in installer requires root?
- Because it uses Android's internal DynamicSystem API, which requires "MANAGE_DYNAMIC_SYSTEM", which is a signature protection level, the convenient way to circumvent it, is by using root. shell (2000) has "INSTALL_DYNAMIC_SYSTEM", which is able to call DSU system-app (this one has "MANAGE_DYNAMIC_SYSTEM") to install images.
5. How about updates?
- Our app comes with a updater, you can check updates in "About" section.
6. Other question? problem?
- Feel free to start a issue, for troubleshooting, don't forget to send logs (logs can be obtained on installation phase, directly on app, when operation mode support installation diagnostics).

## About DSU
DSU (Dynamic System Updates), is a feature introduced on Android 10, that let developers boot GSIs without touching current system partition, this is done by creating new partitions to hold a GSI and a separated userdata, to boot on them when desired.

Unfortunelly, DSU depends on Dynamic Partitions (your device need to support, otherwise, won't work), and most GSIs requires unlocked bootloader to get them booting properly (since only OEM-Signed GSIs are allowed to boot on locked bootloader).

GSIs can be installed via DSU without root access, using ADB, running some commands, you can read more about installation process here: https://developer.android.com/topic/dsu

Once installation finishes, Android creates a persistent notification allowing you to boot into "Dynamic System" (GSI installed via DSU), and you can boot into installed GSI, without touching your system partition, or breaking the "real userdata" partition.

After booting Dynamic System, you can try and test whatever you want, when you need to switch back to device's original system image, everything you need to do, is just, a simple reboot!

When doing a long test, that may requires lots of reboots, this can be a pain, however, is possible to enable "sticky mode", that enforces dynamic system, instead of device's original system image, once tests are done, you can disable sticky mode and return to original system image.

That is basically a quickly explanation about DSU, a amazing feature, like a "dual-boot" solution, limited, however, very safe (since no read-only partition will be modified, and if GSI does not boot, just a simple reboot will return you to the original device's system image).

You can read more about DSU here: https://source.android.com/devices/tech/ota/dynamic-system-updates

## How to enable Sticky Mode?

Reboot to Dynamic System, and:
- use this command on adb: `adb shell gsi_tool enable`
- or from local adb shell:  `gsi_tool enable`
- or from local rooted shell (eg. Termux on rooted GSI):  `su -c 'gsi_tool enable'`

When sticky mode is enabled, device will always boot into dynamic system, instead of device's original system image.

To disable, use the same command, instead of `enable` , use `disable`

## Other

For translators, we now have a Crowdin, feel free send your translations:
<br>https://crowdin.com/translate/dsu-sideloader/<br>
App icon made by [WSTxda](https://github.com/WSTxda)
