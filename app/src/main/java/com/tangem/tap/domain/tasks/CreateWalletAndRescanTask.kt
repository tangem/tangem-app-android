package com.tangem.tap.domain.tasks

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.commands.CreateWalletCommand
import com.tangem.common.CompletionResult

class CreateWalletAndRescanTask : CardSessionRunnable<ScanNoteResponse> {
    override val requiresPin2 = false

    override fun run(session: CardSession, callback: (result: CompletionResult<ScanNoteResponse>) -> Unit) {
        CreateWalletCommand().run(session) { result ->
            when (result) {
                is CompletionResult.Success -> ScanNoteTask().run(session, callback)

                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}