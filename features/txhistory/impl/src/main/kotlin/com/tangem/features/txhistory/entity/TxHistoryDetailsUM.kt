package com.tangem.features.txhistory.entity

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.extensions.TextReference

/**
 * UI model for the in-app transaction details ("Operation") card.
 *
 * One model for all transaction types; the layout family is chosen from the transaction type by
 * `TxInfoToTxHistoryDetailsUMConverter`:
 * - [SingleAsset] — Receive / Send / Transfer
 * - [TwoAssets] — Swap / Onramp
 */
@Immutable
internal sealed interface TxHistoryDetailsUM : TangemBottomSheetConfigContent {

    /** Shared top bar ("Nav bar"): type icon, status-driven title, date+time. */
    val header: HeaderUM

    /** Single-asset layout: Receive / Send / Transfer */
    data class SingleAsset(
        override val header: HeaderUM,
        val amountBlock: AmountBlockUM,
        val counterparty: CounterpartyUM?,
        val rows: List<InfoRowUM>,
    ) : TxHistoryDetailsUM

    /** Two-asset layout: Swap / Onramp */
    data class TwoAssets(
        override val header: HeaderUM,
    ) : TxHistoryDetailsUM

    /**
     * Centered amount block of the single-asset card: token avatar (with network badge), the big signed crypto
     * [amount] and the secondary [fiatAmount].
     *
     * [isFailed] drives the failed visual state — the amount is struck through, recolored to tertiary and carries no
     * `+`/`−` sign (mirrors the status-driven recolor in the shared header).
     */
    @Immutable
    data class AmountBlockUM(
        val currencyIcon: CurrencyIconState,
        val amount: TextReference,
        val fiatAmount: TextReference,
        val isFailed: Boolean,
    )

    /**
     * A single info row of the details card: a [label] on the leading side and its [value] on the trailing side
     * (e.g. `Network fee` → `0.00056 ETH`, `Rate` → `1 POL ≈ 0.36 USDT`). Rendered by [TxHistoryDetailsInfoRows].
     */
    @Immutable
    data class InfoRowUM(
        val label: TextReference,
        val value: TextReference,
    )

    /**
     * Counterparty ("Recipient" / "From") card of the single-asset detail: a leading [avatar], the section [label] over
     * the counterparty [title], and — when [onCopyClick] is non-null — a trailing copy button.
     *
     * The layout is identical across counterparty kinds; the only variance is the [avatar] (see [CounterpartyAvatar])
     * and whether copy is offered. Only the [CounterpartyAvatar.Address] kind is currently produced by
     * [com.tangem.features.txhistory.converter.TxInfoToTxHistoryDetailsUMConverter]; the own-account / own-wallet
     * avatars are populated in a follow-up, once the detail model assembles the same address->owner lookup the list
     * uses (`TxHistoryLookupContext`).
     *
     * @property label Section label above the counterparty: "Recipient" (outgoing) / "From" (incoming).
     * @property title Counterparty value: brief address / account name / wallet name.
     * @property avatar Leading avatar.
     * @property onCopyClick Copy action; `null` hides the copy button (e.g. own-wallet has nothing to copy).
     */
    @Immutable
    data class CounterpartyUM(
        val label: TextReference,
        val title: TextReference,
        val avatar: CounterpartyAvatar,
        val onCopyClick: (() -> Unit)?,
    )

    /** Leading avatar of the [CounterpartyUM] card — the only thing that differs between counterparty kinds. */
    @Immutable
    sealed interface CounterpartyAvatar {

        /** External blockchain address — rendered as an identicon generated from [rawAddress]. */
        data class Address(val rawAddress: String) : CounterpartyAvatar

        /** User's own account — rendered as [iconResId] tinted over [backgroundColor]. */
        data class Account(
            @DrawableRes val iconResId: Int,
            val backgroundColor: Color,
        ) : CounterpartyAvatar

        /** User's own wallet — rendered as the wallet card [deviceIconUM]. */
        data class Wallet(val deviceIconUM: DeviceIconUM) : CounterpartyAvatar
    }

    /**
     * Shared bottom-sheet top bar. The icon glyph and [title] text come from the transaction type; [status] drives
     * the three visual states (in-progress / confirmed / failed) — recoloring the icon circle and the title.
     */
    @Immutable
    data class HeaderUM(
        @DrawableRes val iconRes: Int,
        val status: TransactionItemUM.Content.Status,
        val title: TextReference,
        val subtitle: TextReference,
    )
}