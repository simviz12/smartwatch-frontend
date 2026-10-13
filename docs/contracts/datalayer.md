# Data Layer Contracts

This document specifies the messages and data items exchanged between the Wear OS watch and the Android companion phone via the Wearable Data Layer API.

---

## 1. /pulso/v1/ping

- **Type**: Message (`MessageClient`)
- **Path**: `/pulso/v1/ping`
- **Schema Version**: `1`
- **Sender**: Wear OS Watch (`"watch"`) or Android Phone (`"phone"`)
- **Receiver**: Android Phone or Wear OS Watch
- **Delivery Guarantees**: Best-effort point-to-point delivery to connected node; no automatic retry needed for ping.
- **Payload Format**: JSON (UTF-8 bytes)
- **Max Size Limit**: < 100 KB (Wearable Data Layer limit, payload is ~100 bytes)

### JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "PingMessage",
  "type": "object",
  "required": ["schemaVersion", "from", "sentAt", "counter"],
  "properties": {
    "schemaVersion": {
      "type": "integer",
      "enum": [1],
      "description": "Version of the contract schema."
    },
    "from": {
      "type": "string",
      "enum": ["watch", "phone"],
      "description": "Originating device type."
    },
    "sentAt": {
      "type": "integer",
      "description": "Epoch timestamp in milliseconds when the message was dispatched."
    },
    "counter": {
      "type": "integer",
      "minimum": 0,
      "description": "Incremental counter for pings from this session/device."
    }
  },
  "additionalProperties": false
}
```

---

## 2. /pulso/v1/config

- **Type**: Data Item (`DataClient`, persistent)
- **Path**: `/pulso/v1/config`
- **Schema Version**: `1`
- **Sender**: Android Phone (`"phone"`)
- **Receiver**: Wear OS Watch (`"watch"`)
- **Delivery Guarantees**: Persistent key-value synchronization across reconnects. The watch MUST NOT measure or store health data unless `collectionEnabled=true`. If the phone is unreachable at initial install or config is absent, the watch shows *"Abre Pulso en tu celular para empezar"* and measures nothing.
- **Payload Format**: JSON (UTF-8 bytes) stored under the data item key `config_payload`.
- **Max Size Limit**: < 100 KB (DataClient DataMap size limit).

### JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "ConfigSync",
  "type": "object",
  "required": ["schemaVersion", "collectionEnabled", "updatedAt"],
  "properties": {
    "schemaVersion": {
      "type": "integer",
      "enum": [1]
    },
    "collectionEnabled": {
      "type": "boolean",
      "description": "Explicit user consent and collection master toggle."
    },
    "consentVersion": {
      "type": ["string", "null"],
      "description": "Version string of the privacy policy accepted by the user."
    },
    "windowMinutes": {
      "type": "integer",
      "default": 5,
      "description": "Interval in minutes between window measurements."
    },
    "quietHours": {
      "type": ["object", "null"],
      "properties": {
        "startHour": { "type": "integer", "minimum": 0, "maximum": 23 },
        "startMinute": { "type": "integer", "minimum": 0, "maximum": 59 },
        "endHour": { "type": "integer", "minimum": 0, "maximum": 23 },
        "endMinute": { "type": "integer", "minimum": 0, "maximum": 59 }
      },
      "required": ["startHour", "startMinute", "endHour", "endMinute"]
    },
    "updatedAt": {
      "type": "integer",
      "description": "Epoch timestamp in milliseconds when config was changed."
    }
  },
  "additionalProperties": false
}
```

---

## 3. /pulso/v1/samples

- **Type**: Message (`MessageClient`)
- **Path**: `/pulso/v1/samples`
- **Schema Version**: `1`
- **Sender**: Wear OS Watch (`"watch"`)
- **Receiver**: Android Phone (`"phone"`)
- **Transport Choice & Justification**: `MessageClient` point-to-point chunked batches.
  - *Why not DataClient items?* DataClient persists state indefinitely in local cache until explicitly removed, creating unwanted sync churn for thousands of immutable time-series samples. `MessageClient` provides lightweight streaming delivery that triggers explicit application-level ACKs (`/pulso/v1/samples-ack`).
- **Max Size Limit**: 90 KB per batch payload (well beneath the 100 KB Data Layer Message limit). A single window is ~150 bytes, allowing up to 100 windows per batch safely. Batches with more windows are partitioned into separate batches.
- **Idempotency**: Windows are uniquely identified on both watch and phone by `(watchId, windowStart)`.

### JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "SamplesBatch",
  "type": "object",
  "required": ["schemaVersion", "watchId", "batchId", "windows"],
  "properties": {
    "schemaVersion": {
      "type": "integer",
      "enum": [1]
    },
    "watchId": {
      "type": "string",
      "description": "Unique hardware or node ID of the originating watch."
    },
    "batchId": {
      "type": "string",
      "format": "uuid",
      "description": "UUID identifying this transmission batch."
    },
    "windows": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["windowStart", "windowEnd", "meanHr", "minHr", "maxHr", "source"],
        "properties": {
          "id": { "type": "integer" },
          "windowStart": { "type": "integer" },
          "windowEnd": { "type": "integer" },
          "meanHr": { "type": "number" },
          "minHr": { "type": "number" },
          "maxHr": { "type": "number" },
          "rmssd": { "type": ["number", "null"] },
          "validIbiCount": { "type": "integer" },
          "ibiValidRatio": { "type": "number" },
          "activityState": { "type": "string" },
          "steps": { "type": ["integer", "null"] },
          "onBody": { "type": "boolean" },
          "source": { "type": "string" }
        }
      }
    }
  },
  "additionalProperties": false
}
```

---

## 4. /pulso/v1/samples-ack

- **Type**: Message (`MessageClient`)
- **Path**: `/pulso/v1/samples-ack`
- **Schema Version**: `1`
- **Sender**: Android Phone (`"phone"`)
- **Receiver**: Wear OS Watch (`"watch"`)
- **Delivery Guarantees**: Point-to-point acknowledgment. Watch only marks or deletes acknowledged windows upon receiving this message.

### JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "SamplesAck",
  "type": "object",
  "required": ["schemaVersion", "batchId", "accepted"],
  "properties": {
    "schemaVersion": {
      "type": "integer",
      "enum": [1]
    },
    "batchId": {
      "type": "string",
      "format": "uuid",
      "description": "UUID of the acknowledged batch."
    },
    "accepted": {
      "type": "integer",
      "minimum": 0,
      "description": "Number of samples stored/accepted by the phone."
    }
  },
  "additionalProperties": false
}
```

---

## 5. /pulso/v1/status

- **Type**: Message (`MessageClient`)
- **Path**: `/pulso/v1/status`
- **Schema Version**: `1`
- **Sender**: Wear OS Watch (`"watch"`)
- **Receiver**: Android Phone (`"phone"`)
- **Trigger**: Sent on every sync cycle and whenever status changes (e.g. service starts/stops, battery level drops).

### JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "WatchStatus",
  "type": "object",
  "required": ["schemaVersion", "batteryPct", "serviceRunning", "watchModel", "permissions", "sentAt"],
  "properties": {
    "schemaVersion": {
      "type": "integer",
      "enum": [1]
    },
    "batteryPct": {
      "type": "integer",
      "minimum": 0,
      "maximum": 100
    },
    "serviceRunning": {
      "type": "boolean"
    },
    "lastWindowAt": {
      "type": ["integer", "null"]
    },
    "watchModel": {
      "type": "string"
    },
    "permissions": {
      "type": "object",
      "additionalProperties": { "type": "boolean" }
    },
    "baselineStatus": {
      "type": "object",
      "description": "Status per bucket (NIGHT, MORNING, AFTERNOON, EVENING, SLEEP)",
      "additionalProperties": {
        "type": "object",
        "properties": {
          "state": { "type": "string", "enum": ["LEARNING", "READY"] },
          "windowCount": { "type": "integer" },
          "requiredWindows": { "type": "integer" },
          "daysCollected": { "type": "integer" },
          "requiredDays": { "type": "integer" },
          "progressPct": { "type": "number" },
          "hrMedian": { "type": ["number", "null"] },
          "hrMad": { "type": ["number", "null"] },
          "rmssdMedian": { "type": ["number", "null"] },
          "rmssdMad": { "type": ["number", "null"] }
        },
        "required": ["state", "windowCount", "requiredWindows", "daysCollected", "requiredDays", "progressPct"]
      }
    },
    "sentAt": {
      "type": "integer"
    }
  },
  "additionalProperties": false
}
```

---

## 6. /pulso/v1/commands

- **Type**: Message (`MessageClient`)
- **Path**: `/pulso/v1/commands`
- **Schema Version**: `1`
- **Sender**: Android Phone (`"phone"`)
- **Receiver**: Wear OS Watch (`"watch"`)
- **Delivery Guarantees**: Point-to-point command execution.
- **Commands**:
  - `RESET_BASELINE`: Resets the local calculated baseline on the watch and triggers a new learning cycle.

### JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "WatchCommand",
  "type": "object",
  "required": ["schemaVersion", "command", "sentAt"],
  "properties": {
    "schemaVersion": {
      "type": "integer",
      "enum": [1]
    },
    "command": {
      "type": "string",
      "enum": ["RESET_BASELINE"]
    },
    "sentAt": {
      "type": "integer"
    }
  },
  "additionalProperties": false
}
```
