package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.common.ui.amountScreen.converters.AmountStateConverter
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.MaxEnterAmount
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
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
            userWalletProvider = userWalletProvider,
            iconStateConverter = iconStateConverter,
            maxEnterAmountProvider = maxEnterAmountProvider,
        )
    }

    override fun transform(prevState: StakingUiState): StakingUiState {
        return prevState.copy(
            amountState = createInitialAmountState(),
        )
    }

    private fun createInitialAmountState(): AmountState {
        return amountStateConverter.convert("")
    }
}