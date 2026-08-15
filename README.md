# PUBG Connect - GameLoop & Android Friend Notifier 🎮

A high-performance, lightweight cross-platform friend notification ecosystem for **PUBG Mobile**, supporting both **GameLoop PC** players and **Android Mobile** players under a unified real-time account network.

---

## Key Features & Requirements Matrix

- **🟢 Dual-Platform PUBG Mobile Detection**:
  - **Windows (Desktop App)**: WMI process tracing for GameLoop (`txgameassistant.exe`, `aow_exe.exe`, `com.tencent.ig`).
  - **Android (Mobile App)**: Android `UsageStatsManager` foreground package monitoring with adaptive low-overhead polling (`com.tencent.ig`, `com.pubg.krmobile`, `com.pubg.imobile`, etc.).
- **⚡ Ultra-Low Resource & Battery Footprint**: 0–0.5% CPU when idle, <50MB RAM target. Adaptive intervals (30s heartbeat when playing, dormant when idle).
- **🔔 Smart Notifications**: Notifies friends strictly on `OFFLINE -> ONLINE / PLAYING` transitions (no spam on heartbeats). Includes per-friend mute toggle and global notification controls.
- **🖥️ & 📱 Cross-Platform Badges**: Live friend list displays platform indicators (`🖥️ GameLoop • Online for 14 min` or `📱 Android • Online for 5 min`).
- **⚡ Recent Activity Feed**: Real-time cross-platform activity stream showing when friends start playing PUBG Mobile.
- **👥 Friends & 6-Character Friend IDs**: Unique 6-character Friend IDs (e.g. `A7K92D`), instant lookup, request workflows (send, accept, decline, remove).
- **🛡️ Crash & Disconnect Protection**: 75-second automated heartbeat timeout clears ghost statuses if PC shuts down, phone sleeps, or crashes.
- **💻 Windows System Tray & Android Background Service**: Runs silently in background with tray / foreground service indicators.
- **🔒 Privacy Controls**: Toggle status sharing, friend requests, and playing duration visibility.
- **🧪 Interactive Simulation Mode**: Test PUBG detection and notification alerts on both PC and Android without running the game.

---

## Project Structure

```
f:\Project Work\Pubg application\Desktop Application\
  ├── PUBGConnect.slnx             # Visual Studio / .NET Solution
  ├── start_server.bat             # Quick launch script for backend server
  ├── start_client.bat             # Quick launch script for WPF desktop app
  ├── test_workflow.ps1            # Desktop API test script
  ├── test_crossplatform_workflow.ps1 # Cross-platform (PC + Android) verification
  └── src\
      ├── PubgConnect.Shared\      # DTOs, Enums (UserStatus, PlatformType), SignalR events
      ├── PubgConnect.Server\      # ASP.NET Core SignalR Web API + Activity Feed + Heartbeat Monitor
      ├── PubgConnect.Client\      # Modern Dark WPF Desktop Application (.NET 10)
      └── PubgConnect.Android\     # Native Kotlin Android Application (Jetpack Compose & Material 3)
```

---

## How to Run

### 1. Start Backend Server
Open a terminal in this directory and run:
```powershell
dotnet run --project src/PubgConnect.Server/PubgConnect.Server.csproj
```
Or double-click `start_server.bat`. The server runs at `http://localhost:5000`.

### 2. Start Desktop Application (GameLoop PC)
In a separate terminal or window, run:
```powershell
dotnet run --project src/PubgConnect.Client/PubgConnect.Client.csproj
```
Or double-click `start_client.bat`.

### 3. Open & Run Android Application (Mobile)
Open the `src/PubgConnect.Android` directory in **Android Studio**:
- Build and run on an Android Emulator or connected Android phone.
- Default server address in emulator is preconfigured to `http://10.0.2.2:5000`.
- To grant Usage Access for automatic PUBG detection on device: Go to **Detection** tab and tap **Enable Access**.

---

## Demo Accounts (Pre-seeded)

| User | Email | Password | Friend ID | Typical Platform |
| :--- | :--- | :--- | :--- | :--- |
| **Ali** | `ali@pubg.com` | `password123` | `A7K92D` | 🖥️ GameLoop PC |
| **Ahmed** | `ahmed@pubg.com` | `password123` | `B3M88X` | 📱 Android |
| **Hassan** | `hassan@pubg.com` | `password123` | `C9P41Z` | 📱 Android / PC |
| **Usman** | `usman@pubg.com` | `password123` | `D5R20Y` | 📱 Android / PC |

*Ali and Ahmed are pre-connected as friends for immediate testing.*
