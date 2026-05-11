package com.example.tictactoeapplication.multiplayer

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConnectionManager(private val context: Context) {

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val strategy = Strategy.P2P_STAR
    private val serviceId = "com.example.tictactoeapplication.SERVICE_ID"

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState = _connectionState.asStateFlow()

    private var opponentEndpointId: String? = null

    sealed class ConnectionState {
        object Idle : ConnectionState()
        object Advertising : ConnectionState()
        object Discovering : ConnectionState()
        data class Connected(val endpointName: String) : ConnectionState()
        data class Error(val message: String) : ConnectionState()
        data class PayloadReceived(val move: GameMove) : ConnectionState()
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Automatically accept the connection
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    opponentEndpointId = endpointId
                    _connectionState.value = ConnectionState.Connected(endpointId)
                    stopAdvertising()
                    stopDiscovery()
                }
                else -> {
                    _connectionState.value = ConnectionState.Error("Connection failed: ${result.status.statusMessage}")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            opponentEndpointId = null
            _connectionState.value = ConnectionState.Idle
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { bytes ->
                val move = GameMove.fromByteArray(bytes)
                _connectionState.value = ConnectionState.PayloadReceived(move)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Handle progress if needed
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            connectionsClient.requestConnection("Guest", endpointId, connectionLifecycleCallback)
        }

        override fun onEndpointLost(endpointId: String) {}
    }

    fun startAdvertising(playerName: String) {
        _connectionState.value = ConnectionState.Advertising
        val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startAdvertising(playerName, serviceId, connectionLifecycleCallback, options)
            .addOnFailureListener { e ->
                _connectionState.value = ConnectionState.Error("Advertising failed: ${e.message}")
            }
    }

    fun startDiscovery() {
        _connectionState.value = ConnectionState.Discovering
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startDiscovery(serviceId, endpointDiscoveryCallback, options)
            .addOnFailureListener { e ->
                _connectionState.value = ConnectionState.Error("Discovery failed: ${e.message}")
            }
    }

    fun stopAdvertising() = connectionsClient.stopAdvertising()
    fun stopDiscovery() = connectionsClient.stopDiscovery()

    fun sendMove(move: GameMove) {
        opponentEndpointId?.let { endpointId ->
            val payload = Payload.fromBytes(move.toByteArray())
            connectionsClient.sendPayload(endpointId, payload)
        }
    }

    fun disconnect() {
        connectionsClient.stopAllEndpoints()
        opponentEndpointId = null
        _connectionState.value = ConnectionState.Idle
    }
}
