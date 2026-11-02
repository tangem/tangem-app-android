package com.tangem.features.onramp.entity

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampCurrencies
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.features.onramp.selectcountry.entity.CountryItemState
import com.tangem.features.onramp.selectcountry.entity.CountryListUM
import com.tangem.features.onramp.selectcountry.entity.transformer.UpdateCountryItemsTransformer
import com.tangem.features.onramp.selectcurrency.entity.CurrenciesListUM
import com.tangem.features.onramp.selectcurrency.entity.transformer.UpdateCurrencyItemsTransformer
import com.tangem.features.onramp.utils.ClearSearchBarTransformer
import com.tangem.features.onramp.utils.UpdateSearchBarActiveStateTransformer
import com.tangem.features.onramp.utils.UpdateSearchQueryTransformer
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

internal class OnrampTransformersTest {

    private val searchBar = SearchBarUM(
        placeholderText = TextReference.EMPTY,
        query = "old",
        onQueryChange = {},
        isActive = true,
        onActiveChange = {},
    )

    @Test
    fun `GIVEN matching countries WHEN transformed THEN available country is selected and unavailable country is marked`() {
        // Arrange
        val selected = country(id = "se", name = "Sweden", available = true)
        val unavailable = country(id = "no", name = "Norway", available = false)
        val state = CountryListUM.Loading(searchBar, persistentListOf())

        // Act
        val result = UpdateCountryItemsTransformer(
            maybeCountries = listOf(selected, unavailable).right(),
            defaultCountry = selected,
            query = "swe",
            onRetry = {},
            onCountryClick = {},
        ).transform(state)

        // Assert
        val content = result as CountryListUM.Content
        assertThat(content.items).hasSize(1)
        assertThat(content.items.single()).isInstanceOf(CountryItemState.WithContent.Content::class.java)
        assertThat((content.items.single() as CountryItemState.WithContent.Content).isSelected).isTrue()
    }

    @Test
    fun `GIVEN country error WHEN transformed THEN retry state is preserved`() {
        // Arrange
        val retry = mockk<() -> Unit>(relaxed = true)
        val state = CountryListUM.Loading(searchBar, persistentListOf())

        // Act
        val result = UpdateCountryItemsTransformer(
            maybeCountries = OnrampError.PairsNotFound.left(),
            defaultCountry = null,
            query = "",
            onRetry = retry,
            onCountryClick = {},
        ).transform(state)

        // Assert
        assertThat(result).isInstanceOf(CountryListUM.Error::class.java)
        (result as CountryListUM.Error).onRetry()
        io.mockk.verify { retry() }
    }

    @Test
    fun `GIVEN currencies WHEN transformed THEN popular and other sections are filtered`() {
        // Arrange
        val usd = currency("US Dollar", "USD")
        val eur = currency("Euro", "EUR")
        val state = CurrenciesListUM.Loading(searchBar, persistentListOf())

        // Act
        val result = UpdateCurrencyItemsTransformer(
            maybeCurrencies = OnrampCurrencies(populars = listOf(usd), others = listOf(eur)).right(),
            query = "usd",
            onRetry = {},
            onCurrencyClick = {},
        ).transform(state)

        // Assert
        val content = result as CurrenciesListUM.Content
        assertThat(content.sections).hasSize(1)
        assertThat(content.sections.single().items.single().onrampCurrency).isEqualTo(usd)
    }

    @Test
    fun `GIVEN search bar WHEN query is updated and cleared THEN only intended fields change`() {
        // Act
        val queried = UpdateSearchQueryTransformer("new").transform(searchBar)
        val cleared = ClearSearchBarTransformer(TextReference.EMPTY).transform(queried)
        val active = UpdateSearchBarActiveStateTransformer(false, TextReference.EMPTY).transform(queried)

        // Assert
        assertThat(queried.query).isEqualTo("new")
        assertThat(cleared.query).isEmpty()
        assertThat(cleared.isActive).isFalse()
        assertThat(active.isActive).isFalse()
    }

    private fun country(id: String, name: String, available: Boolean) = OnrampCountry(
        id = id,
        name = name,
        code = id,
        image = "",
        alpha3 = id,
        continent = "Europe",
        defaultCurrency = currency("Euro", "EUR"),
        isOnrampAvailable = available,
    )

    private fun currency(name: String, code: String) = OnrampCurrency(
        name = name,
        code = code,
        image = null,
        precision = 2,
        unit = code,
    )
}