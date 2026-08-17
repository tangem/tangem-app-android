package com.tangem.features.jointaccount.join.invitepreview.state

import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.join.invitepreview.ui.state.JointAccountInvitePreviewUM
import com.tangem.utils.transformer.Transformer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class JointAccountInvitePreviewStateController @Inject constructor() {

    val uiState: StateFlow<JointAccountInvitePreviewUM>
        field = MutableStateFlow(value = getInitialState())

    fun update(transformer: Transformer<JointAccountInvitePreviewUM>) {
        uiState.update(function = transformer::transform)
    }

    // TODO: replace the stubbed invite info once the domain integration lands
    private fun getInitialState(): JointAccountInvitePreviewUM = JointAccountInvitePreviewUM(
        accountName = STUB_ACCOUNT_NAME,
        accountIcon = AccountIconUM.CryptoPortfolio(value = STUB_ICON, color = STUB_COLOR),
        requiredToSign = STUB_REQUIRED_TO_SIGN,
        totalMembers = STUB_TOTAL_MEMBERS,
        creatorName = STUB_CREATOR_NAME,
        wallet = null,
        confirmation = null,
        onCreatorInfoClick = {},
        onContinueClick = {},
        onCloseClick = {},
    )

    private companion object {
        const val STUB_ACCOUNT_NAME = "Family savings"
        const val STUB_CREATOR_NAME = "Igor Sinyak"
        const val STUB_REQUIRED_TO_SIGN = 2
        const val STUB_TOTAL_MEMBERS = 5
        val STUB_ICON = CryptoPortfolioIcon.Icon.Family
        val STUB_COLOR = CryptoPortfolioIcon.Color.Azure
    }
}