package com.pulso.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantMessagesTest {

    @Test
    fun `ask round-trips through bytes`() {
        val ask = AssistantAsk(requestId = "r1", text = "Estoy nervioso", language = "ES", sentAt = 10L)
        assertEquals(ask, AssistantAsk.fromByteArray(ask.toByteArray()))
    }

    @Test
    fun `reply round-trips through bytes`() {
        val reply = AssistantReply(requestId = "r1", text = "Respiremos juntos", suggestsBreathing = true, sentAt = 11L)
        assertEquals(reply, AssistantReply.fromByteArray(reply.toByteArray()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ask rejects blank text`() {
        AssistantAsk(requestId = "r1", text = "  ", sentAt = 1L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ask rejects oversized text`() {
        AssistantAsk(requestId = "r1", text = "a".repeat(AssistantAsk.MAX_TEXT + 1), sentAt = 1L)
    }

    @Test
    fun `reply ignores unknown fields from newer phones`() {
        val json = """{"schemaVersion":1,"requestId":"r9","text":"Hola","sentAt":5,"extra":"x"}"""
        val reply = AssistantReply.fromByteArray(json.toByteArray())
        assertEquals("Hola", reply.text)
        assertTrue(!reply.suggestsBreathing)
    }
}
