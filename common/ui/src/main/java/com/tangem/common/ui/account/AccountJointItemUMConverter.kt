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

/**
 * A joint account row reads like any crypto portfolio row, tagged with a "Joint" badge. A compromised account
 * ([isCompromised]) is dimmed and shows no balance: one "paused" text takes the place of the tokens count.
 */
class AccountJointItemUMConverter(
    private val onClick: () -> Unit,
    private val appCurrency: AppCurrency? = null,
    private val accountBalance: TotalFiatBalance? = null,
    private val isBalanceHidden: Boolean = false,
    private val isCompromised: Boolean = false,
) : Converter<Account.Joint, UserWalletItemUM> {

    override fun convert(value: Account.Joint): UserWalletItemUM {
        val row = AccountPortfolioItemUMConverter(
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

        return if (isCompromised) {
            row.copy(
                isDimmed = true,
                information = UserWalletItemUM.Information.Loaded(
                    value = resourceReference(R.string.joint_account_main_paused_for_safety),
                ),
                balance = UserWalletItemUM.Balance.NotShowing,
            )
        } else {
            row
        }
    }
}