package com.tangem.features.tangempay.entity

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.extensions.TextReference

internal sealed interface TangemPayTransactionState {

    val id: String

    data class Loading(override val id: String) : TangemPayTransactionState

    sealed interface Content : TangemPayTransactionState {

        val onClick: () -> Unit
        val amount: String
        val amountColor: @Composable (() -> Color)
        val title: TextReference
        val subtitle: TextReference
        val time: String

        data class Spend(
            override val id: String,
            override val onClick: () -> Unit,
            override val amount: String,
            override val amountColor: @Composable (() -> Color),
            override val title: TextReference,
            override val subtitle: TextReference,
            override val time: String,
            val iconUrl: String?,
        ) : Content

        data class Payment(
            override val id: String,
            override val onClick: () -> Unit,
            override val amount: String,
            override val amountColor: @Composable (() -> Color),
            override val title: TextReference,
            override val subtitle: TextReference,
            override val time: String,
            val isIncome: Boolean,
        ) : Content

        data class Fee(
            override val id: String,
            override val onClick: () -> Unit,
            override val amount: String,
            override val amountColor: @Composable (() -> Color),
            override val title: TextReference,
            override val subtitle: TextReference,
            override val time: String,
        ) : Content
    }
}