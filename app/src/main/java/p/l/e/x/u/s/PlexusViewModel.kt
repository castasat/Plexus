package p.l.e.x.u.s

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.app.Application
import android.content.DialogInterface.OnClickListener
import android.os.Build.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes.*
import com.google.android.gms.nearby.connection.Strategy.P2P_CLUSTER
import p.l.e.x.u.s.PlexusApp.Companion.log

class PlexusViewModel(application: Application) : AndroidViewModel(application) {
    @SuppressLint("StaticFieldLeak")
    private val appContext = getApplication<Application>().applicationContext

    private val _requestRuntimePermissionLiveData = MutableLiveData<Array<String>>()
    val requestRuntimePermissionLiveData: LiveData<Array<String>>
        get() = _requestRuntimePermissionLiveData

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

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            log("PlexusViewModel.onPayloadReceived(): $endpointId payload = $payload")
            if (payload.type == Payload.Type.BYTES) {
                // bytes payload received
                payload.asBytes()?.let { byteArray ->
                    val bytesString = byteArray.toString(Charsets.UTF_8)
                    log("PlexusViewModel.onPayloadReceived(): BYTES = $bytesString")
                    _showToastLiveData.postValue(bytesString)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            log("PlexusViewModel.onPayloadTransferUpdate(): $endpointId update = $update")
            val status = update.status
            log("PlexusViewModel.onPayloadTransferUpdate(): status = $status")
            when (status) {
                SUCCESS -> {
                    log("PlexusViewModel.onPayloadTransferUpdate(): file or stream payload received")
                    // TODO
                }
                ERROR -> {
                    log("PlexusViewModel.onPayloadTransferUpdate(): error receiving payload")
                    // TODO
                }
                else -> {
                    log(
                        "PlexusViewModel.onPayloadTransferUpdate(): " +
                                "receiving payload - other situation"
                    )
                    // TODO
                }
            }
        }
    }

    // for advertise
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            log("PlexusViewModel.onConnectionInitiated(): $endpointId connectionInfo = $info")
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
            log("PlexusViewModel.onConnectionResult(): $endpointId connectionResolution = $result")
            when (result.status.statusCode) {
                STATUS_OK -> {
                    log("PlexusViewModel.onConnectionResult(): STATUS_OK")
                    // show button to send payload
                    _showSendButtonLiveData.postValue(endpointId)
                }
                STATUS_CONNECTION_REJECTED ->
                    log("PlexusViewModel.onConnectionResult(): STATUS_CONNECTION_REJECTED")
                STATUS_ERROR ->
                    log("PlexusViewModel.onConnectionResult(): STATUS_ERROR")
                else -> log(
                    "PlexusViewModel.onConnectionResult(): " +
                            "statusCode = ${result.status.statusCode}"
                )
            }
        }

        override fun onDisconnected(endpointId: String) {
            log("PlexusViewModel.onDisconnected() p0")
        }
    }

    // for discover
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            log("PlexusViewModel.onEndpointFound(): $endpointId discoveryEndpointInfo = $info")
            Nearby
                .getConnectionsClient(appContext)
                .requestConnection(ADVERTISING, endpointId, connectionLifecycleCallback)
                .addOnSuccessListener { log("PlexusViewModel.onEndpointFound(): success") }
                .addOnFailureListener { exception ->
                    log("PlexusViewModel.onEndpointFound(): error = $exception")
                    exception.printStackTrace()
                }
        }

        override fun onEndpointLost(endpointId: String) {
            log("PlexusViewModel.onEndpointLost(): $endpointId")
        }

    }

    fun init() {
        val permissions =
            if (VERSION.SDK_INT >= VERSION_CODES.S) {
                arrayOf(
                    READ_EXTERNAL_STORAGE,
                    ACCESS_FINE_LOCATION,
                    BLUETOOTH_ADVERTISE,
                    BLUETOOTH_CONNECT,
                    BLUETOOTH_SCAN
                )
            } else if (VERSION.SDK_INT >= VERSION_CODES.M) {
                arrayOf(
                    READ_EXTERNAL_STORAGE,
                    ACCESS_FINE_LOCATION
                )
            } else {
                emptyArray()
            }
        requestRuntimePermissionCheck(permissions)
    }

    private fun requestRuntimePermissionCheck(permissions: Array<String>) =
        _requestRuntimePermissionLiveData.postValue(permissions)

    fun advertise() {
        log("PlexusViewModel.advertise()")
        // TODO start advertising only once
        // TODO stop advertising
        Nearby
            .getConnectionsClient(appContext)
            .startAdvertising(
                ADVERTISING,
                SERVICE_ID,
                connectionLifecycleCallback,
                AdvertisingOptions.Builder().setStrategy(P2P_CLUSTER).build()
            )
            .addOnSuccessListener { log("PlexusViewModel.advertise(): success") }
            .addOnFailureListener { exception ->
                log("PlexusViewModel.advertise(): error = $exception")
                exception.printStackTrace()
            }
    }

    fun discover() {
        log("PlexusViewModel.discover()")
        // TODO start discovering only once
        // TODO stop discovering
        Nearby
            .getConnectionsClient(appContext)
            .startDiscovery(
                SERVICE_ID,
                endpointDiscoveryCallback,
                DiscoveryOptions.Builder().setStrategy(P2P_CLUSTER).build()
            )
            .addOnSuccessListener { log("PlexusViewModel.discover(): success") }
            .addOnFailureListener { exception ->
                log("PlexusViewModel.discover(): error = $exception")
                exception.printStackTrace()
            }
    }

    fun send(endpointId: String) {
        log("PlexusViewModel.send()")
        sendBytes(endpointId)
    }

    private fun sendBytes(endpointId: String) {
        log("PlexusViewModel.sendBytes()")
        Nearby
            .getConnectionsClient(appContext)
            .sendPayload(
                endpointId,
                Payload.fromBytes("Hello".toByteArray(Charsets.UTF_8))
            )
    }

    companion object {
        const val CODE: Int = 0xC0D3
        private const val SERVICE_ID = "p.l.e.x.u.s"
        private const val ADVERTISING = "advertising"
    }
}