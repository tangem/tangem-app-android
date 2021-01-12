package com.tangem.tap.domain.twins

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.Message
import com.tangem.commands.CreateWalletResponse
import com.tangem.commands.PurgeWalletCommand
import com.tangem.commands.common.card.CardStatus
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.hexToBytes
import com.tangem.tasks.CreateWalletTask

class CreateSecondTwinWalletTask(
        private val firstPublicKey: String,
        private val preparingMessage: Message,
        private val creatingWalletMessage: Message
) : CardSessionRunnable<CreateWalletResponse> {
    override val requiresPin2 = true

    override fun run(session: CardSession, callback: (result: CompletionResult<CreateWalletResponse>) -> Unit) {
        if (session.environment.card?.walletPublicKey != null) {
            session.setInitialMessage(preparingMessage)
            PurgeWalletCommand().run(session) { response ->
                when (response) {
                    is CompletionResult.Success -> {
                        session.environment.card =
                                session.environment.card?.copy(status = CardStatus.Empty)
                        finishTask(session, callback)
                    }
                    is CompletionResult.Failure -> callback(CompletionResult.Failure(response.error))
                }
            }
        } else {
            finishTask(session, callback)
        }
    }

    private fun finishTask(session: CardSession, callback: (result: CompletionResult<CreateWalletResponse>) -> Unit) {
        session.setInitialMessage(creatingWalletMessage)
        CreateWalletTask().run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    session.environment.card =
                            session.environment.card?.copy(status = CardStatus.Loaded)
                    WriteProtectedIssuerDataTask(
                            firstPublicKey.hexToBytes(), TwinCardsManager.issuerKeys
                    ).run(session) { writeResult ->
                        when (writeResult) {
                            is CompletionResult.Success -> callback(result)
                            is CompletionResult.Failure ->
                                callback(CompletionResult.Failure(writeResult.error))
                        }
                    }
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}
