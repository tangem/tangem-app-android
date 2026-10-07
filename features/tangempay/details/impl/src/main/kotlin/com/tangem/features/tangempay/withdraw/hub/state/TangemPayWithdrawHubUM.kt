package com.tangem.features.tangempay.withdraw.hub.state

import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class TangemPayWithdrawHubUM(
    val swapSection: SwapSectionUM,
    val bankSection: BankSectionUM,
    val onBackClick: () -> Unit,
) {

    data class SwapSectionUM(
        val withinPortfolio: ActionCardUM,
        val recents: TokenRecentsUM,
    )

    sealed interface BankSectionUM {

        data object Loading : BankSectionUM

        data class Content(
            val toPersonalAccount: ActionCardUM,
            val toBusinessAccount: ActionCardUM,
            val recents: RecentsUM,
            val isAvailable: Boolean,
        ) : BankSectionUM
    }

    data class ActionCardUM(
        val icon: TangemIconUM,
        val title: TextReference,
        val onClick: () -> Unit,
    )

    sealed interface RecentsUM {

        data object Loading : RecentsUM

        data object Empty : RecentsUM

        data class Error(val onRetryClick: () -> Unit) : RecentsUM

        data class Content(
            val recipients: ImmutableList<RecentRecipientUM>,
            val onRecipientClick: (bankAccountId: String) -> Unit,
            val onSeeAllClick: (() -> Unit)?,
        ) : RecentsUM
    }

    data class RecentRecipientUM(
        val id: String,
        val initial: String,
        val name: String,
        val accountNumber: String,
    )

    sealed interface TokenRecentsUM {

        data object Loading : TokenRecentsUM

        data object Empty : TokenRecentsUM

        data class Content(
            val tokens: ImmutableList<RecentTokenUM>,
            val onTokenClick: (currencyId: String) -> Unit,
        ) : TokenRecentsUM
    }

    data class RecentTokenUM(
        val id: String,
        val icon: CurrencyIconState,
        val name: String,
        val accountName: TextReference,
        val accountIcon: AccountIconUM.CryptoPortfolio,
    )
}