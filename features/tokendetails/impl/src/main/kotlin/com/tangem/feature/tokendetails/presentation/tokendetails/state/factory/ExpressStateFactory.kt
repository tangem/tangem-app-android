package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import androidx.compose.runtime.Composable
import com.tangem.common.ui.expressStatus.state.DialogSlot
import com.tangem.common.ui.expressStatus.state.ExpressTransactionsBlockState
import com.tangem.common.ui.tokendetails.TokenDetailsDialogConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.model.ExpressTransactionsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.TokenDetailsDialogs
import com.tangem.utils.Provider
import kotlinx.collections.immutable.persistentListOf

internal class ExpressStateFactory(
    private val currentStateProvider: Provider<ExpressTransactionsBlockState>,
    private val expressTransactionsClickIntents: ExpressTransactionsClickIntents,
) {

    fun getInitialState(): ExpressTransactionsBlockState {
        return ExpressTransactionsBlockState(
            transactions = persistentListOf(),
            transactionsToDisplay = persistentListOf(),
            bottomSheetSlot = null,
            dialogSlot = null,
        )
    }

    fun getStateWithClosedDialog(): ExpressTransactionsBlockState {
        val state = currentStateProvider()
        val slot = state.dialogSlot ?: return state
        return state.copy(dialogSlot = slot.copy(config = slot.config.copy(isShow = false)))
    }

    fun getStateWithClosedBottomSheet(): ExpressTransactionsBlockState {
        val state = currentStateProvider()
        val slot = state.bottomSheetSlot ?: return state
        return state.copy(bottomSheetSlot = slot.copy(config = slot.config.copy(isShown = false)))
    }

    fun getStateWithConfirmHideExpressStatus(): ExpressTransactionsBlockState {
        return currentStateProvider().copy(
            dialogSlot = TokenDetailsDialogConfig(
                isShow = true,
                onDismissRequest = expressTransactionsClickIntents::onDismissDialog,
                content = TokenDetailsDialogConfig.DialogContentConfig.ConfirmExpressStatusHideDialogConfig(
                    onConfirmClick = {
                        expressTransactionsClickIntents.onDisposeExpressStatus()
                        expressTransactionsClickIntents.onDismissDialog()
                    },
                    onCancelClick = expressTransactionsClickIntents::onDismissDialog,
                ),
            ).toDialogSlot(),
        )
    }

    private fun TokenDetailsDialogConfig.toDialogSlot(): DialogSlot {
        val contentLambda: @Composable () -> Unit = {
            TokenDetailsDialogs(this)
        }
        return DialogSlot(config = this, content = contentLambda)
    }
}