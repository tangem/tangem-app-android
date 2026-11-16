package com.tangem.features.earn.impl.overview

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.api.EarnFeatureToggles
import com.tangem.features.earn.api.overview.EarnBannerOverview
import com.tangem.features.earn.api.overview.EarnHeadline
import com.tangem.features.earn.api.overview.EarnOverview
import com.tangem.features.earn.api.overview.EarnOverviews
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.product.api.product.EarnPositionSummary
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.ProductAvailability
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.core.getEmittedValues
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultEarnOverviewSupplierTest {

    private val summarySupplier: EarnProductSummarySupplier = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val toggles: EarnFeatureToggles = mockk()

    private val walletsFlow = MutableStateFlow<List<UserWallet>?>(value = null)
    private val summaryFeed = SummaryFeed()

    private val lendingId = EarnProductId("lending")
    private val stakingId = EarnProductId("staking")
    private val vaultId = EarnProductId("vault")
    private val yieldModeId = EarnProductId("yield_mode")
    private val otherWalletId = UserWalletId("fedcba9876543210")
    private val bnbId = CryptoCurrency.ID.fromValue("coin⟨BNB⟩binancecoin")

    private val registry = EarnProductsRegistry(
        products = setOf(
            TestEarnProduct(id = lendingId, rank = 0),
            TestEarnProduct(id = stakingId, rank = 1, hasLegacyBlock = true),
            TestEarnProduct(id = vaultId, rank = 2),
        ),
        rateSources = setOf(TestEarnRateSource(id = yieldModeId, rank = 3)),
    )

    private val availability = EarnAvailability(
        toggles = toggles,
        userWalletsListRepository = userWalletsListRepository,
    )

    private val selector = EarnHeadlineSelector(
        order = EarnSummaryOrder(registry),
        rateBadgeConverter = EarnRateBadgeConverter(),
    )

    private var previousLocale: Locale = Locale.getDefault()

    @BeforeEach
    fun resetState() {
        clearMocks(summarySupplier, userWalletsListRepository, toggles)
        every { toggles.isEarnEnabled } returns true
        every { userWalletsListRepository.userWallets } returns walletsFlow
        every { summarySupplier.invoke(USER_WALLET_ID) } returns summaryFeed.flow
        walletsFlow.value = listOf(createColdWallet())
        summaryFeed.reset()
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(previousLocale)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Projection {

        @Test
        fun `GIVEN only staking with a position WHEN invoke THEN headline is set and banner is absent`() = runTest {
            // Arrange
            val rate = createRate(fraction = "0.04", type = EarnRateType.APR)
            val route = TestRoute("pooled_staking")
            val staking = createSummary(
                productId = stakingId,
                rate = rate,
                position = EarnPositionSummary(cryptoAmount = BigDecimal("3")),
                route = route,
            )
            val expected = overviewsOf(
                EarnOverview(
                    headline = EarnHeadline(rate = rate, isActive = true, badge = aprBadge("4.00"), route = route),
                    banner = null,
                ),
            )

            // Act
            val actual = collectOverviews(productSummaries(staking))

            // Assert
            assertThat(actual).containsExactly(expected)
        }

        @Test
        fun `GIVEN active staking and idle lending WHEN invoke THEN staking headline and banner without positions`() =
            runTest {
                // Arrange
                val stakingRate = createRate(fraction = "0.04", type = EarnRateType.APR)
                val lendingRate = createRate(fraction = "0.07")
                val route = TestRoute("pooled_staking")
                val staking = createSummary(
                    productId = stakingId,
                    rate = stakingRate,
                    position = EarnPositionSummary(cryptoAmount = BigDecimal("3")),
                    route = route,
                )
                val lending = createSummary(productId = lendingId, rate = lendingRate)
                val expected = overviewsOf(
                    EarnOverview(
                        headline = EarnHeadline(
                            rate = stakingRate,
                            isActive = true,
                            badge = aprBadge("4.00"),
                            route = route,
                        ),
                        banner = EarnBannerOverview(rate = lendingRate, activeCount = 0, totalPosition = null),
                    ),
                )

                // Act
                val actual = collectOverviews(productSummaries(staking, lending))

                // Assert
                assertThat(actual).containsExactly(expected)
            }

        @Test
        fun `GIVEN product and rate source WHEN invoke THEN headline takes both and banner counts the product`() =
            runTest {
                // Arrange
                val lendingRate = createRate(fraction = "0.05")
                val yieldModeRate = createRate(fraction = "0.09")
                val yieldModeRoute = TestRoute("yield_mode")
                val lending = createSummary(productId = lendingId, rate = lendingRate)
                val yieldMode = createSummary(productId = yieldModeId, rate = yieldModeRate, route = yieldModeRoute)
                val expected = overviewsOf(
                    EarnOverview(
                        headline = EarnHeadline(
                            rate = yieldModeRate,
                            isActive = false,
                            badge = apyBadge("9.00"),
                            route = yieldModeRoute,
                        ),
                        banner = EarnBannerOverview(rate = lendingRate, activeCount = 0, totalPosition = null),
                    ),
                )

                // Act
                val actual = collectOverviews(productSummaries(lending, rateSources = listOf(yieldMode)))

                // Assert
                assertThat(actual).containsExactly(expected)
            }

        @Test
        fun `GIVEN rate source only for a currency WHEN invoke THEN overview has headline and no banner`() = runTest {
            // Arrange
            val rate = createRate(fraction = "0.09")
            val route = TestRoute("yield_mode")
            val yieldMode = createSummary(productId = yieldModeId, rate = rate, route = route)
            val expected = overviewsOf(
                EarnOverview(
                    headline = EarnHeadline(rate = rate, isActive = false, badge = apyBadge("9.00"), route = route),
                    banner = null,
                ),
            )

            // Act
            val actual = collectOverviews(productSummaries(rateSources = listOf(yieldMode)))

            // Assert
            assertThat(actual).containsExactly(expected)
        }

        @Test
        fun `GIVEN equal summaries emitted twice WHEN invoke THEN a single frame is emitted`() = runTest {
            // Arrange
            val rate = createRate(fraction = "0.05")
            val lending = createSummary(productId = lendingId, rate = rate)
            val expected = overviewsOf(
                EarnOverview(
                    headline = EarnHeadline(rate = rate, isActive = false, badge = apyBadge("5.00"), route = null),
                    banner = EarnBannerOverview(rate = rate, activeCount = 0, totalPosition = null),
                ),
            )
            val frames = collectOverviews(productSummaries(lending))

            // Act
            summaryFeed.emit(productSummaries(createSummary(productId = lendingId, rate = createRate("0.05"))))
            runCurrent()

            // Assert
            assertThat(frames).containsExactly(expected)
        }

        @Test
        fun `GIVEN product of one currency and rate source of another WHEN invoke THEN currencies stay separate`() =
            runTest {
                // Arrange
                val lendingRate = createRate(fraction = "0.05")
                val yieldModeRate = createRate(fraction = "0.09")
                val yieldModeRoute = TestRoute("yield_mode")
                val lending = createSummary(productId = lendingId, rate = lendingRate)
                val yieldMode = createSummary(productId = yieldModeId, rate = yieldModeRate, route = yieldModeRoute)
                val summaries = EarnProductSummaries(
                    products = mapOf(ETH_ID to listOf(lending)),
                    rateSources = mapOf(bnbId to listOf(yieldMode)),
                    source = StatusSource.ACTUAL,
                )
                val expected = EarnOverviews(
                    byCurrency = mapOf(
                        ETH_ID to EarnOverview(
                            headline = EarnHeadline(
                                rate = lendingRate,
                                isActive = false,
                                badge = apyBadge("5.00"),
                                route = null,
                            ),
                            banner = EarnBannerOverview(rate = lendingRate, activeCount = 0, totalPosition = null),
                        ),
                        bnbId to EarnOverview(
                            headline = EarnHeadline(
                                rate = yieldModeRate,
                                isActive = false,
                                badge = apyBadge("9.00"),
                                route = yieldModeRoute,
                            ),
                            banner = null,
                        ),
                    ),
                    source = StatusSource.ACTUAL,
                )

                // Act
                val actual = collectOverviews(summaries)

                // Assert
                assertThat(actual).containsExactly(expected)
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SourcePassThrough {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN summaries with a source WHEN invoke THEN overviews carry the same source`(model: StatusSource) =
            runTest {
                // Arrange
                val rate = createRate(fraction = "0.04", type = EarnRateType.APR)
                val staking = createSummary(
                    productId = stakingId,
                    rate = rate,
                    position = EarnPositionSummary(cryptoAmount = BigDecimal("3")),
                )
                val expected = overviewsOf(
                    EarnOverview(
                        headline = EarnHeadline(rate = rate, isActive = true, badge = aprBadge("4.00"), route = null),
                        banner = null,
                    ),
                    source = model,
                )

                // Act
                val actual = collectOverviews(productSummaries(staking, source = model))

                // Assert
                assertThat(actual).containsExactly(expected)
            }

        private fun provideTestModels() = StatusSource.entries.toList()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Banner {

        @Test
        fun `GIVEN products with and without legacy block WHEN invoke THEN banner counts only the one without`() =
            runTest {
                // Arrange
                val stakingRate = createRate(fraction = "0.08", type = EarnRateType.APR)
                val lendingRate = createRate(fraction = "0.03")
                val staking = createSummary(
                    productId = stakingId,
                    rate = stakingRate,
                    position = EarnPositionSummary(cryptoAmount = BigDecimal("5")),
                )
                val lending = createSummary(
                    productId = lendingId,
                    rate = lendingRate,
                    position = EarnPositionSummary(cryptoAmount = BigDecimal("2")),
                )
                val expected = overviewsOf(
                    EarnOverview(
                        headline = EarnHeadline(
                            rate = stakingRate,
                            isActive = true,
                            badge = aprBadge("8.00"),
                            route = null,
                        ),
                        banner = EarnBannerOverview(
                            rate = lendingRate,
                            activeCount = 1,
                            totalPosition = BigDecimal("2"),
                        ),
                    ),
                )

                // Act
                val actual = collectOverviews(productSummaries(staking, lending))

                // Assert
                assertThat(actual).containsExactly(expected)
            }

        @Test
        fun `GIVEN only legacy product without position WHEN invoke THEN headline is set and banner is absent`() =
            runTest {
                // Arrange
                val rate = createRate(fraction = "0.04", type = EarnRateType.APR)
                val staking = createSummary(productId = stakingId, rate = rate)
                val expected = overviewsOf(
                    EarnOverview(
                        headline = EarnHeadline(rate = rate, isActive = false, badge = aprBadge("4.00"), route = null),
                        banner = null,
                    ),
                )

                // Act
                val actual = collectOverviews(productSummaries(staking))

                // Assert
                assertThat(actual).containsExactly(expected)
            }

        @Test
        fun `GIVEN a product missing in the registry WHEN invoke THEN headline is set and banner is absent`() =
            runTest {
                // Arrange
                val rate = createRate(fraction = "0.06")
                val unregistered = createSummary(
                    productId = EarnProductId("unregistered"),
                    rate = rate,
                    position = EarnPositionSummary(cryptoAmount = BigDecimal("1")),
                )
                val expected = overviewsOf(
                    EarnOverview(
                        headline = EarnHeadline(rate = rate, isActive = true, badge = apyBadge("6.00"), route = null),
                        banner = null,
                    ),
                )

                // Act
                val actual = collectOverviews(productSummaries(unregistered))

                // Assert
                assertThat(actual).containsExactly(expected)
            }

        @Test
        fun `GIVEN toggle off and suitable wallet WHEN invoke THEN banner is absent and headline stays`() = runTest {
            // Arrange
            val rate = createRate(fraction = "0.05")
            val lending = createSummary(
                productId = lendingId,
                rate = rate,
                position = EarnPositionSummary(cryptoAmount = BigDecimal("2")),
            )
            val expected = overviewsOf(
                EarnOverview(
                    headline = EarnHeadline(rate = rate, isActive = true, badge = apyBadge("5.00"), route = null),
                    banner = null,
                ),
            )
            summaryFeed.emit(productSummaries(lending))
            every { toggles.isEarnEnabled } returns false

            // Act
            val actual = getEmittedValues(createSupplier().invoke(USER_WALLET_ID))
            runCurrent()

            // Assert
            assertThat(actual).containsExactly(expected)
            assertThat(summaryFeed.starts).isEqualTo(1)
        }

        @Test
        fun `GIVEN supplier collected with toggle off WHEN toggle is turned on and invoke again THEN banner is set`() =
            runTest {
                // Arrange
                val rate = createRate(fraction = "0.05")
                val lending = createSummary(
                    productId = lendingId,
                    rate = rate,
                    position = EarnPositionSummary(cryptoAmount = BigDecimal("2")),
                )
                val expected = overviewsOf(
                    EarnOverview(
                        headline = EarnHeadline(rate = rate, isActive = true, badge = apyBadge("5.00"), route = null),
                        banner = EarnBannerOverview(rate = rate, activeCount = 1, totalPosition = BigDecimal("2")),
                    ),
                )
                summaryFeed.emit(productSummaries(lending))
                val supplier = createSupplier()
                every { toggles.isEarnEnabled } returns false
                getEmittedValues(supplier.invoke(USER_WALLET_ID))
                runCurrent()

                // Act
                every { toggles.isEarnEnabled } returns true
                val actual = getEmittedValues(supplier.invoke(USER_WALLET_ID))
                runCurrent()

                // Assert
                assertThat(actual).containsExactly(expected)
            }

        @Test
        fun `GIVEN only counted product is full without position WHEN invoke THEN banner has no rate and positions`() =
            runTest {
                // Arrange
                val vault = createSummary(
                    productId = vaultId,
                    rate = createRate(fraction = "0.06"),
                    availability = ProductAvailability.Full,
                )
                val expected = overviewsOf(
                    EarnOverview(
                        headline = null,
                        banner = EarnBannerOverview(rate = null, activeCount = 0, totalPosition = null),
                    ),
                )

                // Act
                val actual = collectOverviews(productSummaries(vault))

                // Assert
                assertThat(actual).containsExactly(expected)
            }

        @Test
        fun `GIVEN toggle on and product without legacy block WHEN invoke THEN banner is set and skips rate sources`() =
            runTest {
                // Arrange
                val lendingRate = createRate(fraction = "0.05")
                val yieldModeRate = createRate(fraction = "0.09")
                val yieldModeRoute = TestRoute("yield_mode")
                val lending = createSummary(
                    productId = lendingId,
                    rate = lendingRate,
                    position = EarnPositionSummary(cryptoAmount = BigDecimal("1")),
                )
                val yieldMode = createSummary(
                    productId = yieldModeId,
                    rate = yieldModeRate,
                    position = EarnPositionSummary(cryptoAmount = BigDecimal("10")),
                    route = yieldModeRoute,
                )
                val expected = overviewsOf(
                    EarnOverview(
                        headline = EarnHeadline(
                            rate = yieldModeRate,
                            isActive = true,
                            badge = apyBadge("9.00"),
                            route = yieldModeRoute,
                        ),
                        banner = EarnBannerOverview(
                            rate = lendingRate,
                            activeCount = 1,
                            totalPosition = BigDecimal("1"),
                        ),
                    ),
                )

                // Act
                val actual = collectOverviews(productSummaries(lending, rateSources = listOf(yieldMode)))

                // Assert
                assertThat(actual).containsExactly(expected)
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class BannerRate {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN active lending and inactive vault with a higher rate WHEN invoke THEN banner takes the active rate`(
            model: ProductOrderModel,
        ) = runTest {
            // Arrange
            val lendingRate = createRate(fraction = "0.03")
            val vaultRate = createRate(fraction = "0.08")
            val lending = createSummary(
                productId = lendingId,
                rate = lendingRate,
                position = EarnPositionSummary(cryptoAmount = BigDecimal("2")),
            )
            val vault = createSummary(productId = vaultId, rate = vaultRate)
            val expected = overviewsOf(
                EarnOverview(
                    headline = EarnHeadline(
                        rate = lendingRate,
                        isActive = true,
                        badge = apyBadge("3.00"),
                        route = null,
                    ),
                    banner = EarnBannerOverview(rate = lendingRate, activeCount = 1, totalPosition = BigDecimal("2")),
                ),
            )

            // Act
            val actual = collectOverviews(productSummaries(*model.order(lending, vault).toTypedArray()))

            // Assert
            assertThat(actual).containsExactly(expected)
        }

        private fun provideTestModels() = listOf(
            ProductOrderModel(
                name = "active product first",
                order = { lending, vault -> listOf(lending, vault) },
            ),
            ProductOrderModel(
                name = "active product last",
                order = { lending, vault -> listOf(vault, lending) },
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class BannerTotals {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN positions of counted products WHEN invoke THEN banner count and total follow the positions`(
            model: TotalsModel,
        ) = runTest {
            // Arrange
            val products = model.positions.zip(listOf(lendingId, vaultId)).map { (position, id) ->
                createSummary(productId = id, position = position)
            }
            val expected = EarnBannerOverview(
                rate = createRate(),
                activeCount = model.expectedActiveCount,
                totalPosition = model.expectedTotal,
            )

            // Act
            val actual = collectOverviews(productSummaries(*products.toTypedArray()))

            // Assert
            assertThat(actual.single()[ETH_ID]?.banner).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            TotalsModel(
                name = "two positions with amounts",
                positions = listOf(
                    EarnPositionSummary(cryptoAmount = BigDecimal("1.50")),
                    EarnPositionSummary(cryptoAmount = BigDecimal("2.25")),
                ),
                expectedActiveCount = 2,
                expectedTotal = BigDecimal("3.75"),
            ),
            TotalsModel(
                name = "two positions without amounts",
                positions = listOf(
                    EarnPositionSummary(cryptoAmount = null),
                    EarnPositionSummary(cryptoAmount = null),
                ),
                expectedActiveCount = 2,
                expectedTotal = null,
            ),
            TotalsModel(
                name = "a position with an amount and a position without",
                positions = listOf(
                    EarnPositionSummary(cryptoAmount = BigDecimal("1.50")),
                    EarnPositionSummary(cryptoAmount = null),
                ),
                expectedActiveCount = 2,
                expectedTotal = BigDecimal("1.50"),
            ),
            TotalsModel(
                name = "a position with an amount and a product without a position",
                positions = listOf(
                    EarnPositionSummary(cryptoAmount = BigDecimal("1.50")),
                    null,
                ),
                expectedActiveCount = 1,
                expectedTotal = BigDecimal("1.50"),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class UnsuitableWallet {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN unsuitable wallet WHEN invoke THEN Empty is emitted and summaries are never read`(
            model: UnsuitableWalletModel,
        ) = runTest {
            // Arrange
            walletsFlow.value = model.wallets
            summaryFeed.emit(
                productSummaries(
                    createSummary(
                        productId = lendingId,
                        position = EarnPositionSummary(cryptoAmount = BigDecimal("1")),
                    ),
                ),
            )

            // Act
            val actual = getEmittedValues(createSupplier().invoke(USER_WALLET_ID))
            runCurrent()

            // Assert
            assertThat(actual).containsExactly(EarnOverviews.Empty)
            val frame = actual.first()
            assertThat(frame.source).isEqualTo(StatusSource.ACTUAL)
            assertThat(frame.isComplete).isTrue()
            assertThat(frame[ETH_ID]).isNull()
            verify(exactly = 0) { summarySupplier.invoke(USER_WALLET_ID) }
        }

        private fun provideTestModels() = listOf(
            UnsuitableWalletModel(
                name = "cold single-currency wallet",
                wallets = listOf(createSingleCurrencyColdWallet()),
            ),
            UnsuitableWalletModel(
                name = "locked hot wallet",
                wallets = listOf(createHotWallet(isLocked = true)),
            ),
            UnsuitableWalletModel(
                name = "locked cold wallet",
                wallets = listOf(createColdWallet(isLocked = true)),
            ),
            UnsuitableWalletModel(
                name = "wallet id outside the list",
                wallets = listOf(createColdWallet(walletId = otherWalletId)),
            ),
            UnsuitableWalletModel(
                name = "wallets list not loaded",
                wallets = null,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class WalletSwitching {

        private val rate = createRate(fraction = "0.05")

        private val lending = createSummary(
            productId = lendingId,
            rate = rate,
            position = EarnPositionSummary(cryptoAmount = BigDecimal("2")),
        )

        private val overviews = overviewsOf(
            EarnOverview(
                headline = EarnHeadline(rate = rate, isActive = true, badge = apyBadge("5.00"), route = null),
                banner = EarnBannerOverview(rate = rate, activeCount = 1, totalPosition = BigDecimal("2")),
            ),
        )

        @Test
        fun `GIVEN locked wallet WHEN wallet is unlocked THEN Empty is followed by overviews`() = runTest {
            // Arrange
            walletsFlow.value = listOf(createColdWallet(isLocked = true))
            summaryFeed.emit(productSummaries(lending))
            val frames = getEmittedValues(createSupplier().invoke(USER_WALLET_ID))
            runCurrent()
            val summaryStartsWhileLocked = summaryFeed.starts

            // Act
            walletsFlow.value = listOf(createColdWallet())
            runCurrent()

            // Assert
            assertThat(frames).containsExactly(EarnOverviews.Empty, overviews).inOrder()
            assertThat(summaryStartsWhileLocked).isEqualTo(0)
            assertThat(summaryFeed.starts).isEqualTo(1)
            assertThat(summaryFeed.subscribers).isEqualTo(1)
        }

        @Test
        fun `GIVEN suitable wallet WHEN wallet is locked THEN overviews are followed by Empty and summaries dropped`() =
            runTest {
                // Arrange
                summaryFeed.emit(productSummaries(lending))
                val frames = getEmittedValues(createSupplier().invoke(USER_WALLET_ID))
                runCurrent()
                val subscribersWhileSuitable = summaryFeed.subscribers

                // Act
                walletsFlow.value = listOf(createColdWallet(isLocked = true))
                runCurrent()

                // Assert
                assertThat(frames).containsExactly(overviews, EarnOverviews.Empty).inOrder()
                assertThat(subscribersWhileSuitable).isEqualTo(1)
                assertThat(summaryFeed.subscribers).isEqualTo(0)
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class WalletListUpdates {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN suitable wallet WHEN wallet list changes without availability change THEN nothing is resubscribed`(
            model: ListUpdateModel,
        ) = runTest {
            // Arrange
            val rate = createRate(fraction = "0.05")
            val lending = createSummary(
                productId = lendingId,
                rate = rate,
                position = EarnPositionSummary(cryptoAmount = BigDecimal("2")),
            )
            val expected = overviewsOf(
                EarnOverview(
                    headline = EarnHeadline(rate = rate, isActive = true, badge = apyBadge("5.00"), route = null),
                    banner = EarnBannerOverview(rate = rate, activeCount = 1, totalPosition = BigDecimal("2")),
                ),
            )
            val wallet = createColdWallet()
            val otherWallet = createColdWallet(walletId = otherWalletId)
            walletsFlow.value = listOf(wallet, otherWallet)
            val frames = collectOverviews(productSummaries(lending))

            // Act
            walletsFlow.value = model.update(wallet, otherWallet)
            runCurrent()

            // Assert
            assertThat(frames).containsExactly(expected)
            assertThat(summaryFeed.starts).isEqualTo(1)
            assertThat(summaryFeed.subscribers).isEqualTo(1)
        }

        private fun provideTestModels() = listOf(
            ListUpdateModel(
                name = "a wallet is renamed",
                update = { wallet, otherWallet -> listOf(wallet.copy(name = "Renamed"), otherWallet) },
            ),
            ListUpdateModel(
                name = "wallets are reordered",
                update = { wallet, otherWallet -> listOf(otherWallet, wallet) },
            ),
            ListUpdateModel(
                name = "an unrelated wallet is removed",
                update = { wallet, _ -> listOf(wallet) },
            ),
        )
    }


    private fun TestScope.createSupplier() = DefaultEarnOverviewSupplier(
        summarySupplier = summarySupplier,
        availability = availability,
        toggles = toggles,
        registry = registry,
        selector = selector,
        dispatchers = createTestingCoroutineDispatcherProvider(),
    )

    private fun TestScope.collectOverviews(summaries: EarnProductSummaries): List<EarnOverviews> {
        summaryFeed.emit(summaries)
        val frames = getEmittedValues(createSupplier().invoke(USER_WALLET_ID))
        runCurrent()
        return frames
    }

    private fun productSummaries(
        vararg products: EarnProductSummary,
        rateSources: List<EarnProductSummary> = emptyList(),
        source: StatusSource = StatusSource.ACTUAL,
    ) = EarnProductSummaries(
        products = if (products.isEmpty()) emptyMap() else mapOf(ETH_ID to products.toList()),
        rateSources = if (rateSources.isEmpty()) emptyMap() else mapOf(ETH_ID to rateSources),
        source = source,
    )

    private fun overviewsOf(overview: EarnOverview, source: StatusSource = StatusSource.ACTUAL) =
        EarnOverviews(byCurrency = mapOf(ETH_ID to overview), source = source)

    private fun apyBadge(percent: String) =
        resourceReference(R.string.yield_module_earn_badge, wrappedList(percent))

    private fun aprBadge(percent: String) =
        resourceReference(R.string.staking_apr_earn_badge, wrappedList(percent))

    private class SummaryFeed {

        private val shared = MutableSharedFlow<EarnProductSummaries>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

        var starts: Int = 0
            private set

        val flow: Flow<EarnProductSummaries> = shared.onStart { starts++ }

        val subscribers: Int
            get() = shared.subscriptionCount.value

        fun emit(summaries: EarnProductSummaries) {
            shared.tryEmit(summaries)
        }

        fun reset() {
            shared.resetReplayCache()
            starts = 0
        }
    }

    internal data class TotalsModel(
        val name: String,
        val positions: List<EarnPositionSummary?>,
        val expectedActiveCount: Int,
        val expectedTotal: BigDecimal?,
    ) {
        override fun toString(): String = name
    }

    internal data class ProductOrderModel(
        val name: String,
        val order: (EarnProductSummary, EarnProductSummary) -> List<EarnProductSummary>,
    ) {
        override fun toString(): String = name
    }

    internal data class UnsuitableWalletModel(val name: String, val wallets: List<UserWallet>?) {
        override fun toString(): String = name
    }

    internal data class ListUpdateModel(
        val name: String,
        val update: (UserWallet.Cold, UserWallet.Cold) -> List<UserWallet>,
    ) {
        override fun toString(): String = name
    }
}