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
 * A tap on an option applies it straight away, so the filter handed to the tab and the analytics event
 * are the outcome of every pick. Switching to the other scope tab applies nothing by itself: it checks
 * that tab's "All" and offers an Apply footer until the user returns to the tab the sheet opened on.
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
            assertThat(state.isAllOptionSelected()).isTrue()
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
        fun `GIVEN a network both tabs list WHEN the tab is switched THEN all is checked and apply offered`() {
            // Arrange
            val model = createModel(selectedFilter = specific(ethereum))

            // Act
            model.state.value.clickTab(EarnNetworkScope.AllNetworks)

            // Assert
            assertThat(model.state.value.selectedScope()).isEqualTo(EarnNetworkScope.AllNetworks)
            assertThat(model.state.value.selectedNetworkId()).isNull()
            assertThat(model.state.value.isAllOptionSelected()).isTrue()
            assertThat(model.state.value.footer).isNotNull()
        }

        @Test
        fun `GIVEN a specific network applied WHEN switched away and back THEN it is checked again and apply hidden`() {
            // Arrange
            val model = createModel(selectedFilter = specific(ethereum))

            // Act
            model.state.value.clickTab(EarnNetworkScope.AllNetworks)
            model.state.value.clickTab(EarnNetworkScope.MyNetworks)

            // Assert
            assertThat(model.state.value.selectedNetworkId()).isEqualTo("ethereum")
            assertThat(model.state.value.footer).isNull()
        }

        @Test
        fun `GIVEN a network missing from the other tab WHEN the tab is switched THEN apply is offered`() {
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
        fun `GIVEN all networks applied WHEN the tab is switched away and back THEN apply disappears again`() {
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

        @Test
        fun `GIVEN all networks applied WHEN the tab is switched THEN nothing is applied or reported`() {
            // Arrange
            val model = createModel()

            // Act
            model.state.value.clickTab(EarnNetworkScope.MyNetworks)

            // Assert
            assertThat(appliedFilter).isNull()
            verify(exactly = 0) { analyticsEventHandler.send(any()) }
        }
    }

    @Nested
    inner class Picking {

        @Test
        fun `GIVEN all networks applied WHEN a network is tapped THEN it is applied and reported as specific`() {
            // Arrange
            val model = createModel()

            // Act
            model.state.value.clickNetwork("solana")

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
        fun `GIVEN the my networks tab WHEN all is tapped THEN my networks reaches the tab`() {
            // Arrange
            val model = createModel()
            model.state.value.clickTab(EarnNetworkScope.MyNetworks)

            // Act
            model.state.value.clickAllOption()

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
        fun `GIVEN my networks applied WHEN all is tapped on the all tab THEN the filter widens to all networks`() {
            // Arrange
            val model = createModel(selectedFilter = EarnFilterNetwork.MyNetworks(isSelected = true))
            model.state.value.clickTab(EarnNetworkScope.AllNetworks)

            // Act
            model.state.value.clickAllOption()

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

        @Test
        fun `GIVEN a specific network applied WHEN all is tapped THEN the network is cleared`() {
            // Arrange
            val model = createModel(selectedFilter = specific(solana))

            // Act
            model.state.value.clickAllOption()

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

    @Nested
    inner class Apply {

        @Test
        fun `GIVEN all networks applied WHEN my tab applied THEN my networks reach the tab`() {
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
        fun `GIVEN my networks applied WHEN all tab applied THEN all networks reach the tab`() {
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

        @Test
        fun `GIVEN a specific network applied WHEN all tab applied THEN all networks reach the tab`() {
            // Arrange
            val model = createModel(selectedFilter = specific(ethereum))
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