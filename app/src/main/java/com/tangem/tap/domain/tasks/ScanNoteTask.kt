package com.tangem.tap.domain.tasks

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.TangemSdkError
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.commands.Card
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
        val walletManager = try {
            WalletManagerFactory.makeWalletManager(card)
        } catch (exception: Exception) {
            return callback(CompletionResult.Success(ScanNoteResponse(null, card)))
        }
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
    val walletManager = try {
        WalletManagerFactory.makeWalletManager(this)
    } catch (exception: Exception) {
        return CompletionResult.Success(ScanNoteResponse(null, this))
    }
    return CompletionResult.Success(ScanNoteResponse(walletManager, this))
}