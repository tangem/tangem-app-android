import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.operations.PreflightReadMode
import com.tangem.operations.PreflightReadTask
import com.tangem.operations.wallet.CreateWalletCommand
import com.tangem.operations.wallet.CreateWalletResponse
import com.tangem.tap.domain.TapWorkarounds.getTangemNoteBlockchain
import com.tangem.tap.domain.TapWorkarounds.isTangemNote
import com.tangem.tap.domain.TapWorkarounds.isTangemWallet
import com.tangem.tap.domain.tasks.product.ProductCommandProcessor
import com.tangem.tap.domain.twins.isTangemTwin

/**
[REDACTED_AUTHOR]
 */
class CreateProductWalletAndRescanTask : CardSessionRunnable<Card> {

    override fun run(session: CardSession, callback: (result: CompletionResult<Card>) -> Unit) {
        CreateProductWalletTask().run(session) { result ->
            when (result) {
                is CompletionResult.Success -> PreflightReadTask(PreflightReadMode.FullCardRead).run(session, callback)
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}

private class CreateProductWalletTask : CardSessionRunnable<CreateWalletResponse> {

    override fun run(session: CardSession, callback: (result: CompletionResult<CreateWalletResponse>) -> Unit) {
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.CardError()))
            return
        }

        val commandProcessor = when {
            card.isTangemNote() -> CreateWalletTangemNote()
            card.isTangemTwin() -> CreateWalletTangemTwin()
            card.isTangemWallet() -> CreateWalletTangemWallet()
            else -> CreateWalletOtherCards()
        }
        commandProcessor.proceed(card, session, callback)
    }
}

private class CreateWalletTangemNote : ProductCommandProcessor<CreateWalletResponse> {
    override fun proceed(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<CreateWalletResponse>) -> Unit
    ) {
        if (card.supportedCurves.isEmpty()) {
            callback(CompletionResult.Failure(TangemSdkError.CardError()))
            return
        }

        val curvesSupportedByBlockchain = card.getTangemNoteBlockchain()?.getSupportedCurves()
        if (curvesSupportedByBlockchain == null || curvesSupportedByBlockchain.isEmpty()) {
            callback(CompletionResult.Failure(TangemSdkError.CardError()))
            return
        }

        val intersectCurves = card.supportedCurves.intersect(curvesSupportedByBlockchain).toList()
        if (intersectCurves.isEmpty()) {
            callback(CompletionResult.Failure(TangemSdkError.CardError()))
        } else {
            val curve = if (intersectCurves.contains(EllipticCurve.Secp256k1)) {
                EllipticCurve.Secp256k1
            } else {
                intersectCurves[0]
            }
            CreateWalletCommand(curve).run(session, callback)

        }
    }
}

private class CreateWalletTangemWallet : ProductCommandProcessor<CreateWalletResponse> {
    override fun proceed(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<CreateWalletResponse>) -> Unit
    ) {
        throw UnsupportedOperationException()
    }
}

private class CreateWalletTangemTwin : ProductCommandProcessor<CreateWalletResponse> {
    override fun proceed(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<CreateWalletResponse>) -> Unit
    ) {
        throw UnsupportedOperationException()
    }
}

private class CreateWalletOtherCards : ProductCommandProcessor<CreateWalletResponse> {

    private var index = 0

    private val curves = EllipticCurve.values().toList()

    override fun proceed(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<CreateWalletResponse>) -> Unit
    ) {
        val curve = curves[index]
        createWallet(curve, session, callback)
    }

    private fun createWallet(
        curve: EllipticCurve,
        session: CardSession,
        callback: (result: CompletionResult<CreateWalletResponse>) -> Unit
    ) {
        CreateWalletCommand(curve).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    if (index == curves.lastIndex) {
                        callback(CompletionResult.Success(result.data))
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