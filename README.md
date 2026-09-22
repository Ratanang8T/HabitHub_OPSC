# HabitHub

HabitHub is an Android habit-tracking application designed to help users create, manage, monitor, and maintain daily habits.

## Project Purpose

The purpose of HabitHub is to provide users with a simple way to create habits, set goals and reminders, track progress, and manage their personal development activities in one application.

## Main Features

* User registration and login
* User authentication using Firebase
* Habit creation and editing
* Habit categories
* Daily, weekday, and custom habit frequencies
* Custom day selection
* Daily habit goals
* Reminder time selection
* Habit notes
* Habit progress tracking
* Habit statistics
* Habit achievements
* Profile management
* Application settings
* Habit reminders and notifications
* Firebase Firestore data storage

## Design Considerations

The application was designed with usability and simplicity in mind. The interface separates authentication, habit management, progress tracking, achievements, profile information, and settings into dedicated screens.

The habit creation process allows users to specify the habit name, category, frequency, goal, measurement unit, reminder time, and notes. Custom frequency options allow users to select specific days of the week.

The application also includes validation to prevent incomplete or invalid habit information from being submitted.

## Technologies Used

* Kotlin
* Android Studio
* Android SDK
* Firebase Authentication
* Firebase Firestore
* AndroidX
* Gradle
* GitHub
* GitHub Actions

## Testing

The project includes both unit tests and Android instrumented tests.

### Unit Tests

Unit tests are located in:

```text
app/src/test
```

These tests are designed to test application logic independently of the Android framework.

### Android Instrumented Tests

Android instrumented tests are located in:

```text
app/src/androidTest
```

These tests run on an Android device or emulator and are used to test Android-specific functionality and user interface behaviour.

The test suite includes tests covering areas such as:

* Application launch
* Authentication screens
* Habit creation interface
* User interface interactions
* Input validation

## Automated Testing

GitHub Actions is used to automate the project's build and testing process.

The workflow is located at:

```text
.github/workflows/android.yml
```

The workflow is configured to run automatically when changes are pushed to the `main` branch or when a pull request is created.

The automated workflow builds the Android project and runs the configured unit tests to help identify build or test failures before changes are merged.

## How to Run the Application

### Prerequisites

Before running HabitHub, make sure you have:

* Android Studio installed
* Android SDK configured
* An Android device or emulator
* A working internet connection for Gradle and Firebase dependencies

### Steps

1. Clone or download the HabitHub repository.
2. Open the project in Android Studio.
3. Allow Android Studio to synchronise the Gradle project.
4. Connect an Android device or start an Android emulator.
5. Select the HabitHub application configuration.
6. Click **Run** in Android Studio.
7. Wait for the application to build and install on the selected device or emulator.

## Running Tests

### Run Unit Tests

Unit tests can be executed using Android Studio or Gradle.

From the project root, run:

```bash
./gradlew test
```

On Windows, you can run:

```cmd
gradlew.bat test
```

### Run Android Instrumented Tests

Android instrumented tests require a connected Android device or running emulator.

From the project root, run:

```bash
./gradlew connectedAndroidTest
```

## Project Structure

```text
HabitHub/
├── app/
│   └── src/
│       ├── main/              # Application source code and resources
│       ├── test/              # Local unit tests
│       └── androidTest/       # Android instrumented tests
│
├── .github/
│   └── workflows/
│       └── android.yml        # GitHub Actions CI workflow
│
├── gradle/                    # Gradle wrapper configuration
├── build.gradle.kts           # Root Gradle build configuration
├── gradle.properties         # Gradle project properties
├── gradlew                    # Gradle wrapper for Linux/macOS
├── gradlew.bat                # Gradle wrapper for Windows
├── settings.gradle.kts        # Gradle project settings
└── README.md                  # Project documentation
```

## Demo Video

A demonstration video showing the main HabitHub features and application functionality will be provided below.

**Demo Video:** https://youtu.be/LiFfMs2Lg2c

## Authors

Project done by group members: 

1. Zizinhle Masanabo ST10438120
2. Kgaogelo Mokwena ST10450091
3. Ratanang Mothibi ST10446761
4. Ponkana Mokholwane ST10453202
5. Onkatlile Tshele ST10445128
