package com.tangem.features.tangempay.txhistory

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.ColorReference2
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed interface TangemPayTransactionState {

    val id: String

    data class Loading(override val id: String) : TangemPayTransactionState

    sealed interface Content : TangemPayTransactionState {

        val onClick: () -> Unit
        val amount: String
        val amountColor: ColorReference2
        val title: TextReference
        val subtitle: TextReference
        val icon: TangemIconUM
        val time: String

        data class Spend(
            override val id: String,
            override val onClick: () -> Unit,
            override val amount: String,
            override val amountColor: ColorReference2,
            override val title: TextReference,
            override val subtitle: TextReference,
            override val icon: TangemIconUM,
            override val time: String,
            /** Inline cashback badge shown next to [time]. `null` hides it. */
            val cashback: TangemPayTransactionCashbackUM? = null,
        ) : Content

        data class Payment(
            override val id: String,
            override val onClick: () -> Unit,
            override val amount: String,
            override val amountColor: ColorReference2,
            override val title: TextReference,
            override val subtitle: TextReference,
            override val icon: TangemIconUM,
            override val time: String,
        ) : Content

        data class Fee(
            override val id: String,
            override val onClick: () -> Unit,
            override val amount: String,
            override val amountColor: ColorReference2,
            override val title: TextReference,
            override val subtitle: TextReference,
            override val icon: TangemIconUM,
            override val time: String,
        ) : Content

        data class Collateral(
            override val id: String,
            override val onClick: () -> Unit,
            override val amount: String,
            override val amountColor: ColorReference2,
            override val title: TextReference,
            override val subtitle: TextReference,
            override val icon: TangemIconUM,
            override val time: String,
        ) : Content
    }
}

/**
 * Inline cashback badge on a transaction row. [amount] is the pre-formatted, signed value
 * (e.g. `+$5.00`); [style] drives the badge color scheme.
 */
@Immutable
internal data class TangemPayTransactionCashbackUM(
    val amount: String,
    val style: Style,
) {
    enum class Style {
        /** Confirmed cashback — highlighted (blue / info) badge. */
        Confirmed,

        /** Estimated (pending) cashback — neutral (grey) badge. */
        Estimated,
    }
}