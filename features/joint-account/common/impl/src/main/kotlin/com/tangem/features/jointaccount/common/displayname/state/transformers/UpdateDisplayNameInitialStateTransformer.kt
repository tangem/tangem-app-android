package com.tangem.features.jointaccount.common.displayname.state.transformers

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.jointaccount.common.displayname.DisplayNameValidator
import com.tangem.features.jointaccount.common.displayname.ui.state.JointAccountDisplayNameUM
import com.tangem.utils.transformer.Transformer

internal class UpdateDisplayNameInitialStateTransformer(
    private val buttonText: TextReference,
    private val initialName: String,
    private val onNameChange: (String) -> Unit,
    private val onContinueClick: () -> Unit,
    private val onBackClick: () -> Unit,
) : Transformer<JointAccountDisplayNameUM> {

    private val validator = DisplayNameValidator()

    override fun transform(prevState: JointAccountDisplayNameUM): JointAccountDisplayNameUM = prevState.copy(
        name = initialName,
        isError = validator.hasForbiddenChars(initialName),
        buttonText = buttonText,
        isButtonEnabled = validator.isValid(initialName),
        onNameChange = onNameChange,
        onContinueClick = onContinueClick,
        onBackClick = onBackClick,
    )
}