package p.l.e.x.u.s.connection.nearby

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.nearby.connection.ConnectionsClient.MAX_BYTES_DATA_SIZE
import com.google.android.gms.nearby.connection.Payload.fromBytes
import com.google.android.gms.nearby.connection.Strategy.P2P_CLUSTER
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableEmitter
import io.reactivex.rxjava3.schedulers.Schedulers
import p.l.e.x.u.s.application.PlexusApp.Companion.log
import java.util.concurrent.TimeUnit.MINUTES

class NearbyApi(private val appContext: Context) {
    private val _isAdvertisingLiveData = MutableLiveData<Boolean>()
    val isAdvertisingLiveData: LiveData<Boolean>
        get() = _isAdvertisingLiveData
    val isDiscoveringLiveData = MutableLiveData<Boolean>()

    fun sendBytes(endpointId: String): Completable = Completable.fromCallable {
        if (bytes.size < MAX_BYTES_DATA_SIZE) {
            Nearby
                .getConnectionsClient(appContext)
                .sendPayload(endpointId, fromBytes(bytes))
        } else {
            log("NearbyApi.sendBytes(): bytesSize >= MAX_BYTES_DATA_SIZE (32768)")
        }
    }

    fun requestConnection(
        endpointId: String,
        connectionLifecycleCallback: ConnectionLifecycleCallback
    ): Completable =
        Completable.create { emitter: CompletableEmitter ->
            Nearby
                .getConnectionsClient(appContext)
                .requestConnection(
                    SERVICE_ID,
                    endpointId,
                    connectionLifecycleCallback
                )
                .addOnSuccessListener { emitter.onComplete() }
                .addOnFailureListener { exception -> emitter.onError(exception) }
        }

    fun startAdvertising(connectionLifecycleCallback: ConnectionLifecycleCallback): Completable =
        Completable
            .create { emitter: CompletableEmitter ->
                log("NearbyApi.startAdvertising()")
                Nearby
                    .getConnectionsClient(appContext)
                    .startAdvertising(
                        SERVICE_ID,
                        SERVICE_ID,
                        connectionLifecycleCallback,
                        AdvertisingOptions.Builder().setStrategy(P2P_CLUSTER).build()
                    )
                    .addOnSuccessListener {
                        log("NearbyApi.startAdvertising(): completed")
                        emitter.onComplete()
                        _isAdvertisingLiveData.postValue(true)
                    }
                    .addOnFailureListener { exception -> emitter.onError(exception) }
            }
            .delay(MAX_ADVERTISING_INTERVAL_MINUTES, MINUTES, Schedulers.io())
            .andThen(stopAdvertising())

    fun stopAdvertising(): Completable = Completable.fromCallable {
        log("NearbyApi.stopAdvertising()")
        Nearby
            .getConnectionsClient(appContext)
            .stopAdvertising()
        _isAdvertisingLiveData.postValue(false)
    }

    fun startDiscovering(endpointDiscoveryCallback: EndpointDiscoveryCallback): Completable =
        Completable
            .create { emitter: CompletableEmitter ->
                log("NearbyApi.startDiscovering()")
                Nearby
                    .getConnectionsClient(appContext)
                    .startDiscovery(
                        SERVICE_ID,
                        endpointDiscoveryCallback,
                        DiscoveryOptions.Builder().setStrategy(P2P_CLUSTER).build()
                    )
                    .addOnSuccessListener {
                        log("NearbyApi.startDiscovering(): completed")
                        emitter.onComplete()
                        isDiscoveringLiveData.postValue(true)
                    }
                    .addOnFailureListener { exception -> emitter.onError(exception) }
            }
            .delay(MAX_DISCOVERING_INTERVAL_MINUTES, MINUTES, Schedulers.io())
            .andThen { stopDiscovering() }

    fun stopDiscovering(): Completable = Completable.fromCallable {
        log("NearbyApi.stopDiscovering()")
        Nearby
            .getConnectionsClient(appContext)
            .stopDiscovery()
        isDiscoveringLiveData.postValue(false)
    }

    companion object {
        private const val SERVICE_ID = "p.l.e.x.u.s"
        private val bytes = "Hello".toByteArray(Charsets.UTF_8) // TODO
        private const val MAX_ADVERTISING_INTERVAL_MINUTES = 5L
        private const val MAX_DISCOVERING_INTERVAL_MINUTES = 5L
    }
}