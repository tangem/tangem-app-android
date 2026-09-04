package com.tangem.features.jointaccount.main.state.transformers

import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.utils.transformer.Transformer

internal class SetActivationConfirmingTransformer(
    private val isConfirming: Boolean,
) : Transformer<JointAccountMembersUM> {

    override fun transform(prevState: JointAccountMembersUM): JointAccountMembersUM = prevState.copy(
        activation = prevState.activation?.let { activation ->
            activation.copy(confirmation = activation.confirmation?.copy(isConfirming = isConfirming))
        },
    )
}