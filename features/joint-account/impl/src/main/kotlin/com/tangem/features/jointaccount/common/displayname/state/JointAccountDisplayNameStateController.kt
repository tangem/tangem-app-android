package com.tangem.features.jointaccount.common.displayname.state

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.jointaccount.common.displayname.ui.state.JointAccountDisplayNameUM
import com.tangem.utils.transformer.Transformer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class JointAccountDisplayNameStateController @Inject constructor() {

    val uiState: StateFlow<JointAccountDisplayNameUM>
        field = MutableStateFlow(value = getInitialState())

    fun update(transformer: Transformer<JointAccountDisplayNameUM>) {
        uiState.update(function = transformer::transform)
    }

    private fun getInitialState(): JointAccountDisplayNameUM = JointAccountDisplayNameUM(
        name = "",
        isError = false,
        buttonText = TextReference.EMPTY,
        buttonIconRes = null,
        isButtonEnabled = false,
        isButtonLoading = false,
        onNameChange = {},
        onContinueClick = {},
        onBackClick = {},
    )
}