package com.tangem.feature.tester.presentation.excludedblockchains

import androidx.lifecycle.ViewModel
import com.tangem.core.toggle.blockchain.MutableExcludedBlockchainsManager
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.feature.tester.presentation.excludedblockchains.state.ExcludedBlockchainsScreenUM
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
internal class ExcludedBlockchainsViewModel @Inject constructor(
    private val excludedBlockchainsManager: MutableExcludedBlockchainsManager,
) : ViewModel() {

    val uiState: MutableStateFlow<ExcludedBlockchainsScreenUM> = MutableStateFlow(
        value = ExcludedBlockchainsScreenUM(
            popBack = {},
            search = SearchBarUM(
                placeholderText = stringReference(value = "Filter by name or symbol"),
                query = "",
                isActive = false,
                onQueryChange = {},
                onActiveChange = {},
            ),
            blockchains = persistentListOf(),
            onRestartClick = {},
            onRestoreClick = {},
        ),
    )

    fun setupNavigation(router: InnerTesterRouter) {
    }
}