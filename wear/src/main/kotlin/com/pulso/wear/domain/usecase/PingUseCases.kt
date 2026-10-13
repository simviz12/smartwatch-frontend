package com.pulso.wear.domain.usecase

import com.pulso.wear.domain.model.ConnectedPhoneNode
import com.pulso.wear.domain.model.PingResult
import com.pulso.wear.domain.repository.WearDataLayerRepository

class SendPingUseCase(
    private val repository: WearDataLayerRepository
) {
    suspend operator fun invoke(counter: Int): PingResult {
        return repository.sendPing(counter)
    }
}

class GetConnectedPhoneUseCase(
    private val repository: WearDataLayerRepository
) {
    suspend operator fun invoke(): ConnectedPhoneNode? {
        return repository.getConnectedPhoneNode()
    }
}
