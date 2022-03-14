package p.l.e.x.u.s.application

import android.app.Application
import android.util.Log

class PlexusApp : Application() {
    companion object {
        private const val TAG = "p.l.e.x.u.s"
        fun log(message: CharSequence) {
            Log.d(TAG, message.toString())
        }
    }
}