# Riot Account Manager - Developer Documentation

[![GitHub](https://img.shields.io/badge/GitHub-Repository-blue?style=flat-square&logo=github)](https://github.com/DangNghia17/ChangeLogin_Account_RiotClient)

**Repository:** [https://github.com/DangNghia17/ChangeLogin_Account_RiotClient](https://github.com/DangNghia17/ChangeLogin_Account_RiotClient)

## Overview

Riot Account Manager is a Java Swing desktop application for managing and auto-logging into multiple Riot Games accounts. The application uses Java 11+, Swing UI, and AES-256 encryption to protect user data.

## Architecture

### Project Structure

```
src/main/java/com/riotaccountmanager/
├── MainForm.java              # Main form, manages UI and user interactions
├── Account.java               # Model class representing an account
├── AccountManager.java        # Manages account list, save/read from file
├── AccountDialog.java         # Dialog for adding/editing accounts
├── ConfigManager.java         # Manages configuration (Riot Client path)
├── EncryptionHelper.java      # Encrypts/decrypts data using AES-256
├── AutoLoginHelper.java       # Automates login to Riot Client
├── WindowHelper.java          # Helper for Windows API interactions
├── LanguageManager.java       # Manages multi-language support (VI/EN)
├── UIHelper.java              # Helper for UI components
├── UITheme.java               # Theme and colors
├── GradientPanel.java         # Panel with gradient background
├── WelcomeDialog.java         # Welcome dialog
└── AboutDialog.java           # About dialog
```

### Main Components

#### 1. MainForm
- Main application form
- Manages account list (JTable)
- Handles login, add/edit/delete account events
- Manages Riot Client path configuration
- Displays Riot Client status

#### 2. AccountManager
- Manages account list
- Saves/reads from `accounts.dat` file (encrypted)
- Uses JSON for data serialization
- Automatically encrypts/decrypts when saving/reading

#### 3. EncryptionHelper
- AES-256 encryption with key from Machine ID
- Uses Windows Registry to get Machine GUID
- Encrypts/decrypts data before saving/reading file

#### 4. AutoLoginHelper
- Checks if Riot Client is running
- Automatically restores window from system tray
- Calculates accurate click position based on window bounds
- Simulates keyboard/mouse actions to login

#### 5. LanguageManager
- Manages multi-language support (VI/EN)
- Loads resource bundles from `messages_vi.properties` and `messages_en.properties`
- Saves language preference to Java Preferences API
- Supports UTF-8 encoding

## Technologies Used

- **Java 11+**: Main programming language
- **Java Swing**: GUI framework
- **JSON (org.json)**: Serialize account data
- **AES-256**: Encrypt sensitive data
- **Windows API**: Interact with Riot Client (Robot, Process)
- **Java Preferences API**: Save user configuration
- **ResourceBundle**: Manage multi-language support

## Data Structure

### Account
```java
public class Account {
    private String username;
    private String password;
    private String region;
    private String note;
}
```

### Storage Files
- **accounts.dat**: File containing encrypted account list (JSON)
- **config.json**: Configuration file (Riot Client path)
- **Location**: `%LOCALAPPDATA%\RiotAccountManager\`

## Build and Run

### Requirements
- Java JDK 11+
- Maven (optional) or javac directly
- Windows 10/11

### Build from source
```bash
# Compile
javac -encoding UTF-8 -d build/classes -cp "lib/json-20231013.jar" src/main/java/com/riotaccountmanager/*.java

# Copy resources
cp -r src/main/resources/* build/classes/

# Create JAR
jar cfm RiotAccountManager.jar MANIFEST.MF -C build/classes .
```

### Run application
```bash
java -jar RiotAccountManager.jar
```

### Building Executable (.exe)

The application uses Launch4j to create a Windows executable. After building the JAR file, use Launch4j to create the `.exe`:

1. **Requirements:**
   - Launch4j installed
   - Compiled JAR file
   - Custom JRE runtime (created using `jlink`)

2. **File Structure for Distribution:**
   ```
   RiotAccountManager/
   ├── RiotAccountManager.exe    # Main executable (created by Launch4j)
   ├── RiotAccountManager.jar    # JAR file (required by exe)
   └── runtime/                  # Custom JRE folder (required by exe)
       ├── bin/
       ├── lib/
       └── ...
   ```

3. **Important Notes:**
   - The `.exe` file requires both the JAR file and `runtime/` folder in the same directory
   - All three components (exe, jar, runtime) must be included when distributing
   - The `runtime/` folder contains a custom JRE created with `jlink` (smaller than full JRE)
   - Launch4j configuration is in `dist/launch4j-config.xml`

## Security

### Encryption
- Uses AES-256 with key from Machine ID
- Key is generated from Windows Machine GUID (SHA-256 hash)
- Data is encrypted before saving to file
- Can only be decrypted on the same computer

### Safety
- NO data sent to server
- NO internet connection
- Data stored locally only
- Open source, can be verified

## Compatibility

### Riot Client
- Supports latest Riot Client
- Compatible with Vanguard (anti-cheat)
- Uses standard Windows API only
- Does not interfere with process or memory

### Operating System
- Windows 10
- Windows 11
- Not supported on Linux/Mac (uses Windows API)

## Development

### Adding New Features
1. Create new branch from `main`
2. Develop feature
3. Test thoroughly
4. Create Pull Request

### Coding Standards
- Use Java naming conventions
- Code without comments (removed for clean code)
- Use UTF-8 encoding
- Format code according to Java standards

## License

MIT License - See LICENSE file for more details.

## Contributing

All contributions are welcome! Please create an [Issue](https://github.com/DangNghia17/ChangeLogin_Account_RiotClient/issues) or [Pull Request](https://github.com/DangNghia17/ChangeLogin_Account_RiotClient/pulls) on GitHub.

**Repository:** [https://github.com/DangNghia17/ChangeLogin_Account_RiotClient](https://github.com/DangNghia17/ChangeLogin_Account_RiotClient)

