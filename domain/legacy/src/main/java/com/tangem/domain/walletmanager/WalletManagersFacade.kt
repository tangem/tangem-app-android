package com.tangem.domain.walletmanager

import com.tangem.blockchain.blockchains.solana.RentProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.address.Address
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.blockchain.common.address.AddressType
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryState
import com.tangem.domain.walletmanager.model.UpdateWalletManagerResult
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import java.math.BigDecimal

// TODO: Move to its own module
/**
 * A facade for managing wallets.
 */
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
     * Returns network explorer URL of the wallet manager associated with a user's wallet and network.
     *
     * @param userWalletId The ID of the user's wallet.
     * @param network The network.
     * @param addressType Address type.
     *
     * @return The network explorer URL, maybe empty if the wallet manager was not found.
     * */
    suspend fun getExploreUrl(userWalletId: UserWalletId, network: Network, addressType: AddressType): String

    /**
     * Returns transactions count
     *
     * @param userWalletId The ID of the user's wallet.
     * @param network The network.
     */
    suspend fun getTxHistoryState(userWalletId: UserWalletId, network: Network): TxHistoryState

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
        page: Int,
        pageSize: Int,
    ): PaginationWrapper<TxHistoryItem>

    // TODO: Remove after refactoring
    suspend fun getOrCreateWalletManager(
        userWallet: UserWallet,
        blockchain: Blockchain,
        derivationPath: String?,
    ): WalletManager?

    /**
     * Returns ordered list of addresses for selected wallet for given currency
     *
     * @param userWalletId selected wallet id
     * @param network network of currency
     */
    suspend fun getAddress(userWalletId: UserWalletId, network: Network): List<Address>

    /**
     * Returns info about rent if wallet manager implemented [RentProvider], otherwise null
     *
     * @param userWalletId selected wallet id
     * @param network network of currency
     */
    suspend fun getRentInfo(userWalletId: UserWalletId, network: Network): CryptoCurrencyWarning.Rent?

    /**
     * Returns value which indicates if the account balance drops below the existential deposit value, it will be
     * deactivated and any remaining funds will be destroyed.
     */
    suspend fun getExistentialDeposit(userWalletId: UserWalletId, network: Network): BigDecimal?
}