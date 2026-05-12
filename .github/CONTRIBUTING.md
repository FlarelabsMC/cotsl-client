# Contributing to Crypt of the Second Lord
This guide is for those who are interested in contributing to Crypt of the Second Lord. If you are a user looking to test, take a look at the README for information on developer releases.

## Code of Conduct
Please read our [Code of Conduct](./CODE_OF_CONDUCT.md) to understand our standards when participating.

## Licensing
Code you may have contributed will be under your own **open-source** rights. No Code of Conduct violation will remove your rights to code you contributed. Such code may be modified or removed at any point in time during development if necessary.

## Code formatting
We are not necessarily strict with our code formatting rules, but we do ask that you try to have consistent styling with surrounding code.

Please document your code well enough for it to be readable by a fresh developer. Do not abbreviate variables, fields, methods, functions, or class names unless enough context is already given.

Use whitespace and brackets sparingly. More than 5 indentations may be too much. `if`, `for`, and `else` statements usually don't need brackets if they consist of only one line. Feel free to add brackets if it makes the code more readable.

Try to fit your code within the bounds of 150 characters per line. Otherwise, use line breaks.

## Setup and Building
To build from source, you will need:
- Java 25
- **IntelliJ IDEA**, **Eclipse**, or **Visual Studio Code** (with the [Qt Qml plugin](https://marketplace.visualstudio.com/items?itemName=TheQtCompany.qt-qml) for editing QML)
- [A Go installation](https://go.dev/)
- **Qt 6.11.0** and a verified Qt.io account.

If you don't see syntax highlighting for QML files in IntelliJ IDEA Community, you can use Visual Studio Code with the previously mentioned plugin.

### Setting up Qt
**If you're on Linux, KDE Plasma and similar desktop environments may have Qt installed by default.**

- **Windows:** First, download and install the Qt Online Installer from [Qt.io](https://www.qt.io/download). During installation, select the **Qt 6.11.0** component and the **MSVC 2022 64-bit** component.
- **macOS:** Download and install the [Qt Online Installer](https://www.qt.io/download). During installation, select the **Qt 6.11.0** component and the default macOS component.
- **Linux:** Install Qt 6.11.0 with your default package manager, and install the default GCC component.

### Environment variables
You need to set up your environment variables so the buildscript can find Qt. After setting these, restart your IDE and terminal.

#### `QTDIR`
The absolute path to your Qt installation depends on your platform and, of course, where you installed it. Common default paths are:
- **Windows:** `C:\Qt\6.11.0\msvc2022_64`
- **macOS:** `/Users/<user>/Qt/6.11.0/macos` (preferably add `export QTDIR="/Users/<user>/Qt/6.11.0/macos"` to your `~/.zshrc` or `~/.zprofile`)
- **Linux:** `/opt/Qt/6.11.0/gcc_64` (or wherever your package manager or Linux installation places it)

#### `QSB_PATH` (optional)
The path to the Qt Shader Baker (`qsb`) executable. If your `QTDIR` is set correctly, the buildscript can usually find this automatically. If it fails, set this explicitly:
- **Windows:** `C:\Qt\6.11.0\msvc2022_64\bin\qsb.exe`
- **macOS** `/Users/<user>/Qt/6.11.0/macos/bin/qsb`
- **Linux:** `/opt/Qt/6.11.0/gcc_64/bin/qsb`

### Building
To build the project, first, run the `./gradlew generateDialogueMetadata` task, and then run one of the Gradle tasks for your desired operating system:
- `./gradlew jar_linux_x64`
- `./gradlew jar_windows_x64`
- `./gradlew jar_macos`

Executables should only be built through the workflows.

**You can only build for the OS you're currently using. Except Linux. That works always.**<br>
If you want to build for Windows *and* macOS, currently, you will need to set up a virtual machine.

# Notes
- The client and dedicated server code are in the same JAR. You will still need a Qt account and installation to build the JAR yourself.
- You only need to generate dialogue metadata if you are modifying or adding dialogue.