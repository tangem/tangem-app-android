package com.tangem.tasks

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.SessionError
import com.tangem.commands.*
import com.tangem.common.CompletionResult

/**
 * Task that allows to read Tangem card and verify its private key.
 *
 * It performs two commands, [ReadCommand] and [CheckWalletCommand], subsequently.
 */
internal class ScanTask : CardSessionRunnable<Card> {

    override fun run(session: CardSession, callback: (result: CompletionResult<Card>) -> Unit) {

        val card = session.environment.card

        if (card == null) {
            callback(CompletionResult.Failure(SessionError.MissingPreflightRead()))

        } else if (card.cardData?.productMask?.contains(Product.Tag) != false) {
            callback(CompletionResult.Success(card))

        } else if (card.status != CardStatus.Loaded) {
            callback(CompletionResult.Success(card))

        } else if (card.curve == null || card.walletPublicKey == null) {
            callback(CompletionResult.Failure(SessionError.CardError()))

        } else {
            val checkWalletCommand = CheckWalletCommand(card.curve, card.walletPublicKey)

            checkWalletCommand.run(session) { result ->
                when (result) {
                    is CompletionResult.Success -> callback(CompletionResult.Success(card))
                    is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }
}