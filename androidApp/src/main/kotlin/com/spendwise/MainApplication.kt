package com.spendwise

import android.app.Application
import android.util.Log
import com.spendwise.android.BuildConfig
import com.spendwise.domain.AppConfig
import com.spendwise.infrastructure.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.dsl.module

class MainApplication: Application(), KoinComponent {

    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(module {
                single { AppConfig(versionName = BuildConfig.VERSION_NAME) }
            })
        }

        Log.d("Spendwise", "MainApplication onCreate")
    }
}