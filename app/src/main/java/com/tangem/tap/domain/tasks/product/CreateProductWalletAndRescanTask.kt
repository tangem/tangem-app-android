import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.operations.PreflightReadMode
import com.tangem.operations.PreflightReadTask
import com.tangem.operations.wallet.CreateWalletCommand
import com.tangem.operations.wallet.CreateWalletResponse
import com.tangem.tap.domain.ProductType
import com.tangem.tap.domain.TapWorkarounds.getTangemNoteBlockchain
import com.tangem.tap.domain.tasks.product.CreateProductWalletsTask
import com.tangem.tap.domain.tasks.product.ProductCommandProcessor
import com.tangem.tap.domain.tasks.product.getCurvesForNonCreatedWallets

/**
[REDACTED_AUTHOR]
 */
class CreateProductWalletAndRescanTask(
    private val type: ProductType,
) : CardSessionRunnable<Card> {

    override fun run(session: CardSession, callback: (result: CompletionResult<Card>) -> Unit) {
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.CardError()))
            return
        }
        CreateProductWalletTask(card, type).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> PreflightReadTask(PreflightReadMode.FullCardRead).run(session, callback)
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}

private class CreateProductWalletTask(
    private val card: Card,
    private val type: ProductType,
) : CardSessionRunnable<CreateWalletResponse> {

    override fun run(session: CardSession, callback: (result: CompletionResult<CreateWalletResponse>) -> Unit) {
        val commandProcessor = when (type) {
            ProductType.Note -> CreateWalletTangemNote()
            ProductType.Twin -> throw UnsupportedOperationException("Use the TwinCardsManager to create a wallet")
            ProductType.Wallet -> CreateWalletTangemWallet()
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
        //TODO:
    }
}

private class CreateWalletOtherCards : ProductCommandProcessor<CreateWalletResponse> {

    override fun proceed(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<CreateWalletResponse>) -> Unit
    ) {
        val firmwareVersion = card.firmwareVersion
        val task = if (firmwareVersion < FirmwareVersion.MultiWalletAvailable) {
            CreateProductWalletsTask(listOf(card.supportedCurves.first()))
        } else {
            CreateProductWalletsTask(card.getCurvesForNonCreatedWallets())
        }

        task.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> callback(CompletionResult.Success(result.data.createWalletResponses[0]))
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}