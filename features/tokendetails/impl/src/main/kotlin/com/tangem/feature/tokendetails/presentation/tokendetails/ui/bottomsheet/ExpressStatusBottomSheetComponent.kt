package com.tangem.feature.tokendetails.presentation.tokendetails.ui.bottomsheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.expressStatus.ExpressStatusBottomSheetConfig
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.express.ExpressStatusBottomSheet
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.flow.StateFlow

internal class ExpressStatusBottomSheetComponent(
    private val txId: String,
    private val expressTxsFlow: StateFlow<PersistentList<ExpressTransactionStateUM>>,
    private val onDismiss: () -> Unit,
) : ComposableBottomSheetComponent {

    override fun dismiss() {
        onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val expressTxs by expressTxsFlow.collectAsStateWithLifecycle()
        val expressState = remember(expressTxs, txId) {
            expressTxs.firstOrNull { it.info.txId == txId }
        } ?: return

        val config = remember(expressState) {
            TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = ExpressStatusBottomSheetConfig(value = expressState),
            )
        }
        ExpressStatusBottomSheet(config = config)
    }
}