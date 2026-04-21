# Emergency Preparedness Manager

[![Platform](https://img.shields.io/badge/platform-Android-green)](https://developer.android.com)
[![Language](https://img.shields.io/badge/language-Java-orange)](https://www.java.com)
[![Build](https://img.shields.io/badge/build-Gradle-blue)](https://gradle.org)
[![Database](https://img.shields.io/badge/database-Room-lightgrey)](https://developer.android.com/training/data-storage/room)
[![Notifications](https://img.shields.io/badge/notifications-AlarmManager-yellow)](https://developer.android.com/reference/android/app/AlarmManager)
[![License](https://img.shields.io/badge/license-GPLv3-blue)](LICENSE)
[![Min SDK](https://img.shields.io/badge/min%20SDK-26-orange)](https://developer.android.com/about/versions/android-8.0)
[![Target SDK](https://img.shields.io/badge/target%20SDK-35-brightgreen)](https://developer.android.com/about/versions/15)

---

## 📱 Overview

**Emergency Preparedness Manager** is a privacy-first, fully offline Android application for managing emergency preparedness kits, tracking supplies, and monitoring expiration dates with automated reminders.

It is designed for complete local control:
- No internet required
- No cloud storage
- No tracking or analytics

The goal is simple: ensure individuals and families stay prepared for emergencies ranging from natural disasters to everyday disruptions.

---

## ✨ Features

### 📦 Inventory & Kit Management
- Create and manage multiple kits (home, car, travel, workplace, bug-out bag, etc.)
- Track items with quantity, category, brand, purchase date, and expiration date

- ### 🔔 Smart Notifications
- Low stock alerts
- Expiration warnings
- Zero-quantity alerts
- Scheduled kit check reminders (monthly, quarterly, yearly)

### 📊 Reporting & Insights
- Inventory overview report
- Quick search across all kits and items

### 🎨 User Experience
- Material 3 design system
- Light, dark, and system themes
- Swipe-to-delete protection with undo support

### 🔐 Privacy & Offline-First Design
- Fully offline operation
- Local-only storage using Room database
- No external data transmission

---

## 🧠 Architecture & Tech Stack

- **Language:** Java  
- **UI:** XML (Android Views) with Material 3 components  
- **Architecture:** Repository pattern with asynchronous callbacks  
- **Database:** Room (SQLite abstraction)  
- **Background Processing:** ExecutorService + Handler (main thread coordination)  
- **Notifications:** AlarmManager + BroadcastReceiver  
- **Settings:** SharedPreferences + PreferenceFragmentCompat  
- **Build System:** Gradle

---

## Screenshots

### Main Screen
<img src="screenshots/main_screen.png" width="300"/>

### Kit List
<img src="screenshots/kitlist.png" width="300"/>

### Kit Items
<img src="screenshots/itemlist.png" width="300"/>

### Reports
<img src="screenshots/report.png" width="300"/>

### Settings
<img src="screenshots/settings.png" width="300"/>

---

## 🔗 Repository

https://github.com/mattworleydev/emergency-preparedness-manager-app

---

## 🚧 Project Context

Built as a full Android capstone project demonstrating:
- Offline-first application design
- Local database architecture using Room
- Background task scheduling with Android system services
- Real-world inventory and notification logic

---

## Installation

### For Users
- Currently in internal/closed testing on Google Play
- Opt-in access available upon request

### For Developers / Sideloading

1. Clone the repository:
```bash
git clone https://github.com/mattworleydev/emergency-preparedness-manager-app.git
