package com.tangem.tap.domain.tasks.product

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.toMapKey
import com.tangem.common.map
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.wallets.derivations.DerivationStyleProvider
import com.tangem.domain.card.common.TapWorkarounds.isTestCard
import com.tangem.domain.card.configs.CardConfig
import com.tangem.domain.models.scan.CardDTO
import com.tangem.operations.backup.PrimaryCard
import com.tangem.operations.backup.StartPrimaryCardLinkingCommand
import com.tangem.operations.derivation.DeriveMultipleWalletPublicKeysTask
import com.tangem.operations.read.ReadWalletsListCommand
import com.tangem.operations.wallet.CreateWalletTask
import com.tangem.sdk.api.CreateProductWalletTaskResponse
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.operations.wallet.CreateWalletResponse as SdkCreateWalletResponse

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
    private val mnemonic: Mnemonic? = null,
    private val passphrase: String? = null,
    private val shouldReset: Boolean,
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

            else -> CreateWalletTangemWallet(mnemonic, passphrase, shouldReset, derivationStyleProvider, cardDto)
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

/**
 * Uses for multiWallet 1st and 2nd
 */
private class CreateWalletTangemWallet(
    private val mnemonic: Mnemonic?,
    private val passphrase: String?,
    private val shouldReset: Boolean,
    private val derivationStyleProvider: DerivationStyleProvider,
    cardDTO: CardDTO,
) : ProductCommandProcessor<CreateProductWalletTaskResponse> {

    private var primaryCard: PrimaryCard? = null
    private val cardConfig = CardConfig.createConfig(cardDTO)

    override fun proceed(
        card: CardDTO,
        session: CardSession,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        val walletsOnCard = card.wallets.map { it.curve }.toSet()
        if (walletsOnCard.isEmpty()) {
            createMultiWallet(card, session, callback)
        } else if (shouldReset) {
            resetCard(card, session, callback)
        } else {
            callback(CompletionResult.Failure(TangemSdkError.WalletAlreadyCreated()))
        }
    }

    private fun createMultiWallet(
        card: CardDTO,
        session: CardSession,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        CreateWalletsTask(cardConfig.mandatoryCurves, mnemonic, passphrase).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    checkIfAllWalletsCreated(card, session, result.data, callback)
                }
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }

    private fun checkIfAllWalletsCreated(
        card: CardDTO,
        session: CardSession,
        createResponse: CreateWalletsResponse,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        if (card.firmwareVersion < FirmwareVersion.MultiWalletAvailable) {
            proceedWithCreatedWallets(
                card = card,
                createWalletResponses = createResponse.createWalletResponses.map { CreateWalletResponse(it) },
                session = session,
                callback = callback,
            )
            return
        }

        val command = ReadWalletsListCommand()
        command.run(session) { response ->
            when (response) {
                is CompletionResult.Success -> {
                    val cardInitializationValidator = CardInitializationValidator(cardConfig.mandatoryCurves)
                    if (cardInitializationValidator.validateWallets(response.data.wallets)) {
                        proceedWithCreatedWallets(
                            card = card,
                            createWalletResponses = createResponse.createWalletResponses.map {
                                CreateWalletResponse(it)
                            },
                            session = session,
                            callback = callback,
                        )
                    } else {
                        callback(CompletionResult.Failure(TangemSdkError.WalletAlreadyCreated()))
                    }
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(response.error))
            }
        }
    }

    private fun resetCard(
        card: CardDTO,
        session: CardSession,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        val resetCommand = ResetToFactorySettingsTask(allowsRequestAccessCodeFromRepository = false)
        resetCommand.run(session) {
            when (it) {
                is CompletionResult.Success -> {
                    createMultiWallet(card, session, callback)
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(it.error))
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
        StartPrimaryCardLinkingCommand().run(session) { result ->
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
            DemoHelper.isDemoCardId(cardId) -> DemoHelper.config.demoBlockchains.toList()
            card.isTestCard -> listOf(Blockchain.BitcoinTestnet, Blockchain.EthereumTestnet)
            else -> listOf(Blockchain.Bitcoin, Blockchain.Ethereum)
        }
    }

    private companion object
}