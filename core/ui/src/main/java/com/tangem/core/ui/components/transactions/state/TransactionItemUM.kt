package com.tangem.core.ui.components.transactions.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.extensions.TextReference

/**
 * UI model for the redesigned transaction list item ([REDACTED_TASK_KEY]).
 *
 * Mirrors the field set of the legacy [TransactionState] but splits the formatted amount string
 * into a numeric [Content.amount] (with sign) and a separate [Content.currencySymbol], so the
 * redesigned `TransactionItem` composable can render them on independent lines without parsing.
 */
@Immutable
sealed interface TransactionItemUM {

    /** Transaction hash */
    val txHash: String

    /**
     * Content state.
     *
     * @property amount             signed numeric value, e.g. "+0.500913" / "-350.31"; no currency symbol embedded.
     *                              `null` hides the numeric value while [currencySymbol] still shows.
     * @property currencySymbol     currency symbol shown alongside [amount], e.g. "BTC", "USDT"
     * @property isPendingSignature awaiting other co-signers (Joint account); shows a clock badge + "Pending" pill
     */
    data class Content(
        override val txHash: String,
        val amount: String?,
        val currencySymbol: String,
        val time: String,
        val status: Status,
        val direction: Direction,
        val onClick: () -> Unit,
        val icon: TxIcon,
        val title: TextReference,
        val subtitle: ContentSubtitle,
        val timestamp: Long,
        val warning: TextReference? = null,
        val isPendingSignature: Boolean = false,
    ) : TransactionItemUM {

        @Immutable
        sealed class Status {
            data object Failed : Status()
            data object Confirmed : Status()
            data object Unconfirmed : Status()
            data object PendingSignature : Status()
        }

        enum class Direction {
            INCOMING,
            OUTGOING,
        }
    }

    /** Subtitle variants for [Content] rows. */
    @Immutable
    sealed interface ContentSubtitle {
        /** Plain text — for types without a displayable address (Operation, GaslessFee, ClaimRewards, etc.). */
        data class Plain(val text: TextReference) : ContentSubtitle

        /**
         * Plain-text address subtitle without an identicon — renders "prefix: <address>", where [address]
         * (a substring of the resolved [text]) is painted in the primary text color while the prefix stays
         * tertiary. Used for contract / validator addresses, external swap addresses and yield "for: <address>".
         *
         * [text] carries the **raw** address, never a pre-shortened one: the row shortens it to the width the
         * subtitle gets. See `EllipsisText`.
         */
        data class PlainAddress(val text: TextReference, val address: String) : ContentSubtitle

        /**
         * External counterparty address — renders as "to/from: <identicon> <rawAddress>".
         * Used for Transfer to/from external addresses.
         *
         * The address is kept raw here; the row shortens it to the width the subtitle gets. See `EllipsisText`.
         */
        data class ExternalAddress(
            val direction: Direction,
            val rawAddress: String,
        ) : ContentSubtitle

        /**
         * Counterparty matches one of the user's own accounts — renders as "to/from: <accountIcon> <accountName>".
         */
        data class OwnAccount(
            val direction: Direction,
            val accountName: TextReference,
            val avatar: PortfolioAvatar,
        ) : ContentSubtitle

        /**
         * Leading avatar of whichever counterparty a subtitle names a portfolio for — an own account
         * ([OwnAccount], [AssetOwner.Account]) or a saved contact ([Contact]). Resolved from that portfolio's icon:
         * the default `Letter` icon renders the [Monogram] of [Monogram.name], any icon picked in the editor renders
         * as [Resource].
         *
         * [Monogram] carries the whole name rather than its first character because an account name is a
         * [TextReference] — the main account's title is a localized resource, so its letter is only known once the
         * reference resolves against the composition's resources. The shape is the enclosing subtitle's call, not the
         * avatar's: a circle for a contact, a rounded square for an account.
         */
        @Immutable
        sealed interface PortfolioAvatar {

            data class Monogram(val name: TextReference, val color: Color) : PortfolioAvatar

