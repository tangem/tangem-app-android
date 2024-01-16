package com.tangem.domain.walletmanager

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.right
import com.squareup.moshi.Moshi
import com.tangem.blockchain.blockchains.polkadot.ExistentialDepositProvider
import com.tangem.blockchain.blockchains.solana.RentProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.address.EstimationFeeAddressFactory
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.txhistory.TransactionHistoryRequest
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.datasource.asset.AssetReader
import com.tangem.datasource.config.ConfigManager
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import com.tangem.domain.common.util.hasDerivation
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryState
import com.tangem.domain.walletmanager.model.UpdateWalletManagerResult
import com.tangem.domain.walletmanager.utils.*
import com.tangem.domain.walletmanager.utils.WalletManagerFactory
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.math.BigDecimal
import java.util.EnumSet

@Suppress("LargeClass", "TooManyFunctions")
// [REDACTED_TODO_COMMENT]
@Deprecated("Inject the WalletManagerFacade interface using DI instead")
class DefaultWalletManagersFacade(
    private val walletManagersStore: WalletManagersStore,
    private val userWalletsStore: UserWalletsStore,
    configManager: ConfigManager,
    mnemonic: Mnemonic,
    assetReader: AssetReader,
    moshi: Moshi,
) : WalletManagersFacade {

    private val demoConfig by lazy { DemoConfig() }
    private val resultFactory by lazy { UpdateWalletManagerResultFactory() }
    private val walletManagerFactory by lazy { WalletManagerFactory(configManager) }
    private val sdkTokenConverter by lazy { SdkTokenConverter() }
    private val txHistoryStateConverter by lazy { SdkTransactionHistoryStateConverter() }
    private val txHistoryItemConverter by lazy { SdkTransactionHistoryItemConverter(assetReader, moshi) }
    private val estimationFeeAddressFactory by lazy { EstimationFeeAddressFactory(mnemonic) }

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

        walletManagersStore.remove(userWalletId) { walletManager ->
            val wallet = walletManager.wallet
            val blockchainToDerivationPath = wallet.blockchain to wallet.publicKey.derivationPath?.rawPath

            blockchainToDerivationPath in blockchainsToDerivationPaths
        }
    }

    override suspend fun removeTokens(userWalletId: UserWalletId, tokens: Set<CryptoCurrency.Token>) {
        if (tokens.isEmpty()) return

        tokens
            .groupBy(CryptoCurrency.Token::network)
            .forEach { (network, networkTokens) ->
                removeTokens(userWalletId, network, networkTokens)
            }
    }

    private suspend fun removeTokens(
        userWalletId: UserWalletId,
        network: Network,
        networkTokens: List<CryptoCurrency.Token>,
    ) {
        val walletManager = walletManagersStore.getSyncOrNull(
            userWalletId = userWalletId,
            blockchain = Blockchain.fromId(network.id.value),
            derivationPath = network.derivationPath.value,
        ) ?: return
        val tokensToRemove = sdkTokenConverter.convertList(networkTokens)

        tokensToRemove.forEach { token ->
            walletManager.removeToken(token)
        }

        walletManagersStore.store(userWalletId, walletManager)
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
            return UpdateWalletManagerResult.UnreachableWithoutAddresses
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
                    is CryptoCurrency.Token -> TransactionHistoryRequest.FilterType.Contract(currency.contractAddress)
                },
            )
            .let(txHistoryStateConverter::convert)
    }

    override suspend fun getTxHistoryItems(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        page: Int,
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
                page = TransactionHistoryRequest.Page(number = page, size = pageSize),
                filterType = when (currency) {
                    is CryptoCurrency.Coin -> TransactionHistoryRequest.FilterType.Coin
                    is CryptoCurrency.Token -> TransactionHistoryRequest.FilterType.Contract(currency.contractAddress)
                },
            ),
        )

        return when (itemsResult) {
            is Result.Success -> PaginationWrapper(
                page = itemsResult.data.page,
                totalPages = itemsResult.data.totalPages,
                itemsOnPage = itemsResult.data.itemsOnPage,
                items = txHistoryItemConverter.convertList(itemsResult.data.items),
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
            return UpdateWalletManagerResult.UnreachableWithoutAddresses
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
            resultFactory.getNoAccountResult(walletManager = walletManager, customMessage = e.customMessage)
        } catch (e: Throwable) {
            Timber.w(e, "Unable to update a wallet manager for: ${walletManager.wallet.blockchain}")

            resultFactory.getUnreachableResult(walletManager)
        }
    }

    private fun getLastWalletManagerResult(walletManager: WalletManager): UpdateWalletManagerResult {
        return try {
            resultFactory.getResult(walletManager)
        } catch (e: BlockchainSdkError.AccountNotFound) {
            resultFactory.getNoAccountResult(walletManager = walletManager, customMessage = e.customMessage)
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
            ) ?: return null

            walletManagersStore.store(userWalletId, walletManager)
        }

        return walletManager
    }

    @Deprecated("Will be removed in future")
    override suspend fun getStoredWalletManagers(userWalletId: UserWalletId): List<WalletManager> {
        return walletManagersStore.getAllSync(userWalletId)
    }

    @Deprecated(
        "Use NetworkAddress from CryptoCurrencyStatus",
        ReplaceWith("cryptoCurrencyStatus.value.networkAddress"),
    )
    override suspend fun getAddress(userWalletId: UserWalletId, network: Network): List<Address> {
        return getAddresses(userWalletId, network).sortedBy { it.type }
    }

    @Deprecated("Use NetworkAddress from CryptoCurrencyStatus")
    override suspend fun getAddresses(userWalletId: UserWalletId, network: Network): Set<Address> {
        val manager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = network,
        )

        return manager?.wallet?.addresses.orEmpty()
    }

    override suspend fun getRentInfo(userWalletId: UserWalletId, network: Network): CryptoCurrencyWarning.Rent? {
        val manager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = network,
        )
        if (manager !is RentProvider) return null

        return when (val result = manager.minimalBalanceForRentExemption()) {
            is Result.Success -> {
                val balance = manager.wallet.fundsAvailable(AmountType.Coin)
                val outgoingTxs = manager.wallet.recentTransactions
                    .filter {
                        it.sourceAddress == manager.wallet.address &&
                            it.amount.type == AmountType.Coin &&
                            it.status != TransactionStatus.Confirmed
                    }

                val rentExempt = result.data
                val setRent = if (outgoingTxs.isEmpty()) {
                    balance < rentExempt
                } else {
                    val outgoingAmount = outgoingTxs.sumOf { it.amount.value ?: BigDecimal.ZERO }
                    val rest = balance.minus(outgoingAmount)
                    balance < rest
                }

                if (setRent) CryptoCurrencyWarning.Rent(manager.rentAmount(), rentExempt) else null
            }
            is Result.Failure -> null
        }
    }

    @Deprecated("Will be removed in future")
    override suspend fun getExistentialDeposit(userWalletId: UserWalletId, network: Network): BigDecimal? {
        val manager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = network,
        )

        return if (manager is ExistentialDepositProvider) manager.getExistentialDeposit() else null
    }

    @Deprecated("Will be removed in future")
    override suspend fun getDustValue(userWalletId: UserWalletId, network: Network): BigDecimal? {
        val blockchain = Blockchain.fromId(network.id.value)
        val manager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = network.derivationPath.value,
        )

        return manager?.dustValue
    }

    @Deprecated("Will be removed in future")
    override suspend fun getReserveAmount(userWalletId: UserWalletId, network: Network): BigDecimal? {
        val manager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = network,
        )

        return if (manager is ReserveAmountProvider) manager.getReserveAmount() else null
    }

    @Deprecated("Will be removed in future")
    override suspend fun checkIfAccountFunded(userWalletId: UserWalletId, network: Network, address: String): Boolean {
        val manager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = network,
        )

        return if (manager is ReserveAmountProvider) manager.isAccountFunded(address) else true
    }

    @Deprecated("Will be removed in future")
    override suspend fun checkUtxoAmountLimit(
        userWalletId: UserWalletId,
        network: Network,
        amount: BigDecimal,
        fee: BigDecimal,
    ): UtxoAmountLimit? {
        val manager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = network,
        )

        return if (manager is UtxoAmountLimitProvider) manager.checkUtxoAmountLimit(amount, fee) else null
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
    ): Result<TransactionFee>? {
        val walletManager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = network,
        )
        return (walletManager as? TransactionSender)?.getFee(
            amount = amount,
            destination = destination,
        )
    }

    override suspend fun estimateFee(
        amount: Amount,
        userWalletId: UserWalletId,
        network: Network,
    ): Result<TransactionFee>? {
        val blockchain = Blockchain.fromId(network.id.value)
        val walletManager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = network.derivationPath.value,
        )

        val destination = estimationFeeAddressFactory.makeAddress(blockchain)

        return (walletManager as? TransactionSender)?.estimateFee(
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

    @Deprecated("Will be removed in future")
    override suspend fun sendTransaction(
        txData: TransactionData,
        signer: CommonSigner,
        userWalletId: UserWalletId,
        network: Network,
    ): SimpleResult {
        val walletManager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = network,
        )
        return (walletManager as TransactionSender).send(txData, signer)
    }

    override suspend fun getRecentTransactions(
        userWalletId: UserWalletId,
        blockchain: Blockchain,
        derivationPath: String?,
    ): List<TxHistoryItem> {
        val walletManager = getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = derivationPath,
        )

        if (walletManager == null) {
            Timber.e("Unable to get a wallet manager for blockchain: $blockchain")
            return emptyList()
        }

        val transactionDataConverter = TransactionDataToTxHistoryItemConverter(
            walletAddresses = SdkAddressToAddressConverter.convertList(walletManager.wallet.addresses).toSet(),
        )

        return walletManager.wallet.recentTransactions.mapNotNull(transactionDataConverter::convert)
    }

    private fun updateWalletManagerTokensIfNeeded(walletManager: WalletManager, tokens: Set<CryptoCurrency.Token>) {
        if (tokens.isEmpty()) return

        val tokensToAdd = sdkTokenConverter
            .convertList(tokens)
            .filter { it !in walletManager.cardTokens }

        walletManager.addTokens(tokensToAdd)
    }

    private suspend fun getOrCreateWalletManager(userWalletId: UserWalletId, network: Network): WalletManager? {
        val blockchain = Blockchain.fromId(network.id.value)
        return getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = network.derivationPath.value,
        )
    }
}
