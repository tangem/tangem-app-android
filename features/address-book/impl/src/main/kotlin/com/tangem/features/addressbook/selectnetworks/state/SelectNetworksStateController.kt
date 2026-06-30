package com.tangem.features.addressbook.selectnetworks.state

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.addressbook.selectnetworks.ui.state.SelectNetworksUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class SelectNetworksStateController @Inject constructor() {

    val uiState: StateFlow<SelectNetworksUM>
        field = MutableStateFlow(value = getInitialState())

    fun update(transformer: Transformer<SelectNetworksUM>) {
        uiState.update(function = transformer::transform)
    }

    private fun getInitialState(): SelectNetworksUM = SelectNetworksUM(
        searchBar = TangemSearch.State(
            placeholderText = resourceReference(R.string.common_search),
            query = "",
            onQueryChange = {},
            isActive = false,
            onActiveChange = {},
            onClearClick = {},
            onCloseClick = {},
        ),
        networks = persistentListOf(),
        doneButton = TangemButtonUM(
            text = TextReference.Res(R.string.common_done),
            type = TangemButtonType.Primary,
            isEnabled = false,
            onClick = {},
        ),
        onBackClick = {},
    )
}