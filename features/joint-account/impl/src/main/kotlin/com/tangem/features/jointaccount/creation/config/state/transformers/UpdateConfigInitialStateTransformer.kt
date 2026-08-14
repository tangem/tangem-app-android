package com.tangem.features.jointaccount.creation.config.state.transformers

import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.creation.config.ui.state.JointAccountConfigUM
import com.tangem.utils.transformer.Transformer

internal class UpdateConfigInitialStateTransformer(
    private val onNameChange: (String) -> Unit,
    private val onColorClick: (CryptoPortfolioIcon.Color) -> Unit,
    private val onIconClick: (CryptoPortfolioIcon.Icon) -> Unit,
    private val onContinueClick: () -> Unit,
    private val onBackClick: () -> Unit,
) : Transformer<JointAccountConfigUM> {

    override fun transform(prevState: JointAccountConfigUM): JointAccountConfigUM = prevState.copy(
        onNameChange = onNameChange,
        onColorClick = onColorClick,
        onIconClick = onIconClick,
        onContinueClick = onContinueClick,
        onBackClick = onBackClick,
    )
}