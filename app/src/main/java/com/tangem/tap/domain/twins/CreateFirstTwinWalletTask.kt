package com.tangem.tap.domain.twins

import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.domain.common.TwinsHelper
import com.tangem.operations.wallet.CreateWalletResponse
import com.tangem.operations.wallet.CreateWalletTask
import com.tangem.operations.wallet.PurgeWalletCommand

class CreateFirstTwinWalletTask(private val firstCardId: String) : CardSessionRunnable<CreateWalletResponse> {

    override val allowsRequestAccessCodeFromRepository: Boolean = false

    override fun run(
        session: CardSession,
        callback: (result: CompletionResult<CreateWalletResponse>) -> Unit,
    ) {
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
                        CreateWalletTask(EllipticCurve.Secp256k1).run(session) { callback(it) }
                    }
                    is CompletionResult.Failure -> callback(CompletionResult.Failure(response.error))
                }
            }
        } else {
            CreateWalletTask(EllipticCurve.Secp256k1).run(session) { callback(it) }
        }
    }
}
