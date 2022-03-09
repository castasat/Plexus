package p.l.e.x.u.s

import android.app.Application
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel

class PlexusViewModel(application: Application) : AndroidViewModel(application) {
    private val broadcastReceiver = PlexusReceiver()
    private val plexusManager: WifiP2pManager? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        getApplication<Application>().applicationContext
            .getSystemService(AppCompatActivity.WIFI_P2P_SERVICE) as WifiP2pManager?
    }


    fun init() {
        with(getApplication<Application>().applicationContext) {
            // TODO
            val channel = plexusManager?.initialize(this, mainLooper, null)
            registerReceiver(
                broadcastReceiver,
                IntentFilter().apply {
                    addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
                    addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                    addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                    addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                    addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
                }
            )

        }
    }

    override fun onCleared() {
        getApplication<Application>().applicationContext.unregisterReceiver(broadcastReceiver)
        super.onCleared()
    }
}