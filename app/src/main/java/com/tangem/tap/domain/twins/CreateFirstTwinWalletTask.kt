package com.tangem.tap.domain.twins

import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.operations.wallet.CreateWalletResponse
import com.tangem.operations.wallet.CreateWalletTask
import com.tangem.operations.wallet.PurgeWalletCommand
import com.tangem.tap.domain.extensions.getSingleWallet

class CreateFirstTwinWalletTask : CardSessionRunnable<CreateWalletResponse> {
    override fun run(
        session: CardSession,
        callback: (result: CompletionResult<CreateWalletResponse>) -> Unit,
    ) {
        val publicKey = session.environment.card?.getSingleWallet()?.publicKey
        if (publicKey != null) {
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