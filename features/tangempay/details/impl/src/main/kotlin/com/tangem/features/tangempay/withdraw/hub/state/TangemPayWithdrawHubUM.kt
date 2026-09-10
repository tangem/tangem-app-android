package com.tangem.features.tangempay.withdraw.hub.state

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
    )

    data class BankSectionUM(
        val toPersonalAccount: ActionCardUM,
        val toBusinessAccount: ActionCardUM,
        val recents: RecentsUM,
    )

    data class ActionCardUM(
        val icon: TangemIconUM,
        val title: TextReference,
        val onClick: () -> Unit,
    )

    sealed interface RecentsUM {

        data object Loading : RecentsUM

        data class Empty(val onSeeAllClick: (() -> Unit)?) : RecentsUM

        data class Error(val onRetryClick: () -> Unit) : RecentsUM

        data class Content(
            val recipients: ImmutableList<RecentRecipientUM>,
            val onRecipientClick: (bankAccountId: String) -> Unit,
            val onSeeAllClick: () -> Unit,
        ) : RecentsUM
    }

    data class RecentRecipientUM(
        val id: String,
        val initial: String,
        val accent: AvatarAccent,
        val name: String,
        val accountNumber: String,
    )

    enum class AvatarAccent { BLUE, VIOLET, RED, ORANGE, YELLOW, GREEN }
}