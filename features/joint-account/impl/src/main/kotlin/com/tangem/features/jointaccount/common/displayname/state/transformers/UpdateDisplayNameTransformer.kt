package com.tangem.features.jointaccount.common.displayname.state.transformers

import com.tangem.features.jointaccount.common.displayname.DisplayNameValidator
import com.tangem.features.jointaccount.common.displayname.ui.state.JointAccountDisplayNameUM
import com.tangem.utils.transformer.Transformer

internal class UpdateDisplayNameTransformer(
    private val name: String,
) : Transformer<JointAccountDisplayNameUM> {

    private val validator = DisplayNameValidator()

    override fun transform(prevState: JointAccountDisplayNameUM): JointAccountDisplayNameUM {
        if (name.length > DisplayNameValidator.MAX_LENGTH) return prevState

        val hasForbiddenChars = validator.hasForbiddenChars(name)

        return prevState.copy(
            name = name,
            isError = hasForbiddenChars,
            isButtonEnabled = name.isNotBlank() && !hasForbiddenChars,
        )
    }
}