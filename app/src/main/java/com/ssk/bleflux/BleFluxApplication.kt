package com.ssk.bleflux

import android.app.Application
import com.ssk.bleflux.domain.BleFlux
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BleFluxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BleFlux.initialize(this)
    }
}