package com.tangem.features.jointaccount.creation.config.state

import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.creation.config.ui.state.JointAccountConfigUM
import com.tangem.features.jointaccount.creation.impl.R
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class JointAccountConfigStateController @Inject constructor() {

    val uiState: StateFlow<JointAccountConfigUM>
        field = MutableStateFlow(value = getInitialState())

    fun update(transformer: Transformer<JointAccountConfigUM>) {
        uiState.update(function = transformer::transform)
    }

    private fun getInitialState(): JointAccountConfigUM = JointAccountConfigUM(
        name = "",
        namePlaceholder = resourceReference(R.string.common_joint_account),
        icon = AccountIconUM.CryptoPortfolio(value = DEFAULT_ICON, color = DEFAULT_COLOR),
        colors = CryptoPortfolioIcon.Color.entries.toImmutableList(),
        icons = CryptoPortfolioIcon.Icon.entries.toImmutableList(),
        wallet = null,
        // The name starts empty and is required, so the button unlocks with the first valid input
        isContinueEnabled = false,
        onNameChange = {},
        onColorClick = {},
        onIconClick = {},
        onContinueClick = {},
        onBackClick = {},
    )

    private companion object {
        val DEFAULT_ICON = CryptoPortfolioIcon.Icon.Family
        val DEFAULT_COLOR = CryptoPortfolioIcon.Color.Azure
    }
}