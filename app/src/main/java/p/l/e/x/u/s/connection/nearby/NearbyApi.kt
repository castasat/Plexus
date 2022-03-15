package p.l.e.x.u.s.connection.nearby

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.nearby.connection.ConnectionsClient.MAX_BYTES_DATA_SIZE
import com.google.android.gms.nearby.connection.Payload.fromBytes
import com.google.android.gms.nearby.connection.Strategy.P2P_CLUSTER
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableEmitter
import p.l.e.x.u.s.application.PlexusApp.Companion.log

class NearbyApi(private val appContext: Context) {
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

    fun startAdvertising(connectionLifecycleCallback: ConnectionLifecycleCallback): Completable {
        log("NearbyApi.startAdvertising()")
        return Completable.create { emitter: CompletableEmitter ->
            Nearby
                .getConnectionsClient(appContext)
                .startAdvertising(
                    SERVICE_ID,
                    SERVICE_ID,
                    connectionLifecycleCallback,
                    AdvertisingOptions.Builder().setStrategy(P2P_CLUSTER).build()
                )
                .addOnSuccessListener { emitter.onComplete() }
                .addOnFailureListener { exception -> emitter.onError(exception) }
        }
    }

    fun stopAdvertising(): Completable {
        log("NearbyApi.stopAdvertising()")
        return Completable.fromAction {
            Nearby
                .getConnectionsClient(appContext)
                .stopAdvertising()
        }
    }

    fun startDiscovering(endpointDiscoveryCallback: EndpointDiscoveryCallback): Completable {
        log("NearbyApi.startDiscovering()")
        return Completable.create { emitter: CompletableEmitter ->
            Nearby
                .getConnectionsClient(appContext)
                .startDiscovery(
                    SERVICE_ID,
                    endpointDiscoveryCallback,
                    DiscoveryOptions.Builder().setStrategy(P2P_CLUSTER).build()
                )
                .addOnSuccessListener { emitter.onComplete() }
                .addOnFailureListener { exception -> emitter.onError(exception) }
        }
    }

    fun stopDiscovering(): Completable {
        log("NearbyApi.stopDiscovering()")
        return Completable.fromAction {
            Nearby
                .getConnectionsClient(appContext)
                .stopDiscovery()
        }
    }

    companion object {
        private const val SERVICE_ID = "p.l.e.x.u.s"
        private val bytes = "Hello".toByteArray(Charsets.UTF_8) // TODO
    }
}