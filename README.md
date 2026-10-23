# 🎓 Gastoskwela

**Gastoskwela** is a native Android application designed to help students manage their daily allowance, track expenses, and monitor their spending habits in one place. Built with **Kotlin and XML layouts**, it provides a simple way to manage personal finances and develop better budgeting habits.

## 📖 About the Project

Gastoskwela is a student-focused financial tracker developed as a **Mobile Application Development** project. It works offline and stores financial records locally using a **Room database**.

The app includes four main sections:

- 🏠 **Home** — View remaining allowance, spending summaries, budget progress, and recent expenses.
- 💸 **Expenses** — Add, edit, delete, search, and filter daily expenses by category.
- 💰 **Allowance** — Record and manage allowance entries and view the total allowance.
- 📊 **Reports** — Review financial summaries and charts to understand spending habits.

## ✨ Key Features

- **Allowance tracking** — Record and manage allowance entries.
- **Expense management** — Add, edit, delete, search, and categorize personal expenses.
- **Financial dashboard** — View total allowance, total expenses, remaining balance, and budget progress.
- **Spending reports** — Visualize financial summaries and expense trends through charts.
- **Philippine peso formatting** — Display amounts in Philippine pesos (₱).
- **Local data storage** — Use core features offline with Room database.
- **Student-friendly interface** — Simple navigation with custom icons and XML layouts.

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

1. Download the APK from the project submission or build it from source.
2. Transfer the APK to an Android device.
3. Open the APK and allow installation from that source if prompted.
4. Install and launch **Gastoskwela**.

### Build from Source

Clone the repository and open it in Android Studio:

```bash
git clone https://github.com/clarxxinn/Gastoskwela.git
cd Gastoskwela
git checkout development
```

Build a debug APK:

```bash
./gradlew :app:assembleDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
```

The generated APK is located at `app/build/outputs/apk/debug/app-debug.apk`.

## 📂 Project Structure

```text
app/src/main/
├── java/com/clarxxinn/gastoskwela/
│   ├── data/          # Room entities, DAOs, and database
│   ├── fragments/     # Home, Expenses, Allowance, Reports
│   └── utils/         # Shared utilities
├── res/
│   ├── drawable/      # Icons and backgrounds
│   ├── layout/        # XML layouts
│   └── values/        # App resources and themes
└── AndroidManifest.xml
```

## 🎯 Project Purpose

Gastoskwela aims to help students develop better financial management and budgeting habits by organizing their allowance and daily expenses in a single offline Android application.

---

**Gastoskwela** — *Track your gastos, manage your allowance.*
