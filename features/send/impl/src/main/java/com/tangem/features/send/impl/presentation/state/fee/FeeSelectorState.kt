package com.tangem.features.send.impl.presentation.state.fee

import androidx.compose.runtime.Immutable
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal sealed class FeeSelectorState {

    object Loading : FeeSelectorState()

    data class Content(
        val fees: TransactionFee,
        val selectedFee: FeeType = FeeType.MARKET,
        val customValues: ImmutableList<SendTextField.CustomFee> = persistentListOf(),
    ) : FeeSelectorState()

    object Error : FeeSelectorState()
}

enum class FeeType {
    SLOW,
    MARKET,
    FAST,
    CUSTOM,
}