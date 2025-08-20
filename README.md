# TaskPing 📱✅  
**Smart Task Management with Location-Based Reminders**

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=white)
![Google Maps](https://img.shields.io/badge/Google_Maps-4285F4?style=for-the-badge&logo=google-maps&logoColor=white)

---

## 🌟 Overview  
TaskPing is a sophisticated Android task management application that combines traditional to-do list functionality with **innovative location-based reminders**.

Never forget a task again — get notified when you're near a task location!  

---

## ✨ Key Features  

### 📋 Smart Task Management  
- **Priority & Daily Tasks**: Organize tasks by priority level and daily routines  
- **Intuitive UI**: Clean, material design interface with smooth navigation  
- **Date-based Organization**: View tasks by specific dates with an interactive date picker  

### 📍 Location Intelligence  
- **Geofencing Technology**: Automatic reminders when you enter task locations  
- **Map Integration**: Built-in map picker for precise location selection  
- **Google Places API**: Smart location search and autocomplete  

### 🔔 Smart Reminders  
- **Time-based Alarms**: Scheduled reminders for important deadlines  
- **Location-based Notifications**: Get pinged when near task locations  
- **Background Processing**: Works seamlessly even when the app is closed  

### ☁️ Cloud Sync & Offline Support  
- **Firebase Firestore**: Real-time cloud synchronization  
- **Room Database**: Full offline capability with local caching  
- **Auto-sync**: Seamless background data synchronization  

### 👤 User Management  
- **Secure Authentication**: Firebase Auth with email/password  
- **User Profiles**: Personalized task statistics  
- **Onboarding Flow**: Beautiful introduction to app features  

---

## 🛠️ Technical Stack  
- **Language**: Java 
- **Database**: Room (Local), Firebase Firestore (Cloud)  
- **Location Services**: Google Maps API, Geofencing API  
- **Authentication**: Firebase Authentication  
- **Notifications**: Android AlarmManager, WorkManager
- **UI**: Material Design Components, ViewPager2, Navigation Component  

---

## 📦 Installation  

### Prerequisites  
- Android Studio Arctic Fox or later  
- Android SDK 24+ (minSdk 24)  
- Google Maps API key  
- Firebase project setup
  
---

### Setup Steps  

```
git clone https://github.com/DMR3-code/Taskping.git
```

---

### Add API Keys  
Create a `local.properties` file in the root directory.  

```
MAPS_API_KEY=your_google_maps_api_key_here
FIREBASE_API_KEY=your_firebase_api_key_here
```

---

### Firebase Setup
- Create a Firebase project at Firebase Console
- Enable Authentication and Firestore
- Download google-services.json and place it in the app/ directory

---

### Build and Run

```
./gradlew assembleDebug
```

---

### 🎯 Usage

**Adding Tasks**
- Tap "+ Add Task" from main screen
- Set title, description, and category (Priority/Daily)
- Select start and end dates
- Optional: Add location for geofencing reminders
- Save to create task with automatic reminders

**Viewing Tasks**
- *Home Screen*: Overview of today's priority and daily tasks
- *Tasks Screen*: Calendar view with date-based task filtering
- *Task Details*: Complete task information with location preview

**Location Features**
- Tap "Add Location" when creating tasks
- Use the map interface to pin exact locations
- Get automatic notifications when approaching task locations

---

### 🏗️ Project Structure
```
app/
├── activities/          # Android Activities
├── adapters/           # RecyclerView Adapters
├── db/                 # Database (Room, Repository)
├── fragments/          # UI Fragments
├── helpers/            # Utility classes
├── models/             # Data models
└── utils/              # Utility functions
res/
├── layout/             # XML layouts
├── drawable/           # Images and icons
├── values/             # Colors, strings, styles
└── font/               # Custom fonts
```
---

### 🔧 Configuration

**Required Permissions**
```
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION"/>
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"/>
<uses-permission android:name="android.permission.INTERNET"/>
```

---

**Firebase Rules**
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /tasks/{taskId} {
      allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
      allow create: if request.auth != null && request.auth.uid == request.resource.data.userId;
    }
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

---

### 🚀 Future Enhancements

- Task categories and tags
- Recurring tasks functionality
- Task sharing and collaboration
- Advanced analytics and reports
- Wear OS integration
- Web dashboard companion
- Voice command support

---

### 🤝 Contributing

**We welcome contributions!** Please feel free to submit pull requests, create issues, or suggest new features.

- Fork the project
- Create your feature branch (git checkout -b feature/AmazingFeature)
- Commit your changes (git commit -m 'Add some AmazingFeature')
- Push to the branch (git push origin feature/AmazingFeature)
- Open a Pull Request

---

### 🙏 Acknowledgments

- Google Maps Platform for location services
- Firebase for backend services
- Android Jetpack components
- Material Design guidelines

---

### 📞 Support
- If you have any questions or need help, please:
- Check the Issues page
- Create a new issue with a detailed description
- Contact the development team

---

### TaskPing - Never forget what matters, wherever you are! 🎯📍

---






