package com.tangem.features.tangempay.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.ColorReference
import com.tangem.core.ui.extensions.ColorReference2
import com.tangem.core.ui.extensions.ImageReference
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed interface TangemPayTransactionState {

    val id: String

    data class Loading(override val id: String) : TangemPayTransactionState

    sealed interface Content : TangemPayTransactionState {

        val onClick: () -> Unit
        val amount: String
        val amountColor: ColorReference
        val amountColorV2: ColorReference2
        val title: TextReference
        val subtitle: TextReference
        val icon: ImageReference
        val iconV2: TangemIconUM
        val time: String

        data class Spend(
            override val id: String,
            override val onClick: () -> Unit,
            override val amount: String,
            override val amountColor: ColorReference,
            override val amountColorV2: ColorReference2,
            override val title: TextReference,
            override val subtitle: TextReference,
            override val icon: ImageReference,
            override val iconV2: TangemIconUM,
            override val time: String,
        ) : Content

        data class Payment(
            override val id: String,
            override val onClick: () -> Unit,
            override val amount: String,
            override val amountColor: ColorReference,
            override val amountColorV2: ColorReference2,
            override val title: TextReference,
            override val subtitle: TextReference,
            override val icon: ImageReference,
            override val iconV2: TangemIconUM,
            override val time: String,
        ) : Content

        data class Fee(
            override val id: String,
            override val onClick: () -> Unit,
            override val amount: String,
            override val amountColor: ColorReference,
            override val amountColorV2: ColorReference2,
            override val title: TextReference,
            override val subtitle: TextReference,
            override val icon: ImageReference,
            override val iconV2: TangemIconUM,
            override val time: String,
        ) : Content

        data class Collateral(
            override val id: String,
            override val onClick: () -> Unit,
            override val amount: String,
            override val amountColor: ColorReference,
            override val amountColorV2: ColorReference2,
            override val title: TextReference,
            override val subtitle: TextReference,
            override val icon: ImageReference,
            override val iconV2: TangemIconUM,
            override val time: String,
        ) : Content
    }
}