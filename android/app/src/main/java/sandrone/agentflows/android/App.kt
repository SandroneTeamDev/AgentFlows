package sandrone.agentflows.android

import android.app.Application
import com.google.gson.Gson

class App : Application() {
    val gson: Gson by lazy { Gson() }
}
