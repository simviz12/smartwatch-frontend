package com.pulso.wear.data.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.pulso.core.contract.DataLayerContracts
import com.pulso.core.model.PingMessage
import com.pulso.wear.domain.model.ConnectedPhoneNode
import com.pulso.wear.domain.model.PingResult
import com.pulso.wear.domain.repository.WearDataLayerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidWearDataLayerRepository(
    context: Context
) : WearDataLayerRepository {

    private val messageClient = Wearable.getMessageClient(context.applicationContext)
    private val nodeClient = Wearable.getNodeClient(context.applicationContext)

    override suspend fun getConnectedPhoneNode(): ConnectedPhoneNode? = withContext(Dispatchers.IO) {
        try {
            val connectedNodes: List<Node> = Tasks.await(nodeClient.connectedNodes)
            val phoneNode = connectedNodes.firstOrNull { !it.isNearby || it.isNearby } // phone node among connected nodes
            phoneNode?.let {
                ConnectedPhoneNode(id = it.id, displayName = it.displayName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying connected nodes", e)
            null
        }
    }

    override suspend fun sendPing(counter: Int): PingResult = withContext(Dispatchers.IO) {
        val phoneNode = getConnectedPhoneNode()
        if (phoneNode == null) {
            Log.w(TAG, "sendPing aborted: no connected phone node detected")
            return@withContext PingResult.Failure("Celular no conectado (no hay nodos emparejados activos)")
        }

        try {
            val pingMessage = PingMessage(
                schemaVersion = DataLayerContracts.PING_SCHEMA_VERSION,
                from = DataLayerContracts.Source.WATCH,
                sentAt = System.currentTimeMillis(),
                counter = counter
            )
            val payload = pingMessage.toByteArray()

            Log.d(TAG, "Sending ping #$counter to node ${phoneNode.displayName} (${phoneNode.id}) on path ${DataLayerContracts.PING_PATH}")
            Tasks.await(
                messageClient.sendMessage(
                    phoneNode.id,
                    DataLayerContracts.PING_PATH,
                    payload
                )
            )
            Log.i(TAG, "Ping #$counter successfully dispatched to ${phoneNode.displayName}")
            PingResult.Success(phoneNode.displayName)
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: e.javaClass.simpleName
            Log.e(TAG, "Failed to send ping message: $errorMsg", e)
            PingResult.Failure("Error al enviar: $errorMsg")
        }
    }

    companion object {
        private const val TAG = "PulsoWearPing"
    }
}
