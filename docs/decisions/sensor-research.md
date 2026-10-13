# Sensor Research & Feasibility: Wear OS & Samsung Health Sensor SDK

Document Version: 1.0  
Date: 2026-10-12  
Status: Evaluated for Part 1 Architecture Decision

---

## 1. Documentation Consulted
- **Google Health Services for Wear OS**:
  - [Wear OS Health Services Passive Monitoring](https://developer.android.com/training/wearables/health-services/passive) (`PassiveMonitoringClient`, `PassiveListenerService`, `PassiveMonitoringConfig`)
  - [Wear OS Health Services Real-Time Measurements](https://developer.android.com/training/wearables/health-services/measure) (`MeasureClient`, `MeasureCallback`, `DataType.HEART_RATE_BPM`)
  - [Android 14/15 Foreground Services & Health Types](https://developer.android.com/about/versions/14/changes/fgs-types-required#health) (`foregroundServiceType="health"`, `FOREGROUND_SERVICE_HEALTH`)
  - [Health Services Permissions Documentation](https://developer.android.com/training/wearables/health-services/permissions) (`BODY_SENSORS`, `BODY_SENSORS_BACKGROUND`, `ACTIVITY_RECOGNITION`)
- **Samsung Privileged Health Sensor SDK**:
  - [Samsung Health Sensor SDK Guides](https://developer.samsung.com/health-sensor) (`HealthTrackingService`, `HealthTrackerType.HEART_RATE_CONTINUOUS`, `ValueKey.HeartRateSet`)
  - [Samsung Health Sensor Partner Policy & Developer Mode](https://developer.samsung.com/health-sensor/partner-program.html)

---

## 2. API Comparison & Feasibility on Wear OS Watches

| Feature | Standard Health Services (Google) | Samsung Health Sensor SDK |
| :--- | :--- | :--- |
| **Availability** | All Wear OS 3.0+ watches (Pixel Watch, Galaxy Watch 4/5/6/7, TicWatch, etc.) | Samsung Galaxy Watch 4, 5, 6, 7 and Ultra only |
| **Instant Heart Rate (BPM)** | Yes (`DataType.HEART_RATE_BPM` via `MeasureClient` & `PassiveMonitoringClient`) | Yes (`HealthTrackerType.HEART_RATE_CONTINUOUS`) |
| **Inter-Beat Interval (IBI / RR)** | **No** (Standard Wear OS Health Services does not expose raw IBI streams) | **Yes** (via `HEART_RATE_CONTINUOUS` when sensor flag is valid) |
| **Required Permissions** | `BODY_SENSORS`, `BODY_SENSORS_BACKGROUND`, `ACTIVITY_RECOGNITION` | `BODY_SENSORS` |
| **Distribution / Restrictions** | Open to all developers on Google Play Store | Requires Samsung Partner Program approval and registered signature certificate. Local development requires enabling **Developer Mode** on the watch (`Health Sensor Service` tapped 10 times). |
| **Foreground Service Type** | `foregroundServiceType="health"` with `FOREGROUND_SERVICE_HEALTH` | `foregroundServiceType="health"` with `FOREGROUND_SERVICE_HEALTH` |

---

## 3. Architecture Strategy for Pulso (Dual-Path Design)

1. **Path A (Samsung Galaxy Watch with SDK / Developer Mode)**:
   - When the Samsung Health Sensor SDK (`samsung-health-sensor-api`) is linked or present and Developer Mode is active, Pulso samples 60-second windows capturing continuous Heart Rate + valid IBIs.
   - Computes RMSSD and HRV stress indices.

2. **Path B (Standard Wear OS Watches or Samsung without Partner License)**:
   - Uses Google Health Services (`MeasureClient` & `PassiveMonitoringClient`).
   - Collects live BPM, availability state, and passive activity state (asleep/passive/exercise).
   - Honestly displays `"HRV no disponible en este reloj"` and bases stress approximations on resting HR dynamics and elevation baselines.
