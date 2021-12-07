package com.tangem.tap.domain.tasks

import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.operations.PreflightReadMode
import com.tangem.operations.PreflightReadTask
import com.tangem.operations.wallet.CreateWalletCommand

@Deprecated("Use CreateProductWalletTask instead")
class CreateWalletsTask(curves: List<EllipticCurve>? = null) : CardSessionRunnable<Card> {

    private val curves = curves ?: listOf(
            EllipticCurve.Secp256k1,
            EllipticCurve.Ed25519,
            EllipticCurve.Secp256r1,
    )

    private var index = 0

    override fun run(session: CardSession, callback: (result: CompletionResult<Card>) -> Unit) {
        val curve = curves[index]
        createWallet(curve, session, callback)
    }

    private fun createWallet(
        curve: EllipticCurve, session: CardSession,
        callback: (result: CompletionResult<Card>) -> Unit
    ) {

        CreateWalletCommand(curve).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    if (index == curves.lastIndex) {
                        PreflightReadTask(PreflightReadMode.FullCardRead).run(session, callback)
                        return@run
                    }
                    index += 1
                    createWallet(curves[index], session, callback)
                }
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }
}