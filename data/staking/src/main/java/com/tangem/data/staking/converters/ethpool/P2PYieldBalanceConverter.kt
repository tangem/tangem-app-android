package com.tangem.data.staking.converters.ethpool

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.staking.*
import com.tangem.domain.staking.model.ethpool.P2PEthPoolAccount
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import java.math.BigDecimal

/**
 * tmp solution before facade implementation
 */
internal object P2PYieldBalanceConverter {

    private const val ETH_DECIMALS = 18
    private const val ETH_SYMBOL = "ETH"
    private const val ETH_NAME = "Ethereum"
    private const val ETH_COINGECKO_ID = "ethereum"

    fun convert(
        account: P2PEthPoolAccount,
        vault: P2PEthPoolVault,
        address: String,
        source: StatusSource,
    ): YieldBalance {
        val integrationId = "p2p-ethereum-pooled"
        val stakingId = StakingID(
            integrationId = integrationId,
            address = address,
        )

        val balanceItems = buildBalanceItems(account, vault)

        return if (balanceItems.isEmpty()) {
            YieldBalance.Empty(stakingId = stakingId, source = source)
        } else {
            YieldBalance.Data(
                stakingId = stakingId,
                source = source,
                balance = YieldBalanceItem(
                    items = balanceItems,
                    integrationId = integrationId,
                ),
            )
        }
    }

    private fun buildBalanceItems(account: P2PEthPoolAccount, vault: P2PEthPoolVault): List<BalanceItem> = buildList {
        if (account.stake.assets > BigDecimal.ZERO) {
            add(
                createBalanceItem(
                    groupId = "p2p-staked",
                    amount = account.stake.assets,
                    type = BalanceType.STAKED,
                    validatorAddress = vault.vaultAddress,
                ),
            )
        }
    }

    private fun createBalanceItem(
        groupId: String,
        amount: BigDecimal,
        type: BalanceType,
        validatorAddress: String,
    ): BalanceItem {
        return BalanceItem(
            groupId = groupId,
            token = createEthToken(),
            type = type,
            amount = amount,
            rawCurrencyId = ETH_COINGECKO_ID,
            validatorAddress = validatorAddress,
            date = null,
            pendingActions = emptyList(),
            pendingActionsConstraints = emptyList(),
            isPending = false,
        )
    }

    private fun createEthToken(): YieldToken {
        return YieldToken(
            name = ETH_NAME,
            network = NetworkType.ETHEREUM,
            symbol = ETH_SYMBOL,
            decimals = ETH_DECIMALS,
            address = null,
            coinGeckoId = ETH_COINGECKO_ID,
            logoURI = null,
            isPoints = false,
        )
    }
}