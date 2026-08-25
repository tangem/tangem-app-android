package com.tangem.features.feed.earn.model.filters.state

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.earn.model.EarnFilter
import com.tangem.domain.earn.model.EarnFilterNetwork
import com.tangem.domain.earn.model.EarnFilterType
import com.tangem.domain.models.earn.EarnNetworks
import com.tangem.features.feed.earn.createEarnFilter
import com.tangem.features.feed.earn.createEarnHttpError
import com.tangem.features.feed.earn.createEarnNetwork
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * The chips are built with the very lambdas the factory was constructed with, so comparing whole chips
 * also proves the network chip opens the network sheet and clears the network filter — and not the type
 * one.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnFilterChipsFactoryTest {

    private val onNetworkClick: () -> Unit = {}
    private val onTypeClick: () -> Unit = {}
    private val onNetworkClear: () -> Unit = {}
    private val onTypeClear: () -> Unit = {}

    private val factory = EarnFilterChipsFactory(
        onNetworkClick = onNetworkClick,
        onTypeClick = onTypeClick,
        onNetworkClear = onNetworkClear,
        onTypeClear = onTypeClear,
    )

    @ParameterizedTest
    @ProvideTestModels
    fun create(model: ChipsModel) {
        // Act
        val actual = factory.create(filter = model.filter, networks = model.networks)

        // Assert
        assertThat(actual).containsExactlyElementsIn(model.expected).inOrder()
    }

    private fun provideTestModels() = listOf(
        ChipsModel(
            name = "GIVEN the applied filter has not arrived WHEN chips built THEN both chips shimmer",
            filter = null,
            networks = loadedNetworks,
            expected = EarnFilterChipsFactory.LOADING,
        ),
        ChipsModel(
            name = "GIVEN the networks have not arrived WHEN chips built THEN both chips shimmer",
            filter = createEarnFilter(),
            networks = null,
            expected = EarnFilterChipsFactory.LOADING,
        ),
        ChipsModel(
            name = "GIVEN defaults WHEN chips built THEN neither chip offers a clear cross",
            filter = createEarnFilter(),
            networks = loadedNetworks,
            expected = listOf(inactiveNetworkChip, inactiveTypeChip),
        ),
        ChipsModel(
            name = "GIVEN the networks failed to load WHEN chips built THEN the chips are still built",
            filter = createEarnFilter(),
            networks = createEarnHttpError().left(),
            expected = listOf(inactiveNetworkChip, inactiveTypeChip),
        ),
        ChipsModel(
            name = "GIVEN my networks WHEN chips built THEN the network chip can be cleared",
            filter = createEarnFilter(network = EarnFilterNetwork.MyNetworks(isSelected = true)),
            networks = loadedNetworks,
            expected = listOf(
                activeNetworkChip(resourceReference(R.string.earn_filter_my_networks)),
                inactiveTypeChip,
            ),
        ),
        ChipsModel(
            name = "GIVEN a specific network WHEN chips built THEN the chip shows that network's full name",
            filter = createEarnFilter(
                network = EarnFilterNetwork.Specific(
                    isSelected = true,
                    id = "solana",
                    symbol = "SOL",
                    fullName = "Solana",
                ),
            ),
            networks = loadedNetworks,
            expected = listOf(activeNetworkChip(stringReference("Solana")), inactiveTypeChip),
        ),
        ChipsModel(
            name = "GIVEN the staking type WHEN chips built THEN the type chip shows its description",
            filter = createEarnFilter(type = EarnFilterType.STAKING),
            networks = loadedNetworks,
            expected = listOf(
                inactiveNetworkChip,
                activeTypeChip(resourceReference(R.string.earn_filter_staking_type_text)),
            ),
        ),
        ChipsModel(
            name = "GIVEN the yield type WHEN chips built THEN the type chip shows its description",
            filter = createEarnFilter(type = EarnFilterType.YIELD),
            networks = loadedNetworks,
            expected = listOf(inactiveNetworkChip, activeTypeChip(resourceReference(R.string.earn_filter_yield_type_text))),
        ),
    )

    private val inactiveNetworkChip
        get() = TangemFilterItemUM.Inactive(
            id = "network",
            label = resourceReference(R.string.earn_filter_all_networks),
            onClick = onNetworkClick,
        )

    private val inactiveTypeChip
        get() = TangemFilterItemUM.Inactive(
            id = "type",
            label = resourceReference(R.string.earn_filter_all_types),
            onClick = onTypeClick,
        )

    private fun activeNetworkChip(value: TextReference) = TangemFilterItemUM.Active(
        id = "network",
        value = value,
        onClick = onNetworkClick,
        onClearClick = onNetworkClear,
    )

    private fun activeTypeChip(value: TextReference) = TangemFilterItemUM.Active(
        id = "type",
        value = value,
        onClick = onTypeClick,
        onClearClick = onTypeClear,
    )

    internal data class ChipsModel(
        val name: String,
        val filter: EarnFilter?,
        val networks: EarnNetworks?,
        val expected: List<TangemFilterItemUM>,
    ) {
        override fun toString(): String = name
    }

    private companion object {

        val loadedNetworks: EarnNetworks = listOf(createEarnNetwork(isAdded = true)).right()
    }
}