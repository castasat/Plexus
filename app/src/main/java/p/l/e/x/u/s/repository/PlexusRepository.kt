package p.l.e.x.u.s.repository

import android.content.Context
import android.content.DialogInterface.OnClickListener
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import p.l.e.x.u.s.application.PlexusApp

class PlexusRepository(private val appContext: Context) {

    private val _showAlertDialogLiveData =
        MutableLiveData<Triple<OnClickListener, OnClickListener, ConnectionInfo>>()
    val showAlertDialogLiveData: LiveData<Triple<OnClickListener, OnClickListener, ConnectionInfo>>
        get() = _showAlertDialogLiveData

    private val _showSendButtonLiveData = MutableLiveData<String>()
    val showSendButtonLiveData: LiveData<String>
        get() = _showSendButtonLiveData

    private val _showToastLiveData = MutableLiveData<String>()
    val showToastLiveData: LiveData<String>
        get() = _showToastLiveData

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            PlexusApp.log("PlexusViewModel.onEndpointFound(): $endpointId discoveryEndpointInfo = $info")
            Nearby
                .getConnectionsClient(appContext)
                .requestConnection(
                    SERVICE_ID,
                    endpointId,
                    connectionLifecycleCallback
                )
                .addOnSuccessListener { PlexusApp.log("PlexusViewModel.onEndpointFound(): success") }
                .addOnFailureListener { exception ->
                    PlexusApp.log("PlexusViewModel.onEndpointFound(): error = $exception")
                    exception.printStackTrace()
                }
        }

        override fun onEndpointLost(endpointId: String) {
            PlexusApp.log("PlexusViewModel.onEndpointLost(): $endpointId")
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            PlexusApp.log("PlexusViewModel.onConnectionInitiated(): $endpointId connectionInfo = $info")
            _showAlertDialogLiveData.postValue(
                Triple(
                    // positive button listener
                    OnClickListener { _, _ ->
                        Nearby
                            .getConnectionsClient(appContext)
                            .acceptConnection(endpointId, payloadCallback)
                    },
                    // negative button listener
                    OnClickListener { _, _ ->
                        Nearby
                            .getConnectionsClient(appContext)
                            .rejectConnection(endpointId)
                    },
                    // connectionInfo
                    info
                )
            )
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            PlexusApp.log("PlexusViewModel.onConnectionResult(): $endpointId connectionResolution = $result")
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    PlexusApp.log("PlexusViewModel.onConnectionResult(): STATUS_OK")
                    // show button to send payload
                    _showSendButtonLiveData.postValue(endpointId)
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED ->
                    PlexusApp.log("PlexusViewModel.onConnectionResult(): STATUS_CONNECTION_REJECTED")
                ConnectionsStatusCodes.STATUS_ERROR ->
                    PlexusApp.log("PlexusViewModel.onConnectionResult(): STATUS_ERROR")
                else -> PlexusApp.log(
                    "PlexusViewModel.onConnectionResult(): " +
                            "statusCode = ${result.status.statusCode}"
                )
            }
        }

        override fun onDisconnected(endpointId: String) {
            PlexusApp.log("PlexusViewModel.onDisconnected() p0")
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            PlexusApp.log("PlexusViewModel.onPayloadReceived(): $endpointId payload = $payload")
            if (payload.type == Payload.Type.BYTES) {
                // bytes payload received
                payload.asBytes()?.let { byteArray ->
                    val bytesString = byteArray.toString(Charsets.UTF_8)
                    PlexusApp.log("PlexusViewModel.onPayloadReceived(): BYTES = $bytesString")
                    _showToastLiveData.postValue(bytesString)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            PlexusApp.log("PlexusViewModel.onPayloadTransferUpdate(): $endpointId update = $update")
            val status = update.status
            PlexusApp.log("PlexusViewModel.onPayloadTransferUpdate(): status = $status")
            when (status) {
                ConnectionsStatusCodes.SUCCESS -> {
                    PlexusApp.log("PlexusViewModel.onPayloadTransferUpdate(): file or stream payload received")
                    // TODO
                }
                ConnectionsStatusCodes.ERROR -> {
                    PlexusApp.log("PlexusViewModel.onPayloadTransferUpdate(): error receiving payload")
                    // TODO
                }
                else -> {
                    PlexusApp.log(
                        "PlexusViewModel.onPayloadTransferUpdate(): " +
                                "receiving payload - other situation"
                    )
                    // TODO
                }
            }
        }
    }

    fun advertise() {
        // TODO start advertising only once for a limited period of time
        // TODO stop advertising
        Nearby
            .getConnectionsClient(appContext)
            // TODO endpointInfo : byte[] OR name : String
            .startAdvertising(
                SERVICE_ID,
                SERVICE_ID,
                connectionLifecycleCallback,
                AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
            )
            .addOnSuccessListener { PlexusApp.log("PlexusViewModel.advertise(): success") }
            .addOnFailureListener { exception ->
                PlexusApp.log("PlexusViewModel.advertise(): error = $exception")
                exception.printStackTrace()
            }
    }

    fun discover() {
        // TODO start discovering only once
        // TODO stop discovering
        Nearby
            .getConnectionsClient(appContext)
            .startDiscovery(
                SERVICE_ID,
                endpointDiscoveryCallback,
                DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
            )
            .addOnSuccessListener { PlexusApp.log("PlexusViewModel.discover(): success") }
            .addOnFailureListener { exception ->
                PlexusApp.log("PlexusViewModel.discover(): error = $exception")
                exception.printStackTrace()
            }
    }

    companion object {
        private const val SERVICE_ID = "p.l.e.x.u.s"
    }
}