package com.tangem.tap.domain.tasks

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.TangemSdkError
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.commands.CommandResponse
import com.tangem.common.CompletionResult

data class ScanNoteResponse(val walletManager: WalletManager) : CommandResponse

class ScanNoteTask : CardSessionRunnable<ScanNoteResponse> {
    override val requiresPin2 = false

    override fun run(session: CardSession, callback: (result: CompletionResult<ScanNoteResponse>) -> Unit) {
        val walletManager = session.environment.card?.let { WalletManagerFactory.makeWalletManager(it) }
        if (walletManager == null) {
            callback(CompletionResult.Failure(TangemSdkError.CardError()))
            return
        }
        callback(CompletionResult.Success(ScanNoteResponse(walletManager)))
    }
}