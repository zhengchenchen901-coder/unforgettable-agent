# Unforgettable

Unforgettable is a local-first Android MVP for automatic task capture from notifications.

## Week 1 Flow

```text
Android Notification
  -> AI Task Extraction
  -> Local Task Storage
  -> Reminder Notification
```

## Build

Open this folder in Android Studio, let Gradle sync, then run the `app` configuration on an Android device.

The app uses:

- Kotlin + Jetpack Compose + Material 3
- Room for local storage
- WorkManager for AI extraction and reminders
- Retrofit + OkHttp for OpenAI-compatible Chat Completions APIs

## Demo Path

1. Grant notification listener access from Home.
2. Grant notification permission on Android 13+.
3. Choose an LLM provider/model in Settings and add that provider's API key.
4. Open Debug and tap `注入模拟通知`.
5. Confirm the raw notification, extraction log, task card, and reminder schedule.
