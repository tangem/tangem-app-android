package com.tangem.features.foryou.impl

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.staking.BalanceItem
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.staking.YieldBalanceItem
import com.tangem.domain.models.staking.YieldToken
import java.math.BigDecimal

/**
 * Real [CryptoCurrencyStatus.Value] instances shared by the For You tests.
 *
 * They are built rather than mocked because the balance accessors the converters call
 * (`getTotalFiatAmount` / `getTotalCryptoAmount`) read `fiatRate`, `stakingBalance` and `contributions`
 * on top of `amount` / `fiatAmount`. A partial mock answers none of those and throws instead, so every
 * field a reader picks up would otherwise have to be stubbed into every fixture.
 */
internal fun createLoadedValue(
    amount: BigDecimal = BigDecimal.ONE,
    fiatAmount: BigDecimal = BigDecimal.ONE,
    fiatRate: BigDecimal = BigDecimal.ONE,
    staking: StakingBalance? = null,
    source: StatusSource = StatusSource.ACTUAL,
): CryptoCurrencyStatus.Loaded = CryptoCurrencyStatus.Loaded(
    amount = amount,
    fiatAmount = fiatAmount,
    fiatRate = fiatRate,
    priceChange = BigDecimal.ZERO,
    stakingBalance = staking,
    yieldSupplyStatus = null,
    hasCurrentNetworkTransactions = false,
    pendingTransactions = emptySet(),
    networkAddress = createNetworkAddress(),
    sources = CryptoCurrencyStatus.Sources(
        networkSource = source,
        quoteSource = source,
        stakingBalanceSource = source,
    ),
)

/** A non-content status: carries a null fiatAmount (unknown balance), not a resolved zero. */
internal fun createUnreachableValue(): CryptoCurrencyStatus.Unreachable = CryptoCurrencyStatus.Unreachable(
    priceChange = null,
    fiatRate = null,
    networkAddress = null,
)

internal fun createMissedDerivationValue(): CryptoCurrencyStatus.MissedDerivation =
    CryptoCurrencyStatus.MissedDerivation(priceChange = null, fiatRate = null)

/**
 * A StakeKit balance of [staked] units sitting **outside** the network balance, which is how every
 * non-Cardano network reports it — so the accessors add all of it on top of the currency's `amount`.
 */
internal fun createStakedBalance(staked: BigDecimal): StakingBalance.Data.StakeKit = StakingBalance.Data.StakeKit(
    stakingId = StakingID(integrationId = "integration", address = "0x1"),
    source = StatusSource.ACTUAL,
    balance = YieldBalanceItem(
        items = listOf(
            BalanceItem(
                groupId = "group",
                token = YieldToken.ETH,
                type = BalanceType.STAKED,
                amount = staked,
                rawCurrencyId = null,
                validatorAddress = null,
                date = null,
                pendingActions = emptyList(),
                pendingActionsConstraints = emptyList(),
                isPending = false,
            ),
        ),
        integrationId = "integration",
    ),
)

private fun createNetworkAddress(): NetworkAddress = NetworkAddress.Single(
    defaultAddress = NetworkAddress.Address(value = "0x1", type = NetworkAddress.Address.Type.Primary),
)