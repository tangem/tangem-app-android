package com.tangem.data.source.card.task

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.data.source.card.model.ScanException
import com.tangem.data.source.card.model.ScanExceptionWrapper
import com.tangem.data.source.card.model.ScanResult
import com.tangem.data.source.card.task.processor.ScanTwinProcessor
import com.tangem.data.source.card.task.processor.ScanWalletProcessor
import com.tangem.data.source.card.utils.isExcluded
import com.tangem.data.source.card.utils.isNotSupportedInThatRelease
import com.tangem.data.source.card.utils.isTangemTwins
import com.tangem.operations.ScanTask
import com.tangem.store.preferences.PreferencesStore

internal class ScanCardTask(
    private val blockchainsToDerive: Set<Blockchain>,
    private val preferencesStore: PreferencesStore,
    override val allowsRequestAccessCodeFromRepository: Boolean,
) : CardSessionRunnable<ScanResult> {

    override fun run(session: CardSession, callback: (result: CompletionResult<ScanResult>) -> Unit) {
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }

        val error = getErrorIfExcludedCard(card)?.let(::ScanExceptionWrapper)
        if (error != null) {
            callback(CompletionResult.Failure(error))
            return
        }

        val commandProcessor = when {
            card.isTangemTwins -> ScanTwinProcessor()
            else -> ScanWalletProcessor(blockchainsToDerive, preferencesStore)
        }
        commandProcessor.proceed(card, session) { processorResult ->
            when (processorResult) {
                is CompletionResult.Success -> ScanTask().run(session) { scanTaskResult ->
                    when (scanTaskResult) {
                        is CompletionResult.Success -> {
                            // it needs because processorResult.data.card doesn't contains attestation result
                            // and CardWallet.derivedKeys
                            val processorScanResponseWithNewCard = processorResult.data.copy(
                                card = scanTaskResult.data,
                            )
                            callback(CompletionResult.Success(processorScanResponseWithNewCard))
                        }
                        is CompletionResult.Failure -> callback(CompletionResult.Failure(scanTaskResult.error))
                    }
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(processorResult.error))
            }
        }
    }

    private fun getErrorIfExcludedCard(card: Card): ScanException? {
        return when {
            card.isExcluded -> ScanException.CardForDifferentApp
            card.isNotSupportedInThatRelease -> ScanException.CardNotSupportedByRelease
            else -> null
        }
    }
}
