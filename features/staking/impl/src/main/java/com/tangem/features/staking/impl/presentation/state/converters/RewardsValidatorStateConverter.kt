package com.tangem.features.staking.impl.presentation.state.converters

import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.staking.BalanceItem
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.staking.model.StakingIntegration
import com.tangem.domain.staking.model.StakingTarget
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

internal class RewardsValidatorStateConverter(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val integration: StakingIntegration,
) : Converter<Unit, StakingStates.RewardsValidatorsState> {
    override fun convert(value: Unit): StakingStates.RewardsValidatorsState {
        val stakingBalance = cryptoCurrencyStatus.value.stakingBalance
        return if (stakingBalance is StakingBalance.Data.StakeKit) {
            val balances = stakingBalance.balance.items
            StakingStates.RewardsValidatorsState.Data(
                isPrimaryButtonEnabled = true,
                rewards = balances
                    .filter { it.type == BalanceType.REWARDS }
                    .mapRewardBalances(cryptoCurrencyStatus)
                    .toPersistentList(),
            )
        } else {
            // TODO p2p
            StakingStates.RewardsValidatorsState.Empty()
        }
    }

    private fun List<BalanceItem>.mapRewardBalances(cryptoCurrencyStatus: CryptoCurrencyStatus) =
        this.mapNotNull { balance ->
            val target = integration.targets.firstOrNull {
                it.address.contains(balance.validatorAddress.orEmpty(), ignoreCase = true)
            }
            val cryptoValue = balance.amount
            val fiatValue = cryptoCurrencyStatus.value.fiatRate?.times(cryptoValue)

            target?.toBalanceState(
                balance = balance,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                cryptoValue = cryptoValue,
                fiatValue = fiatValue,
            )
        }

    private fun StakingTarget.toBalanceState(
        balance: BalanceItem,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        cryptoValue: BigDecimal,
        fiatValue: BigDecimal?,
    ): BalanceState {
        val appCurrency = appCurrencyProvider()
        val cryptoCurrency = cryptoCurrencyStatus.currency
        val cryptoAmount = stringReference(
            cryptoValue.format {
                crypto(cryptoCurrency)
            },
        )
        val formattedFiatAmount = stringReference(
            fiatValue.format {
                fiat(
                    fiatCurrencyCode = appCurrency.code,
                    fiatCurrencySymbol = appCurrency.symbol,
                )
            },
        )

        return BalanceState(
            groupId = balance.groupId,
            target = this,
            title = stringReference(this.name),
            subtitle = null,
            cryptoValue = cryptoValue.parseBigDecimal(cryptoCurrency.decimals),
            cryptoAmount = cryptoValue,
            formattedCryptoAmount = cryptoAmount,
            fiatAmount = fiatValue,
            formattedFiatAmount = formattedFiatAmount,
            rawCurrencyId = cryptoCurrency.id.rawCurrencyId?.value,
            pendingActions = balance.pendingActions.toPersistentList(),
            isClickable = true,
            type = balance.type,
            isPending = balance.isPending,
            targetAddress = balance.validatorAddress,
        )
    }
}