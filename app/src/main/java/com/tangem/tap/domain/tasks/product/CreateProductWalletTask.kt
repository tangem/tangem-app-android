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
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.common.map
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.KeyWalletPublicKey
import com.tangem.domain.common.ProductType
import com.tangem.domain.common.TapWorkarounds.derivationStyle
import com.tangem.domain.common.TapWorkarounds.getTangemNoteBlockchain
import com.tangem.domain.common.TapWorkarounds.isTestCard
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
    private val type: ProductType,
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

        val commandProcessor = when (type) {
            ProductType.Note -> CreateWalletTangemNote()
            ProductType.Twins -> throw UnsupportedOperationException("Use the TwinCardsManager to create a wallet")
            else -> CreateWalletTangemWallet()
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

private class CreateWalletTangemNote : ProductCommandProcessor<CreateWalletResponse> {
    override fun proceed(
        card: CardDTO,
        session: CardSession,
        callback: (result: CompletionResult<CreateWalletResponse>) -> Unit,
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
            // for еth Note blockchains that support more than 1 curve - secpK1 should be prioritized
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

private class CreateWalletTangemWallet : ProductCommandProcessor<CreateProductWalletTaskResponse> {

    private var primaryCard: PrimaryCard? = null

    override fun proceed(
        card: CardDTO,
        session: CardSession,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        val curves = card.getCurvesForNonCreatedWallets()

        if (curves.isEmpty()) {
            val createWalletResponses = card.wallets.map { wallet ->
                CreateWalletResponse(card.cardId, wallet)
            }
            proceedWithCreatedWallets(card, createWalletResponses, session, callback)
            return
        }

        CreateWalletsTask(curves).run(session) { result ->
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
                        CreateProductWalletTaskResponse(card = session.environment.card!!)
                    )
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
                                        card = session.environment.card!!, primaryCard = primaryCard
                                    )
                                )
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
        createWalletResponse.forEach { response ->
            val blockchainsForCurve = getBlockchains(response.cardId, card).filter {
                it.getSupportedCurves().contains(response.wallet.curve)
            }
            val derivationPaths = blockchainsForCurve.mapNotNull { it.derivationPath(card.derivationStyle) }
            if (derivationPaths.isNotEmpty()) {
                map[response.wallet.publicKey.toMapKey()] = derivationPaths
            }
        }
        if (map.isEmpty()) {
            callback(CompletionResult.Failure(TangemSdkError.UnknownError()))
            return
        }

        DeriveMultipleWalletPublicKeysTask(map)
            .run(session) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        callback(
                            CompletionResult.Success(
                                CreateProductWalletTaskResponse(
                                    card = session.environment.card!!,
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

    private fun getBlockchains(
        cardId: String,
        card: CardDTO,
    ): List<Blockchain> {
        return when {
            DemoHelper.isDemoCardId(cardId) -> DemoHelper.config.demoBlockchains
            card.isTestCard -> listOf(Blockchain.BitcoinTestnet, Blockchain.EthereumTestnet)
            else -> listOf(Blockchain.Bitcoin, Blockchain.Ethereum)
        }
    }
}
