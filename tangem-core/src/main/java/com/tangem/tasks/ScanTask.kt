package com.tangem.tasks

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.TagType
import com.tangem.TangemSdkError
import com.tangem.commands.*
import com.tangem.commands.common.CardDeserializer
import com.tangem.common.CompletionResult

/**
 * Task that allows to read Tangem card and verify its private key.
 *
 * It performs two commands, [ReadCommand] and [CheckWalletCommand], subsequently.
 */
internal class ScanTask : CardSessionRunnable<Card> {

    override val performPreflightRead = false
    override val requiresPin2 = false

    override fun run(session: CardSession, callback: (result: CompletionResult<Card>) -> Unit) {


        if (session.connectedTag == TagType.Slix) {
            readSlixTag(session, callback)
            return
        }

        ReadCommand().run(session) { readResult ->
            when(readResult) {
                is CompletionResult.Failure -> callback(readResult)
                is CompletionResult.Success -> {
                    val card = readResult.data
                    session.environment.card = card
                    session.environment.restoreCardValues()
                    if (session.environment.pin1 != null && session.environment.pin2 != null) {
                        val checkPinCommand = SetPinCommand(session.environment.pin1!!.value, session.environment.pin2!!.value)
                        checkPinCommand.run(session) { result ->
                            when (result) {
                                is CompletionResult.Failure -> {
                                    session.environment.pin2 = null
                                }
                            }
                            runCheckWalletIfNeeded(card, session, callback)
                        }
                    } else {
                        runCheckWalletIfNeeded(card, session, callback)
                    }

                }
            }
        }
    }

    private fun runCheckWalletIfNeeded(
            card: Card, session: CardSession,
            callback: (result: CompletionResult<Card>) -> Unit
    ) {
        if (card.cardData?.productMask?.contains(Product.Tag) != false) {
            callback(CompletionResult.Success(card))

        } else if (card.status != CardStatus.Loaded) {
            callback(CompletionResult.Success(card))

        } else if (card.curve == null || card.walletPublicKey == null) {
            callback(CompletionResult.Failure(TangemSdkError.CardError()))

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

    private fun readSlixTag(session: CardSession, callback: (result: CompletionResult<Card>) -> Unit) {
        session.readSlixTag { result ->
            when (result) {
                is CompletionResult.Success -> {
                    try {
                        val card = CardDeserializer.deserialize(result.data)
                        callback(CompletionResult.Success(card))
                    } catch (error: TangemSdkError) {
                        callback(CompletionResult.Failure(error))
                    }
                }
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }
}