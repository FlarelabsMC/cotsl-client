# CotSL Client
The client and dedicated server mod source code for Crypt of the Second Lord, a Minecraft mod(pack).

# Setup and Building
You will need Java 25, IntelliJ or Eclipse, Qt, and a verified Qt.io account to build from source.

### Setting up Qt
First, download and install Qt Community from [here](https://www.qt.io/download). During installation, make sure to select the corresponding Qt version for this build (e.g `6.11.0`), and make sure to select the MSVC 2022 64-bit component.

### Setting up the project
Next, clone the repository and open the project in your IDE. You will need to set up one of the environment variables as mentioned in the `build.gradle`:
- `QTDIR`: The path to your Qt installation (e.g., `C:/Qt/6.11.0/msvc2022_64`)
- `QSB_PATH`: The path to the `qsb` executable (e.g., `C:/Qt/6.11.0/msvc2022_64/bin/qsb.exe`)
Make sure to restart your IDE after setting the environment variable.

### Building
To build the project, first, run the `./gradlew generateDialogueMetadata` task, and then run one of the Gradle tasks for your desired operating system:
- `./gradlew jar_linux_x64`
- `./gradlew jar_windows_x64`
- `./gradlew jar_macos`

# Notes
- The client and dedicated server code are in the same JAR. You will still need a Qt account and installation to build the JAR yourself.