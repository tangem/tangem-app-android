package com.tangem.features.jointaccount.common.displayname.state.transformers

import com.tangem.features.jointaccount.common.displayname.ui.state.JointAccountDisplayNameUM
import com.tangem.utils.transformer.Transformer

internal class UpdateButtonLoadingTransformer(
    private val isLoading: Boolean,
) : Transformer<JointAccountDisplayNameUM> {

    override fun transform(prevState: JointAccountDisplayNameUM): JointAccountDisplayNameUM =
        prevState.copy(isButtonLoading = isLoading)
}