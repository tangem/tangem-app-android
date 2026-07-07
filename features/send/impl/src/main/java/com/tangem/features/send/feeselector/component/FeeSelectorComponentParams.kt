package com.tangem.features.send.feeselector.component

import com.tangem.features.send.api.entity.FeeSelectorUM
import com.tangem.features.send.api.params.FeeSelectorParams
import com.tangem.features.send.feeselector.model.FeeSelectorIntents
import kotlinx.coroutines.flow.MutableStateFlow

internal class FeeSelectorComponentParams(
    val parentParams: FeeSelectorParams.FeeSelectorDetailsParams,
    val state: MutableStateFlow<FeeSelectorUM>,
    val intents: FeeSelectorIntents,
)