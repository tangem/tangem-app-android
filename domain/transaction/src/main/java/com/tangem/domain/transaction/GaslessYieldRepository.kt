package com.tangem.domain.transaction

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import java.math.BigDecimal

/**
 * Narrow repository interface used by [com.tangem.domain.transaction.usecase.gasless.ResolveGaslessFeePlanUseCase]
 * to query yield-module state without introducing a circular module dependency.
 *
 * [com.tangem.domain.yield.supply.YieldSupplyTransactionRepository] extends this interface.
 */
interface GaslessYieldRepository {

    /** Returns the effective (liquid) protocol balance for [cryptoCurrency], or null if unavailable. */
    suspend fun getEffectiveProtocolBalance(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): BigDecimal?

    /** Returns the yield-module contract address for [cryptoCurrency], or null if unavailable. */
    suspend fun getYieldContractAddress(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): String?

    /**
     * Builds an upgrade-wrapped `withdraw(yieldToken, amount)` call data for the user's yield module.
     * @throws com.tangem.blockchain.yieldsupply.providers.YieldModuleUpgradeUnavailableException
     * @throws com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionIndeterminateException
     */
    suspend fun createPartialWithdrawCallData(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        amount: Amount,
    ): SmartContractCallData
}