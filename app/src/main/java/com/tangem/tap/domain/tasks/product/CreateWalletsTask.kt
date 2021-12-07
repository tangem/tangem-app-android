package com.tangem.tap.domain.tasks.product

import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.operations.CommandResponse
import com.tangem.operations.wallet.CreateWalletCommand
import com.tangem.operations.wallet.CreateWalletResponse

/**
[REDACTED_AUTHOR]
 */
class CreateWalletsResponse(
    val createWalletResponses: List<CreateWalletResponse>
) : CommandResponse

class CreateWalletsTask(
    private val curves: List<EllipticCurve>,
) : CardSessionRunnable<CreateWalletsResponse> {

    private val createdWalletsResponses = mutableListOf<CreateWalletResponse>()

    override fun run(session: CardSession, callback: (result: CompletionResult<CreateWalletsResponse>) -> Unit) {
        val curve = curves[createdWalletsResponses.size]
        createWallet(curve, session, callback)
    }

    private fun createWallet(
        curve: EllipticCurve,
        session: CardSession,
        callback: (result: CompletionResult<CreateWalletsResponse>) -> Unit
    ) {

        CreateWalletCommand(curve).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    createdWalletsResponses.add(result.data)
                    if (createdWalletsResponses.size == curves.size) {
                        callback(CompletionResult.Success(CreateWalletsResponse(createdWalletsResponses)))
                        return@run
                    }
                    createWallet(curves[createdWalletsResponses.size], session, callback)
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}