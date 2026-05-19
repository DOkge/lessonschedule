# Project AGENTS Guide - LessonSchedule

## 1. Описание проекта и доменная область
**LessonSchedule** — это Android-приложение для просмотра расписания учебных занятий.
- **Домен**: Образование (Education / EdTech).
- **Основной функционал**: Загрузка расписания по группам, кеширование данных (offline mode), уведомления о занятиях, виджет для рабочего стола, поддержка темной темы.

## 2. Стек технологий
- **Язык**: Kotlin 2.0.0
- **SDK**: Compile/Target SDK 35, Min SDK 24
- **Архитектура UI**: ViewBinding (XML), Navigation Component
- **База данных**: Room 2.6.1 (KSP)
- **Сеть**: Retrofit 2.11.0 + OkHttp
- **Сериализация**: Kotlinx Serialization 1.6.3
- **Хранение настроек**: DataStore Preferences 1.1.1
- **Асинхронность**: Coroutines & Flow
- **Lifecycle**: ViewModel, LiveData (использование Flow предпочтительнее)

## 3. Архитектурные паттерны
Проект следует принципам **Clean Architecture** (упрощенная версия) и **MVVM**:
- **UI Layer**: Fragments и ViewModels. ViewModels общаются с репозиториями через Flow/Suspend функции.
- **Data Layer**:
    - **Repository Pattern**: Изолирует логику получения данных от UI. Репозиторий (`ScheduleRepository`) управляет переключением между локальным кешем (Room) и удаленным API.
    - **Local Source**: Room DAO.
    - **Remote Source**: Retrofit API.
- **Dependency Injection**: В текущем состоянии используется ручное управление зависимостями (ViewModel Factory).

## 4. Правила оформления кода (Naming Conventions)
- **Классы**: `PascalCase` (например, `ScheduleViewModel`, `LessonEntity`).
- **Функции и переменные**: `camelCase` (например, `getMondayOfWeek()`, `isLoading`).
- **Константы**: `SCREAMING_SNAKE_CASE` (например, `TOTAL_PAGES`).
- **Ресурсы (Layouts/Drawables)**: `snake_case` с префиксом типа (например, `fragment_schedule.xml`, `ic_settings.xml`).
- **Пакеты**: по слоям (`ui`, `data`, `data.local`, `data.remote`, `data.repository`).

## 5. Запрещенные практики и библиотеки
- **Threading**: Запрещено использование `Thread`, `Handler`, `AsyncTask`. Используйте только Coroutines (`viewModelScope`, `lifecycleScope`).
- **Data Passing**: Не передавать большие объекты между фрагментами через `Bundle`. Использовать ID и загружать данные из репозитория/БД.
- **UI**: Избегать логики в Fragments/Activities. Вся бизнес-логика и трансформация данных — в ViewModel или Repository.
- **JSON**: Не использовать `Gson` или `Moshi`. Основная библиотека — `Kotlinx Serialization`.
- **View Access**: Запрещено использование `findViewById`. Использовать только `ViewBinding`.
- **Global Context**: Не хранить ссылки на `Context` в ViewModel или Repository (кроме `ApplicationContext` в необходимых случаях, передаваемых через конструктор).
