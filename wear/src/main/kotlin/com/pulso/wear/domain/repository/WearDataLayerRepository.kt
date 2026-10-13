package com.pulso.wear.domain.repository

import com.pulso.wear.domain.model.ConnectedPhoneNode
import com.pulso.wear.domain.model.PingResult

interface WearDataLayerRepository {
    suspend fun getConnectedPhoneNode(): ConnectedPhoneNode?
    suspend fun sendPing(counter: Int): PingResult
}
