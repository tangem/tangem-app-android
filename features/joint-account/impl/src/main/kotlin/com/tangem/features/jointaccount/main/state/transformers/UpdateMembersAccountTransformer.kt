package com.tangem.features.jointaccount.main.state.transformers

import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountInvite
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.jointaccount.main.converter.JointAccountToMembersUMConverter
import com.tangem.utils.transformer.Transformer

internal class UpdateMembersAccountTransformer(
    private val account: JointAccount,
    private val invites: List<JointAccountInvite>,
    private val sharedInvites: Set<String>,
    private val ownAddress: String?,
    private val callbacks: JointAccountToMembersUMConverter.Callbacks,
    private val accountName: AccountName? = null,
    private val accountIcon: CryptoPortfolioIcon? = null,
) : Transformer<JointAccountMembersUM> {
    override fun transform(prevState: JointAccountMembersUM): JointAccountMembersUM =
        JointAccountToMembersUMConverter(callbacks = callbacks).convert(
            value = JointAccountToMembersUMConverter.Input(
                account = account,
                invites = invites,
                sharedInvites = sharedInvites,
                ownAddress = ownAddress,
                accountName = accountName,
                accountIcon = accountIcon,
            ),
        )
}