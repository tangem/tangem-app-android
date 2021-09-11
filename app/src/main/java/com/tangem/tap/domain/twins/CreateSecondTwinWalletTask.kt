package com.tangem.tap.domain.twins

import com.tangem.*
import com.tangem.commands.wallet.CreateWalletResponse
import com.tangem.commands.wallet.PurgeWalletCommand
import com.tangem.common.CompletionResult
import com.tangem.common.TangemSdkConstants
import com.tangem.common.extensions.hexToBytes
import com.tangem.tap.domain.extensions.getDefaultWalletIndex
import com.tangem.tap.domain.extensions.getSingleWallet
import com.tangem.tasks.CreateWalletTask

class CreateSecondTwinWalletTask(
    private val firstPublicKey: String,
    private val firstCardId: String,
    private val issuerKeys: KeyPair,
    private val preparingMessage: Message,
    private val creatingWalletMessage: Message
) : CardSessionRunnable<CreateWalletResponse> {
    override val requiresPin2 = true



    override fun run(session: CardSession, callback: (result: CompletionResult<CreateWalletResponse>) -> Unit) {
        val card = session.environment.card

        if (card?.getSingleWallet()?.publicKey != null) {

            if (!card.cardId.startsWith(TwinsHelper.getPairCardSeries(firstCardId) ?: "")) {
                callback(CompletionResult.Failure(TangemSdkError.WrongCardType()))
                return
            }

            session.setInitialMessage(preparingMessage)
            PurgeWalletCommand(TangemSdkConstants.getDefaultWalletIndex()).run(session) { response ->
                when (response) {
                    is CompletionResult.Success -> {
                        session.environment.card = session.environment.card?.changeStatusToEmpty()
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
                    session.environment.card = session.environment.card?.changeStatusToLoaded()

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
