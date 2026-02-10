package com.tangem.common.ui.expressStatus.state

import androidx.compose.runtime.Composable
import com.tangem.common.ui.tokendetails.TokenDetailsDialogConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import kotlinx.collections.immutable.PersistentList

data class ExpressTransactionsBlockState(
    val transactions: PersistentList<ExpressTransactionStateUM>,
    val transactionsToDisplay: PersistentList<ExpressTransactionStateUM>,
    val bottomSheetSlot: BottomSheetSlot?,
    val dialogSlot: DialogSlot?,
)

data class BottomSheetSlot(
    val config: TangemBottomSheetConfig,
    val content: @Composable () -> Unit,
)

data class DialogSlot(
    val config: TokenDetailsDialogConfig,
    val content: @Composable () -> Unit,
)