package com.ssk.bleflux.domain

import android.content.Context

object BleFluxInitializer {
    internal lateinit var applicationContext: Context
        private set

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    fun isInitialized(): Boolean = ::applicationContext.isInitialized
}