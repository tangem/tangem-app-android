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
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryState
import com.tangem.domain.walletmanager.model.UpdateWalletManagerResult
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.util.EnumSet
// [REDACTED_TODO_COMMENT]
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
     * @return The result of updating the wallet manager.
     */
    suspend fun update(
        userWalletId: UserWalletId,
        network: Network,
        extraTokens: Set<CryptoCurrency.Token>,
    ): UpdateWalletManagerResult

    /**
     * Removes the wallet managers associated with a user's wallet and networks.
     *
     * @param userWalletId The ID of the user's wallet.
     * @param networks Set of networks
     * */
    suspend fun remove(userWalletId: UserWalletId, networks: Set<Network>)

    suspend fun removeTokens(userWalletId: UserWalletId, tokens: Set<CryptoCurrency.Token>)

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
    ): PaginationWrapper<TxHistoryItem>

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
     * Returns ordered list of addresses for selected wallet for given currency
     *
     * @param userWalletId selected wallet id
     * @param network network of currency
     */
    @Deprecated("Use NetworkAddress from CryptoCurrencyStatus")
    suspend fun getAddress(userWalletId: UserWalletId, network: Network): List<Address>

    /** Returns list of all addresses for all currencies in selected wallet
     *
     * @param userWalletId selected wallet id
     * @param network required to create wallet manager
     */
    @Deprecated("Use NetworkAddress from CryptoCurrencyStatus")
    suspend fun getAddresses(userWalletId: UserWalletId, network: Network): Set<Address>

    /**
     * Returns info about rent if wallet manager implemented [RentProvider], otherwise null
     *
     * @param userWalletId selected wallet id
     * @param network network of currency
     */
    suspend fun getRentInfo(userWalletId: UserWalletId, network: Network): CryptoCurrencyWarning.Rent?

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

    /**
     * Sends transaction
     *
     * @param txData transaction data
     * @param signer card signer
     * @param userWalletId selected wallet id
     * @param network network of currency
     */
    @Deprecated("Will be removed in future")
    suspend fun sendTransaction(
        txData: TransactionData,
        signer: CommonSigner,
        userWalletId: UserWalletId,
        network: Network,
    ): SimpleResult

    /** Get recent transactions of [userWalletId] for [currency] */
    suspend fun getRecentTransactions(userWalletId: UserWalletId, currency: CryptoCurrency): List<TxHistoryItem>

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
}
