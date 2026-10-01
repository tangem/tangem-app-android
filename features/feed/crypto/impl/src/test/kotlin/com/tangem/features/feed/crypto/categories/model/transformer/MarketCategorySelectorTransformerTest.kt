package com.tangem.features.feed.crypto.categories.model.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.MarketCategory
import com.tangem.features.feed.crypto.categories.MarketCategorySelection
import com.tangem.features.feed.crypto.categories.createCategory
import com.tangem.features.feed.crypto.categories.createSearchState
import com.tangem.features.feed.crypto.categories.createSelectorState
import com.tangem.features.feed.crypto.categories.selectionOf
import com.tangem.features.feed.crypto.categories.ui.state.FeaturedCategoryUM
import com.tangem.features.feed.crypto.categories.ui.state.MarketCategoryItemUM
import com.tangem.features.feed.crypto.categories.ui.state.MarketCategorySelectorFooterUM
import com.tangem.features.feed.crypto.categories.ui.state.MarketCategorySelectorUM
import com.tangem.features.feed.crypto.impl.R
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * The transformer rebuilds the screen body from the values it is handed, so the assertions here are
 * about what reaches the state, not about what survives from the previous one — with one exception:
 * the search field and the footer live above the body and must come through untouched.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MarketCategorySelectorTransformerTest {

    private val onLoadMore: () -> Unit = {}

    private val ai = createCategory(id = "ai", name = "AI")
    private val meme = createCategory(id = "meme", name = "Meme", isFeatured = true)

    @ParameterizedTest
    @ProvideTestModels
    fun `previous body is ignored`(model: PrevStateModel) {
        // Act
        val actual = transform(prevState = createSelectorState(content = model.prevContent))

        // Assert
        assertThat(actual).isInstanceOf(MarketCategorySelectorUM.ContentUM.Content::class.java)
    }

    @Test
    fun `GIVEN featured categories WHEN transformed THEN the rail is built from them`() {
        // Act
        val actual = transform(featured = listOf(meme))

        // Assert
        assertThat(actual.featured?.items?.map(FeaturedCategoryUM::id)).containsExactly("meme")
    }

    @Test
    fun `GIVEN no featured categories WHEN transformed THEN the rail is dropped and the list survives`() {
        // Arrange — a failed or empty featured request must not take the screen down with it
        // Act
        val actual = transform(featured = emptyList(), categories = listOf(ai))

        // Assert
        assertThat(actual.featured).isNull()
        assertThat(actual.categories.items.map(MarketCategoryItemUM::id)).containsExactly("ai")
    }

    @Test
    fun `GIVEN a search WHEN transformed THEN the rail is dropped and only matches remain`() {
        // Act — an unfiltered shortcut strip and "no filter" are not search results
        val actual = transform(featured = listOf(meme), categories = listOf(ai), isSearching = true)

        // Assert
        assertThat(actual.featured).isNull()
        assertThat(actual.categories.items.map(MarketCategoryItemUM::id)).containsExactly("ai")
    }

    @Test
    fun `GIVEN categories WHEN transformed THEN the list is titled and carries only the categories`() {
        // Act
        val actual = transform(categories = listOf(ai))

        // Assert
        assertThat(actual.categories.title).isEqualTo(TextReference.Res(R.string.market_categories))
        assertThat(actual.categories.items.map(MarketCategoryItemUM::id)).containsExactly("ai")
    }

    @Test
    fun `GIVEN a selection WHEN transformed THEN both sections agree on what is checked`() {
        // Act
        val actual = transform(featured = listOf(meme), categories = listOf(ai, meme), selection = selectionOf(meme))

        // Assert
        assertThat(actual.featured?.items?.single(FeaturedCategoryUM::isSelected)?.id).isEqualTo("meme")
        assertThat(actual.categories.items.single(MarketCategoryItemUM::isSelected).id).isEqualTo("meme")
    }

    @Test
    fun `GIVEN the screen flags WHEN transformed THEN they reach the state untouched`() {
        // Act
        val actual = transformState(isEdited = true, isLoadingMore = true)

        // Assert
        assertThat(actual.isEdited).isTrue()
        assertThat(actual.body().categories.isLoadingMore).isTrue()
    }

    @Test
    fun `GIVEN the load more callback WHEN transformed THEN the state hands back the very same lambda`() {
        // Act
        val actual = transform()

        // Assert
        assertThat(actual.categories.loadMore).isEqualTo(onLoadMore)
    }

    @Test
    fun `GIVEN a typed query and a footer WHEN transformed THEN neither is rebuilt`() {
        // Arrange — a reload must not unmount the field mid-typing or strip a drafted selection
        val footer = MarketCategorySelectorFooterUM(onReset = {}, onApply = {})
        val prevState = createSelectorState(search = createSearchState(query = "mem", isActive = true), footer = footer)

        // Act
        val actual = transformState(prevState = prevState)

        // Assert
        assertThat(actual.search).isEqualTo(prevState.search)
        assertThat(actual.footer).isSameInstanceAs(footer)
    }

    @Suppress("LongParameterList")
    private fun transformState(
        featured: List<MarketCategory> = emptyList(),
        categories: List<MarketCategory> = emptyList(),
        selection: MarketCategorySelection = MarketCategorySelection(),
        appCurrency: AppCurrency = AppCurrency.Default,
        isSearching: Boolean = false,
        isEdited: Boolean = false,
        isLoadingMore: Boolean = false,
        prevState: MarketCategorySelectorUM = createSelectorState(),
    ): MarketCategorySelectorUM = MarketCategorySelectorTransformer(
        featured = featured,
        categories = categories,
        selection = selection,
        appCurrency = appCurrency,
        isSearching = isSearching,
        isEdited = isEdited,
        isLoadingMore = isLoadingMore,
        onLoadMore = onLoadMore,
        onSelectionChange = {},
    ).transform(prevState)

    @Suppress("LongParameterList")
    private fun transform(
        featured: List<MarketCategory> = emptyList(),
        categories: List<MarketCategory> = emptyList(),
        selection: MarketCategorySelection = MarketCategorySelection(),
        appCurrency: AppCurrency = AppCurrency.Default,
        isSearching: Boolean = false,
        isEdited: Boolean = false,
        isLoadingMore: Boolean = false,
        prevState: MarketCategorySelectorUM = createSelectorState(),
    ): MarketCategorySelectorUM.ContentUM.Content = transformState(
        featured = featured,
        categories = categories,
        selection = selection,
        appCurrency = appCurrency,
        isSearching = isSearching,
        isEdited = isEdited,
        isLoadingMore = isLoadingMore,
        prevState = prevState,
    ).body()

    private fun MarketCategorySelectorUM.body() = content as MarketCategorySelectorUM.ContentUM.Content

    private fun provideTestModels() = listOf(
        PrevStateModel(
            name = "GIVEN a loading screen WHEN transformed THEN content replaces it",
            prevContent = MarketCategorySelectorUM.ContentUM.Loading,
        ),
        PrevStateModel(
            name = "GIVEN a failed screen WHEN transformed THEN content replaces it",
            prevContent = MarketCategorySelectorUM.ContentUM.Error(onRetry = {}),
        ),
        PrevStateModel(
            name = "GIVEN an empty search result WHEN transformed THEN content replaces it",
            prevContent = MarketCategorySelectorUM.ContentUM.Empty,
        ),
        PrevStateModel(
            name = "GIVEN an already rendered screen WHEN transformed THEN it is rebuilt",
            prevContent = MarketCategorySelectorTransformer(
                featured = emptyList(),
                categories = emptyList(),
                selection = MarketCategorySelection(),
                appCurrency = AppCurrency.Default,
                isSearching = false,
                isEdited = false,
                isLoadingMore = false,
                onLoadMore = {},
                onSelectionChange = {},
            ).transform(createSelectorState()).content,
        ),
    )

    internal data class PrevStateModel(val name: String, val prevContent: MarketCategorySelectorUM.ContentUM) {
        override fun toString(): String = name
    }
}