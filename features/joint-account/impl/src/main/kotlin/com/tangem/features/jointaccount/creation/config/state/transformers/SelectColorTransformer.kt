package com.tangem.features.jointaccount.creation.config.state.transformers

import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.creation.config.ui.state.JointAccountConfigUM
import com.tangem.utils.transformer.Transformer

internal class SelectColorTransformer(
    private val color: CryptoPortfolioIcon.Color,
) : Transformer<JointAccountConfigUM> {

    override fun transform(prevState: JointAccountConfigUM): JointAccountConfigUM =
        prevState.copy(icon = prevState.icon.copy(color = color))
}