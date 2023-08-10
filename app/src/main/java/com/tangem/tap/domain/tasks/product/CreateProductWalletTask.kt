package com.tangem.tap.domain.tasks.product

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.toMapKey
import com.tangem.common.map
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.common.DerivationStyleProvider
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.common.extensions.derivationPath
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.KeyWalletPublicKey
import com.tangem.operations.CommandResponse
import com.tangem.operations.backup.PrimaryCard
import com.tangem.operations.backup.StartPrimaryCardLinkingTask
import com.tangem.operations.derivation.DeriveMultipleWalletPublicKeysTask
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.operations.wallet.CreateWalletTask
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.operations.wallet.CreateWalletResponse as SdkCreateWalletResponse

data class CreateProductWalletTaskResponse(
    val card: CardDTO,
    val derivedKeys: Map<KeyWalletPublicKey, ExtendedPublicKeysMap> = mapOf(),
    val primaryCard: PrimaryCard? = null,
) : CommandResponse {
    constructor(
        card: Card,
        derivedKeys: Map<KeyWalletPublicKey, ExtendedPublicKeysMap> = mapOf(),
        primaryCard: PrimaryCard? = null,
    ) : this(
        card = CardDTO(card),
        derivedKeys = derivedKeys,
        primaryCard = primaryCard,
    )
}

private data class CreateWalletResponse(
    val cardId: String,
    val wallet: CardDTO.Wallet,
) {
    constructor(
        sdkResponse: SdkCreateWalletResponse,
    ) : this(
        cardId = sdkResponse.cardId,
        wallet = CardDTO.Wallet(sdkResponse.wallet),
    )
}

