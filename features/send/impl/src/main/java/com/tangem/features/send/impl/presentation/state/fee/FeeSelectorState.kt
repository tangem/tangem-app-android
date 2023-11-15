package com.tangem.features.send.impl.presentation.state.fee

import androidx.compose.runtime.Immutable
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import kotlinx.coroutines.flow.MutableStateFlow

@Immutable
internal sealed class FeeSelectorState {

    object Loading : FeeSelectorState()

    object Empty : FeeSelectorState()

    data class Content(
        val fees: TransactionFee,
        val selectedFee: FeeType = FeeType.MARKET,
        val customValues: MutableStateFlow<List<SendTextField.CustomFee>> = MutableStateFlow(emptyList()),
    ) : FeeSelectorState()
}

enum class FeeType {
    SLOW,
    MARKET,
    FAST,
    CUSTOM,
}