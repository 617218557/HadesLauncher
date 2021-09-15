package com.fc.hadeslauncher

import android.app.Application
import com.hjq.permissions.XXPermissions

class MyApplication : Application() {
    companion object {
        lateinit var application: Application
    }

    override fun onCreate() {
        super.onCreate()
        application = this
        XXPermissions.setScopedStorage(false)
    }
}