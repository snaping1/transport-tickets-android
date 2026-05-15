# Transport Tickets Client (Android)

Android-приложение для покупки билетов на транспорте.

## Стек технологий

- **UI**: Jetpack Compose + Material Design 3
- **Архитектура**: Clean Architecture (data / domain / presentation)
- **DI**: Dagger Hilt
- **БД**: Room (SQLite) — кеш маршрутов и билетов
- **Сеть**: Retrofit + OkHttp + kotlinx.serialization
- **Авторизация**: Firebase Authentication (email/password)
- **Навигация**: Jetpack Navigation Compose
- **Тесты**: JUnit 4, MockK, Compose UI Test

## Предварительные требования

- Android Studio Hedgehog+
- JDK 17
- Firebase проект (Authentication включён)
- Запущенный сервер (см. `transport-tickets-server`)

## Настройка

### 1. Firebase

1. Создайте проект на [Firebase Console](https://console.firebase.google.com)
2. Добавьте Android-приложение с package name `com.transport.tickets`
3. Скачайте `google-services.json` и поместите в `app/`
4. Включите **Email/Password** аутентификацию в Firebase Console → Authentication → Sign-in method

### 2. URL сервера

В `app/build.gradle.kts` измените `BASE_URL`:

```kotlin
// Эмулятор Android (localhost сервер)
buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"")

// Реальное устройство (замените IP)
buildConfigField("String", "BASE_URL", "\"http://192.168.1.100:8080/\"")

// Продакшен
buildConfigField("String", "BASE_URL", "\"https://your-server.com/\"")
```

### 3. Сборка

```bash
./gradlew assembleDebug
# или откройте в Android Studio и нажмите Run
```

## Архитектура

```
app/
├── data/
│   ├── local/          # Room DB, DAOs, entities
│   ├── remote/         # Retrofit API, DTOs
│   └── repository/     # RepositoryImpl + Mappers
├── domain/
│   ├── model/          # Route, Ticket (чистые модели)
│   ├── repository/     # Интерфейсы репозиториев
│   └── usecase/        # GetRoutesUseCase, BuyTicketUseCase, etc.
├── presentation/
│   ├── auth/           # AuthScreen + AuthViewModel
│   ├── routes/         # RoutesScreen + RoutesViewModel
│   ├── purchase/       # PurchaseScreen + PurchaseViewModel
│   ├── tickets/        # MyTicketsScreen + MyTicketsViewModel
│   └── navigation/     # NavHost + Screen sealed class
└── di/                 # Hilt modules
```

## Запуск тестов

```bash
# Юнит-тесты
./gradlew test

# UI-тесты (нужен эмулятор или устройство)
./gradlew connectedAndroidTest
```

## Офлайн-режим

Маршруты и билеты кешируются в Room. При отсутствии сети приложение показывает кешированные данные с соответствующим уведомлением.
