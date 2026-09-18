package com.tangem.features.tangempay.withdraw.bank.choice.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawHeaderUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class TangemPayBankWithdrawChoiceUM(
    val header: BankWithdrawHeaderUM,
    val sectionTitle: TextReference,
    val options: ImmutableList<OptionUM>,
    val isContinueEnabled: Boolean,
    val onBackClick: () -> Unit,
    val onContinueClick: () -> Unit,
) {

    @Immutable
    data class OptionUM(
        val title: TextReference,
        val isSelected: Boolean,
        val onClick: () -> Unit,
    )
}