package com.tangem.features.txhistory.joint.operation.ui

import androidx.compose.runtime.Composable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.ui.TxHistoryDetailsContent
import com.tangem.features.txhistory.ui.TxHistoryDetailsTopNavigation

@Composable
internal fun JointOperationContentSheet(state: TxHistoryDetailsUM.SingleAsset?, onCloseClick: () -> Unit) {
    TangemModalBottomSheet<TxHistoryDetailsUM>(
        containerColor = TangemTheme.colors3.bg.secondary,
        config = TangemBottomSheetConfig(
            isShown = state != null,
            onDismissRequest = onCloseClick,
            content = state ?: TangemBottomSheetConfigContent.Empty,
        ),
        title = { um -> TxHistoryDetailsTopNavigation(header = um.header, onCloseClick = onCloseClick) },
        content = { um -> TxHistoryDetailsContent(state = um) },
    )
}