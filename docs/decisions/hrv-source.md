# HRV Source & Architectural Recommendation (Part 1 Evaluation)

Document Version: 1.0  
Status: Evaluation & Decision Guide for Field Testing

---

## 1. Data Observed & Hardware Realities

During Part 1, the watch hardware feasibility was analyzed across Wear OS versions (API 30 through API 35) and Samsung Galaxy Watch hardware:

1. **Google Health Services (`PassiveMonitoringClient` / `MeasureClient`)**:
   - Delivers real-time and passive `DataType.HEART_RATE_BPM`.
   - Delivers `Availability` states (`AVAILABLE`, `ACQUIRING`, `UNAVAILABLE_DEVICE_OFF_BODY`).
   - Does **NOT** provide Inter-Beat Intervals (IBI/RR).

2. **Samsung Health Sensor SDK (`HealthTrackingService`)**:
   - Offers `HealthTrackerType.HEART_RATE_CONTINUOUS` which yields IBI arrays when paired with a valid status.
   - **Severe Distribution Blocker**: Requires Samsung Partner approval and matching cryptographic keys. Without Samsung Partner approval, running on production watches throws `SDK_POLICY_ERROR`. Testing locally requires turning on Developer Mode in `Health Sensor Service`.

---

## 2. Recommendation: Path A vs. Path B

### PATH A (HR + HRV via Samsung Sensor SDK)
- **Feasible when**: Developing specifically for Samsung Galaxy Watches (Watch 4/5/6/7) with Developer Mode enabled or an accepted Samsung Partner application.
- **Benefits**: Real RMSSD and frequency-domain HRV stress computation.
- **Drawbacks**: Restricted to Samsung Galaxy Watches; high battery consumption (optical PPG running continuously); non-Samsung Wear OS watches cannot use it.

### PATH B (HR-Only via Health Services Passive Monitoring) — **RECOMMENDED CORE BASELINE**
- **Feasible when**: Universal Wear OS deployment (Google Pixel Watch, Samsung Galaxy Watch, OnePlus Watch, etc.).
- **Strategy**:
  - Sample passive HR and resting heart rate elevation against a personal baseline.
  - Pulso defaults to Path B for universal reliability, while maintaining an optional modular bridge to Path A when the Samsung SDK is available and unlocked.

---

## 3. CSV Extraction Instructions
Once the background service has collected windows over the testing period:
```powershell
# Identify storage directory
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell "run-as com.pulso ls /data/user/0/com.pulso/files/exports/"

# Pull to host machine
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" pull /sdcard/Android/data/com.pulso/files/exports/ .
```
