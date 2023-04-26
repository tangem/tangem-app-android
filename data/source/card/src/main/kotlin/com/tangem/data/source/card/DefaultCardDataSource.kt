package com.tangem.data.source.card

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.TangemSdk
import com.tangem.common.CompletionResult
import com.tangem.data.source.card.model.Blockchain
import com.tangem.data.source.card.model.ScanException
import com.tangem.data.source.card.model.ScanResult
import com.tangem.data.source.card.task.ScanCardTask
import com.tangem.data.source.card.utils.toScanException
import com.tangem.store.preferences.PreferencesStore
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import com.tangem.blockchain.common.Blockchain.Companion as SdkBlockchain

internal class DefaultCardDataSource @Inject constructor(
    private val preferencesStore: PreferencesStore,
    private val tangemSdk: TangemSdk,
) : CardDataSource {
    override suspend fun scanCard(
        blockchainsToDerive: List<Blockchain>,
        allowsRequestAccessCodeFromStorage: Boolean,
    ): Either<ScanException, ScanResult> {
        return when (val result = startScanTask(blockchainsToDerive, allowsRequestAccessCodeFromStorage)) {
            is CompletionResult.Success -> result.data.right()
            is CompletionResult.Failure -> result.error.toScanException().left()
        }
    }

    private suspend fun startScanTask(
        blockchainsToDerive: List<Blockchain>,
        allowsRequestAccessCodeFromRepository: Boolean,
    ): CompletionResult<ScanResult> {
        val task = ScanCardTask(
            blockchainsToDerive.map { SdkBlockchain.fromId(it.id) }.toSet(),
            preferencesStore,
            allowsRequestAccessCodeFromRepository,
        )

        return suspendCancellableCoroutine { continuation ->
            tangemSdk.startSessionWithRunnable(task) { result ->
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }
        }
    }
}
