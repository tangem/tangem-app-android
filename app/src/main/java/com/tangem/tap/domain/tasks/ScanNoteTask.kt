package com.tangem.tap.domain.tasks

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.TangemError
import com.tangem.TangemSdkError
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.commands.Card
import com.tangem.commands.CardStatus
import com.tangem.commands.CommandResponse
import com.tangem.commands.Product
import com.tangem.commands.verifycard.VerifyCardCommand
import com.tangem.commands.verifycard.VerifyCardResponse
import com.tangem.common.CompletionResult
import com.tangem.tap.domain.TapSdkError
import com.tangem.tasks.ScanTask

data class ScanNoteResponse(
        val walletManager: WalletManager?,
        val card: Card,
        val verifyResponse: VerifyCardResponse? = null
) : CommandResponse

class ScanNoteTask(val card: Card? = null) : CardSessionRunnable<ScanNoteResponse> {
    override val requiresPin2 = false

    override fun run(session: CardSession, callback: (result: CompletionResult<ScanNoteResponse>) -> Unit) {
        ScanTask().run(session) { result ->
            when (result) {
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))

                is CompletionResult.Success -> {
                    val card = this.card ?: result.data

                    val error = getErrorIfExcludedCard(card)
                    if (error != null) {
                        callback(CompletionResult.Failure(error))
                        return@run
                    }

                    val walletManager = try {
                        WalletManagerFactory.makeWalletManager(card)
                    } catch (exception: Exception) {
                        return@run callback(CompletionResult.Success(ScanNoteResponse(null, card)))
                    }
                    VerifyCardCommand(true).run(session) { verifyResult ->
                        when (verifyResult) {
                            is CompletionResult.Success -> {
                                callback(CompletionResult.Success(ScanNoteResponse(
                                        walletManager, card, verifyResult.data
                                )))
                            }
                            is CompletionResult.Failure -> {
                                callback(CompletionResult.Failure(TangemSdkError.VerificationFailed()))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getErrorIfExcludedCard(card: Card): TangemError? {
        if (card.cardData?.productMask?.contains(Product.Note) != true) {
            return TapSdkError.CardForDifferentApp
        }
        if (excludedBatches.contains(card.cardData?.batchId)) {
            return TapSdkError.CardForDifferentApp
        }
        if (card.status == CardStatus.Purged) return TangemSdkError.CardIsPurged()
        if (card.status == CardStatus.NotPersonalized) return TangemSdkError.NotPersonalized()

        return null
    }

    companion object {
        private val excludedBatches = listOf("0027", "0030", "0031")
    }
}