import com.tangem.blockchain.common.Blockchain
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.CardWallet
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.operations.derivation.DeriveWalletPublicKeysTask
import com.tangem.operations.derivation.ExtendedPublicKeyList
import com.tangem.operations.wallet.CreateWalletResponse
import com.tangem.operations.wallet.CreateWalletTask
import com.tangem.tap.common.extensions.toMapKey
import com.tangem.tap.domain.ProductType
import com.tangem.tap.domain.TapWorkarounds.getTangemNoteBlockchain
import com.tangem.tap.domain.tasks.product.CreateWalletsTask
import com.tangem.tap.domain.tasks.product.ProductCommandProcessor
import com.tangem.tap.domain.tasks.product.getCurvesForNonCreatedWallets
import com.tangem.tap.store

/**
[REDACTED_AUTHOR]
 */
class CreateProductWalletTask(
    private val type: ProductType,
) : CardSessionRunnable<Card> {

    override fun run(session: CardSession, callback: (result: CompletionResult<Card>) -> Unit) {
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
                is CompletionResult.Success -> callback(CompletionResult.Success(session.environment.card!!))
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

private class CreateWalletTangemWallet : ProductCommandProcessor<CreateWalletResponse> {

    override fun proceed(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<CreateWalletResponse>) -> Unit,
    ) {
        val supportedCurves = setOf(EllipticCurve.Secp256k1, EllipticCurve.Ed25519)
        val curves = card.getCurvesForNonCreatedWallets().intersect(supportedCurves).toList()
        CreateWalletsTask(curves).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val response = result.data.createWalletResponses[0]
                    val derivationPaths = listOf(Blockchain.Bitcoin, Blockchain.Ethereum)
                        .mapNotNull { it.derivationPath() }

                    DeriveWalletPublicKeysTask(response.wallet.publicKey, derivationPaths).run(session) {
                        when (it) {
                            is CompletionResult.Success -> {
                                updateDerivedKeys(response.wallet, it.data)
                                callback(CompletionResult.Success(response))
                            }
                            is CompletionResult.Failure -> callback(CompletionResult.Failure(it.error))
                        }
                    }
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}

//TODO: updating derived wallet public keys - make it better later
private fun updateDerivedKeys(wallet: CardWallet, derivedKeys: ExtendedPublicKeyList) {
    val onboardingManager = store.state.globalState.onboardingManager ?: return
    onboardingManager.scanResponse = onboardingManager.scanResponse.copy(
        derivedKeys = mapOf(wallet.publicKey.toMapKey() to derivedKeys)
    )
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