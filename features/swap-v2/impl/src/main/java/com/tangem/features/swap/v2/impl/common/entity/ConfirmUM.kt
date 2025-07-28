package com.tangem.features.swap.v2.impl.common.entity

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.swap.models.SwapDataModel
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class ConfirmUM {

    abstract val isPrimaryButtonEnabled: Boolean

    data class Content(
        override val isPrimaryButtonEnabled: Boolean = false,
        val isTransactionInProcess: Boolean,
        val showTapHelp: Boolean,
        val sendingFooter: TextReference,
        val notifications: ImmutableList<NotificationUM>,
    ) : ConfirmUM()

    data class Success(
        override val isPrimaryButtonEnabled: Boolean = true,
        val transactionDate: Long,
        val txUrl: String,
        val swapDataModel: SwapDataModel,
        val provider: ExpressProvider,
    ) : ConfirmUM()

    data object Empty : ConfirmUM() {
        override val isPrimaryButtonEnabled: Boolean = false
    }
}