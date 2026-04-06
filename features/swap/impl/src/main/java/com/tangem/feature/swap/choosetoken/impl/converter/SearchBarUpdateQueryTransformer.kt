package com.tangem.feature.swap.choosetoken.impl.converter

import com.tangem.feature.swap.choosetoken.impl.ui.ChooseTokenInitialUM
import com.tangem.utils.transformer.Transformer

internal class SearchBarUpdateQueryTransformer(private val newQuery: String) : Transformer<ChooseTokenInitialUM> {

    override fun transform(prevState: ChooseTokenInitialUM): ChooseTokenInitialUM {
        return prevState.copy(
            searchBar = prevState.searchBar.copy(query = newQuery),
        )
    }
}