package com.tangem.commands.common.network

import com.tangem.Log
import kotlinx.coroutines.delay
import java.io.IOException

suspend fun <T> retryIO(
    times: Int = 3,
    initialDelay: Long = 100,
    maxDelay: Long = 1000,
    factor: Double = 2.0,
    block: suspend () -> T): T
{
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (e: IOException) {
            Log.i("Network", e.localizedMessage)
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return block()
}

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Failure(val error: Throwable?) : Result<Nothing>()
}

suspend fun <T>performRequest(block: suspend () -> T): Result<T> {
    return try {
        val result = retryIO { block() }
        Result.Success(result)
    } catch (exception: Exception) {
        Result.Failure(exception)
    }
}