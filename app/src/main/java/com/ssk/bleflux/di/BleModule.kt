package com.ssk.bleflux.di

import android.content.Context
import com.ssk.bleflux.data.connection.BleConnectionImpl
import com.ssk.bleflux.data.connection.BleGattProviderImpl
import com.ssk.bleflux.data.repository.BleRepositoryImpl
import com.ssk.bleflux.data.scan.BleAdapterProviderImpl
import com.ssk.bleflux.data.scan.BlePermissionCheckerImpl
import com.ssk.bleflux.domain.connection.BleConnection
import com.ssk.bleflux.domain.connection.BleGattProvider
import com.ssk.bleflux.domain.repository.BleRepository
import com.ssk.bleflux.domain.scan.BleAdapterProvider
import com.ssk.bleflux.domain.scan.BlePermissionChecker
import com.ssk.bleflux.domain.scan.BleScanner
import com.ssk.bleflux.domain.scan.BleScanExceptionHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BleModule {
    
    @Provides
    @Singleton
    fun provideBleAdapterProvider(@ApplicationContext context: Context): BleAdapterProvider = 
        BleAdapterProviderImpl(context)
    
    @Provides
    @Singleton
    fun provideBlePermissionChecker(@ApplicationContext context: Context): BlePermissionChecker = 
        BlePermissionCheckerImpl(context)
    
    @Provides
    @Singleton
    fun provideBleScanExceptionHandler(@ApplicationContext context: Context): BleScanExceptionHandler = 
        BleScanExceptionHandler(context)
    
    @Provides
    @Singleton
    fun provideBleScanner(
        bleAdapterProvider: BleAdapterProvider,
        permissionChecker: BlePermissionChecker,
        scanExceptionHandler: BleScanExceptionHandler
    ): BleScanner = BleScanner(bleAdapterProvider, permissionChecker, scanExceptionHandler)
    
    @Provides
    @Singleton
    fun provideBleGattProvider(): BleGattProvider = BleGattProviderImpl()
    
    @Provides
    @Singleton
    fun provideBleConnection(
        @ApplicationContext context: Context,
        gattProvider: BleGattProvider
    ): BleConnection = BleConnectionImpl(context, gattProvider)
    
    @Provides
    @Singleton
    fun provideBleRepository(
        bleScanner: BleScanner,
        bleConnection: BleConnection,
        permissionChecker: BlePermissionChecker
    ): BleRepository = BleRepositoryImpl(bleScanner, bleConnection, permissionChecker)
}