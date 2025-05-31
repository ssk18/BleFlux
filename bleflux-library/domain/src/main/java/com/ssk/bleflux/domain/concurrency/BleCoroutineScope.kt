package com.ssk.bleflux.domain.concurrency

import com.ssk.bleflux.domain.exceptions.BleException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlin.coroutines.CoroutineContext

/**
 * Structured concurrency wrapper for BLE operations with proper exception handling
 */
class BleCoroutineScope(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val exceptionHandler: BleExceptionHandler = BleExceptionHandler(ExceptionBufferStrategy.CONFLATED)
) {
    
    private val supervisorJob = SupervisorJob()
    
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
        exceptionHandler.handleUncaughtException(exception)
    }
    
    val scope = CoroutineScope(
        dispatcher + supervisorJob + coroutineExceptionHandler
    )

    /**
     * Execute a suspending BLE operation with proper exception handling
     */
    suspend fun <T> execute(
        operation: String,
        timeoutMs: Long = 30_000L,
        block: suspend CoroutineScope.() -> T
    ): Result<T> = withContext(scope.coroutineContext) {
        try {
            val result = withTimeout(timeoutMs) {
                block()
            }
            Result.success(result)
        } catch (e: TimeoutCancellationException) {
            val timeoutException = exceptionHandler.handleTimeout(operation, timeoutMs)
            Result.failure(timeoutException)
        } catch (e: CancellationException) {
            // CRITICAL: Never catch and wrap CancellationException!
            // It breaks structured concurrency - always rethrow
            throw e
        } catch (e: BleException) {
            Result.failure(e)
        } catch (e: Exception) {
            val mappedException = exceptionHandler.handleUnknownException(operation, e)
            Result.failure(mappedException)
        }
    }

    /**
     * Execute a suspending BLE operation and throw exceptions directly
     */
    suspend fun <T> executeOrThrow(
        operation: String,
        timeoutMs: Long = 30_000L,
        block: suspend CoroutineScope.() -> T
    ): T = withContext(scope.coroutineContext) {
        try {
            withTimeout(timeoutMs) {
                block()
            }
        } catch (e: TimeoutCancellationException) {
            throw exceptionHandler.handleTimeout(operation, timeoutMs)
        } catch (e: CancellationException) {
            // CRITICAL: Never catch and wrap CancellationException!
            // It breaks structured concurrency - always rethrow
            throw e
        } catch (e: BleException) {
            throw e
        } catch (e: Exception) {
            throw exceptionHandler.handleUnknownException(operation, e)
        }
    }

    /**
     * Launch a BLE operation asynchronously with structured concurrency
     */
    fun launch(
        operation: String,
        block: suspend CoroutineScope.() -> Unit
    ): Job = scope.launch {
        try {
            block()
        } catch (e: CancellationException) {
            // CRITICAL: Never catch CancellationException in launch!
            // It breaks structured concurrency - always rethrow
            throw e
        } catch (e: BleException) {
            exceptionHandler.handleBleException(operation, e)
        } catch (e: Exception) {
            val mappedException = exceptionHandler.handleUnknownException(operation, e)
            exceptionHandler.handleBleException(operation, mappedException)
        }
    }

    /**
     * Create an async operation with structured concurrency
     */
    fun <T> async(
        operation: String,
        block: suspend CoroutineScope.() -> T
    ): Deferred<T> = scope.async {
        try {
            block()
        } catch (e: CancellationException) {
            // CRITICAL: Never catch CancellationException in async!
            // It breaks structured concurrency - always rethrow
            throw e
        } catch (e: BleException) {
            exceptionHandler.handleBleException(operation, e)
            throw e
        } catch (e: Exception) {
            val mappedException = exceptionHandler.handleUnknownException(operation, e)
            exceptionHandler.handleBleException(operation, mappedException)
            throw mappedException
        }
    }

    /**
     * Wrap a Flow with proper exception handling
     */
    fun <T> Flow<T>.withBleExceptionHandling(operation: String): Flow<T> = 
        this.flowOn(dispatcher)
            .catch { exception ->
                when (exception) {
                    is CancellationException -> {
                        // CRITICAL: Never catch CancellationException in Flow.catch!
                        // It breaks structured concurrency - always rethrow
                        throw exception
                    }
                    is BleException -> {
                        exceptionHandler.handleBleException(operation, exception)
                        throw exception
                    }
                    else -> {
                        val mappedException = exceptionHandler.handleUnknownException(operation, exception)
                        exceptionHandler.handleBleException(operation, mappedException)
                        throw mappedException
                    }
                }
            }

    /**
     * Cancel all operations and clean up resources
     */
    fun cancel() {
        supervisorJob.cancel()
        exceptionHandler.close()
    }

    /**
     * Check if the scope is active
     */
    val isActive: Boolean
        get() = supervisorJob.isActive
}