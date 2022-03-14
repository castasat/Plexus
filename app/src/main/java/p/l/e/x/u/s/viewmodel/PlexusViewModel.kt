package p.l.e.x.u.s.viewmodel

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.app.Application
import android.os.Build.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.nearby.connection.ConnectionsClient.MAX_BYTES_DATA_SIZE
import p.l.e.x.u.s.application.PlexusApp.Companion.log
import p.l.e.x.u.s.repository.PlexusRepository

class PlexusViewModel(application: Application) : AndroidViewModel(application) {
    val showAlertDialogLiveData by lazy { repository.showAlertDialogLiveData }
    val showSendButtonLiveData by lazy { repository.showSendButtonLiveData }
    val showToastLiveData by lazy { repository.showToastLiveData }

    @SuppressLint("StaticFieldLeak")
    private val appContext = getApplication<Application>().applicationContext

    private val repository by lazy { PlexusRepository(appContext) }

    private val _requestRuntimePermissionLiveData = MutableLiveData<Array<String>>()
    val requestRuntimePermissionLiveData: LiveData<Array<String>>
        get() = _requestRuntimePermissionLiveData

    private val permissions =
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

    fun init() = requestRuntimePermissionCheck(permissions)

    private fun requestRuntimePermissionCheck(permissions: Array<String>) =
        _requestRuntimePermissionLiveData.postValue(permissions)

    fun advertise() {
        log("PlexusViewModel.advertise()")
        repository.advertise()
    }

    fun discover() {
        log("PlexusViewModel.discover()")
        repository.discover()
    }

    fun send(endpointId: String) {
        log("PlexusViewModel.send()")
        sendBytes(endpointId)
    }

    private fun sendBytes(endpointId: String) {
        log("PlexusViewModel.sendBytes()")
        // TODO
        val bytes = "Hello".toByteArray(Charsets.UTF_8)
        if (bytes.size < MAX_BYTES_DATA_SIZE) {
            Nearby
                .getConnectionsClient(appContext)
                .sendPayload(
                    endpointId,
                    Payload.fromBytes(bytes)
                )
        } else {
            log("PlexusViewModel.sendBytes(): bytesSize >= MAX_BYTES_DATA_SIZE (32768)")
            // TODO split and rearrange bytes chunks
        }
    }

    companion object {
        const val CODE: Int = 0xC0D3
    }
}