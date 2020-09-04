package com.tangem.tap.domain.tasks

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.TangemSdkError
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.commands.Card
import com.tangem.commands.CardStatus
import com.tangem.commands.CommandResponse
import com.tangem.commands.verifycard.VerifyCardCommand
import com.tangem.commands.verifycard.VerifyCardResponse
import com.tangem.common.CompletionResult

data class ScanNoteResponse(
        val walletManager: WalletManager?,
        val card: Card,
        val verifyResponse: VerifyCardResponse? = null
) : CommandResponse

class ScanNoteTask : CardSessionRunnable<ScanNoteResponse> {
    override val requiresPin2 = false

    override fun run(session: CardSession, callback: (result: CompletionResult<ScanNoteResponse>) -> Unit) {
        val card = session.environment.card
        card ?: return callback(CompletionResult.Failure(TangemSdkError.CardError()))
        if (card.status == CardStatus.Empty) {
            return callback(CompletionResult.Success(ScanNoteResponse(null, card)))
        }
        val walletManager = WalletManagerFactory.makeWalletManager(card)
                ?: return callback(CompletionResult.Failure(TangemSdkError.CardError()))

        VerifyCardCommand(true).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    callback(CompletionResult.Success(ScanNoteResponse(
                            walletManager, card, result.data
                    )))
                }
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(TangemSdkError.VerificationFailed()))
                }
            }
        }

    }
}

fun Card?.toScanNoteCompletionResult(): CompletionResult<ScanNoteResponse> {
    this ?: return CompletionResult.Failure(TangemSdkError.CardError())
    if (this.status == CardStatus.Empty) {
        return CompletionResult.Success(ScanNoteResponse(null, this))
    }
    val walletManager = WalletManagerFactory.makeWalletManager(this)
            ?: return CompletionResult.Failure(TangemSdkError.CardError())
    return CompletionResult.Success(ScanNoteResponse(walletManager, this))
}