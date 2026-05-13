package com.tangem.core.ui.components.transactions.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
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
     * @property amount         signed numeric value, e.g. "+0.500913" / "-350.31"; no currency symbol embedded
     * @property currencySymbol currency symbol shown alongside [amount], e.g. "BTC", "USDT"
     */
    data class Content(
        override val txHash: String,
        val amount: String,
        val currencySymbol: String,
        val time: String,
        val status: Status,
        val direction: Direction,
        val onClick: () -> Unit,
        @DrawableRes val iconRes: Int,
        val title: TextReference,
        val subtitle: ContentSubtitle,
        val timestamp: Long,
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
        /** Plain text — for types without a directly-displayable address (Operation, GaslessFee, ClaimRewards, etc.). */
        data class Plain(val text: TextReference) : ContentSubtitle

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