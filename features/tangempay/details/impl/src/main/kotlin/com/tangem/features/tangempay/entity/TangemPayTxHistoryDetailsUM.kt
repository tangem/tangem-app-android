package com.tangem.features.tangempay.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.ColorReference
import com.tangem.core.ui.extensions.ImageReference
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class TangemPayTxHistoryDetailsUM(
    val isBalanceHidden: Boolean,
    val title: TextReference,
    val iconState: ImageReference,
    val transactionTitle: TextReference,
    val transactionSubtitle: TextReference,
    val transactionAmount: String,
    val transactionAmountColor: ColorReference,
    val localTransactionText: String?,
    val labelState: LabelUM?,
    val notification: NotificationState?,
    val buttons: ImmutableList<ButtonState>,
    val dismiss: () -> Unit,
) {

    data class NotificationState(
        val config: NotificationConfig,
        val titleColor: ColorReference,
        val iconTint: ColorReference,
        val containerColor: ColorReference?,
    )
}

internal data class ButtonState(
    val text: TextReference,
    val onClick: () -> Unit,
    val startIcon: ImageReference.Res? = null,
)

internal data class TangemPayTxHistoryDetailsUMV2(
    val isBalanceHidden: Boolean,
    val title: TextReference,
    val subtitle: TextReference,
    val iconState: TangemIconUM,
    val transactionTitle: TextReference,
    val transactionCategory: TextReference,
    val mcc: TextReference?,
    val transactionAmount: String,
    val localTransactionText: String?,
    val label: TransactionLabelUM?,
    val buttonState: ButtonState,
    val dismiss: () -> Unit,
)

internal data class TransactionLabelUM(
    val transactionStateType: TransactionStateType,
    val icon: TangemIconUM,
    val title: TextReference,
    val subtitle: TextReference? = null,
)

internal enum class TransactionStateType {
    Completed,
    InProgress,
    Rejected,
    Reversed,
}

@Immutable
internal sealed interface TangemPayTxHistoryDetailsUiState {
    data class Legacy(val state: TangemPayTxHistoryDetailsUM) : TangemPayTxHistoryDetailsUiState
    data class Redesign(val state: TangemPayTxHistoryDetailsUMV2) : TangemPayTxHistoryDetailsUiState
}

internal data class TangemPayTxHistoryDetailsUiStates(
    val legacy: TangemPayTxHistoryDetailsUM,
    val redesign: TangemPayTxHistoryDetailsUMV2,
) {
    fun toUiState(isRedesignEnabled: Boolean): TangemPayTxHistoryDetailsUiState {
        return if (isRedesignEnabled) {
            TangemPayTxHistoryDetailsUiState.Redesign(redesign)
        } else {
            TangemPayTxHistoryDetailsUiState.Legacy(legacy)
        }
    }
}