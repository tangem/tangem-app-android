package com.tangem.common.ui.account

import com.tangem.common.ui.R
import com.tangem.common.ui.userwallet.ext.toUserWalletItemBalance
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.utils.converter.Converter

/**
 * The collectibles account row of an account list. It has nothing to open in the settings, so it takes no click.
 * An unread vault shows a failed balance rather than the zero the wallet total counts it as.
 */
class AccountCollectiblesItemUMConverter(
    private val appCurrency: AppCurrency? = null,
    private val isBalanceHidden: Boolean = false,
) : Converter<AccountStatus.Collectibles, UserWalletItemUM> {

    override fun convert(value: AccountStatus.Collectibles): UserWalletItemUM {
        val name = resourceReference(R.string.collectibles_main_title)

        return UserWalletItemUM(
            id = value.accountId.value,
            name = name,
            information = UserWalletItemUM.Information.None,
            balance = value.value.toFiatBalance().toUserWalletItemBalance(
                appCurrency = appCurrency,
                isBalanceHidden = isBalanceHidden,
            ),
            imageState = UserWalletItemUM.ImageState.Account(name = name, icon = AccountIconUM.Collectibles),
            isEnabled = false,
            onClick = {},
        )
    }

    private fun CollectiblesAccountStatusValue.toFiatBalance(): TotalFiatBalance {
        if (this is CollectiblesAccountStatusValue.Loading) return TotalFiatBalance.Loading

        val amount = fiatBalance ?: return TotalFiatBalance.Failed
        return TotalFiatBalance.Loaded(amount = amount, source = source)
    }
}