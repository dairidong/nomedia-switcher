package com.nomedia.switcher

import android.app.Application
import androidx.work.Configuration

class NoMediaApplication : Application(), Configuration.Provider {
    val appContainer: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        appContainer
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(appContainer.toggleWorkerFactory)
            .build()
}