            data class Resource(@DrawableRes val resId: Int, val color: Color) : PortfolioAvatar
        }

        /**
         * Counterparty matches one of the user's own Tangem Pay (Payment) accounts — renders as
         * "to/from: <Visa icon> <accountName>".
         */
        data class OwnPaymentAccount(val direction: Direction, val accountName: TextReference) : ContentSubtitle

        /**
         * Counterparty matches one of the user's own wallets (cross-wallet transfer with accounts mode disabled) —
         * renders as "to/from: <walletIcon> <walletName>".
         */
        data class OwnWallet(
            val direction: Direction,
            val walletName: String,
            val deviceIconUM: DeviceIconUM,
        ) : ContentSubtitle

        /**
         * Counterparty saved in one of the user's address books — renders as "to/from: <avatar> <contactName>" in
         * place of the address, on either side of any transaction the address is shown for.
         */
        data class Contact(
            val direction: Direction,
            val contactName: String,
            val avatar: PortfolioAvatar,
        ) : ContentSubtitle

        /**
         * Counterparty asset ticker — renders as "to/from: <icon> <SYMBOL>", optionally followed by the counterparty
         * portfolio it settled in ("… in <owner>"). Used for express rows (swap counterparty currency / onramp fiat),
         * e.g. "to: ◎ POL", "from: 🇸🇪 SEK", or the cross-portfolio "to: ◎ POL in 🟦 Main".
         *
         * @property icon resolved counterparty currency icon, rendered via `CurrencyIcon`. `null` when no icon
         *   is available (e.g. onramp fiat carries no `CryptoCurrency`) — the ticker then renders without a leading icon.
         * @property owner counterparty portfolio the leg settled in — a different own account (accounts mode) or wallet
         *   (wallet mode) than the viewed token. `null` when there is nothing to disambiguate: a swap within one
         *   portfolio, an external address, or a single-wallet own leg — the ticker then renders without the "in …" tail.
         */
        data class Asset(
            val direction: Direction,
            val symbol: String,
            val icon: CurrencyIconState?,
            val owner: AssetOwner? = null,
        ) : ContentSubtitle

        /**
         * The counterparty a swap leg settled with, shown as the "in <owner>" tail of an [Asset] subtitle — one of the
         * user's own portfolios, or the [Contact] a foreign payout address is saved as. Mirrors the icon placement of
         * the standalone owner subtitles: [Account] / [PaymentAccount] / [Contact] render the icon before the name,
         * [Wallet] renders the name before its device icon.
         */
        @Immutable
        sealed interface AssetOwner {
            data class Account(
                val name: TextReference,
                val avatar: PortfolioAvatar,
            ) : AssetOwner

            data class PaymentAccount(val name: TextReference) : AssetOwner

            data class Wallet(val name: String, val deviceIconUM: DeviceIconUM) : AssetOwner

            /** Payout address saved in an address book — its [avatar] is drawn as a circle, as contacts are. */
            data class Contact(val name: String, val avatar: PortfolioAvatar) : AssetOwner
        }

        /**
         * What a fee-only row was paid for — "Send <token icon> USDT"; its amount is the fee itself, not anything
         * [symbol] moved. A row that cannot name its asset falls back to [Plain].
         *
         * @property icon token avatar drawn before the ticker; `null` renders the ticker alone.
         */
        data class FeeFor(val symbol: String, val icon: CurrencyIconState?) : ContentSubtitle

        /**
         * DeFi provider counterparty — renders as "to/from: <provider icon> <name>", e.g. "from: 🟣 Aave".
         * Unlike [OwnAccount] (a white-tinted monogram in a colored square), [iconResId] is a bundled full-color
         * provider drawable rendered untinted in a circle.
         *
         * @property iconResId full-color provider drawable, e.g. `img_aave_22`
         */
        data class Provider(
            val direction: Direction,
            val name: TextReference,
            @DrawableRes val iconResId: Int,
        ) : ContentSubtitle

