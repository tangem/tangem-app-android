package com.tangem.features.jointaccount.creation.composition.state

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.features.jointaccount.creation.composition.ui.state.JointAccountCompositionUM
import com.tangem.domain.jointaccount.model.JointAccountConfig.Companion.MAX_MEMBERS_COUNT
import com.tangem.domain.jointaccount.model.JointAccountConfig.Companion.MIN_MEMBERS_COUNT
import com.tangem.utils.transformer.Transformer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class JointAccountCompositionStateController @Inject constructor() {

    val uiState: StateFlow<JointAccountCompositionUM>
        field = MutableStateFlow(value = getInitialState())

    fun update(transformer: Transformer<JointAccountCompositionUM>) {
        uiState.update(function = transformer::transform)
    }

    private fun getInitialState(): JointAccountCompositionUM = JointAccountCompositionUM(
        totalMembers = JointAccountCompositionUM.StepperUM(
            value = MIN_MEMBERS_COUNT,
            isDecrementEnabled = false,
            isIncrementEnabled = true,
            onDecrement = {},
            onIncrement = {},
        ),
        requiredToSign = JointAccountCompositionUM.StepperUM(
            value = MIN_MEMBERS_COUNT,
            isDecrementEnabled = true,
            isIncrementEnabled = false,
            onDecrement = {},
            onIncrement = {},
        ),
        maxMembers = MAX_MEMBERS_COUNT,
        onContinueClick = {},
        onBackClick = {},
    )
}