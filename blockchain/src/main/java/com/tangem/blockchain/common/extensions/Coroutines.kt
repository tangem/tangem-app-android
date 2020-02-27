package com.tangem.blockchain.common.extensions

import com.tangem.CardManager
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.commands.SignResponse
import com.tangem.tasks.TaskEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun <T> retryIO(
        times: Int = 3,
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

class Signer(private val cardManager: CardManager) : TransactionSigner {
    override suspend fun sign(hashes: Array<ByteArray>, cardId: String): TaskEvent<SignResponse> = coroutineScope {
        async {
            suspendCancellableCoroutine<TaskEvent<SignResponse>> { continuation ->
                cardManager.sign(hashes, cardId) { if (continuation.isActive) continuation.resume(it) }
            }
        }.await()
    }
}
