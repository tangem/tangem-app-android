package com.tangem.features.tangempay.withdraw.bank.preview.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawHeaderUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface TangemPayBankWithdrawPreviewUM {

    val onBackClick: () -> Unit

    /** [transfer] is null when the draft has no amount-step data (e.g. after a process-death restore). */
    @Immutable
    data class Summary(
        val header: BankWithdrawHeaderUM,
        val transfer: TransferUM?,
        val recipientCards: ImmutableList<RecipientCardUM>,
        val isSending: Boolean,
        val isSendEnabled: Boolean,
        /** Whether Send announces the card tap that a cold wallet needs to sign the transfer. */
        val isColdWallet: Boolean,
        override val onBackClick: () -> Unit,
        val onCloseClick: () -> Unit,
        val onSendClick: () -> Unit,
    ) : TangemPayBankWithdrawPreviewUM

    /** Full-screen result of a failed create-order call; [onRetryClick] is null when retrying cannot help. */
    @Immutable
    data class Failed(
        val title: TextReference,
        val icon: TangemStatusIcon.Kind,
        val onCloseClick: () -> Unit,
        val onRetryClick: (() -> Unit)?,
    ) : TangemPayBankWithdrawPreviewUM {

        override val onBackClick: () -> Unit get() = onCloseClick
    }

    @Immutable
    data class TransferUM(
        val amount: AmountCardUM,
        val route: RouteUM,
        val sendingSummary: TextReference,
    )

    /**
     * [onClick] is null while sending; otherwise always present — unlike the recipient, the amount can be
     * corrected regardless of whether the recipient is saved or new.
     */
    @Immutable
    data class AmountCardUM(
        val balance: String?,
        val amount: String,
        val icon: CurrencyIconState,
        val symbol: String,
        val onClick: (() -> Unit)?,
    )

    /** [onClick] is null while sending; otherwise it reopens the step the rail is picked on. */
    @Immutable
    data class RouteUM(
        val rail: BankRail,
        val title: TextReference,
        val eta: TextReference,
        val fee: String,
        val onClick: (() -> Unit)? = null,
    )

    @Immutable
    data class DetailRowUM(
        val label: TextReference,
        val value: String,
    )

    /** One rounded card of the recipient summary; [onClick] is null when the recipient cannot be edited here. */
    @Immutable
    data class RecipientCardUM(
        val rows: ImmutableList<DetailRowUM>,
        val onClick: (() -> Unit)?,
    )
}