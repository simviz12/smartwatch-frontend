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

### Example Payload

```json
{
  "schemaVersion": 1,
  "from": "watch",
  "sentAt": 1776043200000,
  "counter": 1
}
```
