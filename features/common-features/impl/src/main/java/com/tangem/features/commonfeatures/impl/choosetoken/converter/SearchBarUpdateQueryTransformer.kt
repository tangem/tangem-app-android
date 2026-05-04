package com.tangem.features.commonfeatures.impl.choosetoken.converter

import com.tangem.features.commonfeatures.impl.choosetoken.ui.ChooseTokenInitialUM
import com.tangem.utils.transformer.Transformer

internal class SearchBarUpdateQueryTransformer(private val newQuery: String) : Transformer<ChooseTokenInitialUM> {

    override fun transform(prevState: ChooseTokenInitialUM): ChooseTokenInitialUM {
        return prevState.copy(
            searchBar = prevState.searchBar.copy(query = newQuery),
        )
    }
}