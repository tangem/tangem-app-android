package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.common.ui.amountScreen.converters.AmountStateConverter
import com.tangem.common.ui.amountScreen.models.AmountParameters
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.model.StakingClickIntents
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.Provider
import com.tangem.utils.transformer.Transformer

internal class SetAmountDataTransformer(
    private val clickIntents: StakingClickIntents,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val userWalletProvider: Provider<UserWallet>,
    private val appCurrencyProvider: Provider<AppCurrency>,
) : Transformer<StakingUiState> {

    private val iconStateConverter by lazy(::CryptoCurrencyToIconStateConverter)

    override fun transform(prevState: StakingUiState): StakingUiState {
        val title = if (prevState.actionType is StakingActionCommonType.Exit) {
            resourceReference(R.string.staking_staked_amount)
        } else {
            stringReference(userWalletProvider().name)
        }
        val cryptoBalanceValue = cryptoCurrencyStatusProvider().value
        val (amount, fiatAmount) = if (prevState.actionType !is StakingActionCommonType.Enter) {
            prevState.balanceState?.cryptoAmount to prevState.balanceState?.fiatAmount
        } else {
            cryptoBalanceValue.amount to cryptoBalanceValue.fiatAmount
        }
        val maxEnterAmount = EnterAmountBoundary(
            amount = amount,
            fiatAmount = fiatAmount,
            fiatRate = cryptoBalanceValue.fiatRate,
        )

        return prevState.copy(
            amountState = AmountStateConverter(
                clickIntents = clickIntents,
                cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
                appCurrencyProvider = appCurrencyProvider,
                iconStateConverter = iconStateConverter,
                maxEnterAmount = maxEnterAmount,
            ).convert(
                AmountParameters(
                    title = title,
                    value = "",
                ),
            ),
        )
    }
}