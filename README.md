# HabitHub

HabitHub is an Android habit-tracking application designed to help users create, manage, monitor and maintain daily habits.

## Project Purpose

The purpose of HabitHub is to provide users with a simple and convenient way to create habits, set goals and reminders, track progress and manage personal development activities in one application.

## Main Features

- User registration and login
- Firebase user authentication
- Google Sign-In integration
- Habit creation, editing and management
- Habit categories
- Daily, weekday and custom habit frequencies
- Custom day selection
- Daily habit goals and measurement units
- Reminder time selection
- Habit notes
- Habit progress tracking and statistics
- Habit achievements
- Profile management
- Application settings
- Habit reminders and notifications
- Firebase Cloud Messaging integration
- Firebase Firestore data storage

## Design Considerations

The application was designed with usability, simplicity and accessibility in mind. The interface separates authentication, habit management, progress tracking, achievements, profile information and settings into dedicated screens.

The habit creation process allows users to specify the habit name, category, frequency, goal, measurement unit, reminder time and notes. Custom frequency options allow users to select specific days of the week.

Input validation is included to help prevent incomplete or invalid habit information from being submitted.

## Technologies Used

- Kotlin
- Android Studio
- Android SDK
- Firebase Authentication
- Firebase Cloud Firestore
- Firebase Cloud Messaging
- Google Sign-In
- AndroidX
- Gradle
- GitHub
- GitHub Actions

## Testing

The project includes unit tests and Android instrumented tests.

### Unit Tests

Unit tests are located in:

`app/src/test`

These tests evaluate application logic independently of the Android framework.

### Android Instrumented Tests

Instrumented tests are located in:

`app/src/androidTest`

These tests run on an Android device or emulator and are used to evaluate Android-specific functionality.

### Running Tests

Run unit tests using:

```bash
./gradlew testDebugUnitTest
```

On Windows:

```cmd
gradlew.bat testDebugUnitTest
```

To run instrumented tests on a connected device or emulator:

```bash
./gradlew connectedAndroidTest
```

## Automated Testing – GitHub Actions

GitHub Actions is used to automate the Android application's build and unit-testing process.

The workflow configuration is located at:

`.github/workflows/android.yml`

The workflow is configured to run when changes are pushed to the `main` branch, when pull requests are created, and when manually triggered.

The automated process includes:

- Checking out the source code.
- Setting up Java and the Android SDK.
- Building the HabitHub Android application.
- Running the configured unit tests.

The project successfully completed a GitHub Actions workflow, confirming that the updated application could be built and its configured unit tests executed successfully.

## How to Run the Application

### Prerequisites

- Android Studio installed.
- Android SDK configured.
- An Android device or emulator.
- Internet connection for Gradle and Firebase dependencies.
- Firebase project configuration.

### Installation Steps

1. Clone or download the HabitHub repository.
2. Open the project in Android Studio.
3. Ensure the required Firebase configuration file is available.
4. Allow Android Studio to synchronise the Gradle project.
5. Connect an Android device or start an emulator.
6. Select the HabitHub application configuration.
7. Click **Run** in Android Studio.
8. Wait for the application to build and install.

## Project Structure

```text
HabitHub/
├── app/
│   └── src/
│       ├── main/              # Application source and resources
│       ├── test/              # Unit tests
│       └── androidTest/       # Instrumented tests
│
├── .github/
│   └── workflows/
│       └── android.yml        # GitHub Actions workflow
│
├── gradle/                    # Gradle wrapper configuration
├── build.gradle.kts           # Root Gradle configuration
├── gradle.properties         # Gradle properties
├── gradlew                    # Linux/macOS Gradle wrapper
├── gradlew.bat                # Windows Gradle wrapper
├── settings.gradle.kts        # Project settings
└── README.md                  # Project documentation
```

## Application Screenshots

Add screenshots demonstrating the following application screens:

- Login and registration
- Home screen
- Habit creation
- Habit management
- Progress and statistics
- Achievements
- Profile and settings

## Demo Video

A demonstration video showing the main HabitHub features and application functionality:

[Watch the HabitHub Demonstration Video](https://youtu.be/pxygbOgzB4E)

## Authors

Project completed by:

1. Zizinhle Masanabo – ST10438120
2. Kgaogelo Mokwena – ST10450091
3. Ratanang Mothibi – ST10446761
4. Ponkana Mokholwane – ST10453202
5. Onkatlile Tshele – ST10445128

## Release Notes

See `RELEASE_NOTES.md` for information about application features, improvements and updates.
