package com.tangem.features.tangempay.withdraw.bank.choice.model

import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/** A single-select wizard step: the caption above the options, the options themselves and the answer already given. */
internal data class BankWithdrawChoiceConfig(
    val sectionTitle: TextReference,
    val options: ImmutableList<BankWithdrawChoiceOption>,
    /** Null until the step has been answered — the step opens with nothing selected. */
    val selectedId: String?,
)

internal data class BankWithdrawChoiceOption(
    val id: String,
    val title: TextReference,
)