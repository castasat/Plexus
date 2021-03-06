package p.l.e.x.u.s.repository

import android.content.Context
import android.content.DialogInterface.OnClickListener
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes.*
import com.google.android.gms.nearby.connection.Payload.Type.*
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.processors.PublishProcessor
import io.reactivex.rxjava3.schedulers.Schedulers
import p.l.e.x.u.s.application.PlexusApp.Companion.log
import p.l.e.x.u.s.connection.nearby.NearbyApi

class PlexusRepository(private val appContext: Context) {
    private val nearbyApi by lazy { NearbyApi(appContext) }

    val isAdvertisingLiveData: LiveData<Boolean>
            by lazy { nearbyApi.isAdvertisingLiveData }

    private val _isDiscoveringLiveData: MutableLiveData<Boolean>
            by lazy { nearbyApi.isDiscoveringLiveData }
    val isDiscoveringLiveData: LiveData<Boolean>
        get() = _isDiscoveringLiveData

    private val _connectionAlertDialogLiveData =
        MutableLiveData<Triple<OnClickListener, OnClickListener, ConnectionInfo>>()
    val connectionAlertDialogLiveData: LiveData<Triple<OnClickListener, OnClickListener, ConnectionInfo>>
        get() = _connectionAlertDialogLiveData

    private val _coarseLocationAlertDialogLiveData = MutableLiveData<String>()
    val coarseLocationAlertDialogLiveData: LiveData<String>
        get() = _coarseLocationAlertDialogLiveData

    private val _showSendButtonLiveData = MutableLiveData<String>()
    val showSendButtonLiveData: LiveData<String>
        get() = _showSendButtonLiveData

    private val _showToastLiveData = MutableLiveData<String>()
    val showToastLiveData: LiveData<String>
        get() = _showToastLiveData

