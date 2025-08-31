# 🚕 MakiTaxi - Android Taxi Application

A modern, real-time taxi booking application built for Android that connects passengers with drivers in Serbia. This project serves as a master's thesis implementation showcasing advanced mobile development concepts and real-time location-based services.

## 📱 Features

### For Passengers
- **Real-time ride booking** with instant driver matching
- **Live driver tracking** with real-time location updates
- **Multiple vehicle types** (Basic, Luxury, Transport)
- **Route visualization** with OSRM integration
- **Driver details** with ratings and contact information
- **Call driver functionality** during active rides
- **Ride history** with detailed trip information
- **PDF report generation** for ride history
- **Route mapping** for past rides

### For Drivers
- **Online/Offline status** management
- **Real-time ride requests** with 30-second response window
- **Route navigation** with GPS integration
- **Ride management** (accept, decline, cancel, finish)
- **Passenger information** display
- **Earnings tracking** and statistics
- **Profile management** with verification system

### Core Features
- **Firebase Authentication** with Google Sign-In
- **Real-time database** synchronization
- **Location-based services** with GeoFire
- **Offline map support** with OSMDroid
- **Push notifications** for ride updates
- **Multi-language support** (Serbian/Cyrillic)
- **Responsive UI** with Material Design

## 🛠 Technology Stack

### Backend & Database
- **Firebase Authentication** - User registration and login
- **Firebase Realtime Database** - Real-time data synchronization
- **GeoFire** - Location-based queries and real-time location tracking

### Frontend & UI
- **Android Native (Java 11)** - Core application development
- **Material Design Components** - Modern UI/UX
- **OSMDroid** - OpenStreetMap integration with offline support
- **Glide** - Image loading and caching

### APIs & Services
- **OSRM (Open Source Routing Machine)** - Route calculation and navigation
- **Photon Geocoding** - Address search and geocoding
- **Nominatim** - Reverse geocoding for address lookup

### Development Tools
- **Android Studio** - IDE
- **Gradle** - Build system
- **Git** - Version control

## 📋 Prerequisites

- **Android Studio** (latest version)
- **Java 11** or higher
- **Android SDK** (API level 28+)
- **Google Play Services** (for location services)
- **Firebase account** and project setup

## 🚀 Installation & Setup

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/MakiTaxi.git
cd MakiTaxi
```

### 2. Firebase Setup
1. Create a new Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Enable Authentication with Email/Password and Google Sign-In
3. Create a Realtime Database with the following structure:
   ```
   /users
   /ride_requests
   /driver_notifications
   /passenger_responses
   /driver_locations
   ```
4. Download `google-services.json` and place it in the `app/` directory

### 3. API Keys Setup
Add the following to your `local.properties` file:
```properties
# OSRM API (public, no key required)
OSRM_BASE_URL=https://router.project-osrm.org/route/v1

# Photon Geocoding (public, no key required)
PHOTON_BASE_URL=https://photon.komoot.io/

