package com.pulso.core.contract

object DataLayerContracts {
    const val PING_PATH = "/pulso/v1/ping"
    const val PING_SCHEMA_VERSION = 1

    const val CONFIG_PATH = "/pulso/v1/config"
    const val CONFIG_KEY = "config_payload"
    const val CONFIG_SCHEMA_VERSION = 1

    const val SAMPLES_PATH = "/pulso/v1/samples"
    const val SAMPLES_SCHEMA_VERSION = 1
    const val MAX_SAMPLES_PER_BATCH = 100
    const val MAX_BATCH_PAYLOAD_BYTES = 90 * 1024 // 90 KB

    const val SAMPLES_ACK_PATH = "/pulso/v1/samples-ack"
    const val SAMPLES_ACK_SCHEMA_VERSION = 1

    const val STATUS_PATH = "/pulso/v1/status"
    const val STATUS_SCHEMA_VERSION = 1

    const val COMMANDS_PATH = "/pulso/v1/commands"
    const val COMMANDS_SCHEMA_VERSION = 1

    const val EVENTS_PATH = "/pulso/v1/events"
    const val EVENTS_SCHEMA_VERSION = 1

    const val EVENTS_ACK_PATH = "/pulso/v1/events-ack"
    const val EVENTS_ACK_SCHEMA_VERSION = 1

    const val FEEDBACK_PATH = "/pulso/v1/feedback"
    const val FEEDBACK_SCHEMA_VERSION = 1

    const val FEEDBACK_ACK_PATH = "/pulso/v1/feedback-ack"
    const val FEEDBACK_ACK_SCHEMA_VERSION = 1

    object Source {
        const val WATCH = "watch"
        const val PHONE = "phone"
    }
}
