package com.ssk.bleflux.domain.concurrency

import com.ssk.bleflux.domain.exceptions.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Buffer strategy for exception handling
 */
enum class ExceptionBufferStrategy {
    /**
     * Keeps only the latest exception, drops older ones if observers are slow.
     * Best for UI updates where you only care about the most recent error.
     */
    CONFLATED,
    
    /**
     * Unlimited buffer size. Can cause memory issues if observers are very slow.
     * Best for logging where you need all exceptions.
     */
    UNLIMITED,
    
    /**
     * Fixed buffer size with suspending send when full.
     * Best for balanced approach between memory and completeness.
     */
    BUFFERED,
    
    /**
     * No buffer, send suspends until received.
     * Best for strict backpressure control.
     */
    RENDEZVOUS
}

/**
 * Centralized exception handler for BLE operations
 */
class BleExceptionHandler(
    bufferStrategy: ExceptionBufferStrategy = ExceptionBufferStrategy.CONFLATED,
    bufferSize: Int = 16
) {
    
    private val _exceptionChannel = when (bufferStrategy) {
        ExceptionBufferStrategy.CONFLATED -> Channel<BleExceptionEvent>(Channel.CONFLATED)
        ExceptionBufferStrategy.UNLIMITED -> Channel<BleExceptionEvent>(Channel.UNLIMITED)
        ExceptionBufferStrategy.BUFFERED -> Channel<BleExceptionEvent>(bufferSize)
        ExceptionBufferStrategy.RENDEZVOUS -> Channel<BleExceptionEvent>(Channel.RENDEZVOUS)
    }
    
    /**
     * Flow of BLE exceptions for UI/logging observers
     * Uses CONFLATED channel to prevent memory issues with slow observers
     */
    val exceptionFlow: Flow<BleExceptionEvent> = _exceptionChannel.receiveAsFlow()

    /**
     * Handle timeout exceptions
     */
    fun handleTimeout(operation: String, timeoutMs: Long): OperationTimeoutException {
        val exception = BleExceptionMapper.createOperationTimeoutException(operation, timeoutMs)
        emitException(BleExceptionEvent.Timeout(operation, timeoutMs, exception))
        return exception
    }

    /**
     * Handle cancellation exceptions
     * 
     * NOTE: This should NEVER be called with CancellationException!
     * CancellationException must always be rethrown to preserve structured concurrency.
     * This method is only for business logic cancellations (user cancelled operation).
     */
    fun handleOperationCancellation(operation: String): OperationCancelledException {
        val exception = BleExceptionMapper.createOperationCancelledException(operation)
        emitException(BleExceptionEvent.Cancelled(operation, exception))
        return exception
    }

    /**
     * Handle unknown exceptions by wrapping them in BLE exceptions
     */
    fun handleUnknownException(operation: String, cause: Throwable): BleException {
        val exception = GenericBleException("Unknown error during operation '$operation': ${cause.message}", cause)
        emitException(BleExceptionEvent.Unknown(operation, cause, exception))
        return exception
    }

    /**
     * Handle BLE-specific exceptions
     */
    fun handleBleException(operation: String, exception: BleException) {
        val event = when (exception) {
            is BleConnectionException -> BleExceptionEvent.Connection(operation, exception)
            is BleGattException -> BleExceptionEvent.Gatt(operation, exception)
            is BleScanException -> BleExceptionEvent.Scan(operation, exception)
            is BleProtocolException -> BleExceptionEvent.Protocol(operation, exception)
            else -> BleExceptionEvent.General(operation, exception)
        }
        emitException(event)
    }

    /**
     * Handle uncaught exceptions from coroutine exception handler
     */
    fun handleUncaughtException(exception: Throwable) {
        val bleException = when (exception) {
            is BleException -> exception
            else -> GenericBleException("Uncaught exception: ${exception.message}", exception)
        }
        emitException(BleExceptionEvent.Uncaught(bleException))
    }

    private fun emitException(event: BleExceptionEvent) {
        _exceptionChannel.trySend(event)
    }
    
    /**
     * Close the exception channel when handler is no longer needed
     */
    fun close() {
        _exceptionChannel.close()
    }
}

/**
 * Sealed class representing different types of BLE exception events
 */
sealed class BleExceptionEvent {
    abstract val exception: BleException
    abstract val timestamp: Long
    
    data class Connection(
        val operation: String,
        override val exception: BleConnectionException,
        override val timestamp: Long = System.currentTimeMillis()
    ) : BleExceptionEvent()
    
    data class Gatt(
        val operation: String,
        override val exception: BleGattException,
        override val timestamp: Long = System.currentTimeMillis()
    ) : BleExceptionEvent()
    
    data class Scan(
        val operation: String,
        override val exception: BleScanException,
        override val timestamp: Long = System.currentTimeMillis()
    ) : BleExceptionEvent()
    
    data class Protocol(
        val operation: String,
        override val exception: BleProtocolException,
        override val timestamp: Long = System.currentTimeMillis()
    ) : BleExceptionEvent()
    
    data class Timeout(
        val operation: String,
        val timeoutMs: Long,
        override val exception: OperationTimeoutException,
        override val timestamp: Long = System.currentTimeMillis()
    ) : BleExceptionEvent()
    
    data class Cancelled(
        val operation: String,
        override val exception: OperationCancelledException,
        override val timestamp: Long = System.currentTimeMillis()
    ) : BleExceptionEvent()
    
    data class Unknown(
        val operation: String,
        val cause: Throwable,
        override val exception: BleException,
        override val timestamp: Long = System.currentTimeMillis()
    ) : BleExceptionEvent()
    
    data class General(
        val operation: String,
        override val exception: BleException,
        override val timestamp: Long = System.currentTimeMillis()
    ) : BleExceptionEvent()
    
    data class Uncaught(
        override val exception: BleException,
        override val timestamp: Long = System.currentTimeMillis()
    ) : BleExceptionEvent()
}