# Nominatim (public, no key required)
NOMINATIM_BASE_URL=https://nominatim.openstreetmap.org/
```

### 4. Build and Run
```bash
./gradlew assembleDebug
```

## 📁 Project Structure

```
MakiTaxi/
├── app/
│   ├── src/main/
│   │   ├── java/com/makitaxi/
│   │   │   ├── config/           # App configuration
│   │   │   ├── driver/           # Driver-specific features
│   │   │   ├── login/            # Authentication
│   │   │   ├── menu/             # Menu and history
│   │   │   ├── model/            # Data models
│   │   │   ├── passenger/        # Passenger-specific features
│   │   │   ├── splashscreens/    # App startup
│   │   │   └── utils/            # Utility classes
│   │   └── res/
│   │       ├── layout/           # UI layouts
│   │       ├── drawable/         # Icons and graphics
│   │       ├── values/           # Strings, colors, themes
│   │       └── anim/             # Animations
│   └── build.gradle.kts          # App-level build config
├── gradle/                       # Gradle wrapper
├── build.gradle.kts              # Project-level build config
└── README.md                     # This file
```

## 🔧 Key Components

### Authentication System
- **Google Sign-In** integration
- **Email/Password** registration and login
- **Password reset** functionality
- **User profile** management

### Real-time Location Services
- **Driver location tracking** with GeoFire
- **Passenger location** services
- **Route calculation** using OSRM API
- **Offline map** support

### Ride Management
- **Ride request** creation and processing
- **Driver matching** algorithm
- **Status tracking** (created, accepted, in progress, finished)
- **Cancellation** handling

### UI/UX Features
- **Material Design** components
- **Responsive layouts** for different screen sizes
- **Smooth animations** and transitions
- **Dark/Light theme** support

## 🗄 Database Schema

### Users Collection
```json
{
  "userId": {
    "fullName": "string",
    "email": "string",
    "phone": "string",
    "userType": "PASSENGER|DRIVER",
    "rating": "number",
    "activeRide": "boolean",
    "carType": "BASIC|LUXURY|TRANSPORT",
    "profilePicture": "string"
  }
}
```

### Ride Requests Collection
```json
{
  "requestId": {
    "passengerId": "string",
    "driverId": "string",
    "pickupAddress": "string",
    "dropoffAddress": "string",
    "pickupLatitude": "number",
    "pickupLongitude": "number",
    "dropoffLatitude": "number",
    "dropoffLongitude": "number",
    "distance": "number",
    "duration": "number",
    "estimatedPrice": "number",
    "carType": "string",
    "status": "NotificationStatus",
    "timestamp": "number"
  }
}
```

## 🔄 Workflow

### Passenger Journey
1. **Registration/Login** → Create account or sign in
2. **Location Selection** → Choose pickup and destination
3. **Ride Request** → Submit request with vehicle type
4. **Driver Matching** → System finds nearby available drivers
5. **Driver Acceptance** → Driver accepts the ride
6. **Ride in Progress** → Real-time tracking and communication
7. **Ride Completion** → Payment and rating

### Driver Journey
1. **Registration/Verification** → Create account and verify documents
2. **Go Online** → Set status to available for rides
3. **Receive Requests** → Get notified of nearby ride requests
4. **Accept/Decline** → Respond within 30 seconds
5. **Navigate to Pickup** → Use GPS navigation
6. **Complete Ride** → Mark ride as finished
7. **Earnings** → Track income and statistics

## 🚨 Status Management

<img width="1220" height="723" alt="Screenshot 2025-08-10 at 20 37 13" src="https://github.com/user-attachments/assets/c1d04572-d078-4d4b-b005-6525eee53e69" />

The application uses a comprehensive status system:

- **CREATED** - Ride request created
- **ACCEPTED_BY_DRIVER** - Driver accepted the ride
- **ACCEPTED_BY_PASSENGER** - Passenger confirmed the ride
- **CANCELLED_BY_DRIVER** - Driver cancelled before acceptance
- **CANCELLED_BY_DRIVER_WHILE_WAITING** - Driver cancelled while waiting for passenger
- **CANCELLED_BY_DRIVER_DURING_RIDE** - Driver cancelled during active ride
- **DECLINED_BY_PASSENGER** - Passenger declined the ride
- **FINISHED** - Ride completed successfully
- **TIMEOUT** - Request timed out
- **DRIVER_EXITED_APP** - Driver left the application
- **PASSENGER_EXITED_APP** - Passenger left the application

## 📊 Performance Features

- **Offline map caching** for reduced data usage
- **Efficient location updates** with throttling
- **Optimized route calculations** with caching
- **Background service** for location tracking
- **Memory management** for large datasets

## 🔒 Security Features

- **Firebase Authentication** for secure user management
- **Real-time database rules** for data protection
- **Input validation** and sanitization
- **Secure API communication** with HTTPS
- **Permission management** for location and phone access

## 🧪 Testing

The project includes:
- **Unit tests** for core functionality
- **Instrumented tests** for UI components
- **Integration tests** for Firebase services
- **Manual testing** scenarios for ride workflows

## 📈 Future Enhancements

- **Payment integration** (credit cards, digital wallets)
- **Advanced analytics** and reporting
- **Multi-language support** expansion
- **Push notifications** for promotions
- **Driver earnings dashboard**
- **Passenger loyalty program**
- **Emergency contact** integration
- **Voice commands** for hands-free operation

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is developed as part of a master's thesis. All rights reserved.

## 👨‍💻 Author

**Bogdan** - Master's Thesis Project

## 🙏 Acknowledgments

- **Firebase** for backend services
- **OSMDroid** for map functionality
- **OSRM** for routing services
- **Material Design** for UI components
- **OpenStreetMap** for map data

**Note**: This application is designed specifically for the Serbian market and includes Cyrillic text support and local business requirements.
