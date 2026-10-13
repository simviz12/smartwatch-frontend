package com.pulso.core.model

import com.pulso.core.contract.DataLayerContracts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PingMessageTest {

    @Test
    fun `serialize and deserialize ping message matches exactly`() {
        val original = PingMessage(
            schemaVersion = 1,
            from = DataLayerContracts.Source.WATCH,
            sentAt = 1776043200000L,
            counter = 42
        )

        val bytes = original.toByteArray()
        val deserialized = PingMessage.fromByteArray(bytes)

        assertEquals(original, deserialized)
        assertEquals(1, deserialized.schemaVersion)
        assertEquals("watch", deserialized.from)
        assertEquals(1776043200000L, deserialized.sentAt)
        assertEquals(42, deserialized.counter)
    }

    @Test
    fun `deserialize from contract json string`() {
        val jsonPayload = """{"schemaVersion":1,"from":"watch","sentAt":1776043200000,"counter":1}"""
        val deserialized = PingMessage.fromByteArray(jsonPayload.encodeToByteArray())

        assertEquals(1, deserialized.schemaVersion)
        assertEquals("watch", deserialized.from)
        assertEquals(1776043200000L, deserialized.sentAt)
        assertEquals(1, deserialized.counter)
    }

    @Test
    fun `reject invalid schema version`() {
        val jsonPayload = """{"schemaVersion":2,"from":"watch","sentAt":1776043200000,"counter":1}"""
        assertThrows(IllegalArgumentException::class.java) {
            PingMessage.fromByteArray(jsonPayload.encodeToByteArray())
        }
    }

    @Test
    fun `reject invalid source`() {
        val jsonPayload = """{"schemaVersion":1,"from":"tablet","sentAt":1776043200000,"counter":1}"""
        assertThrows(IllegalArgumentException::class.java) {
            PingMessage.fromByteArray(jsonPayload.encodeToByteArray())
        }
    }

    @Test
    fun `reject negative counter`() {
        val jsonPayload = """{"schemaVersion":1,"from":"watch","sentAt":1776043200000,"counter":-1}"""
        assertThrows(IllegalArgumentException::class.java) {
            PingMessage.fromByteArray(jsonPayload.encodeToByteArray())
        }
    }
}
