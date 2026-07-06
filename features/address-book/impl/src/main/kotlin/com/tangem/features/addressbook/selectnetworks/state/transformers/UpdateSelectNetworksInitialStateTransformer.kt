package com.tangem.features.addressbook.selectnetworks.state.transformers

import com.tangem.features.addressbook.selectnetworks.ui.state.SelectNetworksUM
import com.tangem.utils.transformer.Transformer

internal class UpdateSelectNetworksInitialStateTransformer(
    private val onQueryChange: (String) -> Unit,
    private val onActiveChange: (Boolean) -> Unit,
    private val onBackClick: () -> Unit,
    private val onDoneClick: () -> Unit,
) : Transformer<SelectNetworksUM> {

    override fun transform(prevState: SelectNetworksUM): SelectNetworksUM {
        return prevState.copy(
            searchBar = prevState.searchBar.copy(
                onQueryChange = onQueryChange,
                onActiveChange = onActiveChange,
                onCloseClick = { onActiveChange(false) },
                onClearClick = { onQueryChange("") },
            ),
            doneButton = prevState.doneButton.copy(onClick = onDoneClick),
            onBackClick = onBackClick,
        )
    }
}