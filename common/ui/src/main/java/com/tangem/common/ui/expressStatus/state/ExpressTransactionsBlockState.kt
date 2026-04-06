package com.tangem.common.ui.expressStatus.state

import androidx.compose.runtime.Composable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import kotlinx.collections.immutable.PersistentList

data class ExpressTransactionsBlockState(
    val transactions: PersistentList<ExpressTransactionStateUM>,
    val transactionsToDisplay: PersistentList<ExpressTransactionStateUM>,
    val bottomSheetSlot: BottomSheetSlot?,
)

data class BottomSheetSlot(
    val config: TangemBottomSheetConfig,
    val content: @Composable () -> Unit,
)