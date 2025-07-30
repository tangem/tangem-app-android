package com.tangem.data.tokens.repository

import com.tangem.blockchain.blockchains.polkadot.ExistentialDepositProvider
import com.tangem.blockchain.common.FeeResourceAmountProvider
import com.tangem.blockchain.common.MinimumSendAmountProvider
import com.tangem.blockchain.common.ReserveAmountProvider
import com.tangem.blockchain.common.UtxoAmountLimitProvider
import com.tangem.data.tokens.converters.UtxoConverter
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.staking.YieldBalance
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.utils.getTotalStakingBalance
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.CurrencyAmount
import com.tangem.domain.tokens.model.blockchains.UtxoAmountLimit
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.isZero
import com.tangem.utils.extensions.orZero
import kotlinx.coroutines.withContext
import java.math.BigDecimal

internal class DefaultCurrencyChecksRepository(
    private val walletManagersFacade: WalletManagersFacade,
    private val coroutineDispatchers: CoroutineDispatcherProvider,
) : CurrencyChecksRepository {

    override suspend fun getExistentialDeposit(userWalletId: UserWalletId, network: Network): BigDecimal? {
        val manager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = network,
        )

        return if (manager is ExistentialDepositProvider) manager.getExistentialDeposit() else null
    }

    override suspend fun getDustValue(userWalletId: UserWalletId, network: Network): BigDecimal? {
        val manager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = network,
        )

        return manager?.dustValue
    }

    override suspend fun getReserveAmount(userWalletId: UserWalletId, network: Network): BigDecimal? {
        val manager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = network,
        )

        return if (manager is ReserveAmountProvider) manager.getReserveAmount() else null
    }

    override suspend fun getMinimumSendAmount(userWalletId: UserWalletId, network: Network): BigDecimal? {
        return withContext(coroutineDispatchers.io) {
            val manager = walletManagersFacade.getOrCreateWalletManager(
                userWalletId = userWalletId,
                network = network,
            )

            if (manager is MinimumSendAmountProvider) manager.getMinimumSendAmount() else null
        }
    }

    override suspend fun getFeeResourceAmount(userWalletId: UserWalletId, network: Network): CurrencyAmount? {
        val manager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = network,
        )

        return if (manager is FeeResourceAmountProvider) {
            val feeResource = manager.getFeeResource()
            CurrencyAmount(
                value = feeResource.value,
                maxValue = feeResource.maxValue,
            )
        } else {
            null
        }
    }

    override suspend fun checkIfFeeResourceEnough(
        amount: BigDecimal,
        userWalletId: UserWalletId,
        network: Network,
    ): Boolean {
        val manager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = network,
        )

        return if (manager is FeeResourceAmountProvider) {
            manager.isFeeEnough(amount)
        } else {
            false
        }
    }

    override suspend fun checkIfAccountFunded(userWalletId: UserWalletId, network: Network, address: String): Boolean {
        val manager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = network,
        )

        return if (manager is ReserveAmountProvider) manager.isAccountFunded(address) else true
    }

    override suspend fun checkUtxoAmountLimit(
        userWalletId: UserWalletId,
        network: Network,
        currency: CryptoCurrency,
        amount: BigDecimal,
        fee: BigDecimal,
    ): UtxoAmountLimit? {
        val manager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = network,
        )
        val utxoAmount = if (manager is UtxoAmountLimitProvider) {
            manager.checkUtxoAmountLimit(amount, fee)
        } else {
            null
        }
        return utxoAmount?.let(UtxoConverter()::convert)
    }

    override suspend fun getRentInfoWarning(
        userWalletId: UserWalletId,
        currencyStatus: CryptoCurrencyStatus,
    ): CryptoCurrencyWarning.Rent? {
        val rentData = walletManagersFacade.getRentInfo(userWalletId, currencyStatus.currency.network) ?: return null
        val balanceValue = currencyStatus.value as? CryptoCurrencyStatus.Loaded ?: return null
        val stakingBalance = balanceValue.yieldBalance as? YieldBalance.Data
        val stakingTotalBalance = stakingBalance?.getTotalStakingBalance(
            blockchainId = currencyStatus.currency.network.rawId,
        ).orZero()
        return when {
            balanceValue.amount.isZero() && stakingTotalBalance.isZero() -> null
            balanceValue.amount < rentData.exemptionAmount && stakingTotalBalance.isZero() -> {
                CryptoCurrencyWarning.Rent(rentData.rent, rentData.exemptionAmount)
            }
            else -> null
        }
    }

    override suspend fun getRentExemptionError(
        userWalletId: UserWalletId,
        currencyStatus: CryptoCurrencyStatus,
        balanceAfterTransaction: BigDecimal,
    ): CryptoCurrencyWarning.Rent? {
        val rentData = walletManagersFacade.getRentInfo(userWalletId, currencyStatus.currency.network) ?: return null
        return when {
            balanceAfterTransaction.isZero() -> null
            balanceAfterTransaction < rentData.exemptionAmount -> {
                CryptoCurrencyWarning.Rent(rentData.rent, rentData.exemptionAmount)
            }
            else -> null
        }
    }
}