package com.tangem.blockchain.common.extensions

import kotlinx.coroutines.delay
import java.io.IOException

suspend fun <T> retryIO(
        times: Int = Int.MAX_VALUE,
        initialDelay: Long = 100,
        maxDelay: Long = 1000,
        factor: Double = 2.0,
        block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (e: IOException) {


        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return block() // last attempt
}


//suspend fun <T: Any> handleRequest(requestFunc: suspend () -> T): Result<T> {
//    return try {
//        Result.success(requestFunc.invoke())
//    } catch (he: HttpException) {
//        Result.failure(he)
////        HttpException
////        SocketTimeoutException
////        IOException
//    }
//}


sealed class Result<out T : Any> {
    data class Success<out T : Any>(val data: T) : Result<T>()
    data class Failure(val error: Throwable?) : Result<Nothing>()
}

sealed class SimpleResult {
    object Success : SimpleResult()
    data class Failure(val error: Throwable?) : SimpleResult()
}

