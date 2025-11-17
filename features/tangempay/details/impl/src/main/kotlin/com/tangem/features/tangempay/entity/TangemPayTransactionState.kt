package com.tangem.features.tangempay.entity

import com.tangem.core.ui.extensions.ColorReference
import com.tangem.core.ui.extensions.ImageReference
import com.tangem.core.ui.extensions.TextReference

internal sealed interface TangemPayTransactionState {

    val id: String

    data class Loading(override val id: String) : TangemPayTransactionState

    sealed interface Content : TangemPayTransactionState {

        val onClick: () -> Unit
        val amount: String
        val amountColor: ColorReference
        val title: TextReference
        val subtitle: TextReference
        val icon: ImageReference
        val time: String

        data class Spend(
            override val id: String,
            override val onClick: () -> Unit,
            override val amount: String,
            override val amountColor: ColorReference,
            override val title: TextReference,
            override val subtitle: TextReference,
            override val icon: ImageReference,
            override val time: String,
        ) : Content

        data class Payment(
            override val id: String,
            override val onClick: () -> Unit,
            override val amount: String,
            override val amountColor: ColorReference,
            override val title: TextReference,
            override val subtitle: TextReference,
            override val icon: ImageReference,
            override val time: String,
        ) : Content

        data class Fee(
            override val id: String,
            override val onClick: () -> Unit,
            override val amount: String,
            override val amountColor: ColorReference,
            override val title: TextReference,
            override val subtitle: TextReference,
            override val icon: ImageReference,
            override val time: String,
        ) : Content

        data class Collateral(
            override val id: String,
            override val onClick: () -> Unit,
            override val amount: String,
            override val amountColor: ColorReference,
            override val title: TextReference,
            override val subtitle: TextReference,
            override val icon: ImageReference,
            override val time: String,
        ) : Content
    }
}