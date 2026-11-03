package com.tangem.features.earn.impl.fake

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.product.api.navigation.EarnFlowRoute
import com.tangem.features.earn.product.api.navigation.EarnInfoRoute
import com.tangem.features.earn.product.api.navigation.EarnPickerRoute
import com.tangem.features.earn.product.api.navigation.EarnRoute
import com.tangem.features.earn.product.api.product.EarnActionItem
import com.tangem.features.earn.product.api.product.EarnBlock
import com.tangem.features.earn.product.api.product.EarnBlockAction
import com.tangem.features.earn.product.api.product.EarnPeriod
import com.tangem.features.earn.product.api.product.EarnPositionState
import com.tangem.features.earn.product.api.product.EarnPositionSummary
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.features.earn.product.api.session.EarnAction
import com.tangem.features.earn.product.api.session.EarnFlowAction
import com.tangem.features.earn.product.api.session.Withdraw
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.assertEitherLeft
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FakeEarnProductTest {

    private val params = EarnProduct.Params(
        userWalletId = UserWalletId("0123456789abcdef"),
        currency = mockk<CryptoCurrency>(),
    )
    private val ethId = CryptoCurrency.ID.fromValue("coin⟨ETH⟩ethereum")
    private val fakeRate = EarnProductRate(fraction = BigDecimal("0.055"), type = EarnRateType.APY)

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN preset WHEN product created THEN id and rank are stable`(model: IdentityModel) {
        // Act
        val product = createProduct(model.preset)

        // Assert
        assertThat(product.id.value to product.rank).isEqualTo(model.expectedId to model.expectedRank)
    }

    private fun provideTestModels() = listOf(
        IdentityModel(FakeEarnProductPreset.Lending, expectedId = "lending", expectedRank = 0),
        IdentityModel(FakeEarnProductPreset.Staking, expectedId = "staking", expectedRank = 1),
        IdentityModel(FakeEarnProductPreset.Vault, expectedId = "vault", expectedRank = 2),
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class EveryPreset {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN preset WHEN state THEN Ready with the fake apy rate`(preset: FakeEarnProductPreset) = runTest {
            // Arrange
            val product = createProduct(preset)

            // Act
            val actual = product.readyState().rate

            // Assert
            assertThat(actual).isEqualTo(fakeRate)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN preset WHEN summaries THEN rate text and rate are the fake apy`(preset: FakeEarnProductPreset) =
            runTest {
                // Arrange
                val product = createProduct(preset)

                // Act
                val summary = product.summary()

                // Assert
                assertThat(summary.rateText to summary.rate).isEqualTo(stringReference("5.5% APY") to fakeRate)
            }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN preset WHEN summaries THEN the only entry is keyed by the ETH currency`(
            preset: FakeEarnProductPreset,
        ) = runTest {
            // Arrange
            val product = createProduct(preset)

            // Act
            val actual = product.summaries(params.userWalletId).first().keys

            // Assert
            assertThat(actual).containsExactly(ethId)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN preset WHEN action handler created THEN nothing is routed`(preset: FakeEarnProductPreset) {
            // Arrange
            val product = createProduct(preset)
            val navigator = FakeEarnNavigator()

            // Act
            product.actionHandler(params, navigator)

            // Assert
            assertThat(navigator.routes).isEmpty()
        }

        private fun provideTestModels() = FakeEarnProductPreset.entries.toList()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class StateContent {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN preset WHEN state THEN projection, how it works and remaining cap follow the preset`(
            model: StateContentModel,
        ) = runTest {
            // Arrange
            val product = createProduct(model.preset)

            // Act
            val state = product.readyState()

            // Assert
            val actual = Triple(state.projection != null, state.howItWorks != null, state.limits.remainingCap != null)
            val expected = Triple(model.hasProjection, model.hasHowItWorks, model.hasRemainingCap)
            assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            StateContentModel(
                preset = FakeEarnProductPreset.Lending,
                hasProjection = true,
                hasHowItWorks = true,
                hasRemainingCap = false,
            ),
            StateContentModel(
                preset = FakeEarnProductPreset.Staking,
                hasProjection = false,
                hasHowItWorks = false,
                hasRemainingCap = false,
            ),
            StateContentModel(
                preset = FakeEarnProductPreset.Vault,
                hasProjection = true,
                hasHowItWorks = true,
                hasRemainingCap = true,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class LendingPosition {

        @Test
        fun `GIVEN no position WHEN state THEN start earning is offered without a block and nothing secondary`() =
            runTest {
                // Arrange
                val product = createProduct(FakeEarnProductPreset.Lending, FakeEarnPosition.None)

                // Act
                val state = product.readyState()

                // Assert
                val expectedPrimary = EarnActionItem(
                    action = Deposit(),
                    block = null,
                    label = stringReference("Start earning"),
                )
                assertThat(state.position).isEqualTo(EarnPositionState.None)
                assertThat(state.primary).isEqualTo(expectedPrimary)
                assertThat(state.secondary).isEmpty()
            }

        @Test
        fun `GIVEN known position WHEN state THEN claim reward, add funds and withdraw are offered`() = runTest {
            // Arrange
            val product = createProduct(FakeEarnProductPreset.Lending, FakeEarnPosition.Known)

            // Act
            val state = product.readyState()

            // Assert
            val expectedPrimary = EarnActionItem(
                action = Deposit(),
                block = null,
                label = stringReference("Add funds"),
            )
            val position = state.position
            assertThat(position).isInstanceOf(EarnPositionState.Known::class.java)
            assertThat((position as EarnPositionState.Known).position.rewards?.claimAction)
                .isEqualTo(fakeClaimAction())
            assertThat(state.primary).isEqualTo(expectedPrimary)
            assertThat(state.secondary).containsExactly(EarnActionItem(Withdraw()))
        }

        @Test
        fun `GIVEN unknown position WHEN state THEN start earning is blocked with retry and nothing secondary`() =
            runTest {
                // Arrange
                val product = createProduct(FakeEarnProductPreset.Lending, FakeEarnPosition.Unknown)

                // Act
                val state = product.readyState()

                // Assert
                val expectedPrimary = EarnActionItem(
                    action = Deposit(),
                    block = EarnBlock(
                        reason = stringReference("Position is not loaded"),
                        action = EarnBlockAction.Retry,
                    ),
                    label = stringReference("Start earning"),
                )
                assertThat(state.position).isEqualTo(EarnPositionState.Unknown)
                assertThat(state.primary).isEqualTo(expectedPrimary)
                assertThat(state.secondary).isEmpty()
            }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN position behavior WHEN summaries THEN position summary only for a known position`(
            model: PositionModel,
        ) = runTest {
            // Arrange
            val product = createProduct(FakeEarnProductPreset.Lending, model.position)

            // Act
            val actual = product.summary().position

            // Assert
            assertThat(actual).isEqualTo(model.expectedSummaryPosition)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN position behavior WHEN summaries THEN source is not actual when the position was not read`(
            model: PositionModel,
        ) = runTest {
            // Arrange
            val product = createProduct(FakeEarnProductPreset.Lending, model.position)

            // Act
            val actual = product.summary().source

            // Assert
            assertThat(actual).isEqualTo(model.expectedSource)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN position behavior WHEN summary click THEN info route if active and picker route if not`(
            model: PositionModel,
        ) = runTest {
            // Arrange
            val product = createProduct(FakeEarnProductPreset.Lending, model.position)
            val navigator = FakeEarnNavigator()
            val action = product.summary().onClickAction

            // Act
            product.actionHandler(params, navigator).onActionClick(action)

            // Assert
            assertThat(navigator.routes).isEqualTo(listOf(model.expectedClickRoute(product.id)))
        }

        private fun provideTestModels() = listOf(
            PositionModel(
                position = FakeEarnPosition.None,
                expectedSummaryPosition = null,
                expectedSource = StatusSource.ACTUAL,
                expectedClickRoute = { EarnPickerRoute(preselected = it) },
            ),
            PositionModel(
                position = FakeEarnPosition.Unknown,
                expectedSummaryPosition = null,
                expectedSource = StatusSource.ONLY_CACHE,
                expectedClickRoute = { EarnPickerRoute(preselected = it) },
            ),
            PositionModel(
                position = FakeEarnPosition.Known,
                expectedSummaryPosition = EarnPositionSummary(cryptoAmount = BigDecimal("1.482")),
                expectedSource = StatusSource.ACTUAL,
                expectedClickRoute = { EarnInfoRoute(productId = it) },
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class KnownPosition {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN known position WHEN state THEN secondary actions and rewards follow the preset`(
            model: KnownPositionModel,
        ) = runTest {
            // Arrange
            val product = createProduct(model.preset, FakeEarnPosition.Known)

            // Act
            val state = product.readyState()

            // Assert
            val rewards = (state.position as EarnPositionState.Known).position.rewards
            assertThat(state.secondary to (rewards != null)).isEqualTo(model.expectedSecondary to model.hasRewards)
        }

        private fun provideTestModels() = listOf(
            KnownPositionModel(
                preset = FakeEarnProductPreset.Lending,
                expectedSecondary = listOf(EarnActionItem(Withdraw())),
                hasRewards = true,
            ),
            KnownPositionModel(
                preset = FakeEarnProductPreset.Staking,
                expectedSecondary = emptyList(),
                hasRewards = false,
            ),
            KnownPositionModel(
                preset = FakeEarnProductPreset.Vault,
                expectedSecondary = listOf(EarnActionItem(Withdraw())),
                hasRewards = false,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ActionClick {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN action of the product WHEN onActionClick THEN exactly one route is navigated`(model: ClickModel) =
            runTest {
                // Arrange
                val product = createProduct(model.preset, model.position)
                val navigator = FakeEarnNavigator()
                val action = when (model.source) {
                    ActionSource.SummaryClick -> product.summary().onClickAction
                    ActionSource.Primary -> product.readyState().primary.action
                }

                // Act
                product.actionHandler(params, navigator).onActionClick(action)

                // Assert
                assertThat(navigator.routes).hasSize(1)
            }

        private fun provideTestModels() = FakeEarnProductPreset.entries.flatMap { preset ->
            FakeEarnPosition.entries.flatMap { position ->
                ActionSource.entries.map { source -> ClickModel(preset, position, source) }
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class HandlerLifecycle {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN two handlers WHEN the second is clicked THEN only the second navigator gets the route`(
            model: RoutingModel,
        ) {
            // Arrange
            val product = createProduct(model.preset)
            val firstNavigator = FakeEarnNavigator()
            val secondNavigator = FakeEarnNavigator()
            product.actionHandler(params, firstNavigator)
            val secondHandler = product.actionHandler(params, secondNavigator)

            // Act
            secondHandler.onActionClick(model.action)

            // Assert
            assertThat(firstNavigator.routes).isEmpty()
            assertThat(secondNavigator.routes).isEqualTo(listOf(model.expectedRoute(product)))
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN two handlers WHEN the first is clicked later THEN it still routes into the first navigator`(
            model: RoutingModel,
        ) {
            // Arrange
            val product = createProduct(model.preset)
            val firstNavigator = FakeEarnNavigator()
            val secondNavigator = FakeEarnNavigator()
            val firstHandler = product.actionHandler(params, firstNavigator)
            product.actionHandler(params, secondNavigator)

            // Act
            firstHandler.onActionClick(model.action)

            // Assert
            assertThat(firstNavigator.routes).isEqualTo(listOf(model.expectedRoute(product)))
            assertThat(secondNavigator.routes).isEmpty()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN two handlers WHEN both are clicked in turn THEN each navigator gets exactly its own route`(
            model: RoutingModel,
        ) {
            // Arrange
            val product = createProduct(model.preset)
            val firstNavigator = FakeEarnNavigator()
            val secondNavigator = FakeEarnNavigator()
            val firstHandler = product.actionHandler(params, firstNavigator)
            val secondHandler = product.actionHandler(params, secondNavigator)

            // Act
            firstHandler.onActionClick(model.action)
            secondHandler.onActionClick(model.action)

            // Assert
            assertThat(firstNavigator.routes).isEqualTo(listOf(model.expectedRoute(product)))
            assertThat(secondNavigator.routes).isEqualTo(listOf(model.expectedRoute(product)))
        }

        private fun provideTestModels() = FakeEarnProductPreset.entries.map { preset ->
            RoutingModel(preset, Deposit()) { product ->
                if (preset == FakeEarnProductPreset.Staking) {
                    FakeForeignRoute
                } else {
                    EarnFlowRoute(productId = product.id, action = Deposit())
                }
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class UnknownActionOnFlowProducts {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN action the handler does not know WHEN onActionClick THEN IllegalStateException and no route`(
            preset: FakeEarnProductPreset,
        ) {
            // Arrange
            val navigator = FakeEarnNavigator()
            val handler = createProduct(preset).actionHandler(params, navigator)

            // Act
            val actual = runCatching { handler.onActionClick(MysteryAction) }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(IllegalStateException::class.java)
            assertThat(actual).hasMessageThat().contains(MysteryAction.id)
            assertThat(navigator.routes).isEmpty()
        }

        private fun provideTestModels() = listOf(FakeEarnProductPreset.Lending, FakeEarnProductPreset.Vault)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ActionRouting {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN preset and action WHEN onActionClick THEN exactly the expected route is navigated`(
            model: RoutingModel,
        ) {
            // Arrange
            val product = createProduct(model.preset)
            val navigator = FakeEarnNavigator()

            // Act
            product.actionHandler(params, navigator).onActionClick(model.action)

            // Assert
            assertThat(navigator.routes).isEqualTo(listOf(model.expectedRoute(product)))
        }

        private fun provideTestModels(): List<RoutingModel> {
            val flowActions: List<EarnFlowAction> = listOf(Deposit(), Withdraw(), fakeClaimAction())
            val stakingActions: List<EarnAction> = flowActions + MysteryAction

            val flowProducts = listOf(FakeEarnProductPreset.Lending, FakeEarnProductPreset.Vault).flatMap { preset ->
                flowActions.map { action ->
                    RoutingModel(preset, action) { product -> EarnFlowRoute(productId = product.id, action = action) }
                }
            }
            val staking = stakingActions.map { action ->
                RoutingModel(FakeEarnProductPreset.Staking, action) { FakeForeignRoute }
            }

            return flowProducts + staking
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class LegacyBlock {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN preset WHEN product and summaries read THEN legacy block flag and summary route match`(
            model: LegacyBlockModel,
        ) = runTest {
            // Arrange
            val product = createProduct(model.preset)

            // Act
            val summary = product.summary()

            // Assert
            assertThat(product.hasLegacyBlock to summary.route)
                .isEqualTo(model.hasLegacyBlock to model.expectedSummaryRoute)
        }

        private fun provideTestModels() = listOf(
            LegacyBlockModel(FakeEarnProductPreset.Lending, hasLegacyBlock = false, expectedSummaryRoute = null),
            LegacyBlockModel(
                preset = FakeEarnProductPreset.Staking,
                hasLegacyBlock = true,
                expectedSummaryRoute = FakeForeignRoute,
            ),
            LegacyBlockModel(FakeEarnProductPreset.Vault, hasLegacyBlock = false, expectedSummaryRoute = null),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ApyHistory {

        @Test
        fun `GIVEN vault WHEN apyHistory THEN Left without an exception`() = runTest {
            // Arrange
            val product = createProduct(FakeEarnProductPreset.Vault)
            val period = product.readyState().apyPeriods.first()

            // Act
            val actual = product.apyHistory(params, period)

            // Assert
            assertEitherLeft(actual, IllegalStateException("fake"))
        }

        @Test
        fun `GIVEN lending WHEN apyHistory for the first two periods THEN two different Right results`() = runTest {
            // Arrange
            val product = createProduct(FakeEarnProductPreset.Lending)
            val periods = product.readyState().apyPeriods.take(2)

            // Act
            val actual = periods.map { product.apyHistory(params, it) }

            // Assert
            assertThat(actual.map { it.isRight() }).containsExactly(true, true)
            assertThat(actual.first()).isNotEqualTo(actual.last())
        }

        @Test
        fun `GIVEN lending WHEN apyHistory for an unknown period THEN Left`() = runTest {
            // Arrange
            val product = createProduct(FakeEarnProductPreset.Lending)

            // Act
            val actual = product.apyHistory(params, EarnPeriod(stringReference("9Y")))

            // Assert
            assertEitherLeft(actual, IllegalArgumentException("Unknown period"))
        }
    }

    private fun createProduct(
        preset: FakeEarnProductPreset,
        position: FakeEarnPosition = FakeEarnPosition.None,
    ): EarnProduct = createFakeEarnProduct(preset, FakeEarnProductBehavior(position))

    private suspend fun EarnProduct.readyState(): EarnProductState.Ready {
        val state = state(params).first()
        return state as? EarnProductState.Ready ?: error("Expected Ready but was $state")
    }

    private suspend fun EarnProduct.summary(): EarnProductSummary =
        summaries(params.userWalletId).first().getValue(ethId)

    internal data class IdentityModel(
        val preset: FakeEarnProductPreset,
        val expectedId: String,
        val expectedRank: Int,
    ) {
        override fun toString(): String = "$preset"
    }

    internal data class StateContentModel(
        val preset: FakeEarnProductPreset,
        val hasProjection: Boolean,
        val hasHowItWorks: Boolean,
        val hasRemainingCap: Boolean,
    ) {
        override fun toString(): String = "$preset"
    }

    internal data class PositionModel(
        val position: FakeEarnPosition,
        val expectedSummaryPosition: EarnPositionSummary?,
        val expectedSource: StatusSource,
        val expectedClickRoute: (EarnProductId) -> EarnRoute,
    ) {
        override fun toString(): String = "$position"
    }

    internal data class KnownPositionModel(
        val preset: FakeEarnProductPreset,
        val expectedSecondary: List<EarnActionItem>,
        val hasRewards: Boolean,
    ) {
        override fun toString(): String = "$preset"
    }

    internal data class ClickModel(
        val preset: FakeEarnProductPreset,
        val position: FakeEarnPosition,
        val source: ActionSource,
    ) {
        override fun toString(): String = "$preset $position $source"
    }

    internal data class RoutingModel(
        val preset: FakeEarnProductPreset,
        val action: EarnAction,
        val expectedRoute: (EarnProduct) -> Route,
    ) {
        override fun toString(): String = "$preset ${action.id}"
    }

    internal data class LegacyBlockModel(
        val preset: FakeEarnProductPreset,
        val hasLegacyBlock: Boolean,
        val expectedSummaryRoute: Route?,
    ) {
        override fun toString(): String = "$preset"
    }

    internal enum class ActionSource { SummaryClick, Primary }
}

private object MysteryAction : EarnAction {

    override val id: String = "mystery"

    override val label: TextReference = stringReference("Mystery")
}