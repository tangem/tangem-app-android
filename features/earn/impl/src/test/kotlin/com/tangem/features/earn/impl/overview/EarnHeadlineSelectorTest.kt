package com.tangem.features.earn.impl.overview

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.api.overview.EarnBadgeAccent
import com.tangem.features.earn.api.overview.EarnHeadline
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.product.api.product.EarnPositionSummary
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.ProductAvailability
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnHeadlineSelectorTest {

    private val lendingId = EarnProductId("lending")
    private val stakingId = EarnProductId("staking")
    private val vaultId = EarnProductId("vault")
    private val yieldModeId = EarnProductId("yield_mode")
    private val stakeKitId = EarnProductId("stakekit")

    private val pooledStakingRoute = TestRoute("pooled_staking")
    private val yieldModeRoute = TestRoute("yield_mode")
    private val stakeKitRoute = TestRoute("stakekit")

    private val registry = EarnProductsRegistry(
        products = setOf(
            TestEarnProduct(lendingId, rank = 0),
            TestEarnProduct(stakingId, rank = 1),
            TestEarnProduct(vaultId, rank = 2),
        ),
        rateSources = setOf(
            TestEarnRateSource(yieldModeId, rank = 3),
            TestEarnRateSource(stakeKitId, rank = 4),
        ),
    )

    private val selector = EarnHeadlineSelector(
        order = EarnSummaryOrder(registry),
        rateBadgeConverter = EarnRateBadgeConverter(),
        registry = registry,
    )

    private var previousLocale: Locale = Locale.getDefault()

    @BeforeEach
    fun pinLocale() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(previousLocale)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Select {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN products and rate sources WHEN select THEN best live candidate is the headline`(
            model: SelectModel,
        ) {
            // Act
            val actual = selector.select(model.products + model.rateSources)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            SelectModel(
                description = "two inactive products, the higher rate is listed first",
                products = listOf(
                    product(lendingId, rate = "0.045"),
                    product(stakingId, rate = "0.032"),
                ),
                expected = apyHeadline(fraction = "0.045", percent = "4.50"),
            ),
            SelectModel(
                description = "two inactive products, the higher rate is listed last",
                products = listOf(
                    product(stakingId, rate = "0.032"),
                    product(lendingId, rate = "0.045"),
                ),
                expected = apyHeadline(fraction = "0.045", percent = "4.50"),
            ),
            SelectModel(
                description = "active staking 3.2% and inactive lending 4.5%",
                products = listOf(
                    product(lendingId, rate = "0.045"),
                    product(stakingId, rate = "0.032", isActive = true),
                ),
                expected = apyHeadline(fraction = "0.032", percent = "3.20", isActive = true),
            ),
            SelectModel(
                description = "two active products with different rates",
                products = listOf(
                    product(lendingId, rate = "0.03", isActive = true),
                    product(stakingId, rate = "0.05", isActive = true),
                ),
                expected = apyHeadline(fraction = "0.05", percent = "5.00", isActive = true),
            ),
            SelectModel(
                description = "two active products with equal rates, the higher rank is listed first",
                products = listOf(
                    product(stakingId, rate = "0.04", isActive = true, type = EarnRateType.APR),
                    product(lendingId, rate = "0.04", isActive = true),
                ),
                expected = apyHeadline(fraction = "0.04", percent = "4.00", isActive = true),
            ),
            SelectModel(
                description = "two inactive products with equal rates, the higher rank is listed first",
                products = listOf(
                    product(stakingId, rate = "0.04", type = EarnRateType.APR),
                    product(lendingId, rate = "0.04"),
                ),
                expected = apyHeadline(fraction = "0.04", percent = "4.00"),
            ),
            SelectModel(
                description = "no active product, staking 3.1% and lending 2.4%",
                products = listOf(
                    product(stakingId, rate = "0.031"),
                    product(lendingId, rate = "0.024"),
                ),
                expected = apyHeadline(fraction = "0.031", percent = "3.10"),
            ),
            SelectModel(
                description = "no active product, staking 3.1% and lending 3.5%",
                products = listOf(
                    product(stakingId, rate = "0.031"),
                    product(lendingId, rate = "0.035"),
                ),
                expected = apyHeadline(fraction = "0.035", percent = "3.50"),
            ),
            SelectModel(
                description = "active lending and inactive vault with a higher rate",
                products = listOf(
                    product(lendingId, rate = "0.03", isActive = true),
                    product(vaultId, rate = "0.08"),
                ),
                expected = apyHeadline(fraction = "0.03", percent = "3.00", isActive = true),
            ),
            SelectModel(
                description = "active product without a rate and an inactive one with a rate",
                products = listOf(
                    product(lendingId, rate = null, isActive = true),
                    product(vaultId, rate = "0.04"),
                ),
                expected = apyHeadline(fraction = "0.04", percent = "4.00"),
            ),
            SelectModel(
                description = "winner with the APR type gets the APR badge",
                products = listOf(
                    product(lendingId, rate = "0.03"),
                    product(stakingId, rate = "0.05", type = EarnRateType.APR),
                ),
                expected = aprHeadline(fraction = "0.05", percent = "5.00"),
            ),
            SelectModel(
                description = "token: active staking and inactive Yield Mode with a higher rate",
                products = listOf(product(stakingId, rate = "0.03", isActive = true)),
                rateSources = listOf(rateSource(yieldModeId, rate = "0.06", route = yieldModeRoute)),
                expected = apyHeadline(fraction = "0.03", percent = "3.00", isActive = true),
            ),
            SelectModel(
                description = "token: active Yield Mode and inactive staking with a higher rate",
                products = listOf(product(stakingId, rate = "0.06")),
                rateSources = listOf(rateSource(yieldModeId, rate = "0.03", route = yieldModeRoute, isActive = true)),
                expected = apyHeadline(fraction = "0.03", percent = "3.00", isActive = true, route = yieldModeRoute),
            ),
            SelectModel(
                description = "token: two inactive, Yield Mode has the higher rate",
                products = listOf(product(stakingId, rate = "0.03")),
                rateSources = listOf(rateSource(yieldModeId, rate = "0.06", route = yieldModeRoute)),
                expected = apyHeadline(fraction = "0.06", percent = "6.00", route = yieldModeRoute),
            ),
            SelectModel(
                description = "token: two inactive, staking has the higher rate",
                products = listOf(product(stakingId, rate = "0.05")),
                rateSources = listOf(rateSource(yieldModeId, rate = "0.03", route = yieldModeRoute)),
                expected = apyHeadline(fraction = "0.05", percent = "5.00"),
            ),
            SelectModel(
                description = "token: two inactive with equal rates, staking (rank 1) beats Yield Mode (rank 3)",
                products = listOf(product(stakingId, rate = "0.04", type = EarnRateType.APR)),
                rateSources = listOf(rateSource(yieldModeId, rate = "0.04", route = yieldModeRoute)),
                expected = aprHeadline(fraction = "0.04", percent = "4.00"),
            ),
            SelectModel(
                description = "unavailable product without a position has the highest rate",
                products = listOf(
                    product(lendingId, rate = "0.03"),
                    product(vaultId, rate = "0.09", availability = ProductAvailability.Unavailable),
                ),
                expected = apyHeadline(fraction = "0.03", percent = "3.00"),
            ),
            SelectModel(
                description = "full product without a position has the highest rate",
                products = listOf(
                    product(lendingId, rate = "0.03"),
                    product(vaultId, rate = "0.09", availability = ProductAvailability.Full),
                ),
                expected = apyHeadline(fraction = "0.03", percent = "3.00"),
            ),
            SelectModel(
                description = "the only product is unavailable without a position",
                products = listOf(product(vaultId, rate = "0.09", availability = ProductAvailability.Unavailable)),
                expected = null,
            ),
            SelectModel(
                description = "the only product is full without a position",
                products = listOf(product(vaultId, rate = "0.09", availability = ProductAvailability.Full)),
                expected = null,
            ),
            SelectModel(
                description = "the only product is full with a position",
                products = listOf(
                    product(vaultId, rate = "0.05", isActive = true, availability = ProductAvailability.Full),
                ),
                expected = apyHeadline(fraction = "0.05", percent = "5.00", isActive = true),
            ),
            SelectModel(
                description = "the only product is unavailable with a position",
                products = listOf(
                    product(vaultId, rate = "0.05", isActive = true, availability = ProductAvailability.Unavailable),
                ),
                expected = apyHeadline(fraction = "0.05", percent = "5.00", isActive = true),
            ),
            SelectModel(
                description = "the only product is available without a rate",
                products = listOf(product(lendingId, rate = null)),
                expected = null,
            ),
            SelectModel(
                description = "no products and no rate sources",
                products = emptyList(),
                expected = null,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SelectRoute {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN winner WHEN select THEN headline route is the route of the winner`(model: SelectModel) {
            // Act
            val actual = selector.select(model.products + model.rateSources)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            SelectModel(
                description = "a rate source with a route wins",
                products = listOf(product(lendingId, rate = "0.03")),
                rateSources = listOf(rateSource(yieldModeId, rate = "0.06", route = yieldModeRoute)),
                expected = apyHeadline(fraction = "0.06", percent = "6.00", route = yieldModeRoute),
            ),
            SelectModel(
                description = "a product with a route wins (Pooled staking)",
                products = listOf(
                    product(lendingId, rate = "0.03"),
                    product(stakingId, rate = "0.06", route = pooledStakingRoute),
                ),
                expected = apyHeadline(fraction = "0.06", percent = "6.00", route = pooledStakingRoute),
            ),
            SelectModel(
                description = "a product without a route wins (Lending) while a rate source with a route loses",
                products = listOf(
                    product(lendingId, rate = "0.05"),
                    product(vaultId, rate = "0.03"),
                ),
                rateSources = listOf(rateSource(yieldModeId, rate = "0.02", route = yieldModeRoute)),
                expected = apyHeadline(fraction = "0.05", percent = "5.00"),
            ),
            SelectModel(
                description = "a product without a route wins (Vault) while Pooled staking loses",
                products = listOf(
                    product(vaultId, rate = "0.07"),
                    product(lendingId, rate = "0.04"),
                    product(stakingId, rate = "0.03", route = pooledStakingRoute),
                ),
                expected = apyHeadline(fraction = "0.07", percent = "7.00"),
            ),
            SelectModel(
                description = "two rate sources, StakeKit has the higher rate",
                products = emptyList(),
                rateSources = listOf(
                    rateSource(yieldModeId, rate = "0.04", route = yieldModeRoute),
                    rateSource(stakeKitId, rate = "0.05", route = stakeKitRoute),
                ),
                expected = apyHeadline(fraction = "0.05", percent = "5.00", route = stakeKitRoute),
            ),
            SelectModel(
                description = "two rate sources, Yield Mode has the higher rate",
                products = emptyList(),
                rateSources = listOf(
                    rateSource(yieldModeId, rate = "0.06", route = yieldModeRoute),
                    rateSource(stakeKitId, rate = "0.05", route = stakeKitRoute),
                ),
                expected = apyHeadline(fraction = "0.06", percent = "6.00", route = yieldModeRoute),
            ),
            SelectModel(
                description = "two rate sources with equal rates, Yield Mode (rank 3) beats StakeKit (rank 4)",
                products = emptyList(),
                rateSources = listOf(
                    rateSource(stakeKitId, rate = "0.05", route = stakeKitRoute),
                    rateSource(yieldModeId, rate = "0.05", route = yieldModeRoute),
                ),
                expected = apyHeadline(fraction = "0.05", percent = "5.00", route = yieldModeRoute),
            ),
            SelectModel(
                description = "two rate sources, the active Yield Mode beats StakeKit with the higher rate",
                products = emptyList(),
                rateSources = listOf(
                    rateSource(yieldModeId, rate = "0.03", route = yieldModeRoute, isActive = true),
                    rateSource(stakeKitId, rate = "0.05", route = stakeKitRoute),
                ),
                expected = apyHeadline(fraction = "0.03", percent = "3.00", isActive = true, route = yieldModeRoute),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class BestActive {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN products WHEN bestActive THEN best active product by order or null`(model: BestActiveModel) {
            // Act
            val actual = selector.bestActive(model.summaries)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<BestActiveModel> {
            val activeLending = product(lendingId, rate = "0.03", isActive = true)
            val activeStaking = product(stakingId, rate = "0.05", isActive = true)
            val activeStakingSameRate = product(stakingId, rate = "0.03", isActive = true)
            val activeLendingWithoutRate = product(lendingId, rate = null, isActive = true)
            val activeFullVault = product(
                vaultId,
                rate = "0.02",
                isActive = true,
                availability = ProductAvailability.Full,
            )
            val inactiveVault = product(vaultId, rate = "0.09")
            val inactiveStaking = product(stakingId, rate = "0.04")

            return listOf(
                BestActiveModel(
                    description = "active lending and inactive vault with a higher rate",
                    summaries = listOf(inactiveVault, activeLending),
                    expected = activeLending,
                ),
                BestActiveModel(
                    description = "two active products with different rates and an inactive one with the highest",
                    summaries = listOf(activeLending, inactiveVault, activeStaking),
                    expected = activeStaking,
                ),
                BestActiveModel(
                    description = "two active products with equal rates, the higher rank is listed first",
                    summaries = listOf(activeStakingSameRate, activeLending),
                    expected = activeLending,
                ),
                BestActiveModel(
                    description = "active product without a rate and active product with a rate",
                    summaries = listOf(activeLendingWithoutRate, activeFullVault),
                    expected = activeFullVault,
                ),
                BestActiveModel(
                    description = "the only active product has no rate",
                    summaries = listOf(inactiveStaking, activeLendingWithoutRate),
                    expected = activeLendingWithoutRate,
                ),
                BestActiveModel(
                    description = "no active products",
                    summaries = listOf(inactiveVault, inactiveStaking),
                    expected = null,
                ),
                BestActiveModel(
                    description = "no products",
                    summaries = emptyList(),
                    expected = null,
                ),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class BadgeAccentOfWinner {

        private val infoProductId = EarnProductId("info_product")
        private val successProductId = EarnProductId("success_product")
        private val infoSourceId = EarnProductId("info_source")
        private val successSourceId = EarnProductId("success_source")

        private val accentRegistry = EarnProductsRegistry(
            products = setOf(
                TestEarnProduct(infoProductId, rank = 0, badgeAccent = EarnBadgeAccent.Info),
                TestEarnProduct(successProductId, rank = 1, badgeAccent = EarnBadgeAccent.Success),
            ),
            rateSources = setOf(
                TestEarnRateSource(infoSourceId, rank = 2, badgeAccent = EarnBadgeAccent.Info),
                TestEarnRateSource(successSourceId, rank = 3, badgeAccent = EarnBadgeAccent.Success),
            ),
        )

        private val accentSelector = EarnHeadlineSelector(
            order = EarnSummaryOrder(accentRegistry),
            rateBadgeConverter = EarnRateBadgeConverter(),
            registry = accentRegistry,
        )

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN candidates WHEN select THEN the headline takes the badge accent of the winner`(
            model: AccentModel,
        ) {
            // Act
            val actual = accentSelector.select(model.candidates)?.badgeAccent

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            AccentModel(
                description = "a product with accent Info wins",
                candidates = listOf(product(infoProductId, rate = "0.05")),
                expected = EarnBadgeAccent.Info,
            ),
            AccentModel(
                description = "a product with accent Success wins",
                candidates = listOf(product(successProductId, rate = "0.05")),
                expected = EarnBadgeAccent.Success,
            ),
            AccentModel(
                description = "a rate source with accent Info wins",
                candidates = listOf(rateSource(infoSourceId, rate = "0.05", route = null)),
                expected = EarnBadgeAccent.Info,
            ),
            AccentModel(
                description = "a rate source with accent Success wins",
                candidates = listOf(rateSource(successSourceId, rate = "0.05", route = null)),
                expected = EarnBadgeAccent.Success,
            ),
            AccentModel(
                description = "an active Info product beats an inactive Success source with a better rate",
                candidates = listOf(
                    product(infoProductId, rate = "0.03", isActive = true),
                    rateSource(successSourceId, rate = "0.09", route = null),
                ),
                expected = EarnBadgeAccent.Info,
            ),
            AccentModel(
                description = "the Success source with the best rate beats an inactive Info product",
                candidates = listOf(
                    product(infoProductId, rate = "0.03"),
                    rateSource(successSourceId, rate = "0.09", route = null),
                ),
                expected = EarnBadgeAccent.Success,
            ),
        )
    }

    internal data class AccentModel(
        val description: String,
        val candidates: List<EarnProductSummary>,
        val expected: EarnBadgeAccent,
    ) {
        override fun toString(): String = description
    }

    private fun product(
        id: EarnProductId,
        rate: String?,
        isActive: Boolean = false,
        availability: ProductAvailability = ProductAvailability.Available,
        type: EarnRateType = EarnRateType.APY,
        route: Route? = null,
    ) = createSummary(
        productId = id,
        rate = rate?.let { createRate(fraction = it, type = type) },
        availability = availability,
        position = if (isActive) EarnPositionSummary(BigDecimal.ONE) else null,
        route = route,
    )

    private fun rateSource(id: EarnProductId, rate: String, route: Route?, isActive: Boolean = false) =
        product(id, rate = rate, isActive = isActive, route = route)

    private fun apyHeadline(fraction: String, percent: String, isActive: Boolean = false, route: Route? = null) =
        EarnHeadline(
            rate = createRate(fraction = fraction, type = EarnRateType.APY),
            isActive = isActive,
            badge = resourceReference(R.string.yield_module_earn_badge, wrappedList(percent)),
            badgeAccent = EarnBadgeAccent.Success,
            route = route,
        )

    private fun aprHeadline(fraction: String, percent: String, isActive: Boolean = false, route: Route? = null) =
        EarnHeadline(
            rate = createRate(fraction = fraction, type = EarnRateType.APR),
            isActive = isActive,
            badge = resourceReference(R.string.staking_apr_earn_badge, wrappedList(percent)),
            badgeAccent = EarnBadgeAccent.Success,
            route = route,
        )

    internal data class SelectModel(
        val description: String,
        val products: List<EarnProductSummary>,
        val rateSources: List<EarnProductSummary> = emptyList(),
        val expected: EarnHeadline?,
    ) {
        override fun toString(): String = description
    }

    internal data class BestActiveModel(
        val description: String,
        val summaries: List<EarnProductSummary>,
        val expected: EarnProductSummary?,
    ) {
        override fun toString(): String = description
    }
}