package com.tangem.features.send.impl.presentation.state.fee

import androidx.compose.runtime.Immutable
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal sealed class FeeSelectorState {

    data class Content(
        val fees: TransactionFee,
        val selectedFee: FeeType = FeeType.Market,
        val customValues: ImmutableList<SendTextField.CustomFee> = persistentListOf(),
    ) : FeeSelectorState()

    data object Loading : FeeSelectorState()

    sealed class Error : FeeSelectorState() {
        data object NetworkError : Error()
        data class TronAccountActivationError(val tokenName: String) : Error()
    }
}

enum class FeeType {
    Slow,
    Market,
    Fast,
    Custom,
}
