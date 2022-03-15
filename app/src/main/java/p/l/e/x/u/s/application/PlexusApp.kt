package p.l.e.x.u.s.application

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.multidex.MultiDex

class PlexusApp : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    companion object {
        private const val TAG = "p.l.e.x.u.s"
        fun log(message: CharSequence) {
            Log.d(TAG, message.toString())
        }
    }
}