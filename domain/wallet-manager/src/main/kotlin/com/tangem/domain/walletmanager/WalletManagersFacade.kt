package com.tangem.domain.walletmanager

import arrow.core.Either
import com.tangem.blockchain.blockchains.solana.RentProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.pagination.Page
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchain.nft.models.NFTCollection
import com.tangem.blockchainsdk.models.UpdateWalletManagerResult
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.models.AssetRequirementsCondition
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.domain.txhistory.models.TxHistoryState
import com.tangem.domain.walletmanager.model.RentData
import com.tangem.domain.walletmanager.model.TokenInfo
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.util.EnumSet

/**
 * A facade for managing wallets.
 */
@Suppress("TooManyFunctions")
interface WalletManagersFacade {

    /**
     * Updates the wallet manager associated with a user's wallet and network.
     *
     * @param userWalletId The ID of the user's wallet.
     * @param network The network.
     * @param extraTokens Additional tokens.
     * @param xpub XPUB string to restore dynamic addresses mode if not yet active.
     * @return The result of updating the wallet manager.
     */
    suspend fun update(
        userWalletId: UserWalletId,
        network: Network,
        extraTokens: Set<CryptoCurrency.Token>,
        xpub: String? = null,
    ): UpdateWalletManagerResult

    /**
     * Removes the wallet managers associated with a user's wallet and networks.
     *
     * @param userWalletId The ID of the user's wallet.
     * @param networks Set of networks
     * */
    suspend fun remove(userWalletId: UserWalletId, networks: Set<Network>)

    suspend fun removeTokens(userWalletId: UserWalletId, tokens: Set<CryptoCurrency.Token>)

    suspend fun removeTokensByTokenInfo(userWalletId: UserWalletId, tokenInfos: Set<TokenInfo>)

    /**
     * Returns [UpdateWalletManagerResult] with last pending transactions
     *
     * @param userWalletId The ID of the user's wallet.
     * @param network The network.
     * @return The result of updating the wallet manager.
     */
    suspend fun updatePendingTransactions(userWalletId: UserWalletId, network: Network): UpdateWalletManagerResult

    /**
     * Returns network explorer URL of the wallet manager associated with a user's wallet and network.
     *
     * @param userWalletId The ID of the user's wallet.
     * @param network The network.
     * @param addressType Address type.
     * @param contractAddress Contract address if currency is Token.
     *
     * @return The network explorer URL, maybe empty if the wallet manager was not found.
     * */
    suspend fun getExploreUrl(
        userWalletId: UserWalletId,
        network: Network,
        addressType: AddressType,
        contractAddress: String?,
    ): String

    /**
     * Returns transactions count
     *
     * @param userWalletId The ID of the user's wallet.
     * @param currency currency.
     */
    suspend fun getTxHistoryState(userWalletId: UserWalletId, currency: CryptoCurrency): TxHistoryState

    /**
     * Returns transaction history items wrapped to pagination
     *
     * @param userWalletId The ID of the user's wallet.
     * @param currency currency.
     * @param page Pagination page.
     * @param pageSize Pagination size.
     */
    suspend fun getTxHistoryItems(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        page: Page,
        pageSize: Int,
    ): PaginationWrapper<TxInfo>

    @Deprecated("Will be removed in future")
    suspend fun getOrCreateWalletManager(
        userWalletId: UserWalletId,
        blockchain: Blockchain,
        derivationPath: String?,
    ): WalletManager?

    @Deprecated("Will be removed in future")
    suspend fun getOrCreateWalletManager(userWalletId: UserWalletId, network: Network): WalletManager?

    @Deprecated("Will be removed in future")
    suspend fun getStoredWalletManagers(userWalletId: UserWalletId): List<WalletManager>

    /**
     * Returns default network address for selected wallet in given network
     *
     * @param userWalletId selected wallet id
     * @param network network of currency
     */
    suspend fun getDefaultAddress(userWalletId: UserWalletId, network: Network): String?

    /** Returns list of all addresses for all currencies in selected wallet
     *
     * @param userWalletId selected wallet id
     * @param network required to create wallet manager
     */
    suspend fun getAddresses(userWalletId: UserWalletId, network: Network): Set<Address>

    /**
     * Returns info about rent if wallet manager implemented [RentProvider], otherwise null
     *
     * @param userWalletId selected wallet id
     * @param network network of currency
     */
    suspend fun getRentInfo(userWalletId: UserWalletId, network: Network): RentData?

    @Deprecated("Will be removed in future")
    fun getAll(userWalletId: UserWalletId): Flow<List<WalletManager>>

    suspend fun validateSignatureCount(
        userWalletId: UserWalletId,
        network: Network,
        signedHashes: Int,
    ): Either<Throwable, Unit>

    /**
     * Returns fee for transaction
     *
     * @param amount of transaction
     * @param destination address
     * @param userWalletId selected wallet id
     * @param network network of currency
     */
    @Deprecated("Will be removed in future")
    suspend fun getFee(
        amount: Amount,
        destination: String,
        userWalletId: UserWalletId,
        network: Network,
    ): Result<TransactionFee>?

    /**
     * Returns estimated fee for transaction
     *
     * @param amount of transaction
     * @param userWalletId selected wallet id
     * @param network network of currency
     */
    suspend fun estimateFee(amount: Amount, userWalletId: UserWalletId, network: Network): Result<TransactionFee>?