class CreateProductWalletTask(
    private val cardTypesResolver: CardTypesResolver,
    private val derivationStyleProvider: DerivationStyleProvider,
    private val seed: ByteArray? = null,
) : CardSessionRunnable<CreateProductWalletTaskResponse> {

    override val allowsRequestAccessCodeFromRepository: Boolean = false

    override fun run(
        session: CardSession,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.CardError()))
            return
        }
        val cardDto = CardDTO(card)

        val commandProcessor = when {
            cardTypesResolver.isTangemNote() -> CreateWalletTangemNote(cardTypesResolver)
            cardTypesResolver.isTangemTwins() ->
                throw UnsupportedOperationException("Use the TwinCardsManager to create a wallet")

            else -> CreateWalletTangemWallet(seed, derivationStyleProvider)
        }
        commandProcessor.proceed(cardDto, session) {
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

private class CreateWalletTangemNote(private val cardTypesResolver: CardTypesResolver) :
    ProductCommandProcessor<CreateWalletResponse> {
    override fun proceed(
        card: CardDTO,
        session: CardSession,
        callback: (result: CompletionResult<CreateWalletResponse>) -> Unit,
    ) {
        if (card.supportedCurves.isEmpty()) {
            callback(CompletionResult.Failure(TangemSdkError.CardError()))
            return
        }

        val curvesSupportedByBlockchain = cardTypesResolver.getBlockchain().getSupportedCurves().toSet()
        if (curvesSupportedByBlockchain.isEmpty()) {
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
            CreateWalletTask(curve).run(session) { result ->
                callback(result.map { CreateWalletResponse(it) })
            }
        }
    }
}

private class CreateWalletTangemWallet(
    private val seed: ByteArray?,
    private val derivationStyleProvider: DerivationStyleProvider,
) : ProductCommandProcessor<CreateProductWalletTaskResponse> {

    private var primaryCard: PrimaryCard? = null

    override fun proceed(
        card: CardDTO,
        session: CardSession,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        val walletsOnCard = card.wallets.map { it.curve }.toSet()
        val curves = card.supportedCurves.intersect(CURVES_FOR_WALLETS).subtract(walletsOnCard).toList()

        if (curves.isEmpty()) {
            val createWalletResponses = card.wallets.map { wallet ->
                CreateWalletResponse(card.cardId, wallet)
            }
            proceedWithCreatedWallets(card, createWalletResponses, session, callback)
            return
        }

        CreateWalletsTask(curves, seed).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    proceedWithCreatedWallets(
                        card = card,
                        createWalletResponses = result.data.createWalletResponses.map { CreateWalletResponse(it) },
                        session = session,
                        callback = callback,
                    )
                }
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }

    private fun proceedWithCreatedWallets(
        card: CardDTO,
        createWalletResponses: List<CreateWalletResponse>,
        session: CardSession,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        when {
            card.settings.isBackupAllowed -> {
                linkPrimaryCard(card, createWalletResponses, session, callback)
            }

            card.settings.isHDWalletAllowed -> {
                deriveKeys(card, createWalletResponses, session, callback)
            }

            else -> {
                callback(
                    CompletionResult.Success(
                        CreateProductWalletTaskResponse(card = session.environment.card!!),
                    ),
                )
            }
        }
    }

    private fun linkPrimaryCard(
        card: CardDTO,
        createWalletResponse: List<CreateWalletResponse>,
        session: CardSession,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        StartPrimaryCardLinkingTask().run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    primaryCard = result.data
                    when {
                        card.settings.isHDWalletAllowed -> {
                            deriveKeys(card, createWalletResponse, session, callback)
                        }

                        else -> {
                            callback(
                                CompletionResult.Success(
                                    CreateProductWalletTaskResponse(
                                        card = session.environment.card!!,
                                        primaryCard = primaryCard,
                                    ),
                                ),
                            )
                        }
                    }
                }

                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }

    private fun deriveKeys(
        card: CardDTO,
        createWalletResponse: List<CreateWalletResponse>,
        session: CardSession,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        val map = mutableMapOf<ByteArrayKey, List<DerivationPath>>()
        var isBlockchainsForCurvesExist = false
        createWalletResponse.forEach { response ->
            val blockchainsForCurve = getBlockchains(response.cardId, card).filter {
                it.getSupportedCurves().contains(response.wallet.curve)
            }
            val derivationPaths = blockchainsForCurve.mapNotNull { blockchain ->
                isBlockchainsForCurvesExist = true
                blockchain.derivationPath(derivationStyleProvider.getDerivationStyle())
            }
            if (derivationPaths.isNotEmpty()) {
                map[response.wallet.publicKey.toMapKey()] = derivationPaths
            }
        }
        val cardEnv = session.environment.card
        if (cardEnv == null) {
            callback(CompletionResult.Failure(TangemSdkError.CardError()))
            return
        }
        if (map.isEmpty()) {
            if (isBlockchainsForCurvesExist) {
                callback(CompletionResult.Failure(TangemSdkError.UnknownError()))
            } else {
                // if there is no blockchains to derive, just return success response with empty derivedKeys
                callback(
                    CompletionResult.Success(
                        CreateProductWalletTaskResponse(
                            card = cardEnv,
                            primaryCard = primaryCard,
                        ),
                    ),
                )
            }
            return
        }

        DeriveMultipleWalletPublicKeysTask(map)
            .run(session) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        callback(
                            CompletionResult.Success(
                                CreateProductWalletTaskResponse(
                                    card = cardEnv,
                                    derivedKeys = result.data.entries,
                                    primaryCard = primaryCard,
                                ),
                            ),
                        )
                    }

                    is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
                }
            }
    }

    private fun getBlockchains(cardId: String, card: CardDTO): List<Blockchain> {
        return when {
            DemoHelper.isDemoCardId(cardId) -> DemoHelper.config.demoBlockchains
            card.isTestCard -> listOf(Blockchain.BitcoinTestnet, Blockchain.EthereumTestnet)
            else -> listOf(Blockchain.Bitcoin, Blockchain.Ethereum)
        }
    }

    private companion object {
        val CURVES_FOR_WALLETS = listOf(EllipticCurve.Secp256k1, EllipticCurve.Ed25519)
    }
}