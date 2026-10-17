package com.tangem.common.ui.account

import com.tangem.common.ui.R
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.utils.converter.Converter

/** A joint account row reads like any crypto portfolio row, tagged with a "Joint" badge */
class AccountJointItemUMConverter(
    private val onClick: () -> Unit,
    private val appCurrency: AppCurrency? = null,
    private val accountBalance: TotalFiatBalance? = null,
    private val isBalanceHidden: Boolean = false,
) : Converter<Account.Joint, UserWalletItemUM> {

    override fun convert(value: Account.Joint): UserWalletItemUM = AccountPortfolioItemUMConverter(
        onClick = onClick,
        appCurrency = appCurrency,
        accountBalance = accountBalance,
        isBalanceHidden = isBalanceHidden,
    ).convert(value).copy(
        titleBadge = TangemBadgeUM(
            text = resourceReference(R.string.common_joint),
            size = TangemBadge.Size.X4,
        ),
    )
}