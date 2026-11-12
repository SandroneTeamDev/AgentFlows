package sandrone.agentflows.android.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class EngineService : Service() {
    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return android.app.Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
