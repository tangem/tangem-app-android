package com.tangem.tap.domain.tasks

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.commands.ReadCommand
import com.tangem.common.CompletionResult
import com.tangem.tasks.CreateWalletTask

class CreateWalletAndRescanTask : CardSessionRunnable<ScanNoteResponse> {
    override val requiresPin2 = false

    override fun run(session: CardSession, callback: (result: CompletionResult<ScanNoteResponse>) -> Unit) {
        CreateWalletTask().run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    ReadCommand().run(session) { readResult ->
                        when (readResult) {
                            is CompletionResult.Success -> ScanNoteTask(readResult.data).run(session, callback)
                            is CompletionResult.Failure -> callback(CompletionResult.Failure(readResult.error))
                        }
                    }
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}