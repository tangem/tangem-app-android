package com.tangem.common.ui.account

import com.tangem.common.ui.R
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.converter.Converter

class AccountPortfolioItemUMConverter(
    private val onClick: () -> Unit,
    private val appCurrency: AppCurrency? = null,
    private val accountBalance: TotalFiatBalance? = null,
    private val isBalanceHidden: Boolean = false,
    private val isEnabled: Boolean = true,
    private val endIcon: UserWalletItemUM.EndIcon = UserWalletItemUM.EndIcon.None,
) : Converter<Account, UserWalletItemUM> {

    override fun convert(value: Account): UserWalletItemUM {
        return with(value) {
            UserWalletItemUM(
                id = UserWalletId(accountId.value),
                name = accountName.toUM().value,
                information = getInfo(account = this),
                balance = getBalanceInfo(),
                isEnabled = isEnabled,
                endIcon = endIcon,
                onClick = onClick,
                imageState = getImageState(account = this),
            )
        }
    }

    private fun getInfo(account: Account): UserWalletItemUM.Information.Loaded = when (account) {
        is Account.CryptoPortfolio -> {
            val text = pluralReference(
                R.plurals.common_tokens_count,
                count = account.tokensCount,
                formatArgs = wrappedList(account.tokensCount),
            )
            UserWalletItemUM.Information.Loaded(text)
        }
    }

    private fun getImageState(account: Account) = when (account) {
        is Account.CryptoPortfolio -> UserWalletItemUM.ImageState.Account(
            name = account.accountName.toUM().value,
            icon = account.icon.toUM(),
        )
    }

    private fun getBalanceInfo(): UserWalletItemUM.Balance {
        return when {
            isBalanceHidden -> UserWalletItemUM.Balance.Hidden
            accountBalance == null -> UserWalletItemUM.Balance.Loading
            else -> {
                when (accountBalance) {
                    is TotalFiatBalance.Loading -> UserWalletItemUM.Balance.Loading
                    is TotalFiatBalance.Failed -> UserWalletItemUM.Balance.Failed
                    is TotalFiatBalance.Loaded -> {
                        if (appCurrency != null) {
                            val formattedAmount = accountBalance.amount.format {
                                fiat(appCurrency.code, appCurrency.symbol)
                            }

                            UserWalletItemUM.Balance.Loaded(
                                value = formattedAmount,
                                isFlickering = accountBalance.source == StatusSource.CACHE,
                            )
                        } else {
                            UserWalletItemUM.Balance.Failed
                        }
                    }
                }
            }
        }
    }
}