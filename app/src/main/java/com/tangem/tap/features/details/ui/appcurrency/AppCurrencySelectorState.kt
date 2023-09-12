package com.tangem.tap.features.details.ui.appcurrency

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.event.StateEvent
import kotlinx.collections.immutable.PersistentList

@Immutable
internal sealed class AppCurrencySelectorState {

    abstract val onBackClick: () -> Unit

    data class Loading(
        override val onBackClick: () -> Unit,
    ) : AppCurrencySelectorState()

    @Immutable
    sealed class Content : AppCurrencySelectorState() {
        abstract val selectedId: String
        abstract val scrollToSelected: StateEvent<Int>
        abstract val items: PersistentList<Currency>
        abstract val onCurrencyClick: (Currency) -> Unit
        abstract val onTopBarActionClick: () -> Unit
    }

    data class Default(
        override val selectedId: String,
        override val items: PersistentList<Currency>,
        override val onCurrencyClick: (Currency) -> Unit,
        override val onBackClick: () -> Unit,
        override val onTopBarActionClick: () -> Unit,
        override val scrollToSelected: StateEvent<Int>,
    ) : Content()

    data class Search(
        override val selectedId: String,
        override val items: PersistentList<Currency>,
        override val scrollToSelected: StateEvent<Int>,
        override val onCurrencyClick: (Currency) -> Unit,
        override val onBackClick: () -> Unit,
        override val onTopBarActionClick: () -> Unit,
        val onSearchInputChange: (String) -> Unit,
    ) : Content()

    data class Currency(
        val id: String,
        val name: String,
    )
}