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
     * @property amount         signed numeric value, e.g. "+0.500913" / "-350.31"; no currency symbol embedded.
     *                          `null` hides the numeric value while [currencySymbol] still shows.
     * @property currencySymbol currency symbol shown alongside [amount], e.g. "BTC", "USDT"
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
    ) : TransactionItemUM {

        @Immutable
        sealed class Status {
            data object Failed : Status()
            data object Confirmed : Status()
            data object Unconfirmed : Status()
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
         * Plain-text address subtitle without an identicon — renders "prefix: <address>", where [highlight]
         * (a substring of the resolved [text]) is painted in the primary text color while the prefix stays
         * tertiary. Used for contract / validator addresses, external swap addresses and yield "for: <address>".
         */
        data class PlainAddress(val text: TextReference, val highlight: String) : ContentSubtitle

        /**
         * External counterparty address — renders as "to/from: <identicon> <briefAddress>".
         * Used for Transfer to/from external addresses.
         */
        data class ExternalAddress(
            val direction: Direction,
            val rawAddress: String,
            val briefAddress: String,
        ) : ContentSubtitle

        /**
         * Counterparty matches one of the user's own accounts — renders as "to/from: <accountIcon> <accountName>".
         */
        data class OwnAccount(
            val direction: Direction,
            val accountName: TextReference,
            @DrawableRes val iconResId: Int,
            val iconBackgroundColor: Color,
        ) : ContentSubtitle

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
         * Counterparty asset ticker — renders as "to/from: <icon> <SYMBOL>". Used for express rows
         * (swap counterparty currency / onramp fiat), e.g. "to: ◎ POL" or "from: 🇸🇪 SEK".
         *
         * @property icon resolved counterparty currency icon, rendered via `CurrencyIcon`. `null` when no icon
         *   is available (e.g. onramp fiat carries no `CryptoCurrency`) — the ticker then renders without a leading icon.
         */
        data class Asset(
            val direction: Direction,
            val symbol: String,
            val icon: CurrencyIconState?,
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
     * @property amount          optional signed numeric value rendered after [label] (e.g. "950.43");
     *                           null for kinds that don't carry amount (Vote, Withdraw, Yield mode)
     * @property currencySymbol  currency symbol rendered after [amount]; null when [amount] is null
     * @property subtitle        optional subtitle (e.g. "to: 33Bd...ga2B" with avatar) for Approve
     */
    data class Pill(
        override val txHash: String,
        val kind: PillKind,
        val status: Content.Status,
        val label: TextReference,
        val amount: String?,
        val currencySymbol: String?,
        val subtitle: PillSubtitle?,
        val timestamp: Long,
        val onClick: () -> Unit,
    ) : TransactionItemUM

    enum class PillKind {
        STAKING,
        YIELD_MODE,
        APPROVE,
    }

    @Immutable
    sealed interface PillSubtitle {
        /** Address subtitle with inline IdentIcon (Blockies 8×8, hashed from [rawAddress]). */
        data class Address(val rawAddress: String, val briefAddress: String) : PillSubtitle
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