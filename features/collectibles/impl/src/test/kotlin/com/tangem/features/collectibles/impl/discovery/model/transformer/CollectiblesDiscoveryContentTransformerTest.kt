package com.tangem.features.collectibles.impl.discovery.model.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.collectibles.CollectibleGachaMachinesBatchListState
import com.tangem.domain.collectibles.models.CollectibleGachaMachinesBatch
import com.tangem.features.collectibles.impl.createBatchListState
import com.tangem.features.collectibles.impl.createMachine
import com.tangem.features.collectibles.impl.discovery.model.converter.CollectiblesDiscoveryPackConverter
import com.tangem.features.collectibles.impl.discovery.ui.state.CollectiblesDiscoveryUM
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.ProvideTestModels
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CollectiblesDiscoveryContentTransformerTest {

    private var reloadClickCount = 0

    private val packConverter = CollectiblesDiscoveryPackConverter(onPackClick = {})

    // The class is PER_CLASS, so the recorded clicks would leak between tests.
    @BeforeEach
    fun resetRecordedClicks() {
        reloadClickCount = 0
    }

    @ParameterizedTest
    @ProvideTestModels
    fun transform(model: TransformModel) {
        // Arrange
        val transformer = createTransformer(batchListState = createBatchListState(status = model.status))

        // Act
        val actual = transformer.transform(prevState = createPrevState(content = model.prevContent))

        // Assert
        assertThat(actual.content.toExpectation()).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        TransformModel(
            description = "GIVEN nothing on screen WHEN status is None THEN content is loading",
            status = PaginationStatus.None,
            prevContent = CollectiblesDiscoveryUM.ContentUM.Loading,
            expected = Expectation.Loading,
        ),
        TransformModel(
            description = "GIVEN nothing on screen WHEN first page is loading THEN content is loading",
            status = PaginationStatus.InitialLoading,
            prevContent = CollectiblesDiscoveryUM.ContentUM.Loading,
            expected = Expectation.Loading,
        ),
        TransformModel(
            description = "GIVEN a failed screen WHEN first page reloads THEN content is loading",
            status = PaginationStatus.InitialLoading,
            prevContent = CollectiblesDiscoveryUM.ContentUM.Error(
                message = stringReference("failed"),
                onReloadClick = {},
            ),
            expected = Expectation.Loading,
        ),
        TransformModel(
            description = "GIVEN an empty screen WHEN first page reloads THEN content is loading",
            status = PaginationStatus.InitialLoading,
            prevContent = CollectiblesDiscoveryUM.ContentUM.Empty(message = stringReference("empty")),
            expected = Expectation.Loading,
        ),
        TransformModel(
            description = "GIVEN packs on screen WHEN status is None THEN they stay and are marked reloading",
            status = PaginationStatus.None,
            prevContent = previousContent(),
            expected = Expectation.Content(isLoadingNextPage = false, isReloading = true),
        ),
        TransformModel(
            description = "GIVEN packs on screen WHEN first page reloads THEN they stay and are marked reloading",
            status = PaginationStatus.InitialLoading,
            prevContent = previousContent(),
            expected = Expectation.Content(isLoadingNextPage = false, isReloading = true),
        ),
        TransformModel(
            description = "GIVEN the first page failed WHEN transform THEN content is an error",
            status = PaginationStatus.InitialLoadingError(throwable = IllegalStateException("boom")),
            prevContent = CollectiblesDiscoveryUM.ContentUM.Loading,
            expected = Expectation.Error,
        ),
        TransformModel(
            description = "GIVEN a next page on its way WHEN transform THEN the footer loader is shown",
            status = PaginationStatus.NextBatchLoading,
            prevContent = CollectiblesDiscoveryUM.ContentUM.Loading,
            expected = Expectation.Content(isLoadingNextPage = true, isReloading = false),
        ),
        TransformModel(
            description = "GIVEN a page settled WHEN transform THEN no loader is shown",
            status = PaginationStatus.Paginating(
                lastResult = BatchFetchResult.Success(data = emptyBatch(), empty = false, last = false),
            ),
            prevContent = CollectiblesDiscoveryUM.ContentUM.Loading,
            expected = Expectation.Content(isLoadingNextPage = false, isReloading = false),
        ),
        TransformModel(
            description = "GIVEN the catalogue ended WHEN transform THEN no loader is shown",
            status = PaginationStatus.EndOfPagination,
            prevContent = CollectiblesDiscoveryUM.ContentUM.Loading,
            expected = Expectation.Content(isLoadingNextPage = false, isReloading = false),
        ),
    )

    @Test
    fun `GIVEN packs on screen WHEN the catalogue reloads THEN the previous rows are kept`() {
        // Arrange
        val prevContent = previousContent()
        val transformer = createTransformer(
            batchListState = createBatchListState(
                batches = listOf(listOf(createMachine(id = "incoming"))),
                status = PaginationStatus.InitialLoading,
            ),
        )

        // Act
        val actual = transformer.transform(prevState = createPrevState(content = prevContent))

        // Assert
        val content = actual.content as CollectiblesDiscoveryUM.ContentUM.Content
        assertThat(content.rows).isEqualTo(prevContent.rows)
    }

    @Test
    fun `GIVEN the first page failed WHEN reload is clicked THEN the retry is delegated`() {
        // Arrange
        val transformer = createTransformer(
            batchListState = createBatchListState(
                status = PaginationStatus.InitialLoadingError(throwable = IllegalStateException("boom")),
            ),
        )

        // Act
        val error = transformer.transform(prevState = createPrevState()).content
            as CollectiblesDiscoveryUM.ContentUM.Error
        error.onReloadClick()

        // Assert
        assertThat(reloadClickCount).isEqualTo(1)
    }

    @Test
    fun `GIVEN machines across two pages WHEN transform THEN all of them are rendered in order`() {
        // Arrange
        val transformer = createTransformer(
            batchListState = createBatchListState(
                batches = listOf(
                    listOf(createMachine(id = "a"), createMachine(id = "b")),
                    listOf(createMachine(id = "c"), createMachine(id = "d")),
                ),
            ),
        )

        // Act
        val actual = transformer.transform(prevState = createPrevState())

        // Assert
        assertThat(actual.content.packIds()).containsExactly("a", "b", "c", "d").inOrder()
    }

    @Test
    fun `GIVEN a machine served by two pages WHEN transform THEN it is rendered once`() {
        // Arrange
        val transformer = createTransformer(
            batchListState = createBatchListState(
                batches = listOf(
                    listOf(createMachine(id = "a"), createMachine(id = "b")),
                    listOf(createMachine(id = "b"), createMachine(id = "c")),
                ),
            ),
        )

        // Act
        val actual = transformer.transform(prevState = createPrevState())

        // Assert
        assertThat(actual.content.packIds()).containsExactly("a", "b", "c").inOrder()
    }

    @Test
    fun `GIVEN no machines WHEN the catalogue ended THEN content is empty`() {
        // Arrange
        val transformer = createTransformer(batchListState = createBatchListState(batches = listOf(emptyList())))

        // Act
        val actual = transformer.transform(prevState = createPrevState())

        // Assert
        assertThat(actual.content).isInstanceOf(CollectiblesDiscoveryUM.ContentUM.Empty::class.java)
    }

    @Test
    fun `GIVEN no machines WHEN a next page is loading THEN content is empty rather than a loader`() {
        // Arrange
        val transformer = createTransformer(
            batchListState = createBatchListState(
                batches = listOf(emptyList()),
                status = PaginationStatus.NextBatchLoading,
            ),
        )

        // Act
        val actual = transformer.transform(prevState = createPrevState())

        // Assert
        assertThat(actual.content).isInstanceOf(CollectiblesDiscoveryUM.ContentUM.Empty::class.java)
    }

    @Test
    fun `GIVEN an odd number of machines WHEN transform THEN the last row holds a single pack`() {
        // Arrange
        val transformer = createTransformer(
            batchListState = createBatchListState(
                batches = listOf(
                    listOf(createMachine(id = "a"), createMachine(id = "b"), createMachine(id = "c")),
                ),
            ),
        )

        // Act
        val actual = transformer.transform(prevState = createPrevState())

        // Assert
        val content = actual.content as CollectiblesDiscoveryUM.ContentUM.Content
        assertThat(content.rows.map { row -> row.packs.map { it.id } })
            .containsExactly(listOf("a", "b"), listOf("c"))
            .inOrder()
    }

    @Test
    fun `GIVEN rows built WHEN transform THEN each row is keyed by its first pack`() {
        // Arrange
        val transformer = createTransformer(
            batchListState = createBatchListState(
                batches = listOf(
                    listOf(createMachine(id = "a"), createMachine(id = "b"), createMachine(id = "c")),
                ),
            ),
        )

        // Act
        val actual = transformer.transform(prevState = createPrevState())

        // Assert
        val content = actual.content as CollectiblesDiscoveryUM.ContentUM.Content
        assertThat(content.rows.map { it.id }).containsExactly("a", "c").inOrder()
    }

    @Test
    fun `GIVEN tabs and a query WHEN transform THEN only the content area changes`() {
        // Arrange
        val prevState = createPrevState(
            tabs = CollectiblesDiscoveryUM.TabsUM.Content(
                tabs = persistentListOf(
                    TangemTabItemUM.Content(id = "all", label = stringReference("All cards"), onClick = {}),
                ),
            ),
            search = createSearchState(query = "pokemon", isActive = true),
        )
        val transformer = createTransformer(batchListState = createBatchListState())

        // Act
        val actual = transformer.transform(prevState = prevState)

        // Assert
        assertThat(actual.tabs).isEqualTo(prevState.tabs)
        assertThat(actual.search).isEqualTo(prevState.search)
    }

    private fun createTransformer(
        batchListState: CollectibleGachaMachinesBatchListState = createBatchListState(),
    ): CollectiblesDiscoveryContentTransformer = CollectiblesDiscoveryContentTransformer(
        batchListState = batchListState,
        packConverter = packConverter,
        onReloadClick = { reloadClickCount++ },
    )

    private fun createPrevState(
        tabs: CollectiblesDiscoveryUM.TabsUM = CollectiblesDiscoveryUM.TabsUM.Hidden,
        content: CollectiblesDiscoveryUM.ContentUM = CollectiblesDiscoveryUM.ContentUM.Loading,
        search: TangemSearch.State = createSearchState(),
    ): CollectiblesDiscoveryUM = CollectiblesDiscoveryUM(tabs = tabs, content = content, search = search)

    private fun createSearchState(
        query: String = "",
        isActive: Boolean = false,
    ): TangemSearch.State = TangemSearch.State(
        placeholderText = stringReference("Search"),
        query = query,
        onQueryChange = {},
        isActive = isActive,
        onActiveChange = {},
    )

    private fun previousContent(): CollectiblesDiscoveryUM.ContentUM.Content {
        val pack = CollectiblesDiscoveryUM.PackUM(
            id = "previous",
            imageUrl = "",
            title = stringReference("Previous"),
            price = stringReference("$1"),
            badge = null,
            onClick = {},
        )
        return CollectiblesDiscoveryUM.ContentUM.Content(
            rows = persistentListOf(CollectiblesDiscoveryUM.PackRowUM(id = pack.id, packs = persistentListOf(pack))),
            isLoadingNextPage = false,
            isReloading = false,
        )
    }

    private fun emptyBatch() =
        CollectibleGachaMachinesBatch(collectibleGachaMachines = emptyList(), requestCursor = null)

    private fun CollectiblesDiscoveryUM.ContentUM.packIds(): List<String> =
        (this as CollectiblesDiscoveryUM.ContentUM.Content).rows.flatMap { row -> row.packs.map { it.id } }

    private fun CollectiblesDiscoveryUM.ContentUM.toExpectation(): Expectation = when (this) {
        is CollectiblesDiscoveryUM.ContentUM.Loading -> Expectation.Loading
        is CollectiblesDiscoveryUM.ContentUM.Error -> Expectation.Error
        is CollectiblesDiscoveryUM.ContentUM.Empty -> Expectation.Empty
        is CollectiblesDiscoveryUM.ContentUM.Content -> Expectation.Content(
            isLoadingNextPage = isLoadingNextPage,
            isReloading = isReloading,
        )
    }

    /** [CollectiblesDiscoveryUM.PackUM] carries an `onClick` lambda, so the states cannot be compared whole. */
    internal sealed interface Expectation {

        data object Loading : Expectation

        data object Error : Expectation

        data object Empty : Expectation

        data class Content(val isLoadingNextPage: Boolean, val isReloading: Boolean) : Expectation
    }

    internal data class TransformModel(
        val description: String,
        val status: PaginationStatus<CollectibleGachaMachinesBatch>,
        val prevContent: CollectiblesDiscoveryUM.ContentUM,
        val expected: Expectation,
    ) {

        override fun toString(): String = description
    }
}