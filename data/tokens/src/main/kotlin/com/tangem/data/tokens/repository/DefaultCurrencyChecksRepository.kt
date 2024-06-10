package com.tangem.data.tokens.repository

import com.tangem.blockchain.blockchains.polkadot.ExistentialDepositProvider
import com.tangem.blockchain.common.FeeResourceAmountProvider
import com.tangem.blockchain.common.ReserveAmountProvider
import com.tangem.blockchain.common.UtxoAmountLimitProvider
import com.tangem.data.tokens.converters.UtxoConverter
import com.tangem.domain.tokens.model.CurrencyAmount
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.blockchains.UtxoAmountLimit
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import java.math.BigDecimal

internal class DefaultCurrencyChecksRepository(
    private val walletManagersFacade: WalletManagersFacade,
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
}