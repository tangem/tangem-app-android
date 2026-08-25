package com.tangem.features.feed.earn.model.filters

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.domain.earn.model.EarnFilterNetwork
import com.tangem.domain.models.earn.EarnNetwork
import com.tangem.features.feed.earn.components.EarnNetworkFilterComponent
import com.tangem.features.feed.earn.createEarnNetwork
import com.tangem.features.feed.earn.model.analytics.EarnAnalyticsEvent
import com.tangem.features.feed.earn.model.analytics.FilterNetworkAnalytic
import com.tangem.features.feed.earn.ui.state.EarnFilterByNetworkBottomSheetContentUM
import com.tangem.features.feed.earn.ui.state.EarnFilterNetworkUM
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * The sheet holds a draft that only reaches the tab on Apply, so most assertions read the draft through
 * the state: which tab is selected, which row is selected, and whether the Reset/Apply footer is there.
 * The footer is the model's own answer to "does the draft differ from what is applied", which makes it
 * the sharpest probe of the draft logic.
 */
internal class EarnNetworkFilterModelTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    private var appliedFilter: EarnFilterNetwork? = null

    private val ethereum = createEarnNetwork(
        networkId = "ethereum",
        fullName = "Ethereum",
        symbol = "ETH",
        isAdded = true,
    )
    private val solana = createEarnNetwork(networkId = "solana", fullName = "Solana", symbol = "SOL", isAdded = false)

    @BeforeEach
    fun reset() {
        clearMocks(analyticsEventHandler)
        appliedFilter = null
    }

    private fun createModel(
        selectedFilter: EarnFilterNetwork = EarnFilterNetwork.AllNetworks(isSelected = true),
        networks: List<EarnNetwork> = listOf(ethereum, solana),
    ) = EarnNetworkFilterModel(
        dispatchers = TestingCoroutineDispatcherProvider(),
        analyticsEventHandler = analyticsEventHandler,
        paramsContainer = MutableParamsContainer(
            EarnNetworkFilterComponent.Params(
                networks = networks,
                selectedFilter = selectedFilter,
                onFilterSelected = { appliedFilter = it },
                onDismiss = {},
            ),
        ),
    )

    @Nested
    inner class InitialState {

        @Test
        fun `GIVEN all networks applied WHEN the sheet opens THEN every network is listed under the all tab`() {
            // Act
            val state = createModel().state.value

            // Assert
            assertThat(state.selectedScope()).isEqualTo(EarnNetworkScope.AllNetworks)
            assertThat(state.networkIds()).containsExactly("ethereum", "solana").inOrder()
            assertThat(state.selectedNetworkId()).isNull()
            assertThat(state.isAllOptionSelected()).isTrue()
            assertThat(state.footer).isNull()
        }

        @Test
        fun `GIVEN my networks applied WHEN the sheet opens THEN only the added networks are listed`() {
            // Act
            val state = createModel(selectedFilter = EarnFilterNetwork.MyNetworks(isSelected = true)).state.value

            // Assert
            assertThat(state.selectedScope()).isEqualTo(EarnNetworkScope.MyNetworks)
            assertThat(state.networkIds()).containsExactly("ethereum")
            assertThat(state.footer).isNull()
        }

        @Test
        fun `GIVEN a specific added network WHEN the sheet opens THEN it opens on the narrower my networks tab`() {
            // Act
            val state = createModel(selectedFilter = specific(ethereum)).state.value

            // Assert
            assertThat(state.selectedScope()).isEqualTo(EarnNetworkScope.MyNetworks)
            assertThat(state.selectedNetworkId()).isEqualTo("ethereum")
            assertThat(state.footer).isNull()
        }

        @Test
        fun `GIVEN a specific network the user does not hold WHEN the sheet opens THEN it opens on the all tab`() {
            // Act
            val state = createModel(selectedFilter = specific(solana)).state.value

            // Assert
            assertThat(state.selectedScope()).isEqualTo(EarnNetworkScope.AllNetworks)
            assertThat(state.selectedNetworkId()).isEqualTo("solana")
            assertThat(state.footer).isNull()
        }

        @Test
        fun `GIVEN two scopes WHEN the sheet opens THEN both tabs are offered`() {
            // Act
            val state = createModel().state.value

            // Assert
            assertThat(state.scopeTabs.map { it.id }).containsExactly("AllNetworks", "MyNetworks").inOrder()
        }
    }

    @Nested
    inner class ScopeSwitching {

        @Test
        fun `GIVEN a network reachable from both tabs WHEN the tab is switched THEN nothing is offered to apply`() {
            // The draft is compared through the filter it produces, so a network that both tabs can reach
            // must not read as a change just because the tab moved.
            // Arrange
            val model = createModel(selectedFilter = specific(ethereum))

            // Act
            model.state.value.clickTab(EarnNetworkScope.AllNetworks)

            // Assert
            assertThat(model.state.value.selectedScope()).isEqualTo(EarnNetworkScope.AllNetworks)
            assertThat(model.state.value.selectedNetworkId()).isEqualTo("ethereum")
            assertThat(model.state.value.footer).isNull()
        }

        @Test
        fun `GIVEN a network missing from the other tab WHEN the tab is switched THEN the pick is dropped`() {
            // Arrange
            val model = createModel(selectedFilter = specific(solana))

            // Act
            model.state.value.clickTab(EarnNetworkScope.MyNetworks)

            // Assert
            assertThat(model.state.value.selectedNetworkId()).isNull()
            assertThat(model.state.value.isAllOptionSelected()).isTrue()
            assertThat(model.state.value.footer).isNotNull()
        }

        @Test
        fun `GIVEN all networks applied WHEN the tab is switched away and back THEN the footer disappears again`() {
            // Arrange
            val model = createModel()

            // Act
            model.state.value.clickTab(EarnNetworkScope.MyNetworks)
            val afterSwitch = model.state.value.footer
            model.state.value.clickTab(EarnNetworkScope.AllNetworks)

            // Assert
            assertThat(afterSwitch).isNotNull()
            assertThat(model.state.value.footer).isNull()
        }
    }

    @Nested
    inner class Picking {

        @Test
        fun `GIVEN all networks applied WHEN a network is picked THEN it is selected and can be applied`() {
            // Arrange
            val model = createModel()

            // Act
            model.state.value.clickNetwork("solana")

            // Assert
            assertThat(model.state.value.selectedNetworkId()).isEqualTo("solana")
            assertThat(model.state.value.isAllOptionSelected()).isFalse()
            assertThat(model.state.value.footer).isNotNull()
        }

        @Test
        fun `GIVEN a picked network WHEN the all option is clicked THEN the pick is cleared`() {
            // Arrange
            val model = createModel(selectedFilter = specific(solana))

            // Act
            model.state.value.clickAllOption()

            // Assert
            assertThat(model.state.value.selectedNetworkId()).isNull()
            assertThat(model.state.value.footer).isNotNull()
        }

        @Test
        fun `GIVEN a pick WHEN the applied network is picked again THEN nothing is left to apply`() {
            // Arrange
            val model = createModel(selectedFilter = specific(solana))
            model.state.value.clickAllOption()

            // Act
            model.state.value.clickNetwork("solana")

            // Assert
            assertThat(model.state.value.footer).isNull()
        }
    }

    @Nested
    inner class Reset {

        @Test
        fun `GIVEN a picked network WHEN reset THEN the sheet returns to all networks`() {
            // Arrange
            val model = createModel(selectedFilter = EarnFilterNetwork.MyNetworks(isSelected = true))
            model.state.value.clickNetwork("ethereum")

            // Act
            model.state.value.footer!!.onReset()

            // Assert
            assertThat(model.state.value.selectedScope()).isEqualTo(EarnNetworkScope.AllNetworks)
            assertThat(model.state.value.selectedNetworkId()).isNull()
        }

        @Test
        fun `GIVEN all networks applied WHEN a pick is reset THEN nothing is left to apply`() {
            // Arrange
            val model = createModel()
            model.state.value.clickNetwork("ethereum")

            // Act
            model.state.value.footer!!.onReset()

            // Assert
            assertThat(model.state.value.footer).isNull()
        }
    }

    @Nested
    inner class Apply {

        @Test
        fun `GIVEN a picked network WHEN applied THEN it reaches the tab and is reported as specific`() {
            // Arrange
            val model = createModel()
            model.state.value.clickNetwork("solana")

            // Act
            model.state.value.footer!!.onApply()

            // Assert
            assertThat(appliedFilter).isEqualTo(
                EarnFilterNetwork.Specific(isSelected = true, id = "solana", symbol = "SOL", fullName = "Solana"),
            )
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    EarnAnalyticsEvent.BestOpportunitiesFilterNetworkApplied(
                        networkId = "solana",
                        filterType = FilterNetworkAnalytic.SPECIFIC,
                    ),
                )
            }
        }

        @Test
        fun `GIVEN the my networks tab and no pick WHEN applied THEN the scope reaches the tab`() {
            // Arrange
            val model = createModel()
            model.state.value.clickTab(EarnNetworkScope.MyNetworks)

            // Act
            model.state.value.footer!!.onApply()

            // Assert
            assertThat(appliedFilter).isEqualTo(EarnFilterNetwork.MyNetworks(isSelected = true))
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    EarnAnalyticsEvent.BestOpportunitiesFilterNetworkApplied(
                        networkId = "",
                        filterType = FilterNetworkAnalytic.MY_NETWORKS,
                    ),
                )
            }
        }

        @Test
        fun `GIVEN my networks applied WHEN reset and applied THEN the filter widens back to all networks`() {
            // Arrange
            val model = createModel(selectedFilter = EarnFilterNetwork.MyNetworks(isSelected = true))
            model.state.value.clickTab(EarnNetworkScope.AllNetworks)

            // Act
            model.state.value.footer!!.onApply()

            // Assert
            assertThat(appliedFilter).isEqualTo(EarnFilterNetwork.AllNetworks(isSelected = true))
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    EarnAnalyticsEvent.BestOpportunitiesFilterNetworkApplied(
                        networkId = "",
                        filterType = FilterNetworkAnalytic.ALL_NETWORKS,
                    ),
                )
            }
        }
    }

    private fun specific(network: EarnNetwork) = EarnFilterNetwork.Specific(
        isSelected = true,
        id = network.networkId,
        symbol = network.symbol,
        fullName = network.fullName,
    )

    private fun EarnFilterByNetworkBottomSheetContentUM.tabs() =
        scopeTabs.filterIsInstance<TangemTabItemUM.Content>()

    private fun EarnFilterByNetworkBottomSheetContentUM.selectedScope(): EarnNetworkScope =
        EarnNetworkScope.valueOf(tabs().single { it.isSelected }.id)

    private fun EarnFilterByNetworkBottomSheetContentUM.clickTab(scope: EarnNetworkScope) {
        tabs().single { it.id == scope.name }.onClick()
    }

    private fun EarnFilterByNetworkBottomSheetContentUM.networkRows() =
        networks.filterIsInstance<EarnFilterNetworkUM.Network>()

    private fun EarnFilterByNetworkBottomSheetContentUM.networkIds() = networkRows().map { it.id }

    private fun EarnFilterByNetworkBottomSheetContentUM.selectedNetworkId(): String? =
        networkRows().firstOrNull { it.isSelected }?.id

    private fun EarnFilterByNetworkBottomSheetContentUM.isAllOptionSelected(): Boolean =
        networks.filterIsInstance<EarnFilterNetworkUM.All>().single().isSelected

    private fun EarnFilterByNetworkBottomSheetContentUM.clickNetwork(id: String) {
        networkRows().single { it.id == id }.onClick()
    }

    private fun EarnFilterByNetworkBottomSheetContentUM.clickAllOption() {
        networks.filterIsInstance<EarnFilterNetworkUM.All>().single().onClick()
    }
}