    // RxJava
    val compositeDisposable = CompositeDisposable()
    private val sendBytesToEndpointProcessor = PublishProcessor.create<Pair<String, String>>()
    private val advertiseProcessor = PublishProcessor.create<Boolean>()
    private val discoverProcessor = PublishProcessor.create<Boolean>()

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
            _connectionAlertDialogLiveData.postValue(
                Triple(
                    // positive button listener
                    OnClickListener {_,_ -> nearbyApi.acceptConnection(endpointId, payloadCallback) },
                    // negative button listener
                    OnClickListener{_,_ -> nearbyApi.rejectConnection(endpointId) },
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
            log("PlexusRepository.onDisconnected(): endpointId = $endpointId")
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            log("PlexusRepository.onPayloadReceived(): $endpointId payload = $payload")
            if (payload.type == BYTES) {
                // bytes payload received
                payload.asBytes()?.let { byteArray ->
                    val bytesString: String = byteArray.toString(Charsets.UTF_8)
                    log("PlexusRepository.onPayloadReceived(): BYTES bytesString = $bytesString")
                    _showToastLiveData.postValue(bytesString)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            log("PlexusRepository.onPayloadTransferUpdate(): $endpointId update = $update")
            val status = update.status
            log("PlexusRepository.onPayloadTransferUpdate(): status = $status")
            when (status) {
                SUCCESS -> { // 0
                    log("PlexusRepository.onPayloadTransferUpdate(): file or stream payload received")
                }
                ERROR -> { // 13
                    log("PlexusRepository.onPayloadTransferUpdate(): error receiving payload")
                }
                else -> {
                    log(
                        "PlexusRepository.onPayloadTransferUpdate(): status = $status"
                    )
                }
            }
        }
    }

    init {
        subscribeToSendBytesToEndpointProcessor()
        subscribeToAdvertiseProcessor()
        subscribeToDiscoverProcessor()
    }

    private fun subscribeToDiscoverProcessor() {
        compositeDisposable.add(
            discoverProcessor
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .distinctUntilChanged() // only once for the same value
                .switchMapCompletable { shouldDiscover: Boolean ->
                    log(
                        "PlexusRepository.subscribeToDiscoverProcessor(): " +
                                "shouldDiscover = $shouldDiscover"
                    )
                    if (shouldDiscover) {
                        log("PlexusRepository.subscribeToDiscoverProcessor(): start")
                        nearbyApi.startDiscovering(endpointDiscoveryCallback)
                    } else {
                        log("PlexusRepository.subscribeToDiscoverProcessor(): stop")
                        nearbyApi.stopDiscovering()
                    }
                }
                .retryWhen { throwableFlowable: Flowable<Throwable> ->
                    throwableFlowable
                        .flatMap { throwable: Throwable ->
                            when {
                                throwable is ApiException && throwable.message == ERROR_8034_MESSAGE -> {
                                    log(
                                        "PlexusRepository.subscribeToDiscoverProcessor(): " +
                                                "throwableMessage = ${throwable.message}"
                                    )
                                    Flowable
                                        .just(1)
                                        .doOnNext {
                                            // switch to stop discovering on UI
                                            _isDiscoveringLiveData.postValue(false)
                                            // show alert to enable GPS
                                            _coarseLocationAlertDialogLiveData.postValue(
                                                "Enable GPS to discover other devices"
                                            )
                                        }
                                }
                                // ???????????????????? ?????????????????? ????????????
                                else -> Flowable.error<Throwable>(throwable)
                            }
                        }
                }
                .subscribe(
                    {
                        log("PlexusRepository.subscribeToDiscoverProcessor(): completed")
                    },
                    { throwable ->
                        log("PlexusRepository.subscribeToDiscoverProcessor(): error = $throwable")
                        throwable.printStackTrace()
                    }
                )
        )
    }

    private fun subscribeToAdvertiseProcessor() {
        compositeDisposable.add(
            advertiseProcessor
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .distinctUntilChanged() // only once for the same value
                .switchMapCompletable { shouldAdvertise: Boolean ->
                    log(
                        "PlexusRepository.subscribeToAdvertiseProcessor(): " +
                                "shouldAdvertise = $shouldAdvertise"
                    )
                    if (shouldAdvertise) {
                        log("PlexusRepository.subscribeToAdvertiseProcessor(): start")
                        nearbyApi.startAdvertising(connectionLifecycleCallback)
                    } else {
                        log("PlexusRepository.subscribeToAdvertiseProcessor(): stop")
                        nearbyApi.stopAdvertising()
                    }
                }
                .subscribe(
                    {
                        log("PlexusRepository.subscribeToAdvertiseProcessor(): completed")
                    },
                    { throwable ->
                        log("PlexusRepository.subscribeToAdvertiseProcessor(): error = $throwable")
                        throwable.printStackTrace()
                    }
                )
        )
    }

    private fun subscribeToSendBytesToEndpointProcessor() {
        compositeDisposable.add(
            sendBytesToEndpointProcessor
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .switchMapCompletable { (endpointId: String, message: String) ->
                    log(
                        "PlexusRepository.subscribeToSendBytesToEndpointProcessor(): " +
                                "endpointId = $endpointId, message = $message"
                    )
                    nearbyApi.sendBytes(endpointId, message)
                }
                .subscribe(
                    {
                        log("PlexusRepository.sendBytes(): completed")
                    },
                    { throwable ->
                        log("PlexusRepository.sendBytes(): error = $throwable")
                        throwable.printStackTrace()
                    }
                )
        )
    }

    fun startAdvertising() {
        log("PlexusRepository.startAdvertising()")
        advertiseProcessor.onNext(true)
    }

    fun stopAdvertising() {
        log("PlexusRepository.stopAdvertising()")
        advertiseProcessor.onNext(false)
    }

    fun startDiscovering() {
        log("PlexusRepository.startDiscovering()")
        discoverProcessor.onNext(true)
    }

    fun stopDiscovering() {
        log("PlexusRepository.stopDiscovering()")
        discoverProcessor.onNext(false)
    }

    fun sendBytes(endpointId: String, message: String) {
        log("PlexusRepository.sendBytes(): endpointId = $endpointId, message = $message")
        sendBytesToEndpointProcessor.onNext(Pair(endpointId, message))
    }

    companion object {
        private const val ERROR_8034_MESSAGE = "8034: MISSING_PERMISSION_ACCESS_COARSE_LOCATION"
    }
}