package p.l.e.x.u.s.repository

import android.content.Context
import android.content.DialogInterface.OnClickListener
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes.*
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import p.l.e.x.u.s.application.PlexusApp.Companion.log
import p.l.e.x.u.s.connection.nearby.NearbyApi

class PlexusRepository(private val appContext: Context) {
    val compositeDisposable = CompositeDisposable()
    private val nearbyApi by lazy { NearbyApi(appContext) }

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
            log("PlexusRepository.onEndpointFound(): $endpointId discoveryEndpointInfo = $info")
            compositeDisposable.add(
                nearbyApi.requestConnection(endpointId, connectionLifecycleCallback)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(
                        {
                            log("PlexusRepository.onEndpointFound(): requestConnection() completed")
                        },
                        { throwable ->
                            log("PlexusRepository.onEndpointFound(): error = $throwable")
                            throwable.printStackTrace()
                        }
                    )
            )
        }

        override fun onEndpointLost(endpointId: String) {
            log("PlexusRepository.onEndpointLost(): $endpointId")
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            log("PlexusRepository.onConnectionInitiated(): $endpointId connectionInfo = $info")
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
            log("PlexusRepository.onConnectionResult(): $endpointId connectionResolution = $result")
            when (result.status.statusCode) {
                STATUS_OK -> {
                    log("PlexusRepository.onConnectionResult(): STATUS_OK")
                    // show button to send payload
                    _showSendButtonLiveData.postValue(endpointId)
                }
                STATUS_CONNECTION_REJECTED ->
                    log("PlexusRepository.onConnectionResult(): STATUS_CONNECTION_REJECTED")
                STATUS_ERROR ->
                    log("PlexusRepository.onConnectionResult(): STATUS_ERROR")
                else -> log(
                    "PlexusRepository.onConnectionResult(): " +
                            "statusCode = ${result.status.statusCode}"
                )
            }
        }

        override fun onDisconnected(endpointId: String) {
            log("PlexusRepository.onDisconnected(): endpointId")
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            log("PlexusRepository.onPayloadReceived(): $endpointId payload = $payload")
            if (payload.type == Payload.Type.BYTES) {
                // bytes payload received
                payload.asBytes()?.let { byteArray ->
                    val bytesString = byteArray.toString(Charsets.UTF_8)
                    log("PlexusRepository.onPayloadReceived(): BYTES = $bytesString")
                    _showToastLiveData.postValue(bytesString)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            log("PlexusRepository.onPayloadTransferUpdate(): $endpointId update = $update")
            val status = update.status
            log("PlexusRepository.onPayloadTransferUpdate(): status = $status")
            when (status) {
                SUCCESS -> {
                    log("PlexusRepository.onPayloadTransferUpdate(): file or stream payload received")
                }
                ERROR -> {
                    log("PlexusRepository.onPayloadTransferUpdate(): error receiving payload")
                }
                else -> {
                    log(
                        "PlexusRepository.onPayloadTransferUpdate(): " +
                                "receiving payload - other situation"
                    )
                }
            }
        }
    }

    fun advertise() {
        log("PlexusRepository.advertise()")
        // TODO start advertising only once for a limited period of time
        // TODO stop advertising
        // TODO send signal to processor
        compositeDisposable.add(
            nearbyApi.advertise(connectionLifecycleCallback)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                    {
                        log("Repository.advertise(): completed")
                    },
                    { throwable ->
                        log("Repository.advertise(): error = $throwable")
                        throwable.printStackTrace()
                    }
                )
        )
    }


    fun discover() {
        log("Repository.discover()")
        // TODO start discovering only once
        // TODO stop discovering
        // TODO send signal to processor
        compositeDisposable.add(
            nearbyApi.discover(endpointDiscoveryCallback)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                    {
                        log("Repository.discover(): completed")
                    },
                    { throwable ->
                        log("Repository.discover(): error = $throwable")
                        throwable.printStackTrace()
                    }
                )
        )
    }

    fun sendBytes(endpointId: String) {
        log("Repository.sendBytes()")
        // TODO send to processor
        compositeDisposable.add(
            nearbyApi.sendBytes(endpointId)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                    {
                        log("Repository.sendBytes(): completed")
                    },
                    { throwable ->
                        log("Repository.sendBytes(): error = $throwable")
                        throwable.printStackTrace()
                    }
                )
        )
    }
}