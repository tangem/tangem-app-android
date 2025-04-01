package com.tangem.domain.walletmanager

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.right
import com.tangem.blockchain.blockchains.solana.RentProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.address.EstimationFeeAddressFactory
import com.tangem.blockchain.common.pagination.Page
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.trustlines.AssetRequirementsManager
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchain.nft.models.NFTCollection
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import com.tangem.domain.common.util.hasDerivation
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.transaction.models.AssetRequirementsCondition
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryState
import com.tangem.domain.walletmanager.model.RentData
import com.tangem.domain.walletmanager.model.SmartContractMethod
import com.tangem.domain.walletmanager.model.TokenInfo
import com.tangem.domain.walletmanager.model.UpdateWalletManagerResult
import com.tangem.domain.walletmanager.utils.*
import com.tangem.domain.walletmanager.utils.WalletManagerFactory
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.math.BigDecimal
import java.util.EnumSet

@Suppress("LargeClass", "TooManyFunctions")
// FIXME: Move to its own module and make internal
@Deprecated("Inject the WalletManagerFacade interface using DI instead")
class DefaultWalletManagersFacade(
    private val walletManagersStore: WalletManagersStore,
    private val userWalletsStore: UserWalletsStore,
    private val assetLoader: AssetLoader,
    private val dispatchers: CoroutineDispatcherProvider,
    blockchainSDKFactory: BlockchainSDKFactory,
) : WalletManagersFacade {

    private val demoConfig by lazy { DemoConfig() }
    private val resultFactory by lazy { UpdateWalletManagerResultFactory() }
    private val walletManagerFactory by lazy { WalletManagerFactory(blockchainSDKFactory) }
    private val sdkTokenConverter by lazy { SdkTokenConverter() }
    private val txHistoryStateConverter by lazy { SdkTransactionHistoryStateConverter() }
    private val sdkPageConverter by lazy { SdkPageConverter() }
    private val cryptoCurrencyTypeConverter by lazy { CryptoCurrencyTypeConverter() }
    private val requirementsConditionConverter by lazy { SdkRequirementsConditionConverter() }
    private val estimationFeeAddressFactory by lazy { EstimationFeeAddressFactory() }

    private val initMutex = Mutex()

    override suspend fun update(
        userWalletId: UserWalletId,
        network: Network,
        extraTokens: Set<CryptoCurrency.Token>,
    ): UpdateWalletManagerResult {
        val userWallet = getUserWallet(userWalletId)
        val blockchain = Blockchain.fromId(network.id.value)
        val derivationPath = network.derivationPath.value

        return getAndUpdateWalletManager(userWallet, blockchain, derivationPath, extraTokens)
    }

    override suspend fun remove(userWalletId: UserWalletId, networks: Set<Network>) {
        if (networks.isEmpty()) return

        val blockchainsToDerivationPaths = networks.map {
            Blockchain.fromId(it.id.value) to it.derivationPath.value
        }

        withContext(dispatchers.io) {
            walletManagersStore.remove(userWalletId) { walletManager ->
                val wallet = walletManager.wallet
                val blockchainToDerivationPath = wallet.blockchain to wallet.publicKey.derivationPath?.rawPath

                blockchainToDerivationPath in blockchainsToDerivationPaths
            }
        }
    }

    override suspend fun removeTokens(userWalletId: UserWalletId, tokens: Set<CryptoCurrency.Token>) {
        if (tokens.isEmpty()) return

        tokens
            .groupBy(CryptoCurrency.Token::network)
            .forEach { (network, networkTokens) ->
                removeTokens(
                    userWalletId = userWalletId,
                    network = network,
                    networkTokens = sdkTokenConverter.convertList(networkTokens),
                )
            }
    }

    override suspend fun removeTokensByTokenInfo(userWalletId: UserWalletId, tokenInfos: Set<TokenInfo>) {
        if (tokenInfos.isEmpty()) return

        tokenInfos
            .groupBy { it.network }
            .forEach { (network, tokenInfoList) ->
                removeTokens(
                    userWalletId = userWalletId,
                    network = network,
                    networkTokens = tokenInfoList.map {
                        Token(
                            name = it.name,
                            symbol = it.symbol,
                            contractAddress = it.contractAddress,
                            decimals = it.decimals,
                            id = it.id,
                        )
                    },
                )
            }
    }

    private suspend fun removeTokens(userWalletId: UserWalletId, network: Network, networkTokens: List<Token>) {
        withContext(dispatchers.io) {
            val walletManager = walletManagersStore.getSyncOrNull(
                userWalletId = userWalletId,
                blockchain = Blockchain.fromId(network.id.value),
                derivationPath = network.derivationPath.value,
            ) ?: return@withContext

            networkTokens.forEach { token ->
                walletManager.removeToken(token)
            }

            walletManagersStore.store(userWalletId, walletManager)
        }
    }

    override suspend fun updatePendingTransactions(
        userWalletId: UserWalletId,
        network: Network,
    ): UpdateWalletManagerResult {
        val userWallet = getUserWallet(userWalletId)
        val blockchain = Blockchain.fromId(network.id.value)
        val derivationPath = network.derivationPath.value

        if (derivationPath != null && !userWallet.scanResponse.hasDerivation(blockchain, derivationPath)) {
            Timber.w("Derivation missed for: $blockchain")
            return UpdateWalletManagerResult.MissedDerivation
        }

        val walletManager = getOrCreateWalletManager(userWalletId, blockchain, derivationPath)
        if (walletManager == null || blockchain == Blockchain.Unknown) {
            Timber.w("Unable to get a wallet manager for blockchain: $blockchain")
            return UpdateWalletManagerResult.Unreachable()
        }

        return getLastWalletManagerResult(walletManager)
    }

    override suspend fun getExploreUrl(
        userWalletId: UserWalletId,
        network: Network,
        addressType: AddressType,
        contractAddress: String?,
    ): String {
        val blockchain = Blockchain.fromId(network.id.value)
        val walletManager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = network,
        )

        requireNotNull(walletManager) {
            "Unable to get a wallet manager for blockchain: $blockchain"
        }

        val address = walletManager
            .wallet
            .addresses
            .find { it.type == addressType }
            ?.value ?: walletManager.wallet.address
        return blockchain.getExploreUrl(address, contractAddress)
    }

    override suspend fun getTxHistoryState(userWalletId: UserWalletId, currency: CryptoCurrency): TxHistoryState {
        val walletManager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = currency.network,
        )

        requireNotNull(walletManager) {
            "Unable to get a wallet manager for blockchain: ${currency.network}"
        }

        return walletManager
            .getTransactionHistoryState(
                address = walletManager.wallet.address,
                filterType = when (currency) {
                    is CryptoCurrency.Coin -> TransactionHistoryRequest.FilterType.Coin
                    is CryptoCurrency.Token -> {
                        val blockchainToken = Token(
                            name = currency.name,
                            symbol = currency.symbol,
                            contractAddress = currency.contractAddress,
                            decimals = currency.decimals,
                            id = currency.id.rawCurrencyId?.value,
                        )
                        TransactionHistoryRequest.FilterType.Contract(blockchainToken)
                    }
                },
            )
            .let(txHistoryStateConverter::convert)
    }

    override suspend fun getTxHistoryItems(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        page: Page,
        pageSize: Int,
    ): PaginationWrapper<TxHistoryItem> {
        val walletManager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = currency.network,
        )

        requireNotNull(walletManager) {
            "Unable to get a wallet manager for blockchain: ${currency.network}"
        }

        val itemsResult = walletManager.getTransactionsHistory(
            request = TransactionHistoryRequest(
                address = walletManager.wallet.address,
                decimals = currency.decimals,
                page = page,
                pageSize = pageSize,
                filterType = when (currency) {
                    is CryptoCurrency.Coin -> TransactionHistoryRequest.FilterType.Coin
                    is CryptoCurrency.Token -> {
                        val blockchainToken = Token(
                            name = currency.name,
                            symbol = currency.symbol,
                            contractAddress = currency.contractAddress,
                            decimals = currency.decimals,
                            id = currency.id.rawCurrencyId?.value,
                        )
                        TransactionHistoryRequest.FilterType.Contract(blockchainToken)
                    }
                },
            ),
        )

        return when (itemsResult) {
            is Result.Success -> PaginationWrapper(
                currentPage = sdkPageConverter.convert(page),
                nextPage = sdkPageConverter.convert(itemsResult.data.nextPage),
                items = SdkTransactionHistoryItemConverter(smartContractMethods = readSmartContractMethods())
                    .convertList(itemsResult.data.items),
            )
            is Result.Failure -> error(itemsResult.error.message ?: itemsResult.error.customMessage)
        }
    }

    private suspend fun getUserWallet(userWalletId: UserWalletId) =
        requireNotNull(userWalletsStore.getSyncOrNull(userWalletId)) {
            "Unable to find a user wallet with provided ID: $userWalletId"
        }

    private suspend fun getAndUpdateWalletManager(
        userWallet: UserWallet,
        blockchain: Blockchain,
        derivationPath: String?,
        extraTokens: Set<CryptoCurrency.Token>,
    ): UpdateWalletManagerResult {
        val scanResponse = userWallet.scanResponse

        if (derivationPath != null && !scanResponse.hasDerivation(blockchain, derivationPath)) {
            Timber.w("Derivation missed for: $blockchain")
            return UpdateWalletManagerResult.MissedDerivation
        }

        val walletManager = getOrCreateWalletManager(
            userWalletId = userWallet.walletId,
            blockchain = blockchain,
            derivationPath = derivationPath,
        )
        if (walletManager == null || blockchain == Blockchain.Unknown) {
            Timber.w("Unable to create or find a wallet manager for blockchain: $blockchain")
            return UpdateWalletManagerResult.Unreachable()
        }

        updateWalletManagerTokensIfNeeded(walletManager, extraTokens)

        return try {
            if (demoConfig.isDemoCardId(userWallet.scanResponse.card.cardId)) {
                updateDemoWalletManager(walletManager)
            } else {
                updateWalletManager(walletManager)
            }
        } finally {
            walletManagersStore.store(userWallet.walletId, walletManager)
        }
    }

    private fun updateDemoWalletManager(walletManager: WalletManager): UpdateWalletManagerResult {
        val amount = demoConfig.getBalance(walletManager.wallet.blockchain)
        walletManager.wallet.setAmount(amount)

        return resultFactory.getDemoResult(walletManager, amount)
    }

    private suspend fun updateWalletManager(walletManager: WalletManager): UpdateWalletManagerResult {
        return try {
            walletManager.update()

            resultFactory.getResult(walletManager)
        } catch (e: BlockchainSdkError.AccountNotFound) {
            resultFactory.getNoAccountResult(
                walletManager = walletManager,
                customMessage = e.customMessage,
                amountToCreateAccount = e.amountToCreateAccount,
            )
        } catch (e: Throwable) {
            Timber.w(e, "Unable to update a wallet manager for: ${walletManager.wallet.blockchain}")

            resultFactory.getUnreachableResult(walletManager)
        }
    }

    private fun getLastWalletManagerResult(walletManager: WalletManager): UpdateWalletManagerResult {
        return try {
            resultFactory.getResult(walletManager)
        } catch (e: BlockchainSdkError.AccountNotFound) {
            resultFactory.getNoAccountResult(
                walletManager = walletManager,
                customMessage = e.customMessage,
                amountToCreateAccount = e.amountToCreateAccount,
            )
        } catch (e: Throwable) {
            Timber.w(e, "Unable to update a wallet manager for: ${walletManager.wallet.blockchain}")

            resultFactory.getUnreachableResult(walletManager)
        }
    }

    @Deprecated("Will be removed in future")
    override suspend fun getOrCreateWalletManager(
        userWalletId: UserWalletId,
        blockchain: Blockchain,
        derivationPath: String?,
    ): WalletManager? {
        initMutex.withLock {
            val userWallet = getUserWallet(userWalletId)
            var walletManager = walletManagersStore.getSyncOrNull(
                userWalletId = userWalletId,
                blockchain = blockchain,
                derivationPath = derivationPath,
            )
            if (walletManager == null) {
                walletManager = walletManagerFactory.createWalletManager(
                    scanResponse = userWallet.scanResponse,
                    blockchain = blockchain,
                    derivationPath = derivationPath?.let { DerivationPath(rawPath = it) },
                )
                walletManager ?: return null

                walletManagersStore.store(userWalletId, walletManager)
            }
            return walletManager
        }
    }

    @Deprecated("Will be removed in future")
    override suspend fun getOrCreateWalletManager(userWalletId: UserWalletId, network: Network): WalletManager? {
        val blockchain = Blockchain.fromId(network.id.value)
        return getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = network.derivationPath.value,
        )
    }

    @Deprecated("Will be removed in future")
    override suspend fun getStoredWalletManagers(userWalletId: UserWalletId): List<WalletManager> {
        return walletManagersStore.getAllSync(userWalletId)
    }

    override suspend fun getDefaultAddress(userWalletId: UserWalletId, network: Network): String? {
        return getAddresses(userWalletId, network)
            .firstOrNull { it.type == AddressType.Default }
            ?.value
    }

    override suspend fun getAddresses(userWalletId: UserWalletId, network: Network): Set<Address> {
        val manager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = network,
        )

        return manager?.wallet?.addresses.orEmpty()
    }

    override suspend fun getRentInfo(userWalletId: UserWalletId, network: Network): RentData? {
        val manager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = network,
        )
        if (manager !is RentProvider) return null

        return when (val result = manager.minimalBalanceForRentExemption()) {
            is Result.Success -> {
                RentData(manager.rentAmount(), result.data)
            }
            is Result.Failure -> null
        }
    }

    @Deprecated("Will be removed in future")
    override fun getAll(userWalletId: UserWalletId): Flow<List<WalletManager>> {
        return walletManagersStore.getAll(userWalletId)
    }

    override suspend fun validateSignatureCount(
        userWalletId: UserWalletId,
        network: Network,
        signedHashes: Int,
    ): Either<Throwable, Unit> {
        return either {
            val walletManager = getOrCreateWalletManager(
                userWalletId = userWalletId,
                network = network,
            )

            val validator = ensureNotNull(walletManager as? SignatureCountValidator) {
                raise(IllegalStateException("Wallet manager is not a SignatureCountValidator"))
            }

            when (val result = validator.validateSignatureCount(signedHashes)) {
                is SimpleResult.Failure -> raise(result.error)
                is SimpleResult.Success -> Unit.right()
            }
        }
    }

    @Deprecated("Will be removed in future")
    override suspend fun getFee(
        amount: Amount,
        destination: String,
        userWalletId: UserWalletId,
        network: Network,
    ): Result<TransactionFee>? = withContext(dispatchers.io) {
        val walletManager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = network,
        )
        (walletManager as? TransactionSender)?.getFee(
            amount = amount,
            destination = destination,
        )
    }

    override suspend fun estimateFee(
        amount: Amount,
        userWalletId: UserWalletId,
        network: Network,
    ): Result<TransactionFee>? = withContext(dispatchers.io) {
        val blockchain = Blockchain.fromId(network.id.value)
        val walletManager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = network.derivationPath.value,
        )

        val destination = estimationFeeAddressFactory.makeAddress(blockchain)

        (walletManager as? TransactionSender)?.estimateFee(
            amount = amount,
            destination = destination,
        )
    }

    @Deprecated("Will be removed in future")
    override suspend fun validateTransaction(
        amount: Amount,
        fee: Amount?,
        userWalletId: UserWalletId,
        network: Network,
    ): EnumSet<TransactionError>? {
        val walletManager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = network,
        )
        return walletManager?.validateTransaction(amount, fee)
    }

    @Deprecated("Will be removed in future")
    override suspend fun createTransaction(
        amount: Amount,
        fee: Fee,
        memo: String?,
        destination: String,
        userWalletId: UserWalletId,
        network: Network,
    ): TransactionData? {
        val walletManager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = network,
        )

        return walletManager?.createTransaction(amount, fee, destination)
    }

    override suspend fun getRecentTransactions(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
    ): List<TxHistoryItem> {
        val walletManager = getOrCreateWalletManager(userWalletId = userWalletId, network = currency.network)

        if (walletManager == null) {
            Timber.e("Unable to get a wallet manager for blockchain: ${currency.network.id}")
            return emptyList()
        }

        val transactionDataConverter = TransactionDataToTxHistoryItemConverter(
            walletAddresses = SdkAddressToAddressConverter.convertList(walletManager.wallet.addresses).toSet(),
            feePaidCurrency = walletManager.wallet.blockchain.feePaidCurrency(),
        )

        return walletManager.wallet.recentTransactions
            .filter { transaction ->
                when (currency) {
                    is CryptoCurrency.Coin -> transaction.amount.type is AmountType.Coin
                    is CryptoCurrency.Token -> transaction.contractAddress == currency.contractAddress
                }
            }
            .mapNotNull(transactionDataConverter::convert)
    }

    override suspend fun tokenBalance(
        userWalletId: UserWalletId,
        network: Network,
        name: String,
        symbol: String,
        contractAddress: String,
        decimals: Int,
        id: String?,
    ): BigDecimal {
        val blockchain = Blockchain.fromId(network.id.value)
        val walletManager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = network.derivationPath.value,
        )
        requireNotNull(walletManager) { "Unable to get a wallet manager for blockchain: $blockchain" }
        return walletManager.wallet.fundsAvailable(
            AmountType.Token(
                token = Token(
                    name = name,
                    symbol = symbol,
                    contractAddress = contractAddress,
                    decimals = decimals,
                    id = id,
                ),
            ),
        )
    }

    override suspend fun getAssetRequirements(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
    ): AssetRequirementsCondition? {
        return withContext(dispatchers.io) {
            val walletManager = getOrCreateWalletManager(userWalletId = userWalletId, network = currency.network)
            val currencyType = cryptoCurrencyTypeConverter.convert(currency)
            if (walletManager !is AssetRequirementsManager) {
                return@withContext null
            }

            val condition = walletManager.requirementsCondition(currencyType) ?: return@withContext null
            requirementsConditionConverter.convert(condition)
        }
    }

    override suspend fun fulfillRequirements(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        signer: TransactionSigner,
    ): SimpleResult {
        return withContext(dispatchers.io) {
            val walletManager = getOrCreateWalletManager(userWalletId = userWalletId, network = currency.network)
            val currencyType = cryptoCurrencyTypeConverter.convert(currency)

            if (walletManager !is AssetRequirementsManager) {
                return@withContext SimpleResult.Failure(
                    BlockchainSdkError.CustomError("WalletManager is not implemented AssetRequirementsManager"),
                )
            }

            walletManager.fulfillRequirements(currencyType, signer)
        }
    }

    override suspend fun discardRequirements(userWalletId: UserWalletId, currency: CryptoCurrency): SimpleResult {
        return withContext(dispatchers.io) {
            val walletManager = getOrCreateWalletManager(userWalletId = userWalletId, network = currency.network)
            val currencyType = cryptoCurrencyTypeConverter.convert(currency)

            if (walletManager !is AssetRequirementsManager) {
                return@withContext SimpleResult.Failure(
                    BlockchainSdkError.CustomError("WalletManager is not implemented AssetRequirementsManager"),
                )
            }

            walletManager.discardRequirements(currencyType)
        }
    }

    override suspend fun checkUtxoConsolidationAvailability(userWalletId: UserWalletId, network: Network): Boolean {
        val blockchain = Blockchain.fromId(network.id.value)
        val walletManager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = network.derivationPath.value,
        ) ?: return false

        return (walletManager as? UtxoBlockchainManager)?.allowConsolidation == true
    }

    override suspend fun getNFTCollections(userWalletId: UserWalletId, network: Network): List<NFTCollection> {
        val blockchain = Blockchain.fromId(network.id.value)
        val walletManager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = network.derivationPath.value,
        ) ?: return emptyList()
        val address = walletManager.wallet.address
        return walletManager.getCollections(address)
    }

    override suspend fun getNFTAssets(
        userWalletId: UserWalletId,
        network: Network,
        collectionIdentifier: NFTCollection.Identifier,
    ): List<NFTAsset> {
        val blockchain = Blockchain.fromId(network.id.value)
        val walletManager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = network.derivationPath.value,
        ) ?: return emptyList()
        val address = walletManager.wallet.address
        return walletManager.getAssets(address, collectionIdentifier)
    }

    override suspend fun getNFTAsset(
        userWalletId: UserWalletId,
        network: Network,
        collectionIdentifier: NFTCollection.Identifier,
        assetIdentifier: NFTAsset.Identifier,
    ): NFTAsset? {
        val blockchain = Blockchain.fromId(network.id.value)
        val walletManager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = network.derivationPath.value,
        ) ?: return null
        return walletManager.getAsset(collectionIdentifier, assetIdentifier)
    }

    override suspend fun getNFTSalePrice(
        userWalletId: UserWalletId,
        network: Network,
        collectionIdentifier: NFTCollection.Identifier,
        assetIdentifier: NFTAsset.Identifier,
    ): NFTAsset.SalePrice? {
        val blockchain = Blockchain.fromId(network.id.value)
        val walletManager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = network.derivationPath.value,
        ) ?: return null
        return walletManager.getSalePrice(collectionIdentifier, assetIdentifier)
    }

    override suspend fun isAccountInitialized(userWalletId: UserWalletId, network: Network): Boolean {
        val walletManager = getOrCreateWalletManager(userWalletId = userWalletId, network = network)
        val initializableAccountWalletManger = walletManager as? InitializableAccount ?: return true
        return initializableAccountWalletManger.accountInitializationState == InitializableAccount.State.INITIALIZED
    }

    private fun updateWalletManagerTokensIfNeeded(walletManager: WalletManager, tokens: Set<CryptoCurrency.Token>) {
        if (tokens.isEmpty()) return

        val tokensToAdd = sdkTokenConverter
            .convertList(tokens)
            .filter { it !in walletManager.cardTokens }

        walletManager.addTokens(tokensToAdd)
    }

    private suspend fun readSmartContractMethods(): Map<String, SmartContractMethod> {
        return assetLoader.loadMap<SmartContractMethod>(fileName = "contract_methods")
    }
}