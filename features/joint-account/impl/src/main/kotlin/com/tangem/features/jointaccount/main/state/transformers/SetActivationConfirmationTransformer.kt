package com.tangem.features.jointaccount.main.state.transformers

import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.utils.transformer.Transformer

internal class SetActivationConfirmationTransformer(
    private val confirmation: JointAccountMembersUM.ActivationUM.ConfirmationUM?,
) : Transformer<JointAccountMembersUM> {

    override fun transform(prevState: JointAccountMembersUM): JointAccountMembersUM = prevState.copy(
        activation = prevState.activation?.copy(confirmation = confirmation),
    )
}