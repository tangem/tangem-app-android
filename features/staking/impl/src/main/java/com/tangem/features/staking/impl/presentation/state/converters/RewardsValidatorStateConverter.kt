package com.tangem.features.staking.impl.presentation.state.converters

import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.stakekit.*
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.utils.Provider
import com.tangem.utils.StringsSigns.PLUS
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

internal class RewardsValidatorStateConverter(
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val yield: Yield,
) : Converter<Unit, StakingStates.RewardsValidatorsState> {
    override fun convert(value: Unit): StakingStates.RewardsValidatorsState {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()

        val yieldBalance = cryptoCurrencyStatus.value.yieldBalance
        return if (yieldBalance is YieldBalance.Data) {
            val balances = yieldBalance.balance.items
            StakingStates.RewardsValidatorsState.Data(
                isPrimaryButtonEnabled = true,
                rewards = balances
                    .filter { it.type == BalanceType.REWARDS }
                    .mapRewardBalances(cryptoCurrencyStatus)
                    .toPersistentList(),
            )
        } else {
            StakingStates.RewardsValidatorsState.Empty()
        }
    }

    private fun List<BalanceItem>.mapRewardBalances(cryptoCurrencyStatus: CryptoCurrencyStatus) =
        this.mapNotNull { balance ->
            val validator = yield.validators.firstOrNull {
                it.address.contains(balance.validatorAddress.orEmpty(), ignoreCase = true)
            }
            val cryptoValue = balance.amount
            val fiatValue = cryptoCurrencyStatus.value.fiatRate?.times(cryptoValue)
            val unbondingPeriod = yield.metadata.cooldownPeriod.days
            validator?.toBalanceState(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                cryptoValue = cryptoValue,
                fiatValue = fiatValue,
                unbondingPeriod = pluralReference(
                    id = R.plurals.common_days,
                    count = unbondingPeriod,
                    formatArgs = wrappedList(unbondingPeriod),
                ),
                pendingActions = balance.pendingActions.toPersistentList(),
            )
        }

    private fun Yield.Validator.toBalanceState(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        cryptoValue: BigDecimal,
        fiatValue: BigDecimal?,
        unbondingPeriod: TextReference,
        pendingActions: ImmutableList<PendingAction>,
    ): BalanceState {
        val appCurrency = appCurrencyProvider()
        val cryptoCurrency = cryptoCurrencyStatus.currency

        val cryptoAmount = stringReference(
            BigDecimalFormatter.formatCryptoAmount(
                cryptoAmount = cryptoValue,
                cryptoCurrency = cryptoCurrency,
            ),
        )
        val fiatAmount = combinedReference(
            stringReference(PLUS),
            stringReference(
                BigDecimalFormatter.formatFiatAmount(
                    fiatAmount = fiatValue,
                    fiatCurrencyCode = appCurrency.code,
                    fiatCurrencySymbol = appCurrency.symbol,
                ),
            ),
        )

        return BalanceState(
            validator = this,
            cryptoValue = cryptoValue.parseBigDecimal(cryptoCurrency.decimals),
            cryptoDecimal = cryptoValue,
            cryptoAmount = cryptoAmount,
            fiatAmount = fiatAmount,
            rawCurrencyId = cryptoCurrency.id.rawCurrencyId,
            unbondingPeriod = unbondingPeriod,
            pendingActions = pendingActions,
        )
    }
}