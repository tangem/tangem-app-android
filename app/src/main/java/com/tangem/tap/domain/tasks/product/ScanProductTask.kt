package com.tangem.tap.domain.tasks.product

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.card.WalletData
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.toHexString
import com.tangem.common.extensions.toMapKey
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.operations.CommandResponse
import com.tangem.operations.PreflightReadMode
import com.tangem.operations.PreflightReadTask
import com.tangem.operations.ScanTask
import com.tangem.operations.backup.PrimaryCard
import com.tangem.operations.backup.StartPrimaryCardLinkingTask
import com.tangem.operations.derivation.DeriveMultipleWalletPublicKeysTask
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.operations.issuerAndUserData.ReadIssuerDataCommand
import com.tangem.tap.domain.ProductType
import com.tangem.tap.domain.TapSdkError
import com.tangem.tap.domain.TapWorkarounds
import com.tangem.tap.domain.TapWorkarounds.derivationStyle
import com.tangem.tap.domain.TapWorkarounds.getTangemNoteBlockchain
import com.tangem.tap.domain.TapWorkarounds.isExcluded
import com.tangem.tap.domain.TapWorkarounds.isNotSupportedInThatRelease
import com.tangem.tap.domain.TapWorkarounds.useOldStyleDerivation
import com.tangem.tap.domain.extensions.getPrimaryCurve
import com.tangem.tap.domain.extensions.getSingleWallet
import com.tangem.tap.domain.tokens.BlockchainNetwork
import com.tangem.tap.domain.tokens.CurrenciesRepository
import com.tangem.tap.domain.twins.TwinsHelper
import com.tangem.tap.preferencesStorage

data class ScanResponse(
    val card: Card,
    val productType: ProductType,
    val walletData: WalletData?,
    val secondTwinPublicKey: String? = null,
    val derivedKeys: Map<KeyWalletPublicKey, ExtendedPublicKeysMap> = mapOf(),
    val primaryCard: PrimaryCard? = null
) : CommandResponse {

    fun getBlockchain(): Blockchain {
        if (productType == ProductType.Note) return getTangemNoteBlockchain(card)
            ?: return Blockchain.Unknown
        val blockchainName: String = walletData?.blockchain ?: return Blockchain.Unknown
        return Blockchain.fromId(blockchainName)
    }

    fun getPrimaryToken(): Token? {
        val cardToken = walletData?.token ?: return null
        return Token(
            cardToken.name,
            cardToken.symbol,
            cardToken.contractAddress,
            cardToken.decimals,
        )
    }

    fun isTangemNote(): Boolean = productType == ProductType.Note
    fun isTangemWallet(): Boolean = productType == ProductType.Wallet
    fun isTangemTwins(): Boolean = productType == ProductType.Twins

    fun supportsHdWallet(): Boolean = card.settings.isHDWalletAllowed
    fun supportsBackup(): Boolean = card.settings.isBackupAllowed

    fun twinsIsTwinned(): Boolean =
        card.isTangemTwins() && walletData != null && secondTwinPublicKey != null
}

typealias KeyWalletPublicKey = ByteArrayKey

class ScanProductTask(
    val card: Card? = null,
    private val currenciesRepository: CurrenciesRepository?,
    private val additionalBlockchainsToDerive: Collection<Blockchain>? = null
) : CardSessionRunnable<ScanResponse> {

    override fun run(
        session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit,
    ) {
        val card = this.card ?: session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }

        val error = getErrorIfExcludedCard(card)
        if (error != null) {
            callback(CompletionResult.Failure(error))
            return
        }

        val commandProcessor = when {
            TapWorkarounds.isTangemNote(card) -> ScanNoteProcessor()
            card.isTangemTwins() -> ScanTwinProcessor()
            else -> ScanWalletProcessor(currenciesRepository, additionalBlockchainsToDerive)
        }
        commandProcessor.proceed(card, session) { processorResult ->
            when (processorResult) {
                is CompletionResult.Success -> ScanTask().run(session) { scanTaskResult ->
                    when (scanTaskResult) {
                        is CompletionResult.Success -> callback(
                            CompletionResult.Success(
                                processorResult.data
                            )
                        )
                        is CompletionResult.Failure -> callback(
                            CompletionResult.Failure(
                                scanTaskResult.error
                            )
                        )
                    }
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(processorResult.error))
            }
        }
    }

    private fun getErrorIfExcludedCard(card: Card): TangemError? {
        if (card.isExcluded()) return TapSdkError.CardForDifferentApp
        if (card.isNotSupportedInThatRelease()) return TapSdkError.CardNotSupportedByRelease

        return null
    }
}

