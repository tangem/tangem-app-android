package com.tangem.features.send.impl.presentation.state.fee

import androidx.compose.runtime.Immutable
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class FeeSelectorState(
    val fees: TransactionFee? = null,
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val selectedFee: FeeType = FeeType.Market,
    val customValues: ImmutableList<SendTextField.CustomFee> = persistentListOf(),
)

enum class FeeType {
    Slow,
    Market,
    Fast,
    Custom,
}
