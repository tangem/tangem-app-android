package com.tangem.features.send.feeselector.component

import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.api.subcomponents.feeSelector.params.FeeSelectorParams
import com.tangem.features.send.feeselector.model.FeeSelectorIntents
import kotlinx.coroutines.flow.MutableStateFlow

internal class FeeSelectorComponentParams(
    val parentParams: FeeSelectorParams.FeeSelectorDetailsParams,
    val state: MutableStateFlow<FeeSelectorUM>,
    val intents: FeeSelectorIntents,
)