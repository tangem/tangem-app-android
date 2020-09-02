package com.tangem.tap.domain.tasks

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.TangemSdkError
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.commands.Card
import com.tangem.commands.CardStatus
import com.tangem.commands.CommandResponse
import com.tangem.common.CompletionResult

data class ScanNoteResponse(
        val walletManager: WalletManager?,
        val card: Card
) : CommandResponse

class ScanNoteTask : CardSessionRunnable<ScanNoteResponse> {
    override val requiresPin2 = false

    override fun run(session: CardSession, callback: (result: CompletionResult<ScanNoteResponse>) -> Unit) {
        val card = session.environment.card
        callback(card.toScanNoteCompletionResult())
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