private fun Card.isTangemTwins(): Boolean = TwinsHelper.getTwinCardNumber(cardId) != null

private class ScanNoteProcessor : ProductCommandProcessor<ScanResponse> {
    override fun proceed(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit
    ) {
        callback(
            CompletionResult.Success(
                ScanResponse(
                    card,
                    ProductType.Note,
                    session.environment.walletData
                )
            )
        )
    }
}

private class ScanWalletProcessor(
    private val currenciesRepository: CurrenciesRepository?,
    private val additionalBlockchainsToDerive: Collection<Blockchain>? = null
) : ProductCommandProcessor<ScanResponse> {

    var primaryCard: PrimaryCard? = null

    override fun proceed(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit
    ) {
        createMissingWalletsIfNeeded(card, session, callback)
    }

    private fun createMissingWalletsIfNeeded(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit
    ) {
        if (card.wallets.isEmpty() || card.firmwareVersion < FirmwareVersion.MultiWalletAvailable) {
            startLinkingForBackupIfNeeded(card, session, callback)
            return
        }

        val curvesToCreate = card.getCurvesForNonCreatedWallets()
        if (curvesToCreate.isEmpty()) {
            startLinkingForBackupIfNeeded(card, session, callback)
            return
        }

        CreateWalletsTask(curvesToCreate).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    PreflightReadTask(
                        PreflightReadMode.FullCardRead,
                        card.cardId
                    ).run(session) { readResult ->
                        when (readResult) {
                            is CompletionResult.Success -> {
                                startLinkingForBackupIfNeeded(card, session, callback)
                            }
                            is CompletionResult.Failure -> callback(
                                CompletionResult.Failure(
                                    readResult.error
                                )
                            )
                        }
                    }
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun startLinkingForBackupIfNeeded(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit
    ) {
        val activationIsFinished =
            preferencesStorage.usedCardsPrefStorage.isActivationFinished(card.cardId)

        if (card.backupStatus == Card.BackupStatus.NoBackup &&
            !activationIsFinished && card.wallets.isNotEmpty()
        ) {
            StartPrimaryCardLinkingTask().run(session) { linkingResult ->
                when (linkingResult) {
                    is CompletionResult.Success -> {
                        primaryCard = linkingResult.data
                        deriveKeysIfNeeded(card, session, callback)
                    }
                    is CompletionResult.Failure -> {
                        deriveKeysIfNeeded(card, session, callback)
                    }
                }
            }
        } else {
            deriveKeysIfNeeded(card, session, callback)
        }
    }

    private fun deriveKeysIfNeeded(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit
    ) {
        val derivations = collectDerivations(card)
        if (derivations.isEmpty() || !card.settings.isHDWalletAllowed) {
            callback(
                CompletionResult.Success(
                    ScanResponse(
                        card = card,
                        productType = ProductType.Wallet,
                        walletData = session.environment.walletData,
                        primaryCard = primaryCard
                    )
                )
            )
            return
        }

        DeriveMultipleWalletPublicKeysTask(derivations).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val response = ScanResponse(
                        card = card,
                        productType = ProductType.Wallet,
                        walletData = session.environment.walletData,
                        derivedKeys = result.data.entries,
                        primaryCard = primaryCard
                    )
                    callback(CompletionResult.Success(response))

                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun getBlockchainsToDerive(card: Card): List<BlockchainNetwork> {
        val currenciesRepository = currenciesRepository ?: return emptyList()
        val cardCurrencies = currenciesRepository.loadSavedCurrencies(card.cardId, card.derivationStyle).toMutableList()

        val blockchainsToDerive = cardCurrencies.ifEmpty {
            mutableListOf(
                BlockchainNetwork(Blockchain.Bitcoin, card),
                BlockchainNetwork(Blockchain.Ethereum, card))
        }

        if (card.settings.isHDWalletAllowed) {
            blockchainsToDerive.addAll(
                listOf(
                    BlockchainNetwork(Blockchain.Ethereum, card),
                    BlockchainNetwork(Blockchain.Binance, card),
                    BlockchainNetwork(Blockchain.EthereumTestnet, card)
                )
            )
        }
        if (additionalBlockchainsToDerive != null) {
            blockchainsToDerive.addAll(additionalBlockchainsToDerive.map { BlockchainNetwork(it, card) })
        }
        if (!card.useOldStyleDerivation) {
            blockchainsToDerive.removeAll(
                listOf(
                Blockchain.BSC, Blockchain.BSCTestnet,
                Blockchain.Polygon, Blockchain.PolygonTestnet,
                Blockchain.RSK,
                Blockchain.Fantom, Blockchain.FantomTestnet,
                Blockchain.Avalanche, Blockchain.AvalancheTestnet,
            ).map { BlockchainNetwork(it, card) }
            )
        }
        return blockchainsToDerive.distinct()
    }

    private fun collectDerivations(card: Card): Map<ByteArrayKey, List<DerivationPath>> {
        val blockchains = getBlockchainsToDerive(card)
        val derivations = mutableMapOf<ByteArrayKey, List<DerivationPath>>()

        blockchains.forEach { blockchain ->
            val curve = blockchain.blockchain.getPrimaryCurve()

            val wallet = card.wallets.firstOrNull { it.curve == curve } ?: return@forEach
            if (wallet.chainCode == null) return@forEach

            val key = wallet.publicKey.toMapKey()
            val path = blockchain.derivationPath?.let { DerivationPath(it) }
            if (path != null) {
                val addedDerivations = derivations[key]
                if (addedDerivations != null) {
                    derivations[key] = addedDerivations + path
                } else {
                    derivations[key] = listOf(path)
                }
            }
        }
        return derivations
    }
}

private class ScanTwinProcessor : ProductCommandProcessor<ScanResponse> {
    override fun proceed(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit,
    ) {
        ReadIssuerDataCommand().run(session) { readDataResult ->
            when (readDataResult) {
                is CompletionResult.Success -> {
                    val publicKey = card.getSingleWallet()?.publicKey
                    if (publicKey == null) {
                        callback(
                            CompletionResult.Success(
                                ScanResponse(
                                    card,
                                    ProductType.Twins,
                                    null
                                )
                            )
                        )
                        return@run
                    }
                    val verified =
                        TwinsHelper.verifyTwinPublicKey(readDataResult.data.issuerData, publicKey)
                    if (verified) {
                        val twinPublicKey = readDataResult.data.issuerData.sliceArray(0 until 65)
                        val walletData = session.environment.walletData
                        val response = ScanResponse(
                            card,
                            ProductType.Twins,
                            walletData,
                            twinPublicKey.toHexString()
                        )
                        callback(CompletionResult.Success(response))
                    } else {
                        callback(
                            CompletionResult.Success(
                                ScanResponse(
                                    card,
                                    ProductType.Twins,
                                    null
                                )
                            )
                        )
                    }
                }
                is CompletionResult.Failure ->
                    callback(CompletionResult.Success(ScanResponse(card, ProductType.Twins, null)))
            }
        }
    }

}

fun Card.getCurvesForNonCreatedWallets(): List<EllipticCurve> {
    val curvesPresent = wallets.map { it.curve }.toSet()
    val curvesForNonCreatedWallets = supportedCurves
        .subtract(curvesPresent + EllipticCurve.Secp256r1)
    return curvesForNonCreatedWallets.toList()
}
