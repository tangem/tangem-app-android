package com.tangem.tap.domain.twins

import com.tangem.Message
import com.tangem.common.CompletionResult
import com.tangem.common.KeyPair
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.extensions.hexToBytes
import com.tangem.domain.common.TwinsHelper
import com.tangem.operations.wallet.CreateWalletResponse
import com.tangem.operations.wallet.CreateWalletTask
import com.tangem.operations.wallet.PurgeWalletCommand
import com.tangem.tap.domain.twins.CreateFirstTwinWalletTask.Companion.runAttestation

class CreateSecondTwinWalletTask(
    private val firstPublicKey: String,
    private val firstCardId: String,
    private val issuerKeys: KeyPair,
    private val preparingMessage: Message,
    private val creatingWalletMessage: Message,
) : CardSessionRunnable<CreateWalletResponse> {

    override val allowsRequestAccessCodeFromRepository: Boolean = false

    override fun run(session: CardSession, callback: (result: CompletionResult<CreateWalletResponse>) -> Unit) {
        val card = session.environment.card
        val publicKey = card?.wallets?.firstOrNull()?.publicKey
        if (publicKey != null) {
            val currentTwinCardNumber = TwinsHelper.getTwinCardNumber(card.cardId)
            if (TwinsHelper.getTwinCardNumber(firstCardId) == currentTwinCardNumber) {
                currentTwinCardNumber?.pairNumber()?.let {
                    callback(CompletionResult.Failure(WrongTwinCard(it)))
                }
                return
            }

            if (!TwinsHelper.isTwinsCompatible(firstCardId, card.cardId)) {
                callback(CompletionResult.Failure(IncompatibleTwinCard()))
                return
            }

            session.setMessage(preparingMessage)
            PurgeWalletCommand(publicKey).run(session) { response ->
                when (response) {
                    is CompletionResult.Success -> {
                        session.environment.card = session.environment.card?.setWallets(emptyList())
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
        session.setMessage(creatingWalletMessage)
        CreateWalletTask(EllipticCurve.Secp256k1).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    runAttestation(session) { attestationResponse ->
                        when (attestationResponse) {
                            is CompletionResult.Success -> writeProtectedIssuerData(session, result, callback)
                            is CompletionResult.Failure -> callback(CompletionResult.Failure(attestationResponse.error))
                        }
                    }
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun writeProtectedIssuerData(
        session: CardSession,
        result: CompletionResult.Success<CreateWalletResponse>,
        callback: CompletionCallback<CreateWalletResponse>,
    ) {
        session.environment.card = session.environment.card?.updateWallet(result.data.wallet)

        WriteProtectedIssuerDataTask(firstPublicKey.hexToBytes(), issuerKeys).run(session) { writeResult ->
            when (writeResult) {
                is CompletionResult.Success -> callback(result)
                is CompletionResult.Failure -> callback(CompletionResult.Failure(writeResult.error))
            }
        }
    }
}