        enum class Direction { TO, FROM }
    }

    /**
     * Compact status pill — used for Staking / YieldMode / Approve transactions where the row format
     * is replaced by a single chip with status-aware colors.
     *
     * @property kind            controls leading icon and color tint
     * @property status          drives background/text colors and Failed/Unconfirmed icon override
     * @property label           full pill label text (already composed by converter, e.g. "Staked")
     * @property amount          optional value rendered after [label] — a signed number (e.g. "950.43") or, for an
     *                           unlimited approval, a resolved word like "Unlimited"; null for kinds that don't
     *                           carry amount (Vote, Withdraw, Yield mode)
     * @property currencySymbol  currency symbol rendered after [amount]; null when [amount] is null
     * @property subtitle        optional subtitle — the spender address for Approve, the DeFi provider for a yield
     *                           supply / withdrawal
     */
    data class Pill(
        override val txHash: String,
        val kind: PillKind,
        val status: Content.Status,
        val label: TextReference,
        val amount: TextReference?,
        val currencySymbol: String?,
        val subtitle: PillSubtitle?,
        val timestamp: Long,
        val onClick: () -> Unit,
    ) : TransactionItemUM

    enum class PillKind {
        STAKING,
        YIELD_MODE,

        /** Funds moved in or out of the yield position — a settled one carries no leading icon, only its provider. */
        YIELD_SUPPLY,
        APPROVE,
    }

    @Immutable
    sealed interface PillSubtitle {
        /**
         * Address subtitle with inline IdentIcon (Blockies 8×8, hashed from [rawAddress]). The address is kept raw
         * here; the pill shortens it to the width it gets. See `EllipsisText`.
         */
        data class Address(val rawAddress: String) : PillSubtitle

        /**
         * DeFi provider the funds moved to or from — "to:/from: <provider icon> Aave", the pill's counterpart of
         * [ContentSubtitle.Provider]: [iconResId] is a bundled full-color drawable rendered untinted in a circle.
         */
        data class Provider(
            val direction: ContentSubtitle.Direction,
            val name: TextReference,
            @DrawableRes val iconResId: Int,
        ) : PillSubtitle
    }

    data class Loading(override val txHash: String) : TransactionItemUM

    data class Locked(override val txHash: String) : TransactionItemUM
}

/**
 * Status-circle icon for a [TransactionItemUM.Content] row.
 *
 * [Vector] holds a design-system [ImageVector] (`Icons.ic_*`) — the migration target; [Res] holds a `@DrawableRes`
 * XML fallback for transaction types that don't have a DS3 icon yet (e.g. gear / claim rewards).
 */
@Immutable
sealed interface TxIcon {

    /** Design-system vector icon (`Icons.ic_*`). */
    data class Vector(val imageVector: ImageVector) : TxIcon

    /**
     * Legacy XML drawable fallback.
     *
     * Forced temporary bridge: a few transaction types still map to legacy XML drawables that have no DS3
     * `Icons.ic_*` counterpart yet (e.g. gear / claim rewards), while some DS3 icons in turn have no legacy
     * drawable. Once the DS team ships the full icon set, every call site switches to [Vector] and this whole
     * subclass is removed.
     */
    @Deprecated(
        message = "Temporary fallback for tx types missing a DS3 icon. Migrate to TxIcon.Vector once the DS team " +
            "ships the remaining Icons.ic_* glyphs; this subclass will then be removed.",
        replaceWith = ReplaceWith("TxIcon.Vector(imageVector)"),
    )
    data class Res(@DrawableRes val resId: Int) : TxIcon
}

/** Resolves a [TxIcon] to the [ImageVector] to draw: the DS3 vector directly, or the legacy XML drawable parsed. */
@Suppress("DEPRECATION")
@Composable
fun TxIcon.asImageVector(): ImageVector = when (this) {
    is TxIcon.Vector -> imageVector
    is TxIcon.Res -> ImageVector.vectorResource(resId)
}