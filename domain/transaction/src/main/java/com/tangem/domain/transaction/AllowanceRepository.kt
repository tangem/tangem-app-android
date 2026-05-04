package com.tangem.domain.transaction

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.models.AllowanceInfo
import java.math.BigDecimal

/**
 * Repository interface for managing token allowances in the context of blockchain transactions.
 */
interface AllowanceRepository {

    /**
     * Retrieves the allowance information for a specific spender and required amount.
     *
     * @param userWalletId          The ID of the user's wallet.
     * @param cryptoCurrency        The cryptocurrency for which the allowance is being checked (must be a token).
     * @param spenderAddress        The address of the spender for whom the allowance is being checked.
     * @param requiredAmount        The amount that is required for the transaction.
     *
     * @return An [AllowanceInfo] object that indicates whether the current allowance.
     * @throws IllegalStateException if the provided [cryptoCurrency] is not a token.
     */
    suspend fun getAllowanceInfo(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        spenderAddress: String,
        requiredAmount: BigDecimal,
    ): AllowanceInfo

    /**
     * Retrieves the current allowance for a specific spender.
     *
     * @param userWalletId          The ID of the user's wallet.
     * @param cryptoCurrency        The cryptocurrency for which the allowance is being checked (must be a token).
     * @param spenderAddress        The address of the spender for whom the allowance is being checked.
     *
     * @return The current allowance as a [BigDecimal].
     * @throws IllegalStateException if the provided [cryptoCurrency] is not a token.
     */
    suspend fun getAllowance(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        spenderAddress: String,
    ): BigDecimal
}