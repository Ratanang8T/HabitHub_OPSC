# HabitHub

HabitHub is an Android habit-tracking application designed to help users create, manage, monitor, and maintain daily habits.

## Project Purpose

The purpose of HabitHub is to provide users with a simple way to create habits, set goals and reminders, track progress, and manage their personal development activities in one application.

## Main Features

- User registration and login
- User authentication using Firebase
- Habit creation and editing
- Habit categories
- Daily, weekday, and custom habit frequencies
- Custom day selection
- Daily habit goals
- Reminder time selection
- Habit notes
- Habit progress tracking
- Habit statistics
- Habit achievements
- Profile management
- Application settings
- Habit reminders and notifications
- Firebase Firestore data storage

## Design Considerations

The application was designed with usability and simplicity in mind. The interface separates authentication, habit management, progress tracking, achievements, profile information, and settings into dedicated screens.

The habit creation process allows users to specify the habit name, category, frequency, goal, measurement unit, reminder time, and notes. Custom frequency options allow users to select specific days of the week.

The application also includes validation to prevent incomplete habit information from being submitted.

## Technologies Used

- Kotlin
- Android Studio
- Android SDK
- Firebase Authentication
- Firebase Firestore
- AndroidX
- Gradle
- GitHub
- GitHub Actions

## Testing

The project includes both unit tests and Android instrumented tests.

Unit tests are located in:

`app/src/test`

Android instrumented tests are located in:

`app/src/androidTest`

The tests include application launch testing, authentication screen testing, and habit creation interface testing.

## Automated Testing

GitHub Actions is configured to automatically build the Android application and run the unit tests when changes are pushed to the main branch or when a pull request is created.

The workflow is located at:

`.github/workflows/android.yml`

## How to Run the Application

1. Clone or download the HabitHub repository.
2. Open the project in Android Studio.
3. Allow Gradle to synchronise the project.
4. Connect an Android device or start an Android emulator.
5. Run the application using Android Studio.

## Project Structure

```text
HabitHub
├── app
│   └── src
│       ├── main
│       ├── test
│       └── androidTest
├── .github
│   └── workflows
│       └── android.yml
├── gradle
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
└── README.md