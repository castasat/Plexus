package p.l.e.x.u.s.viewmodel

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.app.Application
import android.os.Build.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import p.l.e.x.u.s.repository.PlexusRepository

class PlexusViewModel(application: Application) : AndroidViewModel(application) {
    @SuppressLint("StaticFieldLeak")
    private val appContext = getApplication<Application>().applicationContext
    private val repository by lazy { PlexusRepository(appContext) }

    val showAlertDialogLiveData by lazy { repository.showAlertDialogLiveData }
    val showSendButtonLiveData by lazy { repository.showSendButtonLiveData }
    val showToastLiveData by lazy { repository.showToastLiveData }

    private val _requestRuntimePermissionLiveData = MutableLiveData<Array<String>>()
    val requestRuntimePermissionLiveData: LiveData<Array<String>>
        get() = _requestRuntimePermissionLiveData

    fun init() = requestRuntimePermissionCheck(permissions)

    private fun requestRuntimePermissionCheck(permissions: Array<String>) =
        _requestRuntimePermissionLiveData.postValue(permissions)

    fun advertise() = repository.advertise()
    fun discover() = repository.discover()
    fun send(endpointId: String) = sendBytes(endpointId)
    private fun sendBytes(endpointId: String) = repository.sendBytes(endpointId)

    override fun onCleared() {
        super.onCleared()
        repository.compositeDisposable.clear()
    }

    companion object {
        const val CODE: Int = 0xC0D3
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
    }
}