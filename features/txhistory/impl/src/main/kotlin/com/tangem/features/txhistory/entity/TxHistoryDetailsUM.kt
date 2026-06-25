package com.tangem.features.txhistory.entity

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TxIcon
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * UI model for the in-app transaction details ("Operation") card.
 *
 * One model for all transaction types; the layout family is chosen from the transaction type by
 * `TxHistoryInfoToTxHistoryDetailsUMConverter`:
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
        val rows: ImmutableList<InfoRowUM>,
    ) : TxHistoryDetailsUM

    /**
     * Two-asset layout: Swap / Onramp.
     *
     * [from] ("You send") → [to] ("You receive") exchange block. Both are nullable: when a leg cannot be built (e.g. a
     * future express variant with no asset data) the card falls back to a header-only placeholder. [statusBanner] is
     * the express status plaque under the block, `null` until status is known. [rows] carries, in order, the provider
     * row (its name), the effective-rate row, and the network-fee row pulled from the matched on-chain leg
     * (`ExpressTx.txInfo`); each is dropped when its data is unavailable. [providerButton] is the bottom "Go to
     * provider" / "Go to verification" CTA, `null` unless the deal is on a provider-actionable terminal with a link.
     */
    data class TwoAssets(
        override val header: HeaderUM,
        val from: AssetUM? = null,
        val to: AssetUM? = null,
        val statusBanner: StatusBannerUM? = null,
        val rows: ImmutableList<InfoRowUM> = persistentListOf(),
        val providerButton: ProviderButtonUM? = null,
    ) : TxHistoryDetailsUM

    /**
     * Express status plaque under the two-asset block. The UI animates between successive emissions.
     *
     * @property severity Plaque colors (background tint + text/icon color).
     * @property title Status line, e.g. "Awaiting funds" / "Confirmed" / "Failed".
     * @property subtitle Optional second line (e.g. the refund hint on a failed terminal).
     * @property isLoading `true` → trailing rotating loader (in-progress); `false` → static [severity] glyph.
     */
    data class StatusBannerUM(
        val severity: Severity,
        val title: TextReference,
        val subtitle: TextReference? = null,
        val isLoading: Boolean,
    ) {

        /** Visual severity of the [StatusBannerUM] — selects the background tint and the text/icon color. */
        enum class Severity { Info, Success, Error, Warning }
    }

    /**
     * Bottom call-to-action of the two-asset card, shown only on the provider-actionable terminals of an express deal
     * (failed / expired → "Go to provider"; KYC verification → "Go to verification") and only when the deal carries a
     * provider link. [onClick] opens that link (`ExpressTx.externalTxUrl`).
     *
     * @property text Button label ("Go to provider" / "Go to verification").
     * @property onClick Opens the provider's page for this deal.
     */
    data class ProviderButtonUM(
        val text: TextReference,
        val onClick: () -> Unit,
    )

    /**
     * One side of the two-asset block: the [label] over the signed [amount], with the [currencyIcon] on the trailing
     * side. [owner] `null` → plain label ("You send"); non-null → "From"/"To" prefix plus the resolved own account /
     * wallet decoration. [isFaded] renders the failed amount (struck through, recolored to tertiary); an in-flight leg is
     * not faded — it carries a `~` estimate sign instead.
     *
     * [currencyIcon] is `null` when the leg has no icon to show — the onramp fiat side carries no `CryptoCurrency` and
     * no country flag is rendered (no data); the trailing icon slot is then left empty.
     */
    data class AssetUM(
        val label: TextReference,
        val owner: AssetOwnerUM?,
        val amount: TextReference,
        val currencyIcon: CurrencyIconState?,
        val isFaded: Boolean,
    )

    /**
     * Counterparty rendered inline in an [AssetUM.label] of a swap leg. Carries the [name] plus a kind-specific 16dp
     * decoration: an own account / own wallet when the leg's address resolves to one of the user's portfolios, or an
     * external blockchain [Address] (e.g. a send-and-swap payout to a non-user address).
     */
    @Immutable
    sealed interface AssetOwnerUM {

        val name: TextReference

        /** User's own account — the [iconResId] glyph tinted over [backgroundColor], shown **before** the [name]. */
        data class Account(
            override val name: TextReference,
            @DrawableRes val iconResId: Int,
            val backgroundColor: Color,
        ) : AssetOwnerUM

        /** User's own wallet — the wallet card [deviceIconUM], shown **after** the [name]. */
        data class Wallet(
            override val name: TextReference,
            val deviceIconUM: DeviceIconUM,
        ) : AssetOwnerUM

        /**
         * External blockchain address — the [name] is the brief address, decorated with an identicon generated from
         * [rawAddress], shown **before** the [name].
         */
        data class Address(
            override val name: TextReference,
            val rawAddress: String,
        ) : AssetOwnerUM
    }

    /**
     * Centered amount block of the single-asset card: token avatar (with network badge), the big signed crypto
     * [amount] and the secondary [fiatAmount].
     *
     * [fiatAmount] is `null` while no fiat value is available (`TxInfo` has no fiat field yet) — the fiat line is then
     * omitted entirely rather than shown as a placeholder.
     *
     * [isFailed] drives the failed visual state — the amount is struck through, recolored to tertiary and carries no
     * `+`/`−` sign (mirrors the status-driven recolor in the shared header).
     */
    data class AmountBlockUM(
        val currencyIcon: CurrencyIconState,
        val amount: TextReference,
        val fiatAmount: TextReference? = null,
        val isFailed: Boolean,
    )

    /**
     * A single info row of the details card: a [label] on the leading side and its [value] on the trailing side
     * (e.g. `Network fee` → `0.00056 ETH`, `Rate` → `1 POL ≈ 0.36 USDT`). Rendered by [TxHistoryDetailsInfoRows].
     *
     * [trailingIconRes] is an optional glyph drawn after the [value] (e.g. the arrow-up-right link affordance on the
     * provider row); `null` leaves the trailing slot text-only.
     *
     * [onClick] makes the row tappable (e.g. the provider row opens the provider page); `null` makes it non-interactive.
     */
    data class InfoRowUM(
        val label: TextReference,
        val value: TextReference,
        @DrawableRes val trailingIconRes: Int? = null,
        val onClick: (() -> Unit)? = null,
    )

    /**
     * Counterparty ("Recipient" / "From") card of the single-asset detail: a leading [avatar], the section [label] over
     * the counterparty [title], and — when [onCopyClick] is non-null — a trailing copy button.
     *
     * The layout is identical across counterparty kinds; the only variance is the [avatar] (see [CounterpartyAvatar])
     * and whether copy is offered. Only the [CounterpartyAvatar.Address] kind is currently produced by
     * [com.tangem.features.txhistory.converter.TxHistoryInfoToTxHistoryDetailsUMConverter]; resolving the own-account /
     * own-wallet avatars here (the `TxHistoryLookupContext` is already wired for the swap/onramp legs) is a follow-up.
     *
     * @property label Section label above the counterparty: "Recipient" (outgoing) / "From" (incoming).
     * @property title Counterparty value: brief address / account name / wallet name.
     * @property avatar Leading avatar.
     * @property onCopyClick Copy action; `null` hides the copy button (e.g. own-wallet has nothing to copy).
     */
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
     *
     * [menu] is the header's overflow context-menu content; empty leaves the trailing menu button inert.
     */
    data class HeaderUM(
        val icon: TxIcon,
        val status: TransactionItemUM.Content.Status,
        val title: TextReference,
        val subtitle: TextReference,
        val menu: ImmutableList<MenuItemUM> = persistentListOf(),
    )

    /**
     * One row of the header's overflow context menu: a leading [icon] glyph and a [title] label. [isDestructive]
     * renders the row in the error color (e.g. a remove action); [onClick] runs the action and is expected to also
     * dismiss the menu at the call site.
     */
    data class MenuItemUM(
        val icon: ImageVector,
        val title: TextReference,
        val isDestructive: Boolean = false,
        val onClick: () -> Unit,
    )
}