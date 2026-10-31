# GStore

**Android Apps Store for AOSP Devices Without Built-in GMS**

GStore is an open-source Android app store designed for non-root AOSP devices that do not include Google Mobile Services (GMS). It helps make Android apps work in environments where Google Play Services is unavailable by providing automatic microG compatibility patching.

[Download Latest Release](https://github.com/GStore0/GStore/releases) · [Report an Issue](https://github.com/GStore0/GStore/issues) · [View Source Code](https://github.com/GStore0/GStore)

> **GStore is a fork of [Aurora Store](https://gitlab.com/AuroraOSS/AuroraStore)** (GitHub mirror: [whyorean/AuroraStore](https://github.com/whyorean/AuroraStore)),
> Copyright Aurora OSS, The Calyx Institute and contributors, licensed under **GPL-3.0-or-later**.
> It has been modified and renamed; it is not affiliated with or endorsed by Aurora OSS.
> See [NOTICE](NOTICE) for the list of modifications and [LICENSE](LICENSE) for the licence text.

## Join Our Telegram Community

Stay updated with the latest news, releases, and announcements. Have questions or need help? Join the community!

- **[Telegram Channel](https://t.me/GStore000)** — News, updates, and announcements.
- **[Telegram Group](https://t.me/GSrore000_chat)** — Questions, discussions, and community support.

## Overview

Many Android devices use an AOSP-based operating system without Google Mobile Services. Apps that depend on Google Play Services may fail to launch or may have limited functionality on these devices.

GStore aims to improve compatibility by combining an open-source app store with automatic compatibility modifications and a compatible microG implementation.

GStore is designed for:

- Non-root AOSP-based Android devices.
- Devices without built-in Google Mobile Services.
- Custom Android systems and Android head units.
- Users who want to install Android apps without modifying the system partition.
- Devices using a compatible microG implementation.

The project was initially developed and tested on BYD DiLink Android head units, but it is not limited to BYD vehicles.

## Features

- **App discovery and downloads** — Search for and download Android applications through the store.
- **Automatic microG compatibility patching** — Modify supported applications to improve compatibility with microG.
- **Rootless operation** — Perform supported patching and installation workflows without requiring root access.
- **microG compatibility detection** — Detect whether a compatible microG environment is available.
- **Guided setup** — Prompt users to install the required microG components when necessary.
- **Native library patching** — Support compatibility modifications involving native libraries.
- **Device compatibility support** — Help applications run in environments without built-in GMS.

Compatibility modifications are applied where supported. Not every application can be made compatible with microG.

## How It Works

GStore combines an app store with a compatibility layer intended for AOSP devices without built-in GMS.

1. Browse or search for an application.
2. Download the application package.
3. Check the availability of the required microG components.
4. Apply supported compatibility modifications when needed.
5. Install the resulting application.
6. Launch the application and verify its functionality.

The exact workflow depends on the application, device, Android version, and available installation permissions.

GStore does not replace the Android operating system or automatically provide every Google service. It helps bridge compatibility gaps where supported.

## Getting Started

### Requirements

- An Android device running an AOSP-based operating system.
- No built-in Google Mobile Services required.
- Network access to download applications and required components.
- Permission to install applications from the selected source.
- A compatible microG implementation for applications that require Google services.

Root access is not required for the supported installation and patching workflow.

### Installation

1. Download the latest release from the [GStore Releases page](https://github.com/GStore0/GStore/releases).
2. Install the GStore APK on your Android device.
3. Open GStore and complete the initial setup.
4. Configure your preferred supported account or download method.
5. Search for an application and start the download.
6. If prompted, install the compatible microG bundle supplied for GStore.
7. Allow the required background-start or autostart permissions for the microG components if your device provides these settings.
8. Install and launch the downloaded application.

**Important:** Use the microG components compatible with GStore. Installing multiple conflicting implementations of Google Play Services or microG can cause unexpected behavior. If another implementation is already installed, resolve any package conflicts before proceeding.

On customized Android systems, additional manufacturer-specific settings may be necessary to keep background services running.

## microG Compatibility

GStore is intended to support applications in environments that do not have the original Google Mobile Services framework.

Depending on the application, compatibility modifications may address issues involving:

- Google Play Services package detection.
- Package signatures and certificate checks.
- Installer-source verification.
- Applications that expect specific GMS metadata.
- Native libraries requiring supported compatibility handling.

These modifications do not reproduce the complete functionality of Google Play Services.

Some applications require additional services, proprietary components, device certification, Play Integrity, DRM, or server-side verification that cannot be resolved by patching alone.

### Compatibility Notes

- Use the microG implementation supplied or recommended by the project.
- Keep the compatible microG components installed and updated.
- Enable autostart or background execution where required.
- Reinstall an application after changing its compatibility configuration when necessary.
- Test critical application features on your own device.

## Tested Applications

The following applications have been tested or reported to work in the project's development environment. Functionality may vary by version and device.

| Application | Compatibility notes |
| --- | --- |
| Google Maps | Basic functionality may work |
| Google Search | Depends on the installed version and services |
| Google Gemini | Depends on account, device, and service compatibility |
| Google News | Depends on the installed version |
| Gmail | Login and synchronization may vary |
| YouTube | Some versions may require additional patching |
| YouTube Music | Some versions may require additional patching |

This list is informational, not a guarantee of compatibility on every AOSP device.

Applications may stop working after updates, even if earlier versions worked correctly.

## Known Limitations

GStore cannot guarantee that every application will work on every device.

Potential limitations include:

- Play Integrity or device certification requirements.
- Applications that require proprietary Google services.
- Native code that performs its own service or signature verification.
- DRM-protected content and Widevine requirements.
- Google account authentication restrictions.
- Push notifications and background execution limitations.
- Missing Android framework features or manufacturer-specific services.
- Applications that require Android Automotive OS or other specialized system APIs.

For example, applications designed specifically for Android Automotive OS may require car services that are not present on a standard AOSP installation.

Compatibility depends on the application, Android version, device configuration, and microG implementation.

## Privacy and Security

GStore is an open-source project. Users can inspect its source code and report potential security issues through the repository.

Please keep the following in mind:

- Applications downloaded through the store are developed and maintained by their respective publishers.
- Compatibility patching may change application behavior.
- Installing APK files from any source carries security risks.
- Only install applications you trust.
- Review application permissions before granting access.
- Disable optional crash reporting if you do not want diagnostic information sent to the project.
- Do not assume that an application is secure or fully functional simply because it installs successfully.

GStore does not guarantee the security, availability, or continued compatibility of third-party applications.

## Troubleshooting

### An application will not launch

1. Confirm that the required microG components are installed.
2. Check whether another Google Play Services or microG implementation conflicts with the installed components.
3. Verify that background execution is allowed.
4. Reinstall the application using GStore.
5. Check the application logs and existing [GitHub issues](https://github.com/GStore0/GStore/issues).

### An application crashes after patching

The application may use unsupported APIs, native verification, DRM, or other services that cannot be handled by the current compatibility patches.

Check existing issues before reporting the problem.

### Google login does not work

Account authentication depends on the application, microG configuration, device environment, and Google's current requirements. Successful installation does not guarantee successful authentication.

### An application stops working after an update

A new version may introduce additional dependencies or change its verification behavior. Try to identify the affected application version and provide a crash report if optional crash reporting is enabled.

## Reporting Issues

If you encounter a problem, open an issue in the repository:

[GitHub Issues](https://github.com/GStore0/GStore/issues)

Include the following information when possible:

- Android version.
- Device model and system version.
- GStore version.
- Application name and version.
- Whether microG is installed and working.
- Steps to reproduce the problem.
- Relevant logs or crash traces.

Remove personal information, authentication tokens, and other sensitive data before sharing logs.

Please search existing issues before creating a new report.

## Project Status

GStore is an evolving open-source project. Features, compatibility patches, and supported application versions may change as development continues.

Contributions, bug reports, testing, and compatibility feedback are welcome.

## Credits

GStore builds upon the work of existing open-source projects, including:

- [Aurora Store](https://github.com/whyorean/AuroraStore) — The upstream app store project.
- [ARSCLib](https://github.com/REAndroid/ARSCLib) — Android resource manipulation library.
- [AndroidHiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass) — Access to restricted Android APIs.
- [microG](https://github.com/microg) — Open-source implementations of selected Google services.

See the project's source code, license files, and third-party notices for additional attribution and licensing information.

## License

GStore is based on Aurora Store and incorporates third-party open-source components.

Refer to the repository's [LICENSE](https://github.com/GStore0/GStore/blob/master/LICENSE), [LICENSES directory](https://github.com/GStore0/GStore/tree/master/LICENSES), and applicable third-party licenses for the terms governing the code and included components.

## Disclaimer

GStore is an independent, community-developed project. It is not affiliated with, endorsed by, or sponsored by Google, Google Play, microG, or the developers of applications downloaded through the store.

GStore does not own or guarantee the availability of third-party applications or services accessed through it.

All trademarks and application names belong to their respective owners.

Use the software at your own risk. The developers provide no guarantee that any particular application, service, or compatibility modification will work on your device.