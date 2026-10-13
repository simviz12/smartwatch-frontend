package com.pulso.core.contract

object DataLayerContracts {
    const val PING_PATH = "/pulso/v1/ping"
    const val PING_SCHEMA_VERSION = 1

    object Source {
        const val WATCH = "watch"
        const val PHONE = "phone"
    }
}
