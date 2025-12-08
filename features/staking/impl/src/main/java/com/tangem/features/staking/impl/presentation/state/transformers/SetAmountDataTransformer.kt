package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.amountScreen.converters.AmountAccountConverter
import com.tangem.common.ui.amountScreen.converters.AmountStateConverter
import com.tangem.common.ui.amountScreen.models.AmountParameters
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.model.StakingClickIntents
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.Provider
import com.tangem.utils.transformer.Transformer

@Suppress("LongParameterList")
internal class SetAmountDataTransformer(
    private val clickIntents: StakingClickIntents,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val userWalletProvider: Provider<UserWallet>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val isBalanceHidden: Boolean,
    private val isAccountsModeEnabled: Boolean,
    private val account: Account.CryptoPortfolio?,
) : Transformer<StakingUiState> {

    private val iconStateConverter by lazy(::CryptoCurrencyToIconStateConverter)

    override fun transform(prevState: StakingUiState): StakingUiState {
        val accountTitleUM = when (prevState.actionType) {
            is StakingActionCommonType.Exit -> AccountTitleUM.Text(resourceReference(R.string.staking_staked_amount))
            is StakingActionCommonType.Enter -> AmountAccountConverter(
                isAccountsMode = isAccountsModeEnabled,
                walletTitle = stringReference(userWalletProvider().name),
                prefixText = resourceReference(R.string.common_from),
            ).convert(account)
            else -> AccountTitleUM.Text(resourceReference(R.string.common_amount))
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
                iconStateConverter = iconStateConverter,
                maxEnterAmount = maxEnterAmount,
                appCurrency = appCurrencyProvider(),
                cryptoCurrencyStatus = cryptoCurrencyStatusProvider(),
                isBalanceHidden = isBalanceHidden,
                accountTitleUM = accountTitleUM,
            ).convert(
                AmountParameters(
                    title = stringReference(userWalletProvider().name),
                    value = "",
                ),
            ),
        )
    }
}