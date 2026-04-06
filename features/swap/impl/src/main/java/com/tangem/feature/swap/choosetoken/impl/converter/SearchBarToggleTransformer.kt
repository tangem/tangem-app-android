package com.tangem.feature.swap.choosetoken.impl.converter

import com.tangem.feature.swap.choosetoken.impl.ui.ChooseTokenInitialUM
import com.tangem.utils.transformer.Transformer

internal class SearchBarToggleTransformer(private val isActive: Boolean) : Transformer<ChooseTokenInitialUM> {

    override fun transform(prevState: ChooseTokenInitialUM): ChooseTokenInitialUM {
        return prevState.copy(
            searchBar = prevState.searchBar.copy(isActive = isActive),
        )
    }
}