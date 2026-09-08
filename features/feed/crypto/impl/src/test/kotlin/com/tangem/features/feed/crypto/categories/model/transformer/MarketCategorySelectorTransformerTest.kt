package com.tangem.features.feed.crypto.categories.model.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.MarketCategory
import com.tangem.features.feed.crypto.categories.ALL_ROW_ID
import com.tangem.features.feed.crypto.categories.MarketCategorySelection
import com.tangem.features.feed.crypto.categories.createCategory
import com.tangem.features.feed.crypto.categories.selectionOf
import com.tangem.features.feed.crypto.categories.ui.state.FeaturedCategoryUM
import com.tangem.features.feed.crypto.categories.ui.state.MarketCategoryItemUM
import com.tangem.features.feed.crypto.categories.ui.state.MarketCategorySelectorUM
import com.tangem.features.feed.crypto.impl.R
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * The transformer rebuilds the whole screen from the values it is handed, so the assertions here are
 * about what reaches the state, not about what survives from the previous one.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MarketCategorySelectorTransformerTest {

    private val onLoadMore: () -> Unit = {}
    private val onReset: () -> Unit = {}
    private val onApply: () -> Unit = {}

    private val ai = createCategory(id = "ai", name = "AI")
    private val meme = createCategory(id = "meme", name = "Meme", isFeatured = true)

    @ParameterizedTest
    @ProvideTestModels
    fun `previous state is ignored`(model: PrevStateModel) {
        // Act
        val actual = transform(prevState = model.prevState)

        // Assert
        assertThat(actual).isInstanceOf(MarketCategorySelectorUM.Content::class.java)
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
        assertThat(actual.categories.items.map(MarketCategoryItemUM::id)).containsExactly(ALL_ROW_ID, "ai").inOrder()
    }

    @Test
    fun `GIVEN categories WHEN transformed THEN the list is titled and carries the All row`() {
        // Act
        val actual = transform(categories = listOf(ai))

        // Assert
        assertThat(actual.categories.title).isEqualTo(TextReference.Res(R.string.market_categories))
        assertThat(actual.categories.items.first().id).isEqualTo(ALL_ROW_ID)
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
        val actual = transform(isEdited = true, isLoadingMore = true)

        // Assert
        assertThat(actual.isEdited).isTrue()
        assertThat(actual.categories.isLoadingMore).isTrue()
    }

    @Test
    fun `GIVEN the screen callbacks WHEN transformed THEN the state hands back the very same lambdas`() {
        // Act
        val actual = transform()

        // Assert
        assertThat(actual.categories.loadMore).isEqualTo(onLoadMore)
        assertThat(actual.footer.onReset).isEqualTo(onReset)
        assertThat(actual.footer.onApply).isEqualTo(onApply)
    }

    @Suppress("LongParameterList")
    private fun transform(
        featured: List<MarketCategory> = emptyList(),
        categories: List<MarketCategory> = emptyList(),
        selection: MarketCategorySelection = MarketCategorySelection(),
        appCurrency: AppCurrency = AppCurrency.Default,
        isEdited: Boolean = false,
        isLoadingMore: Boolean = false,
        prevState: MarketCategorySelectorUM = MarketCategorySelectorUM.Loading,
    ): MarketCategorySelectorUM.Content = MarketCategorySelectorTransformer(
        featured = featured,
        categories = categories,
        selection = selection,
        appCurrency = appCurrency,
        isEdited = isEdited,
        isLoadingMore = isLoadingMore,
        onLoadMore = onLoadMore,
        onReset = onReset,
        onApply = onApply,
        onSelectionChange = {},
    ).transform(prevState) as MarketCategorySelectorUM.Content

    private fun provideTestModels() = listOf(
        PrevStateModel(
            name = "GIVEN a loading screen WHEN transformed THEN content replaces it",
            prevState = MarketCategorySelectorUM.Loading,
        ),
        PrevStateModel(
            name = "GIVEN a failed screen WHEN transformed THEN content replaces it",
            prevState = MarketCategorySelectorUM.Error(onRetry = {}),
        ),
        PrevStateModel(
            name = "GIVEN an already rendered screen WHEN transformed THEN it is rebuilt",
            prevState = MarketCategorySelectorTransformer(
                featured = emptyList(),
                categories = emptyList(),
                selection = MarketCategorySelection(),
                appCurrency = AppCurrency.Default,
                isEdited = false,
                isLoadingMore = false,
                onLoadMore = {},
                onReset = {},
                onApply = {},
                onSelectionChange = {},
            ).transform(MarketCategorySelectorUM.Loading),
        ),
    )

    internal data class PrevStateModel(val name: String, val prevState: MarketCategorySelectorUM) {
        override fun toString(): String = name
    }
}