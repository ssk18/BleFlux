package com.ssk.bleflux

import android.app.Application
import com.ssk.bleflux.domain.BleFlux

// @HiltAndroidApp
class BleFluxApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize BleFlux Library with application context
        BleFlux.initialize(this)
    }
}