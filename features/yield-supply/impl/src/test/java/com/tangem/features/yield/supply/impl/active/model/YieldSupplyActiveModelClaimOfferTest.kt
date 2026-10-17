package com.tangem.features.yield.supply.impl.active.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.yield.supply.models.YieldBoostStatus
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.mock.yield.MockYieldSupply
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldSupplyActiveModelClaimOfferTest : YieldSupplyActiveModelTestBase() {

    @BeforeEach
    fun setUp() {
        every { network.rawId } returns AVALANCHE_NETWORK_ID
        coEvery { getYieldBoostStatusUseCase(any(), any()) } returns YieldBoostStatus.NotStarted.right()
        every { claimTracker.isClaimInFlight(any(), any()) } returns flowOf(false)
        coEvery { yieldSupplyGetTokenStatusUseCase(any()) } returns MockYieldSupply.market().right()
        coEvery { getYieldRewardsSummaryUseCase(any(), any()) } returns MockYieldSupply.claimableSummary().right()
    }

    @Test
    fun `GIVEN claimable rewards WHEN stop earning is tapped THEN the claim sheet is offered`() = runTest {
        // Arrange
        val model = createModelWithAddress()
        val slot = FakeSlotHost(model.slotNavigation)
        advanceUntilIdle()

        // Act
        model.onStopEarning()
        advanceUntilIdle()

        // Assert
        assertThat(slot.configuration).isEqualTo(
            YieldSupplyActiveRoute.Claim(chainId = MockYieldSupply.market().chainId, userAddress = USER_ADDRESS),
        )

        model.onDestroy()
    }

    @Test
    fun `GIVEN the claim was already offered WHEN stop earning is tapped again THEN the withdrawal opens`() =
        runTest {
            // Arrange
            val model = createModelWithAddress()
            val slot = FakeSlotHost(model.slotNavigation)
            advanceUntilIdle()
            model.onStopEarning()
            advanceUntilIdle()

            // Act
            model.onStopEarning()
            advanceUntilIdle()

            // Assert
            assertThat(slot.configuration).isEqualTo(YieldSupplyActiveRoute.Exit)

            model.onDestroy()
        }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN nothing to offer WHEN stop earning is tapped THEN the withdrawal opens`(model: NoOfferModel) = runTest {
        // Arrange
        model.arrange(this@YieldSupplyActiveModelClaimOfferTest)
        val activeModel = createModelWithAddress()
        val slot = FakeSlotHost(activeModel.slotNavigation)
        advanceUntilIdle()

        // Act
        activeModel.onStopEarning()
        advanceUntilIdle()

        // Assert
        assertThat(slot.configuration).isEqualTo(YieldSupplyActiveRoute.Exit)

        activeModel.onDestroy()
    }

    @Test
    fun `GIVEN a network that batches WHEN stop earning is tapped THEN the withdrawal opens without a rewards lookup`() =
        runTest {
            // Arrange
            every { network.rawId } returns NETWORK_ID
            val model = createModelWithAddress()
            val slot = FakeSlotHost(model.slotNavigation)
            advanceUntilIdle()
            forgetRewardsLookups()

            // Act
            model.onStopEarning()
            advanceUntilIdle()

            // Assert
            assertThat(slot.configuration).isEqualTo(YieldSupplyActiveRoute.Exit)
            coVerify(exactly = 0) { getYieldRewardsSummaryUseCase(any(), any()) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN a decision in progress WHEN stop earning is tapped again THEN the rewards are looked up once`() =
        runTest {
            // Arrange
            coEvery { getYieldRewardsSummaryUseCase(any(), any()) } coAnswers {
                delay(LOOKUP_MILLIS)
                MockYieldSupply.claimableSummary().right()
            }
            val model = createModelWithAddress()
            val slot = FakeSlotHost(model.slotNavigation)
            advanceUntilIdle()
            forgetRewardsLookups()

            // Act
            model.onStopEarning()
            model.onStopEarning()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { getYieldRewardsSummaryUseCase(any(), any()) }
            assertThat(slot.configuration).isEqualTo(
                YieldSupplyActiveRoute.Claim(chainId = MockYieldSupply.market().chainId, userAddress = USER_ADDRESS),
            )

            model.onDestroy()
        }

    @Test
    fun `GIVEN the rewards backend stalls WHEN stop earning is tapped THEN the withdrawal opens after the timeout`() =
        runTest {
            // Arrange
            coEvery { getYieldRewardsSummaryUseCase(any(), any()) } coAnswers { awaitCancellation() }
            val model = createModelWithAddress()
            val slot = FakeSlotHost(model.slotNavigation)
            advanceUntilIdle()

            // Act
            model.onStopEarning()
            advanceUntilIdle()

            // Assert
            assertThat(slot.configuration).isEqualTo(YieldSupplyActiveRoute.Exit)

            model.onDestroy()
        }

    private fun provideTestModels() = listOf(
        NoOfferModel(reason = "the backend defers the claim") {
            coEvery { getYieldRewardsSummaryUseCase(any(), any()) } returns MockYieldSupply.deferredSummary().right()
        },
        NoOfferModel(reason = "a claim is already in flight") {
            every { claimTracker.isClaimInFlight(any(), any()) } returns flowOf(true)
        },
        NoOfferModel(reason = "the market is unknown") {
            coEvery { yieldSupplyGetTokenStatusUseCase(any()) } returns IllegalStateException("no market").left()
        },
    )

    internal data class NoOfferModel(
        val reason: String,
        val arrange: YieldSupplyActiveModelClaimOfferTest.() -> Unit,
    ) {
        override fun toString(): String = reason
    }

    /** The screen reads the rewards for its banner too; only the lookups the tap makes are counted. */
    private fun forgetRewardsLookups() {
        clearMocks(getYieldRewardsSummaryUseCase, answers = false, recordedCalls = true)
    }

    /** The address the offer is keyed by arrives with the balance, so the status has to carry one. */
    private fun TestScope.createModelWithAddress(): YieldSupplyActiveModel =
        createModel(dispatchers = createTestingCoroutineDispatcherProvider()).also { model ->
            model.cryptoCurrencyStatusFlow.value = mockk(relaxed = true) {
                every { currency } returns token
                every { value.networkAddress?.defaultAddress?.value } returns USER_ADDRESS
            }
        }

    private companion object {
        const val USER_ADDRESS = "0xowner"
        const val AVALANCHE_NETWORK_ID = "avalanche"
        const val LOOKUP_MILLIS = 1_000L
    }
}