package com.tangem.features.collectibles.impl.packdetails.model.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.collectibles.CollectibleGachaMachineNftsBatchListState
import com.tangem.features.collectibles.impl.createNft
import com.tangem.features.collectibles.impl.createNftsBatchListState
import com.tangem.features.collectibles.impl.machinedetails.model.transformer.CollectiblesPrizePoolTransformer
import com.tangem.features.collectibles.impl.machinedetails.ui.state.CollectiblesMachineDetailsUM
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.ProvideTestModels
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CollectiblesPrizePoolTransformerTest {

    private val content = CollectiblesMachineDetailsUM.Content(
        onBackClick = {},
        title = stringReference("Elite pack"),
        imageUrl = null,
        tiers = persistentListOf(),
        details = persistentListOf(),
        prizePool = CollectiblesMachineDetailsUM.PrizePoolUM.Loading,
        footer = CollectiblesMachineDetailsUM.FooterUM(
            isTurboEnabled = false,
            onTurboCheckedChange = {},
            onTurboInfoClick = {},
            expectedValue = stringReference("$53.95"),
            buyButtonText = stringReference("Open for 50 USDC"),
            onBuyClick = {},
        ),
    )

    @ParameterizedTest
    @ProvideTestModels
    fun transform(model: TestModel) {
        // Arrange
        val transformer = CollectiblesPrizePoolTransformer(
            batchListState = model.batchListState,
            onReloadClick = {},
        )

        // Act
        val actual = transformer.transform(content)

        // Assert
        assertThat((actual as CollectiblesMachineDetailsUM.Content).prizePool).isInstanceOf(model.expected)
    }

    @Test
    fun `GIVEN two pages WHEN transform THEN every collectible is laid out two per row`() {
        // Arrange
        val batchListState = createNftsBatchListState(
            batches = listOf(
                listOf(createNft(id = "a"), createNft(id = "b")),
                listOf(createNft(id = "c")),
            ),
        )

        // Act
        val actual = CollectiblesPrizePoolTransformer(batchListState, onReloadClick = {}).transform(content)

        // Assert
        val pool = (actual as CollectiblesMachineDetailsUM.Content).prizePool
        assertThat(pool).isInstanceOf(CollectiblesMachineDetailsUM.PrizePoolUM.Content::class.java)
        val rows = (pool as CollectiblesMachineDetailsUM.PrizePoolUM.Content).rows
        assertThat(rows.map { row -> row.prizes.map { it.id } }).containsExactly(listOf("a", "b"), listOf("c"))
            .inOrder()
    }

    @Test
    fun `GIVEN a collectible served by two pages WHEN transform THEN the copies are dropped`() {
        // Arrange
        val batchListState = createNftsBatchListState(
            batches = listOf(listOf(createNft(id = "a")), listOf(createNft(id = "a"), createNft(id = "b"))),
        )

        // Act
        val actual = CollectiblesPrizePoolTransformer(batchListState, onReloadClick = {}).transform(content)

        // Assert
        val pool = (actual as CollectiblesMachineDetailsUM.Content).prizePool
        val ids = (pool as CollectiblesMachineDetailsUM.PrizePoolUM.Content).rows.flatMap { it.prizes }.map { it.id }
        assertThat(ids).containsExactly("a", "b").inOrder()
    }

    @Test
    fun `GIVEN the machine has not arrived WHEN transform THEN the state is untouched`() {
        // Arrange
        val loading = CollectiblesMachineDetailsUM.Loading(onBackClick = {})

        // Act
        val actual = CollectiblesPrizePoolTransformer(createNftsBatchListState(), onReloadClick = {})
            .transform(loading)

        // Assert
        assertThat(actual).isSameInstanceAs(loading)
    }

    internal data class TestModel(
        val batchListState: CollectibleGachaMachineNftsBatchListState,
        val expected: Class<out CollectiblesMachineDetailsUM.PrizePoolUM>,
    )

    private fun provideTestModels() = listOf(
        TestModel(
            batchListState = createNftsBatchListState(status = PaginationStatus.None),
            expected = CollectiblesMachineDetailsUM.PrizePoolUM.Loading::class.java,
        ),
        TestModel(
            batchListState = createNftsBatchListState(status = PaginationStatus.InitialLoading),
            expected = CollectiblesMachineDetailsUM.PrizePoolUM.Loading::class.java,
        ),
        TestModel(
            batchListState = createNftsBatchListState(
                status = PaginationStatus.InitialLoadingError(IllegalStateException("boom")),
            ),
            expected = CollectiblesMachineDetailsUM.PrizePoolUM.Error::class.java,
        ),
        TestModel(
            batchListState = createNftsBatchListState(status = PaginationStatus.NextBatchLoading),
            expected = CollectiblesMachineDetailsUM.PrizePoolUM.Content::class.java,
        ),
        TestModel(
            batchListState = createNftsBatchListState(
                status = PaginationStatus.Paginating(
                    BatchFetchResult.Success(
                        data = emptyList(),
                        empty = true,
                        last = true
                    )
                ),
            ),
            expected = CollectiblesMachineDetailsUM.PrizePoolUM.Content::class.java,
        ),
        TestModel(
            batchListState = createNftsBatchListState(status = PaginationStatus.EndOfPagination),
            expected = CollectiblesMachineDetailsUM.PrizePoolUM.Content::class.java,
        ),
        TestModel(
            batchListState = createNftsBatchListState(batches = emptyList()),
            expected = CollectiblesMachineDetailsUM.PrizePoolUM.Empty::class.java,
        ),
    )
}