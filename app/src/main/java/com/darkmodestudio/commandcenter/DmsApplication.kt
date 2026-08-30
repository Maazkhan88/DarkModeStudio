package com.darkmodestudio.commandcenter

import android.app.Application
import com.darkmodestudio.commandcenter.core.sync.DmsSyncWorker

class DmsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            DmsSyncWorker.schedule(this, 15)
        } catch (_: Exception) {
            // Avoid throwing during Robolectric unit test initialization
        }
    }
}
