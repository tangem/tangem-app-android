package com.tangem.features.earn.impl.banner.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.api.EarnBannerComponent
import com.tangem.features.earn.api.overview.EarnBannerOverview
import com.tangem.features.earn.api.overview.EarnOverview
import com.tangem.features.earn.api.overview.EarnOverviewSupplier
import com.tangem.features.earn.api.overview.EarnOverviews
import com.tangem.features.earn.impl.banner.ui.state.EarnBannerUM
import com.tangem.features.earn.impl.fake.FakeEarnPosition
import com.tangem.features.earn.impl.fake.FakeEarnProductPreset
import com.tangem.features.earn.impl.fake.FakeForeignRoute
import com.tangem.features.earn.impl.fake.createFakeEarnProduct
import com.tangem.features.earn.impl.fake.fakeSummary
import com.tangem.features.earn.impl.overview.EarnProductSummaries
import com.tangem.features.earn.impl.overview.EarnProductSummarySupplier
import com.tangem.features.earn.impl.overview.EarnProductsRegistry
import com.tangem.features.earn.impl.overview.USER_WALLET_ID
import com.tangem.features.earn.impl.overview.createRate
import com.tangem.features.earn.impl.sheet.EarnYieldSheetConfig
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnBannerModelTest {

    private val router: Router = mockk(relaxed = true)
    private val overviewSupplier: EarnOverviewSupplier = mockk()
    private val summarySupplier: EarnProductSummarySupplier = mockk()
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase = mockk()
    private val currency: CryptoCurrency = MockCryptoCurrencyFactory().ethereum
    private val otherCurrency: CryptoCurrency = MockCryptoCurrencyFactory().bitcoin

    private val lendingId = EarnProductId("lending")
    private val vaultId = EarnProductId("vault")

    private val lending = createFakeEarnProduct(FakeEarnProductPreset.Lending)
    private val staking = createFakeEarnProduct(FakeEarnProductPreset.Staking)
    private val registry = EarnProductsRegistry(products = setOf(lending, staking), rateSources = emptySet())

    private val frames = MutableStateFlow(EarnOverviews.Empty)
    private val productFrames = MutableStateFlow(lendingAndStaking())
    private val isBalanceHidden = MutableStateFlow(false)

    @BeforeEach
    fun setUp() {
        clearMocks(router, overviewSupplier, summarySupplier, getBalanceHidingSettingsUseCase)
        frames.value = EarnOverviews.Empty
        productFrames.value = lendingAndStaking()
        isBalanceHidden.value = false
        every { overviewSupplier(USER_WALLET_ID) } returns frames
        every { summarySupplier(USER_WALLET_ID) } returns productFrames
        every { getBalanceHidingSettingsUseCase.isBalanceHidden() } returns isBalanceHidden
    }

    @Test
    fun `GIVEN overview with no positions WHEN onEnableClick THEN router pushes EarnEntry from the banner with the picker start`() =
        runTest {
            // Arrange
            frames.value = overviewsOf(activeCount = 0)
            val model = createModel()

            // Act
            model.onEnableClick(preselected = vaultId)

            // Assert
            verify(exactly = 1) {
                router.push(
                    AppRoute.EarnEntry(
                        userWalletId = USER_WALLET_ID,
                        cryptoCurrency = currency,
                        source = AppRoute.EarnEntry.Source.EarnBanner,
                        start = AppRoute.EarnEntry.Start.Picker(preselected = vaultId),
                    ),
                    any(),
                )
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN Offer state WHEN the Enable button of the banner is clicked THEN router pushes EarnEntry from the banner`() =
        runTest {
            // Arrange
            frames.value = overviewsOf(activeCount = 0)
            val model = createModel()
            val offer = model.uiState.value as EarnBannerUM.Offer

            // Act
            offer.onEnableClick()

            // Assert
            verify(exactly = 1) {
                router.push(
                    AppRoute.EarnEntry(
                        userWalletId = USER_WALLET_ID,
                        cryptoCurrency = currency,
                        source = AppRoute.EarnEntry.Source.EarnBanner,
                        start = AppRoute.EarnEntry.Start.Picker(preselected = lendingId),
                    ),
                    any(),
                )
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN overview with positions WHEN onBannerClick THEN the sheet slot is activated and the router is not used`() =
        runTest {
            // Arrange
            frames.value = overviewsOf(activeCount = 2)
            val model = createModel()
            val sheetSlot = model.sheetNavigation.trackSlot()

            // Act
            model.onBannerClick()
            advanceUntilIdle()

            // Assert
            assertThat(sheetSlot).containsExactly(EarnYieldSheetConfig)
            verify(exactly = 0) { router.push(any(), any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN Enabled state WHEN the banner is clicked THEN the sheet slot is activated`() = runTest {
        // Arrange
        frames.value = overviewsOf(activeCount = 1)
        val model = createModel()
        val sheetSlot = model.sheetNavigation.trackSlot()
        val enabled = model.uiState.value as EarnBannerUM.Enabled

        // Act
        enabled.onClick()
        advanceUntilIdle()

        // Assert
        assertThat(sheetSlot).containsExactly(EarnYieldSheetConfig)
        model.onDestroy()
    }

    @Test
    fun `GIVEN a single active product WHEN the banner is clicked THEN its Info opens from the banner without the sheet`() =
        runTest {
            // Arrange
            productFrames.value = summariesOf(fakeSummary(FakeEarnProductPreset.Lending, FakeEarnPosition.Known))
            frames.value = overviewsOf(activeCount = 1)
            val model = createModel()
            val sheetSlot = model.sheetNavigation.trackSlot()

            // Act
            model.onBannerClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                router.push(
                    AppRoute.EarnEntry(
                        userWalletId = USER_WALLET_ID,
                        cryptoCurrency = currency,
                        source = AppRoute.EarnEntry.Source.EarnBanner,
                        start = AppRoute.EarnEntry.Start.Info(productId = lending.id),
                    ),
                    any(),
                )
            }
            assertThat(sheetSlot).isEmpty()
            model.onDestroy()
        }

    @Test
    fun `GIVEN a single active pooled staking WHEN the banner is clicked THEN the route of the product is pushed`() =
        runTest {
            // Arrange
            productFrames.value = summariesOf(fakeSummary(FakeEarnProductPreset.Staking, FakeEarnPosition.Known))
            frames.value = overviewsOf(activeCount = 1)
            val model = createModel()
            val sheetSlot = model.sheetNavigation.trackSlot()

            // Act
            model.onBannerClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { router.push(FakeForeignRoute, any()) }
            assertThat(sheetSlot).isEmpty()
            model.onDestroy()
        }

    @Test
    fun `GIVEN a single product while other products are still loading WHEN the banner is clicked THEN the sheet opens`() =
        runTest {
            // Arrange
            productFrames.value = summariesOf(
                fakeSummary(FakeEarnProductPreset.Lending, FakeEarnPosition.Known),
                isAwaitingProducts = true,
            )
            frames.value = overviewsOf(activeCount = 1)
            val model = createModel()
            val sheetSlot = model.sheetNavigation.trackSlot()

            // Act
            model.onBannerClick()
            advanceUntilIdle()

            // Assert
            assertThat(sheetSlot).containsExactly(EarnYieldSheetConfig)
            verify(exactly = 0) { router.push(any(), any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN open sheet slot WHEN onDismiss THEN the slot is dismissed`() = runTest {
        // Arrange
        frames.value = overviewsOf(activeCount = 2)
        val model = createModel()
        val sheetSlot = model.sheetNavigation.trackSlot()
        model.onBannerClick()
        advanceUntilIdle()

        // Act
        model.onDismiss()

        // Assert
        assertThat(sheetSlot).containsExactly(EarnYieldSheetConfig, null).inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN overviews without the currency WHEN model created THEN uiState stays Hidden`() = runTest {
        // Arrange
        frames.value = EarnOverviews(
            byCurrency = mapOf(otherCurrency.id to overviewOf(activeCount = 0)),
            source = StatusSource.ACTUAL,
        )

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value).isEqualTo(EarnBannerUM.Hidden)
        model.onDestroy()
    }

    @Test
    fun `GIVEN EarnOverviews Empty WHEN model created THEN uiState stays Hidden`() = runTest {
        // Arrange
        frames.value = EarnOverviews.Empty

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value).isEqualTo(EarnBannerUM.Hidden)
        model.onDestroy()
    }

    @Test
    fun `GIVEN CACHE frame with a banner WHEN model created THEN uiState is Offer`() = runTest {
        // Arrange
        frames.value = overviewsOf(activeCount = 0, source = StatusSource.CACHE)

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(EarnBannerUM.Offer::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN CACHE frame then ACTUAL frame with another best product WHEN Enable is clicked THEN the new one is preselected`() =
        runTest {
            // Arrange
            frames.value = overviewsOf(activeCount = 0, source = StatusSource.CACHE, bestProductId = lendingId)
            val model = createModel()
            frames.value = overviewsOf(activeCount = 0, source = StatusSource.ACTUAL, bestProductId = vaultId)
            advanceUntilIdle()
            val offer = model.uiState.value as EarnBannerUM.Offer

            // Act
            offer.onEnableClick()

            // Assert
            verify(exactly = 1) {
                router.push(
                    AppRoute.EarnEntry(
                        userWalletId = USER_WALLET_ID,
                        cryptoCurrency = currency,
                        source = AppRoute.EarnEntry.Source.EarnBanner,
                        start = AppRoute.EarnEntry.Start.Picker(preselected = vaultId),
                    ),
                    any(),
                )
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN banner without a best product WHEN Enable is clicked THEN the picker has no preselected product`() =
        runTest {
            // Arrange
            frames.value = overviewsOf(activeCount = 0, bestProductId = null)
            val model = createModel()
            val offer = model.uiState.value as EarnBannerUM.Offer

            // Act
            offer.onEnableClick()

            // Assert
            verify(exactly = 1) {
                router.push(
                    AppRoute.EarnEntry(
                        userWalletId = USER_WALLET_ID,
                        cryptoCurrency = currency,
                        source = AppRoute.EarnEntry.Source.EarnBanner,
                        start = AppRoute.EarnEntry.Start.Picker(preselected = null),
                    ),
                    any(),
                )
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN ONLY_CACHE frame with a banner WHEN model created THEN uiState is Offer`() = runTest {
        // Arrange
        frames.value = overviewsOf(activeCount = 0, source = StatusSource.ONLY_CACHE)

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(EarnBannerUM.Offer::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN hidden balance WHEN model created THEN Enabled description masks the amount`() = runTest {
        // Arrange
        frames.value = overviewsOf(activeCount = 2)
        isBalanceHidden.value = true

        // Act
        val model = createModel()

        // Assert
        val enabled = model.uiState.value as EarnBannerUM.Enabled
        assertThat(enabled.description).isEqualTo(activeCountText(amount = "***", count = 2))
        model.onDestroy()
    }

    @Test
    fun `GIVEN Enabled banner WHEN balance hiding is switched THEN the amount is masked and shown again`() = runTest {
        // Arrange
        frames.value = overviewsOf(activeCount = 1)
        val model = createModel()
        val shown = (model.uiState.value as EarnBannerUM.Enabled).description

        // Act
        isBalanceHidden.value = true
        advanceUntilIdle()
        val masked = (model.uiState.value as EarnBannerUM.Enabled).description
        isBalanceHidden.value = false
        advanceUntilIdle()
        val shownAgain = (model.uiState.value as EarnBannerUM.Enabled).description

        // Assert
        assertThat(shown).isNotEqualTo(activeCountText(amount = "***", count = 1))
        assertThat(masked).isEqualTo(activeCountText(amount = "***", count = 1))
        assertThat(shownAgain).isEqualTo(shown)
        model.onDestroy()
    }

    @Test
    fun `GIVEN open sheet slot WHEN the overview turns Hidden THEN the slot is not closed`() = runTest {
        // Arrange
        frames.value = overviewsOf(activeCount = 2)
        val model = createModel()
        val sheetSlot = model.sheetNavigation.trackSlot()
        model.onBannerClick()
        advanceUntilIdle()

        // Act
        frames.value = EarnOverviews.Empty
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isEqualTo(EarnBannerUM.Hidden)
        assertThat(sheetSlot).containsExactly(EarnYieldSheetConfig)
        model.onDestroy()
    }

    /** Mirrors what a real `childSlot` would track, since bare [SlotNavigation] only relays events. */
    private fun SlotNavigation<EarnYieldSheetConfig>.trackSlot(): List<EarnYieldSheetConfig?> {
        val tracked = mutableListOf<EarnYieldSheetConfig?>()
        subscribe { event -> tracked.add(event.transformer(tracked.lastOrNull())) }
        return tracked
    }

    private fun lendingAndStaking() = summariesOf(
        fakeSummary(FakeEarnProductPreset.Lending, FakeEarnPosition.Known),
        fakeSummary(FakeEarnProductPreset.Staking, FakeEarnPosition.None),
    )

    private fun summariesOf(vararg summaries: EarnProductSummary, isAwaitingProducts: Boolean = false) =
        EarnProductSummaries(
            products = mapOf(currency.id to summaries.toList()),
            rateSources = emptyMap(),
            source = if (isAwaitingProducts) StatusSource.CACHE else StatusSource.ACTUAL,
            isAwaitingProducts = isAwaitingProducts,
        )

    private fun overviewsOf(
        activeCount: Int,
        source: StatusSource = StatusSource.ACTUAL,
        bestProductId: EarnProductId? = lendingId,
    ) = EarnOverviews(
        byCurrency = mapOf(currency.id to overviewOf(activeCount, bestProductId)),
        source = source,
    )

    private fun overviewOf(activeCount: Int, bestProductId: EarnProductId? = lendingId) = EarnOverview(
        headline = null,
        banner = EarnBannerOverview(
            rate = createRate("0.055"),
            bestProductId = bestProductId,
            activeCount = activeCount,
            totalPosition = BigDecimal.ONE.takeIf { activeCount > 0 },
        ),
    )

    private fun TestScope.createModel(): EarnBannerModel {
        val model = EarnBannerModel(
            dispatchers = createTestingCoroutineDispatcherProvider(),
            paramsContainer = MutableParamsContainer(EarnBannerComponent.Params(USER_WALLET_ID, currency)),
            overviewSupplier = overviewSupplier,
            summarySupplier = summarySupplier,
            registry = registry,
            getBalanceHidingSettingsUseCase = getBalanceHidingSettingsUseCase,
            converter = EarnBannerConverter(),
            router = router,
        )
        advanceUntilIdle()

        return model
    }
}