package `in`.caffeinelabs.cassettecat

import android.app.Application
import `in`.caffeinelabs.cassettecat.data.diagnostics.CrashLogRepository

class CassetteCatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogRepository(this).install()
    }
}
