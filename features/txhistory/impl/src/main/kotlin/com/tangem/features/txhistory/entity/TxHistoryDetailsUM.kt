package com.tangem.features.txhistory.entity

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.common.ui.cosign.CosignRequestRowUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TxIcon
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.txhistory.model.TxHistoryInfo
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

    /**
     * App-wide "hide balances" setting. When `true` the card masks its amounts with stars — the single-asset amount and
     * its fiat line, both exchange legs, and the info rows marked [hideable][InfoRowUM.isValueHideable] (the network
     * fee). The provider and rate rows are not amounts of the user's funds and stay visible.
     */
    val isBalanceHidden: Boolean

    /** Single-asset layout: Receive / Send / Transfer */
    data class SingleAsset(
        override val header: HeaderUM,
        override val isBalanceHidden: Boolean,
        val amountBlock: AmountBlockUM,
        val counterparty: CounterpartyUM?,
        val rows: ImmutableList<InfoRowUM>,
        /** Warning plaque under the amount block (currently: the unlimited-approval "High Risk" notice). */
        val statusBanner: StatusBannerUM? = null,
        /**
         * Joint-account cosigning row ("Signatures X of Y"), shown while the transaction awaits member signatures.
         * `null` for a non-joint transaction or once it has left the awaiting-signatures state.
         */
        val cosign: CosignRequestRowUM? = null,
        /** Sign/Signed/Rejected action and its caption, shown together with [cosign]. `null` alongside it. */
        val cosignAction: CosignActionUM? = null,
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
        override val isBalanceHidden: Boolean,
        val from: AssetUM? = null,
        val to: AssetUM? = null,
        val statusBanner: StatusBannerUM? = null,
        val rows: ImmutableList<InfoRowUM> = persistentListOf(),
        val providerButton: ProviderButtonUM? = null,
    ) : TxHistoryDetailsUM

    /**
     * Warning/status plaque rendered under the amount block. On [TwoAssets] it is the express deal's live status
     * (animates between successive emissions); on [SingleAsset] it is currently only the static unlimited-approval
     * "High Risk" notice (no animation between emissions needed there).
     *
     * @property style Visual identity of the status — selects the plaque colors, the trailing glyph and the title
     * motion together. One value per distinct look, so a terminal that shares a colour but not a glyph (the red
     * [Expired][Style.Expired] clock, the red [Refunded][Style.Refunded] arrow) is its own entry rather than a colour
     * plus a separate glyph override.
     * @property title Status line, e.g. "Awaiting funds" / "Confirmed" / "Failed".
     * @property subtitle Optional second line (e.g. the refund hint on a failed terminal). May carry a styled
     * tappable part (e.g. the "Learn more" of the refunded terminal) — rendered as an annotated reference.
     * @property isLoading `true` → trailing rotating loader (in-progress); `false` → the [style]'s static glyph.
     */
    data class StatusBannerUM(
        val style: Style,
        val title: TextReference,
        val subtitle: TextReference? = null,
        val isLoading: Boolean,
    ) {

        /** Visual identity of the [StatusBannerUM] — drives the plaque colors, the trailing glyph and the title motion. */
        enum class Style { Info, Success, Warning, Error, Refunded, Expired }
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
     * Bottom CTA of a joint-account pending transaction: "Sign" ([isEnabled] — mine to act on) or the terminal
     * "Signed" / "Rejected" ( `!isEnabled` — reflects my own past action). [caption] explains the auto-send
     * threshold ("Auto-sends once 3 of 5 members sign") or the outcome of a decline.
     *
     * @property isLoading `true` while the signing action (e.g. NFC card scan) is in flight.
     * @property iconEnd Optional trailing glyph on the button (e.g. a scan/NFC icon on "Sign").
     */
    data class CosignActionUM(
        val buttonText: TextReference,
        val caption: TextReference,
        val isEnabled: Boolean,
        val isLoading: Boolean = false,
        val iconEnd: TangemIconUM? = null,
        val onClick: () -> Unit,
    )

    /**
     * One side of the two-asset block: the [label] over the signed [amount], with the [currencyIcon] on the trailing
     * side. [owner] `null` → plain label ("You send"); non-null → "From"/"To" prefix plus the resolved own account /
     * wallet decoration. [isFaded] renders the failed amount (struck through, recolored to tertiary); an in-flight leg is
     * not faded — it carries a `~` estimate sign instead.
     *
     * [currencyIcon] is `null` when the leg has no icon to show — e.g. the onramp fiat side when its paid fiat currency
     * is unresolved; the trailing icon slot is then left empty. When resolved, the fiat leg shows its currency icon.
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
     * decoration: an own account / own wallet when the leg's address resolves to one of the user's portfolios, a
     * [Contact] when it is saved in an address book, or an external blockchain [Address] (e.g. a send-and-swap payout
     * to an address the user has not saved).
     */
    @Immutable
    sealed interface AssetOwnerUM {

        val name: TextReference

        /**
         * User's own account — its portfolio [icon] over the icon's color, shown **before** the [name]. The avatar is
         * drawn by the design-system `AccountIcon`, so a `Letter` icon renders the monogram of [name].
         */
        data class Account(
            override val name: TextReference,
            val icon: AccountIconUM.CryptoPortfolio,
        ) : AssetOwnerUM

        /** User's own Tangem Pay (Payment) account — the Visa icon, shown **before** the [name]. */
        data class PaymentAccount(override val name: TextReference) : AssetOwnerUM

        /** User's own wallet — the wallet card [deviceIconUM], shown **after** the [name]. */
        data class Wallet(
            override val name: TextReference,
            val deviceIconUM: DeviceIconUM,
        ) : AssetOwnerUM

        /**
         * Leg address saved in one of the user's address books — the contact's [name] in place of the address, with
         * its portfolio [icon] shown **before** the name and drawn as a circle, the shape contacts carry everywhere.
         */
        data class Contact(
            override val name: TextReference,
            val icon: AccountIconUM.CryptoPortfolio,
        ) : AssetOwnerUM

        /**
         * External blockchain address — the address itself, decorated with an identicon generated from it, shown
         * **before** the [name]. The address is kept raw; the leg shortens it to the width it gets.
         */
        data class Address(val rawAddress: String) : AssetOwnerUM {
            override val name: TextReference = stringReference(rawAddress)
        }
    }

    /**
     * Centered amount block of the single-asset card: the [icon] (a single token avatar, or a yield-supply asset+Aave
     * pair), an optional [label] above the amount ("Supplied" / "Returned" for yield supply), the big [amount] and the
     * secondary [fiatAmount].
     *
     * [label] is `null` for the plain single-asset types (Send / Receive / Transfer / staking) — the line is then
     * omitted. [fiatAmount] prices [amount] at the viewed currency's current rate; it is `null` when there is no rate
     * to price it with (or the amount is not in the viewed currency, as an approval's allowance is) — the fiat line is
     * then omitted entirely rather than shown as a placeholder.
     *
     * [isFailed] drives the failed visual state — the amount is struck through, recolored to tertiary and carries no
     * `+`/`−` sign (mirrors the status-driven recolor in the shared header).
     */
    data class AmountBlockUM(
        val icon: AmountIconUM,
        val amount: TextReference,
        val label: TextReference? = null,
        val fiatAmount: TextReference? = null,
        val isFailed: Boolean,
    )

    /** Icon shown above the amount of the single-asset card. */
    @Immutable
    sealed interface AmountIconUM {

        /** A single token avatar — Send / Receive / Transfer / staking. */
        data class Single(val currencyIcon: CurrencyIconState) : AmountIconUM

        /**
         * Two overlapping avatars: [leading] is drawn on top (left, with a background-colored ring), [trailing] behind
         * it (right). Used by yield supply — the asset and the hard-wired Aave protocol icon — with the order set by the
         * transaction direction: Aave leads on "Supplied" (enter), the asset leads on "Returned" (exit).
         */
        data class OverlappingPair(val leading: Item, val trailing: Item) : AmountIconUM

        /** One avatar of an [OverlappingPair]. */
        @Immutable
        sealed interface Item {

            /** A token avatar backed by its [state]. */
            data class Currency(val state: CurrencyIconState) : Item

            /** A hard-wired drawable (e.g. the Aave protocol icon). */
            data class Resource(@DrawableRes val resId: Int) : Item
        }
    }

    /**
     * A single info row of the details card: a [label] on the leading side and its [value] on the trailing side
     * (e.g. `Network fee` → `0.00056 ETH`, `Rate` → `1 POL ≈ 0.36 USDT`). Rendered by [TxHistoryDetailsInfoRows].
     *
     * [trailingIconRes] is an optional glyph drawn after the [value] (e.g. the arrow-up-right link affordance on the
     * provider row); `null` leaves the trailing slot text-only.
     *
     * [onClick] makes the row tappable (e.g. the provider row opens the provider page); `null` makes it non-interactive.
     *
     * [isValueHideable] marks the [value] as an amount of the user's own funds (the network fee), so it is masked under
     * [isBalanceHidden]; a provider name or an exchange rate is not, and stays visible.
     *
     * [isValueAddress] marks the [value] as a raw blockchain address (the validator row falling back to one), so it is
     * shortened from its middle to the width the row leaves it instead of being cut at its end like an amount.
     */
    data class InfoRowUM(
        val label: TextReference,
        val value: TextReference,
        @DrawableRes val trailingIconRes: Int? = null,
        val onClick: (() -> Unit)? = null,
        val isValueHideable: Boolean = false,
        val isValueAddress: Boolean = false,
    )

    /**
     * Counterparty ("Recipient" / "From") card of the single-asset detail: a leading [avatar], the section [label] over
     * the counterparty [title], and a tap that copies the counterparty address.
     *
     * The layout is identical across counterparty kinds; the only variance is the [avatar] (see [CounterpartyAvatar]).
     * The kind is resolved through the shared `TxHistoryLookupContext` — the same own-account / own-wallet resolution
     * the history list applies to its row subtitle — but it only decides what the card *shows*: an own account or
     * wallet renders its display name and still copies the address behind it.
     *
     * @property label Section label above the counterparty: "Recipient" (outgoing) / "From" (incoming).
     * @property title Counterparty value: the address itself, or an account / wallet / contact name.
     * @property avatar Leading avatar.
     * @property titleIcon Currency avatar drawn after the [title] — the asset of a fee-only card ("120.03 USDT ⓣ").
     * `null` on a counterparty card, which names an address rather than an asset.
     * @property onCopyClick Copies the counterparty address — the same address the header menu's "Copy address" row
     * copies, so the two entry points never disagree. `null` on a card with no address behind it (the fee-only
     * card), which is then not tappable.
     */
    data class CounterpartyUM(
        val label: TextReference,
        val title: CounterpartyTitle,
        val avatar: CounterpartyAvatar,
        val titleIcon: CurrencyIconState? = null,
        val onCopyClick: (() -> Unit)? = null,
    )

    /**
     * Value a [CounterpartyUM] shows. The two are kept apart so the card can size an address to the width it gets: a
     * [Name] is cut at its end like any label, an [Address] in its middle, where the groups a user recognizes an
     * address by survive.
     */
    @Immutable
    sealed interface CounterpartyTitle {

        /** A resolved name — own account, own wallet, saved contact — or the asset line of a fee-only card. */
        data class Name(val value: TextReference) : CounterpartyTitle

        /** The counterparty address, kept raw: the card decides how much of it fits. */
        data class Address(val value: String) : CounterpartyTitle
    }

    /** Leading avatar of the [CounterpartyUM] card — the only thing that differs between counterparty kinds. */
    @Immutable
    sealed interface CounterpartyAvatar {

        /** External blockchain address — rendered as an identicon generated from [rawAddress]. */
        data class Address(val rawAddress: String) : CounterpartyAvatar

        /** A plain type glyph over the neutral avatar circle — the ↑ of a fee-only card, which has no owner. */
        data class Glyph(val icon: TxIcon) : CounterpartyAvatar

        /**
         * User's own account — rendered by the design-system `AccountIcon` from its portfolio [icon], so a `Letter`
         * icon renders the monogram of [name] instead of a placeholder glyph.
         */
        data class Account(
            val name: TextReference,
            val icon: AccountIconUM.CryptoPortfolio,
        ) : CounterpartyAvatar

        /** User's own Tangem Pay (Payment) account — rendered as the Visa icon. */
        data object PaymentAccount : CounterpartyAvatar

        /** User's own wallet — rendered as the wallet card [deviceIconUM]. */
        data class Wallet(val deviceIconUM: DeviceIconUM) : CounterpartyAvatar

        /**
         * Recipient saved in an address book — the same avatar as [Account], drawn from the contact's own portfolio
         * [icon]; only the circular contact shape tells the two apart.
         */
        data class Contact(val name: String, val icon: AccountIconUM.CryptoPortfolio) : CounterpartyAvatar
    }

    /**
     * Shared bottom-sheet top bar. The icon glyph and [title] text come from the transaction type; [status] drives
     * the three visual states (in-progress / confirmed / failed) — recoloring the icon circle and the title.
     *
     * [statusStyle] overrides those colors where the bucket alone reads wrong — a deal paused by the provider is
     * in flight, but must alert in amber rather than look like ordinary progress.
     *
     * [subtitle] is normally the transaction date; [subtitleStyle] recolors it independently of [status] — e.g. a
     * joint-account transaction awaiting member signatures shows "Pending" there instead of a date, in
     * [SubtitleStyle.Warning], regardless of the on-chain [status].
     *
     * [menu] is the header's overflow context-menu content; empty leaves the trailing menu button inert.
     */
    data class HeaderUM(
        val icon: TxIcon,
        val status: TransactionItemUM.Content.Status,
        val title: TextReference,
        val subtitle: TextReference,
        val statusStyle: StatusStyle = StatusStyle.Default,
        val subtitleStyle: SubtitleStyle = SubtitleStyle.Default,
        val menu: ImmutableList<MenuItemUM> = persistentListOf(),
        val debugModel: TxHistoryInfo? = null,
    ) {

        /**
         * Visual identity of the [status]-tinted parts — the icon circle, its glyph and [HeaderUM.title].
         * [Default] takes the colors of the [HeaderUM.status] bucket; [Warning] paints all three amber.
         */
        enum class StatusStyle { Default, Warning }

        /** Visual identity of [HeaderUM.subtitle], independent of the on-chain [HeaderUM.status]. */
        enum class SubtitleStyle { Default, Warning }
    }

    /**
     * One row of the header's overflow context menu: a leading [icon] glyph and a [title] label. [isDestructive]
     * renders the row in the error color (e.g. a remove action); [action] is run on tap and the call site is expected to
     * also dismiss the menu.
     */
    data class MenuItemUM(
        val icon: ImageVector,
        val title: TextReference,
        val isDestructive: Boolean = false,
        val action: Action,
    ) {

        /** What a [MenuItemUM] does on tap. */
        @Immutable
        sealed interface Action {

            /** Runs [onClick] as-is. */
            data class Direct(val onClick: () -> Unit) : Action

            /**
             * Shares [text]. Composing the summary is a pure conversion, but turning it into a string needs the
             * `Resources` only the UI has, so the reference is resolved at composition (as the legacy express share
             * sheet did) and the resulting string is handed to [onShare].
             */
            data class Share(val text: TextReference, val onShare: (String) -> Unit) : Action
        }
    }
}