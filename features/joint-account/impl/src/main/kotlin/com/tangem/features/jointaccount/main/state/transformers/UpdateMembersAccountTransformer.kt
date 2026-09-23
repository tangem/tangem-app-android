package com.tangem.features.jointaccount.main.state.transformers

import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.jointaccount.main.converter.JointAccountToMembersUMConverter
import com.tangem.utils.transformer.Transformer

/**
 * Projects the domain [account] and the [invites] this device holds onto the screen via
 * [JointAccountToMembersUMConverter]. The activation-confirmation sheet lives in its own child slot (see
 * `JointAccountMembersModel.activationConfirmationNavigation`), not in [JointAccountMembersUM], so a plain
 * account refresh has nothing of that kind to preserve here.
 */
internal class UpdateMembersAccountTransformer(
    private val account: JointAccount,
    private val invites: List<String>,
    private val ownAddress: String?,
    private val callbacks: JointAccountToMembersUMConverter.Callbacks,
) : Transformer<JointAccountMembersUM> {

    override fun transform(prevState: JointAccountMembersUM): JointAccountMembersUM =
        JointAccountToMembersUMConverter(callbacks = callbacks).convert(
            value = JointAccountToMembersUMConverter.Input(
                account = account,
                invites = invites,
                ownAddress = ownAddress,
            ),
        )
}