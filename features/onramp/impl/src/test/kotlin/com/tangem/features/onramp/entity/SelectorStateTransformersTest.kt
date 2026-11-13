package com.tangem.features.onramp.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.onramp.selectcountry.entity.CountryItemState
import com.tangem.features.onramp.selectcountry.entity.CountryListUM
import com.tangem.features.onramp.selectcountry.entity.transformer.UpdateCountryItemsErrorTransformer
import com.tangem.features.onramp.selectcountry.entity.transformer.UpdateCountryItemsLoadingTransformer
import com.tangem.features.onramp.selectcurrency.entity.CurrenciesListUM
import com.tangem.features.onramp.selectcurrency.entity.CurrenciesSection
import com.tangem.features.onramp.selectcurrency.entity.CurrencyItemState
import com.tangem.features.onramp.selectcurrency.entity.transformer.UpdateCurrencyItemsErrorTransformer
import com.tangem.features.onramp.selectcurrency.entity.transformer.UpdateCurrencyItemsLoadingTransformer
import com.tangem.features.onramp.utils.UpdateSearchBarCallbacksTransformer
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

internal class SelectorStateTransformersTest {
    private val search = SearchBarUM(TextReference.EMPTY, "query", {}, true, {})
    private val retry: () -> Unit = mockk()

    @Test
    fun `GIVEN country content WHEN error then loading THEN query and retry callback are retained`() {
        // Arrange
        val previous = CountryListUM.Content(search, persistentListOf())
        val items = persistentListOf(CountryItemState.Loading("loading"))

        // Act
        val error = UpdateCountryItemsErrorTransformer(retry).transform(previous)
        val loading = UpdateCountryItemsLoadingTransformer(items).transform(error)

        // Assert
        assertThat(error).isEqualTo(CountryListUM.Error(search, retry))
        assertThat(loading).isEqualTo(CountryListUM.Loading(search, items))
    }

    @Test
    fun `GIVEN currency content WHEN error then loading THEN query and retry callback are retained`() {
        // Arrange
        val previous = CurrenciesListUM.Content(search, persistentListOf())
        val sections = persistentListOf(
            CurrenciesSection(TextReference.EMPTY, persistentListOf(CurrencyItemState.Loading("loading"))),
        )

        // Act
        val error = UpdateCurrencyItemsErrorTransformer(retry).transform(previous)
        val loading = UpdateCurrencyItemsLoadingTransformer(sections).transform(error)

        // Assert
        assertThat(error).isEqualTo(CurrenciesListUM.Error(search, retry))
        assertThat(loading).isEqualTo(CurrenciesListUM.Loading(search, sections))
    }

    @Test
    fun `GIVEN search state WHEN callbacks bound THEN all other fields are preserved`() {
        // Arrange
        val query: (String) -> Unit = mockk()
        val active: (Boolean) -> Unit = mockk()

        // Act
        val state = UpdateSearchBarCallbacksTransformer(query, active).transform(search)

        // Assert
        assertThat(state).isEqualTo(search.copy(onQueryChange = query, onActiveChange = active))
    }
}