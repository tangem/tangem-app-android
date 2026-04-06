package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import com.tangem.common.ui.expressStatus.state.ExpressTransactionsBlockState
import com.tangem.utils.Provider
import kotlinx.collections.immutable.persistentListOf

internal class ExpressStateFactory(
    private val currentStateProvider: Provider<ExpressTransactionsBlockState>,
) {

    fun getInitialState(): ExpressTransactionsBlockState {
        return ExpressTransactionsBlockState(
            transactions = persistentListOf(),
            transactionsToDisplay = persistentListOf(),
            bottomSheetSlot = null,
        )
    }

    fun getStateWithClosedBottomSheet(): ExpressTransactionsBlockState {
        val state = currentStateProvider()
        return state.copy(bottomSheetSlot = null)
    }
}