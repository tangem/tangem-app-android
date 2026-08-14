package com.tangem.tap.domain.tasks.product

import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.common.map
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.crypto.hdWallet.DerivationNode
import com.tangem.crypto.hdWallet.masterkey.AnyMasterKeyFactory
import com.tangem.domain.card.common.TapWorkarounds.isTestCard
import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.card.configs.CardConfig
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.wallets.derivations.DerivationsHelper
import com.tangem.domain.wallets.derivations.derivationStyleProvider
import com.tangem.operations.backup.PrimaryCard
import com.tangem.operations.backup.StartPrimaryCardLinkingCommand
import com.tangem.operations.derivation.DeriveMultipleWalletPublicKeysTask
import com.tangem.operations.masterSecret.CreateMasterSecretCommand
import com.tangem.operations.read.ReadMasterSecretCommand
import com.tangem.operations.read.ReadWalletsListCommand
import com.tangem.operations.wallet.CreateWalletTask
import com.tangem.sdk.api.CreateProductWalletTaskResponse
import com.tangem.operations.wallet.CreateWalletResponse as SdkCreateWalletResponse

/** BIP-85 root derivation node index: m/83696968' */
private const val BIP85_ROOT_NODE_INDEX = 83696968L

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
    private val mnemonic: Mnemonic? = null,
    private val passphrase: String? = null,
    private val shouldReset: Boolean,
    private val derivationsHelper: DerivationsHelper,
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
            /**
             * @workaround isDemoNoteAsMultiwallet
             * There were produced 20k Note demo cards that should work like multiwallet (except Onboarding)
             * for that reasons we've just added some specific checks for their BatchId
             */
            DemoConfig.isDemoNoteAsMultiwallet(card.cardId) || cardTypesResolver.isTangemNote() ->
                CreateWalletTangemNote(cardTypesResolver)
            cardTypesResolver.isTangemTwins() ->
                throw UnsupportedOperationException("Use the TwinCardsManager to create a wallet")

            else -> CreateWalletTangemWallet(
                derivationsHelper = derivationsHelper,
                mnemonic = mnemonic,
                passphrase = passphrase,
                shouldReset = shouldReset,
                cardDTO = cardDto,
            )
        }
        commandProcessor.proceed(cardDto, session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val createProductWalletTaskResponse = when (commandProcessor) {
                        is CreateWalletTangemWallet -> {
                            result.data as CreateProductWalletTaskResponse
                        }

                        else -> CreateProductWalletTaskResponse(card = requireNotNull(session.environment.card))
                    }
                    callback(CompletionResult.Success(createProductWalletTaskResponse))
                }

                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
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
    private val derivationsHelper: DerivationsHelper,
    private val mnemonic: Mnemonic?,
    private val passphrase: String?,
    private val shouldReset: Boolean,
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
                    val sdkCard = session.environment.card
                    if (sdkCard == null) {
                        callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
                        return@run
                    }
                    val updatedCard = CardDTO(sdkCard)
                    if (card.cardId != updatedCard.cardId) {
                        callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
                        return@run
                    }
                    if (card.firmwareVersion >= FirmwareVersion.v8) {
                        createMasterSecret(
                            card = updatedCard,
                            session = session,
                            createWalletsResponse = result.data,
                            callback = callback,
                        )
                    } else {
                        checkIfAllWalletsCreated(
                            card = updatedCard,
                            session = session,
                            createResponse = result.data,
                            callback = callback,
                        )
                    }
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

    private fun createMasterSecret(
        card: CardDTO,
        session: CardSession,
        createWalletsResponse: CreateWalletsResponse,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        // when importing a wallet from a mnemonic, the master secret must be deterministic:
        // the BIP-85 root key (m/83696968') derived from the mnemonic + passphrase
        val bip85MasterKey = runCatching {
            mnemonic?.let { mn ->
                AnyMasterKeyFactory(mnemonic = mn, passphrase = passphrase.orEmpty())
                    .makeMasterKey(EllipticCurve.Secp256k1)
                    .derivePrivateKey(node = DerivationNode.Hardened(BIP85_ROOT_NODE_INDEX))
            }
        }.getOrElse { error ->
            callback(CompletionResult.Failure(TangemSdkError.ExceptionError(error)))
            return
        }
        CreateMasterSecretCommand(privateKey = bip85MasterKey).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    // save the card with derived wallets and a master secret
                    checkMasterSecret(
                        card = card,
                        session = session,
                        createWalletsResponse = createWalletsResponse,
                        callback = callback,
                    )
                }
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }

    private fun checkMasterSecret(
        card: CardDTO,
        session: CardSession,
        createWalletsResponse: CreateWalletsResponse,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        ReadMasterSecretCommand().run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    if (result.data.masterSecret == null) {
                        // Distinct from WalletAlreadyCreated: callers treat that error as
                        // "card already has a wallet" and offer a factory reset dialog
                        callback(
                            CompletionResult.Failure(
                                TangemSdkError.ExceptionError(
                                    IllegalStateException("Master secret was not created"),
                                ),
                            ),
                        )
                        return@run
                    }
                    checkIfAllWalletsCreated(
                        card = card,
                        session = session,
                        createResponse = createWalletsResponse,
                        callback = callback,
                    )
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun resetCard(
        card: CardDTO,
        session: CardSession,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        val resetCommand = ResetToFactorySettingsTask(allowsRequestAccessCodeFromRepository = false)
        resetCommand.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    createMultiWallet(
                        card = card,
                        session = session,
                        callback = callback,
                    )
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
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
                linkPrimaryCard(
                    card = card,
                    createWalletResponses = createWalletResponses,
                    session = session,
                    callback = callback,
                )
            }

            card.shouldDeriveKeys() -> {
                deriveKeys(
                    card = card,
                    createWalletResponses = createWalletResponses,
                    session = session,
                    callback = callback,
                )
            }

            else -> {
                callback(
                    CompletionResult.Success(
                        CreateProductWalletTaskResponse(card = requireNotNull(session.environment.card)),
                    ),
                )
            }
        }
    }

    private fun linkPrimaryCard(
        card: CardDTO,
        createWalletResponses: List<CreateWalletResponse>,
        session: CardSession,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        StartPrimaryCardLinkingCommand().run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    primaryCard = result.data
                    when {
                        card.shouldDeriveKeys() -> {
                            deriveKeys(
                                card = card,
                                createWalletResponses = createWalletResponses,
                                session = session,
                                callback = callback,
                            )
                        }

                        else -> {
                            callback(
                                CompletionResult.Success(
                                    CreateProductWalletTaskResponse(
                                        card = requireNotNull(session.environment.card),
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

    private fun CardDTO.shouldDeriveKeys(): Boolean {
        return this.settings.isHDWalletAllowed && this.firmwareVersion < FirmwareVersion.v8
    }

    private fun deriveKeys(
        card: CardDTO,
        session: CardSession,
        createWalletResponses: List<CreateWalletResponse>,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        val derivations = derivationsHelper.getDefaultDerivations(
            derivationStyleProvider = card.derivationStyleProvider,
            cardId = card.cardId,
            isTestCard = card.isTestCard,
            wallets = createWalletResponses.map { it.wallet },
        )
        val cardEnv = session.environment.card
        if (cardEnv == null) {
            callback(CompletionResult.Failure(TangemSdkError.CardError()))
            return
        }

        DeriveMultipleWalletPublicKeysTask(derivations)
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
}