package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.SendUiStateType
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.state.SendNotification
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.toFormattedString
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

@Suppress("LongParameterList")
internal class FeeNotificationFactory(
    private val currentStateProvider: Provider<SendUiState>,
    private val stateRouterProvider: Provider<StateRouter>,
    private val clickIntents: SendClickIntents,
) {

    fun create() = stateRouterProvider().currentState
        .filter { it.type == SendUiStateType.Fee }
        .map {
            val state = currentStateProvider()
            val feeState = state.feeState ?: return@map persistentListOf()
            buildList {
                when (val feeSelectorState = feeState.feeSelectorState) {
                    FeeSelectorState.Loading -> Unit
                    FeeSelectorState.Error -> {
                        addFeeUnreachableNotification(feeSelectorState)
                    }
                    is FeeSelectorState.Content -> {
                        val customFee = feeSelectorState.customValues
                        val selectedFee = feeSelectorState.selectedFee
                        addTooHighNotification(feeSelectorState.fees, selectedFee, customFee)
                    }
                }
            }.toImmutableList()
        }

    private fun MutableList<SendNotification>.addFeeUnreachableNotification(feeSelectorState: FeeSelectorState) {
        if (feeSelectorState is FeeSelectorState.Error) {
            add(SendNotification.Warning.NetworkFeeUnreachable(clickIntents::feeReload))
        }
    }

    private fun MutableList<SendNotification>.addTooHighNotification(
        transactionFee: TransactionFee,
        selectedFee: FeeType,
        customFee: List<SendTextField.CustomFee>,
    ) {
        val multipleFees = transactionFee as? TransactionFee.Choosable ?: return
        val highValue = multipleFees.priority.amount.value ?: return
        val customAmount = customFee.firstOrNull() ?: return
        val customValue = customAmount.value.parseToBigDecimal(customAmount.decimals)
        val diff = customValue / highValue
        if (selectedFee == FeeType.Custom && diff > FEE_MAX_DIFF) {
            add(SendNotification.Warning.TooHigh(diff.toFormattedString(HIGH_FEE_DIFF_DECIMALS)))
        }
    }

    companion object {
        private val FEE_MAX_DIFF = BigDecimal(5)
        private const val HIGH_FEE_DIFF_DECIMALS = 0
    }
}
