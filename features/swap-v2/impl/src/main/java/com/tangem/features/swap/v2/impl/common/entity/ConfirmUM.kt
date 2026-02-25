package com.tangem.features.swap.v2.impl.common.entity

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.swap.models.SwapDataModel
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class ConfirmUM {

    abstract val isPrimaryButtonEnabled: Boolean

    data class Content(
        override val isPrimaryButtonEnabled: Boolean = false,
        val isTransactionInProcess: Boolean,
        val isShowTapHelp: Boolean,
        val sendingFooter: TextReference,
        val notifications: ImmutableList<NotificationUM>,
        val tosUM: TosUM?,
    ) : ConfirmUM() {
        data class TosUM(
            val tosLink: LegalUM?,
            val policyLink: LegalUM?,
        )

        data class LegalUM(
            val title: TextReference,
            val link: String,
        )
    }

    data class Success(
        override val isPrimaryButtonEnabled: Boolean = true,
        val transactionDate: Long,
        val txUrl: String,
        val swapDataModel: SwapDataModel,
        val provider: ExpressProvider,
        val amountUM: SwapAmountUM,
        val destinationUM: DestinationUM,
        val feeSelectorUM: FeeSelectorUM,
    ) : ConfirmUM()

    data object Empty : ConfirmUM() {
        override val isPrimaryButtonEnabled: Boolean = false
    }
}