# scrcpy Android 13 Compatible

<img src="app/data/scrcpy.svg" width="128" height="128" alt="scrcpy" align="right" />

Ever wanted to use Virtual Display on SCRCPY, but your phone doesn't support automatic input routing?

Well, we made a version that is compatible with it.

![screenshot](assets/screenshot-debian-600.jpg)

## What is this?

This is a modified version of **scrcpy v4.1** focused on virtual-display input routing.

The goal is to make:

```text
--mouse=uhid
--keyboard=uhid
```

work correctly with a **virtual Android display** on Android versions where the standard implementation does not provide the required input routing.

## Why does this exist?

Android's input-routing APIs have changed between versions.

SCRCPY already supports virtual displays, but the UHID routing mechanism used by newer Android versions relies on APIs that are not available in the same form on older versions.

This project attempts to add the appropriate compatibility path instead of simply changing an Android version check.

## Current status

🧪 **Experimental**

This is currently a testing project.

The modification needs to be tested on real Android 13 and Android 14 devices before being considered stable.

The primary test device is:

```text
Samsung Galaxy A32 4G
Android 13
SCRCPY v4.1
UHID keyboard + mouse
```

The important test is not simply whether the virtual display starts.

The modification must actually route UHID input to the **virtual display**.

## Installation

### Windows

**[⬇️ Download Windows Android 13 Compatible](../../actions/workflows/release.yml)**

The Windows package is intended to work like the normal SCRCPY v4.1 Windows package: extract it and run SCRCPY from the extracted folder.

The package contains the normal Windows files, including:

```text
scrcpy.exe
scrcpy-noconsole.vbs
scrcpy-server
adb.exe
AdbWinApi.dll
AdbWinUsbApi.dll
open_a_terminal_here.bat
```

The Windows client (`scrcpy.exe`) remains the normal SCRCPY v4.1 client. The Android-side `scrcpy-server` is the component containing the modified compatibility code.

This means the package keeps the normal SCRCPY file layout while using the modified Android server.

### Install

1. Click the Windows download button above and choose the latest successful workflow artifact.
2. Download the Windows x64 ZIP if you are using a normal 64-bit Windows PC.
3. Extract the ZIP to a folder.
4. Enable USB debugging on the Android device.
5. Connect the device with USB.
6. Open a terminal in the extracted folder.
7. Run:

```text
scrcpy.exe --mouse=uhid --keyboard=uhid
```

The official SCRCPY Windows release is also distributed as a ZIP that is extracted before running, so this project follows the same model.

> **Important:** This project does not replace the Windows `scrcpy.exe` client with a different Android implementation. The Android compatibility change is in the bundled `scrcpy-server`.

### Linux

**[⬇️ Download Linux x86_64 Android 13 Compatible](../../actions/workflows/release.yml)**

The Linux x86_64 package is built by the project's automated release workflow, but it has **not been personally tested by the developer**.

If you have a Linux x86_64 system, please test it and report whether virtual-display UHID input works. Include your Linux distribution/version, Android device/version, and any relevant errors or logs.

### macOS

**[⬇️ Download macOS Android 13 Compatible](../../actions/workflows/release.yml)**

The project builds packages for both:

```text
macOS x86_64
macOS arm64 (Apple Silicon)
```

These packages have **not been personally tested by the developer**.

If you have a Mac, please test the appropriate package and report whether virtual-display UHID input works. Include your macOS version, Mac architecture, Android device/version, and any relevant errors or logs.

With your permission, useful Linux or macOS testing reports may be credited in the README with the tester's GitHub username.

## Testing

The primary development and testing platform is **Windows**.

Currently tested on:

```text
Windows
Samsung Galaxy A32 4G
Android 13
SCRCPY v4.1
```

Linux and macOS packages are built automatically, but they have not been personally tested by the developer.

**Linux and macOS testers are wanted.** If you test this project, please let us know whether it works or not. Include your operating system and version, Android device and version, whether virtual-display input worked, and any relevant errors or logs if possible.

With your permission, useful testing reports may be credited in the README with the tester's GitHub username.

## Example

The intended setup is:

```text
        Android Device
             │
      ┌──────┴──────┐
      │             │
Physical Display   Virtual Display
      │             │
      │             └── UHID input
      │
      └────────────── SCRCPY
                         │
                         ▼
                         PC
```

## Important

This is **not an official SCRCPY release**.

It is an independent modification based on SCRCPY v4.1.

The original SCRCPY project, its authors, copyright notices, and license remain acknowledged.

Android 15+ behavior is intended to remain compatible with the existing implementation. Compatibility with each Android version must be verified through testing.

## Hidden message

If you are reading the raw README instead of just looking at the rendered page, here's something for you:

### Hex

```text
49 20 6D 61 64 65 20 74 68 69 73 20 62 65 63 61 75 73 65 20 49 20 68 61 64 20 74 68 65 20 73 61 6D 65 20 70 72 6F 62 6C 65 6D 20 61 73 20 79 6F 75
```

Decode it if you want.

### Binary

```text
01010111 01101000 01100001 01110100 00100000 01100100 01101001 01100100 00100000 01111001 01101111 01110101 00100000 01100101 01100001 01110010 01101110 00100000 01100110 01110010 01101111 01101101 00100000 01110010 01100101 01100001 01100100 01101001 01101110 01100111 00100000 01110100 01101000 01101001 01110011 00111111
```

Decode it if you want. There is no second message immediately explaining what you just decoded. ;)
