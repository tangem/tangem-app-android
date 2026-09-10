package com.tangem.features.tangempay.withdraw.hub.state

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_swap_horizontal_20
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.ActionCardUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.AvatarAccent
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.RecentRecipientUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.RecentsUM
import com.tangem.features.tangempay.withdraw.toShortName
import kotlinx.collections.immutable.toImmutableList

internal class TangemPayWithdrawHubUMFactory(private val callbacks: Callbacks) {

    fun create(): TangemPayWithdrawHubUM = TangemPayWithdrawHubUM(
        swapSection = TangemPayWithdrawHubUM.SwapSectionUM(
            withinPortfolio = ActionCardUM(
                icon = TangemIconUM.Icon(
                    imageVector = Icons.ic_arrow_swap_horizontal_20,
                    tintReference = { TangemTheme.colors3.icon.staticDark },
                ),
                title = resourceReference(R.string.tangempay_withdraw_hub_within_portfolio),
                onClick = callbacks.onWithinPortfolioClick,
            ),
        ),
        bankSection = TangemPayWithdrawHubUM.BankSectionUM(
            toPersonalAccount = ActionCardUM(
                icon = TangemIconUM.Icon(
                    iconRes = R.drawable.ic_user_filled_16,
                    tintReference = { TangemTheme.colors3.icon.staticDark },
                ),
                title = resourceReference(R.string.tangempay_withdraw_hub_to_personal_account),
                onClick = callbacks.onToPersonalAccountClick,
            ),
            toBusinessAccount = ActionCardUM(
                icon = TangemIconUM.Icon(
                    iconRes = R.drawable.ic_briefcase_filled_16,
                    tintReference = { TangemTheme.colors3.icon.staticDark },
                ),
                title = resourceReference(R.string.tangempay_withdraw_hub_to_business_account),
                onClick = callbacks.onToBusinessAccountClick,
            ),
            recents = RecentsUM.Loading,
        ),
        onBackClick = callbacks.onBackClick,
    )

    fun createRecents(recents: List<BankAccount>, hasSavedAccounts: Boolean): RecentsUM {
        if (recents.isEmpty()) {
            return RecentsUM.Empty(onSeeAllClick = callbacks.onSeeAllClick.takeIf { hasSavedAccounts })
        }
        return RecentsUM.Content(
            recipients = recents.map(::toRecipient).toImmutableList(),
            onRecipientClick = callbacks.onRecentRecipientClick,
            onSeeAllClick = callbacks.onSeeAllClick,
        )
    }

    fun createRecentsError(): RecentsUM = RecentsUM.Error(onRetryClick = callbacks.onRetryRecentsClick)

    private fun toRecipient(account: BankAccount): RecentRecipientUM {
        val name = account.details.beneficiary.toShortName()
        return RecentRecipientUM(
            id = account.id,
            initial = name.take(1).uppercase(),
            accent = AvatarAccent.entries[account.id.hashCode().mod(AvatarAccent.entries.size)],
            name = name,
            accountNumber = account.details.accountNumber,
        )
    }

    data class Callbacks(
        val onBackClick: () -> Unit,
        val onWithinPortfolioClick: () -> Unit,
        val onToPersonalAccountClick: () -> Unit,
        val onToBusinessAccountClick: () -> Unit,
        val onRecentRecipientClick: (bankAccountId: String) -> Unit,
        val onSeeAllClick: () -> Unit,
        val onRetryRecentsClick: () -> Unit,
    )
}