package com.tangem.domain.staking.single

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.test.mock.staking.MockStaking
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SingleStakingBalanceProducerTest {

    private val analyticsExceptionHandler: AnalyticsExceptionHandler = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(analyticsExceptionHandler)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SelectStakingBalance {

        private val data = MockStaking.stakeKitBalance()
        private val empty = StakingBalance.Empty(MockStaking.stakingId(), StatusSource.ACTUAL)
        private val error = StakingBalance.Error(MockStaking.stakingId())

        private fun select(balances: List<StakingBalance>) = SingleStakingBalanceProducer.selectStakingBalance(
            currentStakingId = MockStaking.stakingId(),
            currentBalances = balances,
            analyticsExceptionHandler = analyticsExceptionHandler,
        )

        @Test
        fun `GIVEN no balances WHEN selectStakingBalance THEN null and no exception sent`() {
            // Act
            val actual = select(emptyList())

            // Assert
            assertThat(actual).isNull()
            verify(exactly = 0) { analyticsExceptionHandler.sendException(any()) }
        }

        @Test
        fun `GIVEN single balance WHEN selectStakingBalance THEN it is returned without exception`() {
            // Act
            val actual = select(listOf(empty))

            // Assert
            assertThat(actual).isEqualTo(empty)
            verify(exactly = 0) { analyticsExceptionHandler.sendException(any()) }
        }

        @Test
        fun `GIVEN several balances with data second WHEN selectStakingBalance THEN data balance returned`() {
            // Act
            val actual = select(listOf(empty, data))

            // Assert
            assertThat(actual).isEqualTo(data)
        }

        @Test
        fun `GIVEN several balances without data WHEN selectStakingBalance THEN first returned`() {
            // Act
            val actual = select(listOf(error, empty))

            // Assert
            assertThat(actual).isEqualTo(error)
        }

        @Test
        fun `GIVEN several balances WHEN selectStakingBalance THEN multiple balances exception sent`() {
            // Arrange
            val slot = slot<ExceptionAnalyticsEvent>()
            every { analyticsExceptionHandler.sendException(capture(slot)) } just Runs

            // Act
            select(listOf(empty, data))

            // Assert
            val event = slot.captured
            assertThat(event.exception).isInstanceOf(IllegalStateException::class.java)
            assertThat(event.exception).hasMessageThat().isEqualTo("Multiple balances found for staking ID")
            assertThat(event.params.keys).containsExactly("stakingId", "balances")
            assertThat(event.params["stakingId"]).isEqualTo(MockStaking.stakingId().toString())
        }

        @Test
        fun `GIVEN several balances WHEN selectStakingBalance THEN balances param lists each balance`() {
            // Arrange
            val slot = slot<ExceptionAnalyticsEvent>()
            every { analyticsExceptionHandler.sendException(capture(slot)) } just Runs

            // Act
            select(listOf(empty, data))

            // Assert
            assertThat(slot.captured.params["balances"]).isEqualTo("$empty,$data")
        }
    }
}