    /**
     * Validates transaction
     *
     * @param amount of transaction
     * @param fee of transaction
     * @param userWalletId selected wallet id
     * @param network network of currency
     */
    @Deprecated("Will be removed in future")
    suspend fun validateTransaction(
        amount: Amount,
        fee: Amount?,
        userWalletId: UserWalletId,
        network: Network,
    ): EnumSet<TransactionError>?

    /**
     * Creates transaction [TransactionData]
     *
     * @param amount of transaction
     * @param fee of transaction
     * @param memo of transaction optional
     * @param destination address
     * @param userWalletId selected wallet id
     * @param network network of currency
     */
    @Suppress("LongParameterList")
    @Deprecated("Will be removed in future")
    suspend fun createTransaction(
        amount: Amount,
        fee: Fee,
        memo: String?,
        destination: String,
        userWalletId: UserWalletId,
        network: Network,
    ): TransactionData?

    /** Get recent transactions of [userWalletId] for [currency] */
    suspend fun getRecentTransactions(userWalletId: UserWalletId, currency: CryptoCurrency): List<TxInfo>

    @Suppress("LongParameterList")
    suspend fun tokenBalance(
        userWalletId: UserWalletId,
        network: Network,
        name: String,
        symbol: String,
        contractAddress: String,
        decimals: Int,
        id: String? = null,
    ): BigDecimal

    @Throws(IllegalStateException::class)
    suspend fun getNativeTokenBalance(
        userWalletId: UserWalletId,
        networkId: String,
        derivationPath: String?,
    ): BigDecimal

    /**
     * Computes the on-chain miner fee embedded in a Bitcoin swap [psbtBase64], in satoshi.
     *
     * Swap providers return a "naked" PSBT whose fee is implied by `sum(inputs) - sum(outputs)`
     * rather than reported separately. Returns `null` if the wallet manager is unavailable, the
     * network is not a PSBT-capable Bitcoin chain, or the fee cannot be derived from the PSBT.
     */
    suspend fun getPsbtFee(userWalletId: UserWalletId, network: Network, psbtBase64: String): BigDecimal?

    /**
     * Get requirements for asset(currency)
     * @return null if there's no requirement, otherwise [AssetRequirementsCondition].
     */
    suspend fun getAssetRequirements(userWalletId: UserWalletId, currency: CryptoCurrency): AssetRequirementsCondition?

    suspend fun fulfillRequirements(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        signer: TransactionSigner,
    ): SimpleResult

    suspend fun discardRequirements(userWalletId: UserWalletId, currency: CryptoCurrency): SimpleResult

    /**
     * Indicates self send availability
     *
     * @param userWalletId selected user wallet
     * @param network availability for network
     */
    suspend fun checkSelfSendAvailability(userWalletId: UserWalletId, network: Network): Boolean

    suspend fun getNFTCollections(userWalletId: UserWalletId, network: Network): List<NFTCollection>

    suspend fun getNFTAssets(
        userWalletId: UserWalletId,
        network: Network,
        collectionIdentifier: NFTCollection.Identifier,
    ): List<NFTAsset>

    suspend fun getNFTAsset(
        userWalletId: UserWalletId,
        network: Network,
        collectionIdentifier: NFTCollection.Identifier,
        assetIdentifier: NFTAsset.Identifier,
    ): NFTAsset?

    suspend fun getNFTSalePrice(
        userWalletId: UserWalletId,
        network: Network,
        collectionIdentifier: NFTCollection.Identifier,
        assetIdentifier: NFTAsset.Identifier,
    ): NFTAsset.SalePrice?

    suspend fun getNFTExploreUrl(network: Network, assetIdentifier: NFTAsset.Identifier): String?

    /**
     * If wallet manager implements [InitializableAccount] then returns [InitializableAccount.isAccountInitialized]
     * value. Otherwise always return true
     */
    suspend fun isAccountInitialized(userWalletId: UserWalletId, network: Network): Boolean

    // region Dynamic Addresses

    suspend fun enableXpubMode(userWalletId: UserWalletId, network: Network, xpub: String): SimpleResult

    suspend fun disableXpubMode(userWalletId: UserWalletId, network: Network): SimpleResult

    suspend fun isDynamicAddressesEnabled(userWalletId: UserWalletId, network: Network): Boolean

    suspend fun getDynamicAddressesReceiveAddress(userWalletId: UserWalletId, network: Network): String?

    suspend fun getDynamicAddressesLastUsedReceiveAddress(userWalletId: UserWalletId, network: Network): String?

    suspend fun hasDynamicAddressesNonBaseBalances(userWalletId: UserWalletId, network: Network): Boolean

    /**
     * Silently probes the xpub for balances on non-base derived addresses.
     * Does not mutate wallet manager state; can be called when dynamic addresses mode is disabled.
     *
     * @return true if any non-base derived address has a non-zero balance, false on probe failure,
     * when the network doesn't support dynamic addresses, or when no extra funds were found.
     */
    suspend fun probeHasFundsOnAdditionalAddresses(userWalletId: UserWalletId, network: Network, xpub: String): Boolean

    // endregion Dynamic Addresses
}