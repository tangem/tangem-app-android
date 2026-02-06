package com.tangem.features.send.v2.feeselector.component

import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.features.send.v2.feeselector.model.FeeSelectorIntents
import kotlinx.coroutines.flow.MutableStateFlow

internal class FeeSelectorComponentParams(
    val parentParams: FeeSelectorParams.FeeSelectorDetailsParams,
    val state: MutableStateFlow<FeeSelectorUM>,
    val intents: FeeSelectorIntents,
)