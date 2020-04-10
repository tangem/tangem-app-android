package com.tangem.tasks

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.SessionError
import com.tangem.commands.CardStatus
import com.tangem.commands.CheckWalletCommand
import com.tangem.commands.CreateWalletCommand
import com.tangem.commands.CreateWalletResponse
import com.tangem.common.CompletionResult

class CreateWalletTask : CardSessionRunnable<CreateWalletResponse> {

    override fun run(session: CardSession, callback: (result: CompletionResult<CreateWalletResponse>) -> Unit) {
        val curve = session.environment.card?.curve
        if (curve == null) {
            callback(CompletionResult.Failure(SessionError.CardError()))
            return
        }

        val command = CreateWalletCommand()
        command.run(session) { createWalletResult ->
            when (createWalletResult) {
                is CompletionResult.Failure -> callback(createWalletResult)
                is CompletionResult.Success -> {
                    if (createWalletResult.data.status != CardStatus.Loaded) {
                        callback(CompletionResult.Failure(SessionError.UnknownError()))
                    } else {
                        val checkWalletCommand = CheckWalletCommand(
                                curve, createWalletResult.data.walletPublicKey
                        )
                        checkWalletCommand.run(session) { result ->
                            when (result) {
                                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
                                is CompletionResult.Success -> callback(createWalletResult)
                            }
                        }
                    }
                }
            }
        }
    }
}