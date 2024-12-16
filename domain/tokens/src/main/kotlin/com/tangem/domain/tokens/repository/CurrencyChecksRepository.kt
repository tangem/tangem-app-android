package com.tangem.domain.tokens.repository

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.CurrencyAmount
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.blockchains.UtxoAmountLimit
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.wallets.models.UserWalletId
import java.math.BigDecimal

interface CurrencyChecksRepository {

    /**
     * Returns value which indicates if the account balance drops below the existential deposit value, it will be
     * deactivated and any remaining funds will be destroyed.
     */
    suspend fun getExistentialDeposit(userWalletId: UserWalletId, network: Network): BigDecimal?

    /** Returns dust value */
    suspend fun getDustValue(userWalletId: UserWalletId, network: Network): BigDecimal?

    /** Returns reserve amount which is required to create an account */
    suspend fun getReserveAmount(userWalletId: UserWalletId, network: Network): BigDecimal?

    /** Returns minimum send transaction amount */
    suspend fun getMinimumSendAmount(userWalletId: UserWalletId, network: Network): BigDecimal?

    /** Returns a fee resource amount available and max for paying fees in several blockchains */
    suspend fun getFeeResourceAmount(userWalletId: UserWalletId, network: Network): CurrencyAmount?

    /** Is fee resource enough for a transaction amount */
    suspend fun checkIfFeeResourceEnough(amount: BigDecimal, userWalletId: UserWalletId, network: Network): Boolean

    /** Returns true if account with [address] was reserved with minimum amount */
    suspend fun checkIfAccountFunded(userWalletId: UserWalletId, network: Network, address: String): Boolean

    /** Checks if transaction amount is within the UTXO limit */
    suspend fun checkUtxoAmountLimit(
        userWalletId: UserWalletId,
        network: Network,
        currency: CryptoCurrency,
        amount: BigDecimal,
        fee: BigDecimal,
    ): UtxoAmountLimit?

    /**
     * Checks if need warning for Solana rent
     */
    suspend fun getRentInfoWarning(
        userWalletId: UserWalletId,
        currencyStatus: CryptoCurrencyStatus,
    ): CryptoCurrencyWarning.Rent?

    /**
     * Checks if need error before Solana tx about rent exemption
     * @param balanceAfterTransaction if non null, checks with balance remains after transaction
     */
    suspend fun getRentExemptionError(
        userWalletId: UserWalletId,
        currencyStatus: CryptoCurrencyStatus,
        balanceAfterTransaction: BigDecimal,
    ): CryptoCurrencyWarning.Rent?
}