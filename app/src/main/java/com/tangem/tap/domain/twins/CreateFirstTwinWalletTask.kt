package com.tangem.tap.domain.twins

import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.domain.common.TwinsHelper
import com.tangem.operations.attestation.AttestationTask
import com.tangem.operations.wallet.CreateWalletResponse
import com.tangem.operations.wallet.CreateWalletTask
import com.tangem.operations.wallet.PurgeWalletCommand

class CreateFirstTwinWalletTask(private val firstCardId: String) : CardSessionRunnable<CreateWalletResponse> {

    override val allowsRequestAccessCodeFromRepository: Boolean = false

    override fun run(session: CardSession, callback: CompletionCallback<CreateWalletResponse>) {
        val card = session.environment.card
        val publicKey = card?.wallets?.firstOrNull()?.publicKey
        if (publicKey != null) {
            val requiredTwinCardNumber = TwinsHelper.getTwinCardNumber(firstCardId)
            if (requiredTwinCardNumber != TwinsHelper.getTwinCardNumber(card.cardId)) {
                requiredTwinCardNumber?.let {
                    callback(CompletionResult.Failure(WrongTwinCard(it)))
                }
                return
            }

            PurgeWalletCommand(publicKey).run(session) { response ->
                when (response) {
                    is CompletionResult.Success -> {
                        session.environment.card = session.environment.card?.setWallets(emptyList())
                        createWallet(session, callback)
                    }
                    is CompletionResult.Failure -> callback(CompletionResult.Failure(response.error))
                }
            }
        } else {
            createWallet(session, callback)
        }
    }

    private fun createWallet(session: CardSession, callback: CompletionCallback<CreateWalletResponse>) {
        CreateWalletTask(EllipticCurve.Secp256k1).run(session) { createWalletResponse ->
            when (createWalletResponse) {
                is CompletionResult.Success -> {
                    runAttestation(session) { attestationResponse ->
                        when (attestationResponse) {
                            is CompletionResult.Success -> callback(createWalletResponse)
                            is CompletionResult.Failure -> callback(CompletionResult.Failure(attestationResponse.error))
                        }
                    }
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(createWalletResponse.error))
            }
        }
    }

    companion object {

        internal fun runAttestation(session: CardSession, callback: CompletionCallback<*>) {
            val mode = session.environment.config.attestationMode
            val secureStorage = session.environment.secureStorage
            val attestationTask = AttestationTask(mode, secureStorage)

            attestationTask.run(session, callback)
        }
    }
}