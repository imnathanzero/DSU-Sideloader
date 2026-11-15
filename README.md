# DSU Sideloader

A modernized Material 3 Expressive application designed to help users effortlessly install Generic System Images (GSIs) via Android's Dynamic System Updates (DSU) feature.

## Features & Highlights

- **Material 3 Expressive UI**: Crafted with modern Material 3 Expressive styling, dynamic color theming, high-contrast elevated surfaces, and fluid motion.
- **Bottom Navigation Bar & Fluid Pager**: Seamless bottom navigation featuring `InstallMobile` icon, full `HorizontalPager` swipe transitions, and a real-time sliding indicator.
- **Preserve Userdata**: Keep your existing guest user data intact when updating an already installed DSU instance! Toggleable directly from the Settings screen.
- **Live Alert Notifications**: Ongoing real-time notification shade updates showing exact partition and installation progress.
- **Device Information**: Quick diagnostic card in the About screen displaying your device model, Android version, security patch level, architecture, and dynamic partition compatibility.
- **Refined & Clean**: Removed legacy Magisk module packaging and redundant image size dialogs for a streamlined, foolproof experience.

## Requirements

- Android 10 or higher (API 29+)
- Unlocked bootloader
- Device with Dynamic Partitions support
- A GSI image you want to test (supported formats: `.gz`, `.xz`, `.img`, and DSU `.zip` packages)

### Recommended GSI Sources
- [phhusson Treble Community GSIs](https://github.com/phhusson/treble_experimentations/wiki/Generic-System-Image-%28GSI%29-list)
- [Official Google GSI Releases](https://developer.android.com/topic/generic-system-image/releases)

*Always make sure to pick a GSI matching your device's CPU architecture (arm64/arm) and VNDK requirements.*

## How to Use

1. **Install and Launch**: Open DSU Sideloader.
2. **Grant Storage Folder**: Choose a folder to store temporary extracted images during preparation.
3. **Select GSI File**: Tap the file picker card to select your GSI image (`.gz`, `.xz`, `.img`, or `.zip`).
4. **Configure Userdata**: Choose how many gigabytes to allocate for the guest system's `/data` partition.
5. **Preserve Userdata on Updates**: If updating an existing DSU installation, enable *Preserve Userdata* in the Settings tab to retain your apps and files!
6. **Tap Install**: The app will process the image, create partitions, and stream the installation with real-time live alert progress notifications.
7. **Boot into Dynamic System**: Once completed, reboot directly into your new system from the app (Root mode) or via the Android system notification!

## Operation Modes

DSU Sideloader automatically selects the best available execution mode:

1. **Root**:
   - Uses Android's internal `DynamicSystem` APIs via root shell.
   - Built-in installer applies images directly without system app redirection.
   - Discard, reboot, and status management directly from the app.
2. **Shizuku**:
   - Best experience for non-rooted devices.
   - Automatically starts the DSU verification activity without running manual ADB terminal commands.
   - Supports real-time logcat diagnostics.
3. **System**:
   - When running as a privileged system app (`priv-app`).
4. **ADB**:
   - Universal fallback for any device.
   - Prepares the image and generates a one-click shell command to execute via `adb shell`.

## Sticky Mode

By default, rebooting your phone returns you back to your primary stock OS. To make the Dynamic System persist across reboots ("Sticky Mode"):

```bash
# Via ADB shell:
adb shell gsi_tool enable

# Or from local root shell (e.g. Termux in the GSI):
su -c 'gsi_tool enable'
```

To disable sticky mode and return to your stock OS:
```bash
adb shell gsi_tool disable
```

## Credits

- **Original Author**: [VegaBobo](https://github.com/VegaBobo)
- **Fork Maintainer**: [Nathan (@imnathanzero)](https://github.com/imnathanzero)
- **UI & Icon Design**: [WSTxda](https://github.com/WSTxda)
- Open source community and contributors.
