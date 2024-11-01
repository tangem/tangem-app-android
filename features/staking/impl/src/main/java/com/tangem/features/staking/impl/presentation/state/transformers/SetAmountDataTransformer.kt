package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.common.ui.amountScreen.converters.AmountStateConverter
import com.tangem.common.ui.amountScreen.models.AmountParameters
import com.tangem.common.ui.amountScreen.models.MaxEnterAmount
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.viewmodel.StakingClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.transformer.Transformer

internal class SetAmountDataTransformer(
    private val clickIntents: StakingClickIntents,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val userWalletProvider: Provider<UserWallet>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val maxEnterAmountProvider: Provider<MaxEnterAmount>,
) : Transformer<StakingUiState> {

    private val iconStateConverter by lazy(::CryptoCurrencyToIconStateConverter)

    private val amountStateConverter by lazy(LazyThreadSafetyMode.NONE) {
        AmountStateConverter(
            clickIntents = clickIntents,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
            appCurrencyProvider = appCurrencyProvider,
            iconStateConverter = iconStateConverter,
            maxEnterAmountProvider = maxEnterAmountProvider,
        )
    }

    override fun transform(prevState: StakingUiState): StakingUiState {
        val title = if (prevState.actionType == StakingActionCommonType.Exit) {
            resourceReference(R.string.staking_staked_amount)
        } else {
            stringReference(userWalletProvider().name)
        }

        return prevState.copy(
            amountState = amountStateConverter.convert(
                AmountParameters(
                    title = title,
                    value = "",
                ),
            ),
        )
    }
}
