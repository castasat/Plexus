package p.l.e.x.u.s

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import p.l.e.x.u.s.PlexusApp.Companion.log

class PlexusReceiver : BroadcastReceiver() {
    private val _actionFlow = MutableSharedFlow<PlexusAction>()
    val actionFlow: SharedFlow<PlexusAction> = _actionFlow

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.action?.let { action ->
            when (action) {
                WIFI_P2P_DISCOVERY_CHANGED_ACTION ->
                    _actionFlow.tryEmit(PlexusAction.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
                WIFI_P2P_PEERS_CHANGED_ACTION ->
                    _actionFlow.tryEmit(PlexusAction.WIFI_P2P_PEERS_CHANGED_ACTION)
                WIFI_P2P_STATE_CHANGED_ACTION ->
                    _actionFlow.tryEmit(PlexusAction.WIFI_P2P_STATE_CHANGED_ACTION)
                WIFI_P2P_CONNECTION_CHANGED_ACTION ->
                    _actionFlow.tryEmit(PlexusAction.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                WIFI_P2P_THIS_DEVICE_CHANGED_ACTION ->
                    _actionFlow.tryEmit(PlexusAction.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
                else -> {
                    log("PlexusReceiver.onReceive(): unknown action")
                }
            }
        }
    }
}