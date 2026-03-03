package com.resolve.notification.studio

import android.app.Application
import timber.log.Timber

class NotificationStudioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}
