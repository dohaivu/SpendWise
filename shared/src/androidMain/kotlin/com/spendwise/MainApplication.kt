package com.spendwise

import android.app.Application
import co.touchlab.kermit.Logger
import com.spendwise.infrastructure.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent

class MainApplication: Application(), KoinComponent {

    companion object {
        lateinit var instance: MainApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidLogger()
            androidContext(this@MainApplication)
        }
        instance = this

        Logger.d("MainApplication onCreate")
    }
}