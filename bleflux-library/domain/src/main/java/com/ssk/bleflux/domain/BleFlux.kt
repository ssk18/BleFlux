package com.ssk.bleflux.domain

import android.content.Context

/**
 * Main entry point for BleFlux library
 */
object BleFlux {
    
    /**
     * Initialize the BleFlux library with application context
     * Call this from your Application.onCreate()
     */
    fun initialize(context: Context) {
        BleFluxInitializer.initialize(context)
    }
    
    /**
     * Check if the library has been initialized
     */
    fun isInitialized(): Boolean = BleFluxInitializer.isInitialized()
    
    /**
     * Get the application context (only available after initialization)
     */
    val context: Context
        get() {
            if (!isInitialized()) {
                throw IllegalStateException("BleFlux must be initialized first. Call BleFlux.initialize(context) in your Application.onCreate()")
            }
            return BleFluxInitializer.applicationContext
        }
}