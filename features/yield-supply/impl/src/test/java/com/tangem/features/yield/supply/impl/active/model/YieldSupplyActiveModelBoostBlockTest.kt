package com.tangem.features.yield.supply.impl.active.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.yield.supply.models.YieldBoostStatus
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalCoroutinesApi::class)
internal class YieldSupplyActiveModelBoostBlockTest : YieldSupplyActiveModelTestBase() {

    private val enrolledStatus = YieldBoostStatus.Enrolled(
        tokenName = "USDT",
        networkId = NETWORK_ID,
        moduleAddress = "0xModule",
        userAddress = "0xUser",
        contractAddress = CONTRACT_ADDRESS,
        qualificationEndDate = Clock.System.now() + 100.days,
    )

    private fun createModel() = createModel(dispatchers = TestingCoroutineDispatcherProvider())

    @Test
    fun `GIVEN cached status NotStarted WHEN model created THEN force refreshes and shows boost text`() = runTest {
        coEvery { getYieldBoostStatusUseCase(userWalletId, false) } returns YieldBoostStatus.NotStarted.right()
        coEvery { getYieldBoostStatusUseCase(userWalletId, true) } returns enrolledStatus.right()

        val model = createModel()

        assertThat(model.uiState.value.boostText).isNotNull()
        coVerify(exactly = 1) { getYieldBoostStatusUseCase(userWalletId, false) }
        coVerify(exactly = 1) { getYieldBoostStatusUseCase(userWalletId, true) }
    }

    @Test
    fun `GIVEN cached status already Enrolled WHEN model created THEN does not force refresh`() = runTest {
        coEvery { getYieldBoostStatusUseCase(userWalletId, false) } returns enrolledStatus.right()

        val model = createModel()

        assertThat(model.uiState.value.boostText).isNotNull()
        coVerify(exactly = 1) { getYieldBoostStatusUseCase(userWalletId, false) }
        coVerify(exactly = 0) { getYieldBoostStatusUseCase(userWalletId, true) }
    }

    @Test
    fun `GIVEN status NotStarted even after refresh WHEN model created THEN shows no boost text`() = runTest {
        coEvery { getYieldBoostStatusUseCase(userWalletId, false) } returns YieldBoostStatus.NotStarted.right()
        coEvery { getYieldBoostStatusUseCase(userWalletId, true) } returns YieldBoostStatus.NotStarted.right()

        val model = createModel()

        assertThat(model.uiState.value.boostText).isNull()
        coVerify(exactly = 1) { getYieldBoostStatusUseCase(userWalletId, false) }
        coVerify(exactly = 1) { getYieldBoostStatusUseCase(userWalletId, true) }
    }
}