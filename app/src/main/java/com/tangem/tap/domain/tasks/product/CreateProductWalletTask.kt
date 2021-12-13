import com.tangem.blockchain.common.Blockchain
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.common.hdWallet.ExtendedPublicKey
import com.tangem.operations.CommandResponse
import com.tangem.operations.backup.PrimaryCard
import com.tangem.operations.backup.StartPrimaryCardLinkingTask
import com.tangem.operations.derivation.DeriveWalletPublicKeysTask
import com.tangem.operations.wallet.CreateWalletResponse
import com.tangem.operations.wallet.CreateWalletTask
import com.tangem.tap.common.extensions.toMapKey
import com.tangem.tap.domain.ProductType
import com.tangem.tap.domain.TapWorkarounds.getTangemNoteBlockchain
import com.tangem.tap.domain.tasks.product.CreateWalletsTask
import com.tangem.tap.domain.tasks.product.KeyWalletPublicKey
import com.tangem.tap.domain.tasks.product.ProductCommandProcessor
import com.tangem.tap.domain.tasks.product.getCurvesForNonCreatedWallets


data class CreateProductWalletTaskResponse(
    val card: Card,
    val derivedKeys: Map<KeyWalletPublicKey, List<ExtendedPublicKey>> = mapOf(),
    val primaryCard: PrimaryCard? = null
) : CommandResponse

class CreateProductWalletTask(
    private val type: ProductType,
) : CardSessionRunnable<CreateProductWalletTaskResponse> {

    override fun run(
        session: CardSession,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit
    ) {
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.CardError()))
            return
        }

        val commandProcessor = when (type) {
            ProductType.Note -> CreateWalletTangemNote()
            ProductType.Twins -> throw UnsupportedOperationException("Use the TwinCardsManager to create a wallet")
            ProductType.Wallet -> CreateWalletTangemWallet()
            else -> CreateWalletOtherCards()
        }
        commandProcessor.proceed(card, session) {
            when (it) {
                is CompletionResult.Success -> {
                    val result = when (commandProcessor) {
                        is CreateWalletTangemWallet -> {
                            it.data as CreateProductWalletTaskResponse
                        }
                        else -> CreateProductWalletTaskResponse(card = session.environment.card!!)
                    }
                    callback(CompletionResult.Success(result))
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(it.error))
            }
        }
    }
}

private class CreateWalletTangemNote : ProductCommandProcessor<CreateWalletResponse> {
    override fun proceed(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<CreateWalletResponse>) -> Unit,
    ) {
        if (card.supportedCurves.isEmpty()) {
            callback(CompletionResult.Failure(TangemSdkError.CardError()))
            return
        }

        val curvesSupportedByBlockchain = getTangemNoteBlockchain(card)?.getSupportedCurves()
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
            CreateWalletTask(curve).run(session, callback)
        }
    }
}

private class CreateWalletTangemWallet : ProductCommandProcessor<CreateProductWalletTaskResponse> {

    private var primaryCard: PrimaryCard? = null
    private var createWalletResponse: CreateWalletResponse? = null

    override fun proceed(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        val supportedCurves = setOf(EllipticCurve.Secp256k1, EllipticCurve.Ed25519)
        val curves = card.getCurvesForNonCreatedWallets().intersect(supportedCurves).toList()
        CreateWalletsTask(curves).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    createWalletResponse = result.data.createWalletResponses[0]
                    linkPrimaryCard(card, session, callback)
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun linkPrimaryCard(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        StartPrimaryCardLinkingTask().run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    primaryCard = result.data
                    deriveKeys(card, session, callback)
                }
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }

    private fun deriveKeys(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        val derivationPaths = listOf(Blockchain.Bitcoin, Blockchain.Ethereum)
            .mapNotNull { it.derivationPath() }
        val response = createWalletResponse.guard {
            callback(CompletionResult.Failure(TangemSdkError.UnknownError()))
            return
        }

        DeriveWalletPublicKeysTask(response.wallet.publicKey, derivationPaths)
            .run(session) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        val derivedKeys = mapOf(response.wallet.publicKey.toMapKey() to result.data)
                        callback(CompletionResult.Success(CreateProductWalletTaskResponse(
                            card = session.environment.card!!,
                            derivedKeys = derivedKeys,
                            primaryCard = primaryCard
                        )))
                    }
                    is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
                }
            }
    }
}

private class CreateWalletOtherCards : ProductCommandProcessor<CreateWalletResponse> {

    override fun proceed(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<CreateWalletResponse>) -> Unit,
    ) {
        val firmwareVersion = card.firmwareVersion
        val task = if (firmwareVersion < FirmwareVersion.MultiWalletAvailable) {
            CreateWalletsTask(listOf(card.supportedCurves.first()))
        } else {
            CreateWalletsTask(card.getCurvesForNonCreatedWallets())
        }

        task.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> callback(CompletionResult.Success(result.data.createWalletResponses[0]))
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}