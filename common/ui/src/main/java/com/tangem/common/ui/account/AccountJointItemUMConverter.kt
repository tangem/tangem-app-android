package com.tangem.common.ui.account

import com.tangem.common.ui.R
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.account.Account
import com.tangem.utils.converter.Converter

/**
 * Maps an [Account.Joint] to a [UserWalletItemUM] for account lists, tagging the row with a "Joint" badge.
 *
 * Joint accounts carry no per-account fiat balance (the Safe is not an EOA), so the row shows no balance.
 */
class AccountJointItemUMConverter(
    private val onClick: () -> Unit,
) : Converter<Account.Joint, UserWalletItemUM> {

    override fun convert(value: Account.Joint): UserWalletItemUM = with(value) {
        UserWalletItemUM(
            id = accountId.value,
            name = accountName.toUM().value,
            titleBadge = TangemBadgeUM(
                text = resourceReference(R.string.common_joint),
                size = TangemBadge.Size.X4,
            ),
            information = UserWalletItemUM.Information.Loaded(
                value = pluralReference(
                    id = R.plurals.common_tokens_count,
                    count = cryptoCurrencies.size,
                    formatArgs = wrappedList(cryptoCurrencies.size),
                ),
            ),
            balance = UserWalletItemUM.Balance.NotShowing,
            isEnabled = true,
            onClick = onClick,
            imageState = UserWalletItemUM.ImageState.Account(
                name = accountName.toUM().value,
                icon = CryptoPortfolioIconConverter.convert(icon),
            ),
        )
    }
}