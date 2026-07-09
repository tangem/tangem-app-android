package com.tangem.features.addressbook.selectnetworks.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.addressbook.selectnetworks.ui.state.SelectNetworksUM
import com.tangem.features.addressbook.selectnetworks.ui.state.SelectNetworksUM.SelectAllButtonUM
import com.tangem.test.core.ProvideTestModels
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UpdateNetworksContentTransformerTest {

    private val ethereum = Blockchain.Ethereum
    private val bitcoin = Blockchain.Bitcoin
    private val polygon = Blockchain.Polygon
    private val allBlockchains = listOf(ethereum, bitcoin, polygon)

    @Test
    fun `GIVEN blank query WHEN transform THEN all matched networks shown in order`() {
        // Arrange
        val transformer = createTransformer(matchedBlockchains = allBlockchains, query = "")

        // Act
        val result = transformer.transform(createPrevState())

        // Assert
        assertThat(result.networks.map { it.id })
            .containsExactly(ethereum.toNetworkId(), bitcoin.toNetworkId(), polygon.toNetworkId())
            .inOrder()
    }

    @Test
    fun `GIVEN network item WHEN transform THEN mapped fields match blockchain`() {
        // Arrange
        val transformer = createTransformer(matchedBlockchains = listOf(ethereum), query = "")

        // Act
        val item = transformer.transform(createPrevState()).networks.single()

        // Assert
        assertThat(item.id).isEqualTo(ethereum.toNetworkId())
        assertThat(item.name).isEqualTo(ethereum.fullName)
        assertThat(item.symbol).isEqualTo(ethereum.currency)
    }

    @Test
    fun `GIVEN non-blank query WHEN transform THEN only matching networks shown`() {
        // Arrange
        val transformer = createTransformer(matchedBlockchains = allBlockchains, query = "bitcoin")

        // Act
        val result = transformer.transform(createPrevState())

        // Assert
        assertThat(result.networks.map { it.id }).containsExactly(bitcoin.toNetworkId())
    }

    @Test
    fun `GIVEN query matching nothing WHEN transform THEN no networks shown`() {
        // Arrange
        val transformer = createTransformer(matchedBlockchains = allBlockchains, query = "no-such-network")

        // Act
        val result = transformer.transform(createPrevState())

        // Assert
        assertThat(result.networks).isEmpty()
    }

    @Test
    fun `GIVEN selected network ids WHEN transform THEN isSelected reflects selection`() {
        // Arrange
        val transformer = createTransformer(
            matchedBlockchains = allBlockchains,
            selectedNetworkIds = setOf(ethereum.toNetworkId(), polygon.toNetworkId()),
        )

        // Act
        val selectionById = transformer.transform(createPrevState()).networks.associate { it.id to it.isSelected }

        // Assert
        assertThat(selectionById).containsExactly(
            ethereum.toNetworkId(), true,
            bitcoin.toNetworkId(), false,
            polygon.toNetworkId(), true,
        )
    }

    @Test
    fun `GIVEN item WHEN onCheckedChange invoked THEN onToggle called with network id`() {
        // Arrange
        val toggled = mutableListOf<String>()
        val transformer = createTransformer(
            matchedBlockchains = listOf(ethereum),
            onToggle = toggled::add,
        )
        val item = transformer.transform(createPrevState()).networks.single()

        // Act
        item.onCheckedChange()

        // Assert
        assertThat(toggled).containsExactly(ethereum.toNetworkId())
    }

    @Test
    fun `GIVEN non-empty selection WHEN transform THEN done button enabled`() {
        // Arrange
        val transformer = createTransformer(
            matchedBlockchains = allBlockchains,
            selectedNetworkIds = setOf(ethereum.toNetworkId()),
        )

        // Act
        val result = transformer.transform(createPrevState(isDoneEnabled = false))

        // Assert
        assertThat(result.doneButton.isEnabled).isTrue()
    }

    @Test
    fun `GIVEN empty selection WHEN transform THEN done button disabled`() {
        // Arrange
        val transformer = createTransformer(
            matchedBlockchains = allBlockchains,
            selectedNetworkIds = emptySet(),
        )

        // Act
        val result = transformer.transform(createPrevState(isDoneEnabled = true))

        // Assert
        assertThat(result.doneButton.isEnabled).isFalse()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `WHEN transform THEN select-all button state matches selection and search`(model: SelectAllButtonModel) {
        // Arrange
        val transformer = createTransformer(
            matchedBlockchains = model.matchedBlockchains,
            selectedNetworkIds = model.selectedNetworkIds,
            isSearchActive = model.isSearchActive,
        )

        // Act
        val result = transformer.transform(createPrevState())

        // Assert
        assertThat(result.selectAllButton).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        SelectAllButtonModel(
            description = "search active hides the button even when all selected",
            matchedBlockchains = allBlockchains,
            selectedNetworkIds = allBlockchains.map { it.toNetworkId() }.toSet(),
            isSearchActive = true,
            expected = SelectAllButtonUM.Empty,
        ),
        SelectAllButtonModel(
            description = "search active hides the button when none selected",
            matchedBlockchains = allBlockchains,
            selectedNetworkIds = emptySet(),
            isSearchActive = true,
            expected = SelectAllButtonUM.Empty,
        ),
        SelectAllButtonModel(
            description = "all selected shows Clear All",
            matchedBlockchains = allBlockchains,
            selectedNetworkIds = allBlockchains.map { it.toNetworkId() }.toSet(),
            isSearchActive = false,
            expected = SelectAllButtonUM.ClearAll,
        ),
        SelectAllButtonModel(
            description = "partial selection shows Select All",
            matchedBlockchains = allBlockchains,
            selectedNetworkIds = setOf(ethereum.toNetworkId()),
            isSearchActive = false,
            expected = SelectAllButtonUM.SelectAll,
        ),
        SelectAllButtonModel(
            description = "no selection shows Select All",
            matchedBlockchains = allBlockchains,
            selectedNetworkIds = emptySet(),
            isSearchActive = false,
            expected = SelectAllButtonUM.SelectAll,
        ),
        SelectAllButtonModel(
            description = "no matched networks shows Select All",
            matchedBlockchains = emptyList(),
            selectedNetworkIds = emptySet(),
            isSearchActive = false,
            expected = SelectAllButtonUM.SelectAll,
        ),
    )

    private fun createTransformer(
        matchedBlockchains: List<Blockchain>,
        query: String = "",
        selectedNetworkIds: Set<String> = emptySet(),
        isSearchActive: Boolean = false,
        onToggle: (String) -> Unit = {},
    ) = UpdateNetworksContentTransformer(
        matchedBlockchains = matchedBlockchains,
        query = query,
        selectedNetworkIds = selectedNetworkIds,
        isSearchActive = isSearchActive,
        onToggle = onToggle,
    )

    private fun createPrevState(isDoneEnabled: Boolean = false) = SelectNetworksUM(
        searchBar = TangemSearch.State(
            placeholderText = resourceReference(R.string.common_search),
            query = "",
            onQueryChange = {},
            isActive = false,
            onActiveChange = {},
        ),
        networks = persistentListOf(),
        selectAllButton = SelectAllButtonUM.Empty,
        doneButton = TangemButtonUM(
            text = TextReference.Res(R.string.common_done),
            type = TangemButtonType.Primary,
            isEnabled = isDoneEnabled,
            onClick = {},
        ),
        onBackClick = {},
        onSelectAllClick = {},
    )

    internal data class SelectAllButtonModel(
        val description: String,
        val matchedBlockchains: List<Blockchain>,
        val selectedNetworkIds: Set<String>,
        val isSearchActive: Boolean,
        val expected: SelectAllButtonUM,
    ) {
        override fun toString(): String = description
    }
}