scrcpy Android 13 Compatible

<img src="app/data/scrcpy.svg" width="128" height="128" alt="scrcpy" align="right" />Ever wanted to use Virtual Display on SCRCPY, but your phone doesn't support automatic input routing?

Well, we made a version that is compatible with it.



What is this?

This is a modified version of scrcpy v4.1 focused on virtual-display input routing.

The goal is to make:

--mouse=uhid
--keyboard=uhid

work correctly with a virtual Android display on Android versions where the standard implementation does not provide the required input routing.

Why does this exist?

Android's input-routing APIs have changed between versions.

SCRCPY already supports virtual displays, but the UHID routing mechanism used by newer Android versions relies on APIs that are not available in the same form on older versions.

This project attempts to add the appropriate compatibility path instead of simply changing an Android version check.

Current status

🧪 Experimental

This is currently a testing project.

The modification needs to be tested on real Android 13 and Android 14 devices before being considered stable.

The primary test device is:

Samsung Galaxy A32 4G
Android 13
SCRCPY v4.1
UHID keyboard + mouse

The important test is not simply whether the virtual display starts.

The modification must actually route UHID input to the virtual display.


Important

This is not an official SCRCPY release.

It is an independent modification based on SCRCPY v4.1.

The original SCRCPY project, its authors, copyright notices, and license remain acknowledged.

Android 15+ behavior is intended to remain compatible with the existing implementation. Compatibility with each Android version must be verified through testing.

Hidden message

If you are reading the raw README instead of just looking at the rendered page, here's something for you:

Hex

49 20 6D 61 64 65 20 74 68 69 73 20 62 65 63 61 75 73 65 20 49 20 68 61 64 20 74 68 65 20 73 61 6D 65 20 70 72 6F 62 6C 65 6D 20 61 73 20 79 6F 75


Binary

01010111 01101000 01100001 01110100 00100000 01100100 01101001 01100100 00100000 01111001 01101111 01110101 00100000 01100101 01100001 01110010 01101110 00100000 01100110 01110010 01101111 01101101 00100000 01110010 01100101 01100001 01100100 01101001 01101110 01100111 00100000 01110100 01101000 01101001 01110011 00111111

Decode them if you want. There is no second message immediately explaining what you just decoded. ;)