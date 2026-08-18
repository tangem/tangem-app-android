package com.tangem.features.commonfeatures.impl.addtoportfolio.ui

import com.google.common.truth.Truth.assertThat
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorBridge
import com.tangem.features.commonfeatures.impl.addtoportfolio.converter.AvailableToAddDataConverter
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultAddToPortfolioManagerTest {

    private val availableToAddDataConverter: AvailableToAddDataConverter = mockk()
    private val portfolioFetcherFactory: PortfolioFetcher.Factory = mockk()
    private val ownFetcher: PortfolioFetcher = mockk()
    private val bridge: PortfolioSelectorBridge = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(availableToAddDataConverter, portfolioFetcherFactory, ownFetcher, bridge)
        every { ownFetcher.data } returns emptyFlow()
        every { bridge.data } returns emptyFlow()
        every { portfolioFetcherFactory.create(any(), any()) } returns ownFetcher
    }

    @Test
    fun `GIVEN no bridge WHEN the manager is created THEN it loads every multi-currency wallet itself`() = runTest {
        // Act
        val manager = createManager(testScope = this, portfolioSelectorBridge = null)

        // Assert
        assertThat(manager.portfolioFetcher).isSameInstanceAs(ownFetcher)
        verify(exactly = 1) {
            portfolioFetcherFactory.create(
                mode = PortfolioFetcher.Mode.All(isOnlyMultiCurrency = true),
                scope = any(),
            )
        }
    }

    @Test
    fun `GIVEN a bridge WHEN the manager is created THEN the bridge is the only source of portfolios`() = runTest {
        // Act
        val manager = createManager(testScope = this, portfolioSelectorBridge = bridge)

        // Assert
        assertThat(manager.portfolioFetcher).isSameInstanceAs(bridge)
        verify(exactly = 0) { portfolioFetcherFactory.create(any(), any()) }
    }

    private fun createManager(
        testScope: TestScope,
        portfolioSelectorBridge: PortfolioSelectorBridge?,
    ): DefaultAddToPortfolioManager = DefaultAddToPortfolioManager(
        availableToAddDataConverter = availableToAddDataConverter,
        settings = AddToPortfolioManager.Settings(),
        analyticsParams = AddToPortfolioManager.AnalyticsParams(source = null),
        scope = testScope.backgroundScope,
        portfolioSelectorBridge = portfolioSelectorBridge,
        dispatchers = TestingCoroutineDispatcherProvider(),
        portfolioFetcherFactory = portfolioFetcherFactory,
    )
}