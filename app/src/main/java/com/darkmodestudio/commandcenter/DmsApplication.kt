package com.darkmodestudio.commandcenter

import android.app.Application
import com.darkmodestudio.commandcenter.core.sync.DmsSyncWorker

class DmsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DmsSyncWorker.schedule(this, 15)
    }
}
