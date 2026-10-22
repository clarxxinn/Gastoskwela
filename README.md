# 🎓 Gastoskwela

**Gastoskwela** is a native Android application that helps students manage their daily allowance, track expenses, and monitor school payments in one place. Built with **Kotlin and XML layouts**, it provides a simple way to understand where money goes and stay prepared for upcoming school expenses.

## 📥 Download

**Latest Release: v1.0.0**

[**Download Gastoskwela APK**](https://github.com/clarxxinn/Gastoskwela/releases/latest)

Download the latest available version of Gastoskwela from the official GitHub Releases page.

**Requirements:** Android 8.0 (Oreo) or later.

## 📖 About the Project

Gastoskwela is a student-focused financial tracker developed as a **Mobile Application Development** project. It works offline and saves records locally using a **Room database**.

The app includes five main sections:

- 🏠 **Home** — View remaining allowance, spending summaries, budget progress, recent expenses, and upcoming school payments.
- 💸 **Expenses** — Add, edit, delete, search, and filter expenses by category.
- 💰 **Allowance** — Record and manage allowance entries and view the total allowance.
- 📚 **School Payments** — Track tuition, projects, supplies, school fees, events, and other payments, including partial payments and due dates.
- 📊 **Reports** — Review financial summaries and charts.

## ✨ Key Features

- **Allowance and expense tracking** with Philippine peso (₱) formatting
- **School payment management** with upcoming, overdue, and completed payment statuses
- **Payment reminders** accessible from the notification bell on the Home screen
- **Partial payment tracking** with remaining balances
- **Dashboard calculations** that include paid school payments in total spending
- **Local data storage** — no internet connection required for core features
- **Custom icons and a clean, student-friendly interface**

## 🛠️ Built With

| Technology | Purpose |
|---|---|
| Kotlin | Application logic |
| XML | Android user interface |
| Android Studio | Development environment |
| Room Database | Local data persistence |
| Coroutines & Flow | Reactive database updates |
| Material Components | UI components |

## 📱 Installation

1. Visit the [Gastoskwela Releases](https://github.com/clarxxinn/Gastoskwela/releases/latest) page.
2. Download **`Gastoskwela.apk`** from the Assets section.
3. Open the downloaded APK on your Android device.
4. Allow installation from the source if prompted.
5. Install and launch **Gastoskwela**.

### Build from Source

Clone the repository and open it in Android Studio:

```bash
git clone https://github.com/clarxxinn/Gastoskwela.git
```

Build a debug APK using:

```bash
./gradlew assembleDebug
```

On Windows PowerShell, use `.\gradlew.bat :app:assembleDebug`.

The generated APK is located at `app/build/outputs/apk/debug/app-debug.apk`.

## 📂 Project Structure

```text
app/src/main/
├── java/com/clarxxinn/gastoskwela/
│   ├── data/          # Room entities, DAOs, and database
│   ├── fragments/     # Home, Expenses, Allowance, Payments, Reports
│   └── utils/         # Shared utilities
├── res/
│   ├── drawable/      # Icons and backgrounds
│   ├── layout/        # XML layouts
│   └── values/        # App resources and themes
└── AndroidManifest.xml
```

## 🎯 Project Purpose

Gastoskwela aims to help students build better budgeting habits by keeping allowance, expenses, and school payment obligations organized in a single offline Android app.

---

**Gastoskwela** — *Track your gastos, manage your allowance.*
