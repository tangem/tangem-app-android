package com.tangem.tap.domain.tasks.product

import com.tangem.blockchain.blockchains.cardano.CardanoUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.deserialization.WalletDataDeserializer
import com.tangem.common.extensions.*
import com.tangem.common.tlv.Tlv
import com.tangem.common.tlv.TlvDecoder
import com.tangem.crypto.CryptoUtils
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.common.BlockchainNetwork
import com.tangem.domain.common.DerivationStyleProvider
import com.tangem.domain.common.TapWorkarounds.isExcluded
import com.tangem.domain.common.TapWorkarounds.isNotSupportedInThatRelease
import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.common.TapWorkarounds.useOldStyleDerivation
import com.tangem.domain.common.TwinsHelper
import com.tangem.domain.common.configs.CardConfig
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.operations.ScanTask
import com.tangem.operations.backup.PrimaryCard
import com.tangem.operations.backup.StartPrimaryCardLinkingTask
import com.tangem.operations.derivation.DeriveMultipleWalletPublicKeysTask
import com.tangem.operations.files.ReadFilesTask
import com.tangem.operations.issuerAndUserData.ReadIssuerDataCommand
import com.tangem.tap.domain.TapSdkError
import com.tangem.tap.domain.tokens.UserTokensRepository
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import kotlinx.coroutines.launch
import kotlin.collections.set

class ScanProductTask(
    val card: Card? = null,
    private val userTokensRepository: UserTokensRepository?,
    private val additionalBlockchainsToDerive: Collection<Blockchain>? = null,
    override val allowsRequestAccessCodeFromRepository: Boolean = false,
) : CardSessionRunnable<ScanResponse> {

    override fun run(session: CardSession, callback: (result: CompletionResult<ScanResponse>) -> Unit) {
        val card = this.card ?: session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }
        val cardDto = CardDTO(card)

        val error = getErrorIfExcludedCard(cardDto)
        if (error != null) {
            callback(CompletionResult.Failure(error))
            return
        }

        val commandProcessor = when {
            cardDto.isTangemTwins -> ScanTwinProcessor()
            else -> ScanWalletProcessor(userTokensRepository, additionalBlockchainsToDerive)
        }
        commandProcessor.proceed(cardDto, session) { processorResult ->
            when (processorResult) {
                is CompletionResult.Success -> ScanTask().run(session) { scanTaskResult ->
                    when (scanTaskResult) {
                        is CompletionResult.Success -> {
                            // it need because processorResult.data.card doesn't contains attestation result
                            // and CardWallet.derivedKeys
                            val processorScanResponseWithNewCard = processorResult.data.copy(
                                card = CardDTO(scanTaskResult.data),
                            )
                            callback(CompletionResult.Success(processorScanResponseWithNewCard))
                        }
                        is CompletionResult.Failure -> callback(CompletionResult.Failure(scanTaskResult.error))
                    }
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(processorResult.error))
            }
        }
    }

    private fun getErrorIfExcludedCard(cardDto: CardDTO): TangemError? {
        if (cardDto.isExcluded) return TapSdkError.CardForDifferentApp
        if (cardDto.isNotSupportedInThatRelease) return TapSdkError.CardNotSupportedByRelease
        return null
    }
}

