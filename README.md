# CotSL Client
The client and dedicated server mod source code for Crypt of the Second Lord, a Minecraft mod(pack).

# Running
To run the mod, you will need to either download a JAR from the Releases page or build it from source.

I recommend using Prism Launcher.

- Install the latest NeoForge version for 26.1. Install the mod into the mods folder of that instance, as usual.
- Add the JAR as a Java agent in the JVM arguments, or through Prism. In Prism, you can do this by going to the instance, clicking the Versions tab, and on the right, click Add Agents and select the same mod JAR you just added to the mods folder. Otherwise, you can add the following JVM argument: `-javaagent:<jar path>.jar`
- Launch as normal. If you don't see a launcher, **please report it as an issue** and send your full log.

# Setup and Building
To build from source, you will need:
- Java 25
- **IntelliJ IDEA**, **Eclipse**, or **Visual Studio Code** (with the [Qt Qml plugin](https://marketplace.visualstudio.com/items?itemName=TheQtCompany.qt-qml) for editing QML)
- **Qt 6.10.0** and a verified Qt.io account.

If you don't see syntax highlighting for QML files in IntelliJ IDEA Community, you can use Visual Studio Code with the previously mentioned plugin.

## Setting up Qt
**If you're on Linux, KDE Plasma and similar desktop environments should have Qt installed by default. Otherwise, you can install it using your preferred package manager, or by following the next instructions.**

First, download and install the Qt Online Installer from [Qt.io](https://www.qt.io/download).
During installation, select the **Qt 6.10.0** component:
- **Windows:** Make sure to check the **MSVC 2022 64-bit** component.
- **macOS:** Install the default macOS component.
- **Linux:** Install the default desktop GCC component.

## Environment variables
You need to set up your environment variables so the buildscript can find Qt. After setting these, restart your IDE and terminal.

### `QTDIR`
The absolute path to your Qt installation depends on your platform and, of course, where you installed it. Common default paths are:
- **Windows:** `C:\Qt\6.10.0\msvc2022_64`
- **macOS:** `/Users/<user>/Qt/6.10.0/macos` (preferably add `export QTDIR="/Users/<user>/Qt/6.10.0/macos"` to your `~/.zshrc` or `~/.zprofile`)
- **Linux:** `/opt/Qt/6.10.0/gcc_64` (or wherever your package manager or Linux installation places it)

### `QSB_PATH` (optional)
The path to the Qt Shader Baker (`qsb`) executable. If your `QTDIR` is set correctly, the buildscript can usually find this automatically. If it fails, set this explicitly:
- **Windows:** `C:\Qt\6.10.0\msvc2022_64\bin\qsb.exe`
- **macOS** `/Users/<user>/Qt/6.10.0/macos/bin/qsb`
- **Linux:** `/opt/Qt/6.10.0/gcc_64/bin/qsb`

## Building
To build the project, first, run the `./gradlew generateDialogueMetadata` task, and then run one of the Gradle tasks for your desired operating system:
- `./gradlew jar_linux_x64`
- `./gradlew jar_windows_x64`
- `./gradlew jar_macos`

**You can only build for the OS you're currently using. Except Linux. That works always.**<br>
If you want to build for Windows *and* macOS, currently, you will need to set up a virtual machine.

# Notes
- The client and dedicated server code are in the same JAR. You will still need a Qt account and installation to build the JAR yourself.
- You only need to generate dialogue metadata if you are modifying or adding dialogue.