private class ScanWalletProcessor(
    private val userTokensRepository: UserTokensRepository?,
    private val additionalBlockchainsToDerive: Collection<Blockchain>? = null,
) : ProductCommandProcessor<ScanResponse> {

    var primaryCard: PrimaryCard? = null
    override fun proceed(
        card: CardDTO,
        session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit,
    ) {
        @Suppress("MagicNumber")
        if (card.firmwareVersion.doubleValue >= 4.39 && card.settings.maxWalletsCount == 1) {
            readFile(card, session, callback)
            return
        }

        startLinkingForBackupIfNeeded(card, session, callback)
    }

    private fun readFile(
        card: CardDTO,
        session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit,
    ) {
        val funToContinue = { startLinkingForBackupIfNeeded(card, session, callback) }

        ReadFilesTask(fileName = "blockchainInfo", walletPublicKey = null).run(session) { result ->

            when (result) {
                is CompletionResult.Success -> {
                    val file = result.data.firstOrNull()
                    val counter = file?.counter
                    val signature = file?.signature

                    val tlv = file?.data?.let { Tlv.deserialize(it) }
                    val walletData = tlv?.let { WalletDataDeserializer.deserialize(TlvDecoder(tlv)) }

                    if (walletData == null || counter == null || signature == null) {
                        funToContinue()
                        return@run
                    }

                    val dataToVerify = card.cardId.hexToBytes() + file.data + counter.toByteArray()
                    val isVerified = CryptoUtils.verify(
                        publicKey = card.issuer.publicKey,
                        message = dataToVerify,
                        signature = signature,
                    )
                    if (!isVerified) {
                        funToContinue()
                        return@run
                    }

                    if (walletData.blockchain != "ANY") {
                        callback(
                            CompletionResult.Success(
                                ScanResponse(
                                    card = card,
                                    productType = determineProductTypeForSingleCurrencyWallet(card),
                                    walletData = walletData,
                                ),
                            ),
                        )
                    } else {
                        funToContinue()
                    }
                }
                is CompletionResult.Failure -> {
                    when (result.error) {
                        is TangemSdkError.FileNotFound, is TangemSdkError.InsNotSupported -> {
                            funToContinue()
                        }
                        else -> callback(CompletionResult.Failure(result.error))
                    }
                }
            }
        }
    }

    private fun determineProductTypeForSingleCurrencyWallet(card: CardDTO): ProductType {
        return if (card.isStart2Coin) {
            ProductType.Start2Coin
        } else {
            ProductType.Note
        }
    }

    private fun startLinkingForBackupIfNeeded(
        card: CardDTO,
        session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit,
    ) {
        val activationInProgress = preferencesStorage.usedCardsPrefStorage.isActivationInProgress(card.cardId)

        @Suppress("ComplexCondition")
        if (card.backupStatus == CardDTO.BackupStatus.NoBackup && card.wallets.isNotEmpty() && activationInProgress) {
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
        card: CardDTO,
        session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit,
    ) {
        val productType = ProductType.Wallet
        val config = CardConfig.createConfig(card)
        scope.launch {
            val scanResponse = ScanResponse(
                card = card,
                productType = productType,
                walletData = session.environment.walletData,
                primaryCard = primaryCard,
            )
            val derivations =
                collectDerivations(card, config, scanResponse.derivationStyleProvider)
            if (derivations.isEmpty() || !card.settings.isHDWalletAllowed) {
                callback(CompletionResult.Success(scanResponse))
                return@launch
            }

            DeriveMultipleWalletPublicKeysTask(derivations).run(session) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        val response = scanResponse.copy(derivedKeys = result.data.entries)
                        callback(CompletionResult.Success(response))
                    }
                    is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }

    private suspend fun getBlockchainsToDerive(
        card: CardDTO,
        derivationStyleProvider: DerivationStyleProvider,
    ): List<BlockchainNetwork> {
        val userTokensRepository = userTokensRepository ?: return emptyList()
        val blockchainsToDerive = userTokensRepository.loadBlockchainsToDerive(card)
            .toMutableList()
            .ifEmpty { getDefaultBlockchains(derivationStyleProvider) }

        if (card.settings.isHDWalletAllowed) {
            blockchainsToDerive += getEthereumBlockchains(derivationStyleProvider)
        }

        additionalBlockchainsToDerive?.let {
            blockchainsToDerive += getAdditionalBlockchainToDerive(derivationStyleProvider, it)
        }

        // we should generate second key for cardano
        // because cardano address generation for wallet2 requires keys from 2 derivations
        // https://developers.cardano.org/docs/get-started/cardano-serialization-lib/generating-keys/
        val secondCardanoNetwork = blockchainsToDerive
            .find { it.blockchain == Blockchain.Cardano }
            ?.let { getCardanoSecondNetwork(it) }
        secondCardanoNetwork?.let { blockchainsToDerive.add(it) }

        // pay attention to this
        if (!card.useOldStyleDerivation) {
            removeUnnecessaryBlockchains(blockchainsToDerive, derivationStyleProvider)
        }

        return blockchainsToDerive.distinct()
    }

    private fun getDefaultBlockchains(
        derivationStyleProvider: DerivationStyleProvider,
    ): MutableList<BlockchainNetwork> {
        return mutableListOf(
            BlockchainNetwork(
                blockchain = Blockchain.Bitcoin,
                derivationStyleProvider = derivationStyleProvider,
            ),
            BlockchainNetwork(
                blockchain = Blockchain.Ethereum,
                derivationStyleProvider = derivationStyleProvider,
            ),
        )
    }

    private fun getEthereumBlockchains(derivationStyleProvider: DerivationStyleProvider): List<BlockchainNetwork> {
        return listOf(
            BlockchainNetwork(
                blockchain = Blockchain.Ethereum,
                derivationStyleProvider = derivationStyleProvider,
            ),
            BlockchainNetwork(
                blockchain = Blockchain.EthereumTestnet,
                derivationStyleProvider = derivationStyleProvider,
            ),
        )
    }

    private fun getAdditionalBlockchainToDerive(
        derivationStyleProvider: DerivationStyleProvider,
        collection: Collection<Blockchain>,
    ): List<BlockchainNetwork> {
        return collection.map {
            BlockchainNetwork(
                blockchain = it,
                derivationStyleProvider = derivationStyleProvider,
            )
        }
    }

    private fun getCardanoSecondNetwork(cardanoBlockchainNetwork: BlockchainNetwork): BlockchainNetwork? {
        val cardanoStandardDerivation = cardanoBlockchainNetwork.derivationPath?.let { DerivationPath(it) }
            ?: return null
        val cardanoPatchedDerivation = CardanoUtils.extendedDerivationPath(cardanoStandardDerivation)
        return BlockchainNetwork(
            blockchain = Blockchain.Cardano,
            derivationPath = cardanoPatchedDerivation.rawPath,
            tokens = emptyList(),
        )
    }

    private fun removeUnnecessaryBlockchains(
        blockchainsToDerive: MutableList<BlockchainNetwork>,
        derivationStyleProvider: DerivationStyleProvider,
    ) {
        blockchainsToDerive.removeAll(
            listOf(
                Blockchain.BSC, Blockchain.BSCTestnet,
                Blockchain.Polygon, Blockchain.PolygonTestnet,
                Blockchain.RSK,
                Blockchain.Fantom, Blockchain.FantomTestnet,
                Blockchain.Avalanche, Blockchain.AvalancheTestnet,
            ).map {
                BlockchainNetwork(
                    blockchain = it,
                    derivationStyleProvider = derivationStyleProvider,
                )
            },
        )
    }

    private suspend fun collectDerivations(
        card: CardDTO,
        config: CardConfig,
        derivationStyleProvider: DerivationStyleProvider,
    ): Map<ByteArrayKey, List<DerivationPath>> {
        val blockchains = getBlockchainsToDerive(card, derivationStyleProvider)
        val derivations = mutableMapOf<ByteArrayKey, List<DerivationPath>>()

        blockchains.forEach { blockchain ->
            val curve = config.primaryCurve(blockchain.blockchain)
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

@Suppress("MagicNumber")
private class ScanTwinProcessor : ProductCommandProcessor<ScanResponse> {
    override fun proceed(
        card: CardDTO,
        session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit,
    ) {
        ReadIssuerDataCommand().run(session) { readDataResult ->
            when (readDataResult) {
                is CompletionResult.Success -> {
                    val publicKey = card.wallets.firstOrNull()?.publicKey
                    if (publicKey == null) {
                        val response = ScanResponse(
                            card = card,
                            productType = ProductType.Twins,
                            walletData = null,
                        )
                        callback(CompletionResult.Success(response))
                        return@run
                    }

                    val verified = TwinsHelper.verifyTwinPublicKey(readDataResult.data.issuerData, publicKey)
                    val response = if (verified) {
                        val twinPublicKey = readDataResult.data.issuerData.sliceArray(0 until 65)
                        val walletData = session.environment.walletData
                        ScanResponse(
                            card = card,
                            productType = ProductType.Twins,
                            walletData = walletData,
                            secondTwinPublicKey = twinPublicKey.toHexString(),
                        )
                    } else {
                        ScanResponse(
                            card = card,
                            productType = ProductType.Twins,
                            walletData = null,
                        )
                    }
                    callback(CompletionResult.Success(response))
                }
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Success(ScanResponse(card, ProductType.Twins, null)))
                }
            }
        }
    }
}
