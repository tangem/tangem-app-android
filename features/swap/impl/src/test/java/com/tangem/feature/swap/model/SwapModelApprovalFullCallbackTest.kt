package com.tangem.feature.swap.model

import com.tangem.domain.models.currency.CryptoCurrency
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for [SwapModel.approvalFullCallback] (the full give-approval flow callback).
 *
 * Covers the synchronously-verifiable side effects. The quote-reload path
 * ([SwapModel] `startLoadingQuotesFromLastState`) short-circuits because `dataState.amount` is null
 * in these setups, keeping the callbacks side-effect-bounded.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SwapModelApprovalFullCallbackTest : SwapModelTestBase() {

    @BeforeEach
    fun setUp() {
        setUpBase()
    }

    @Test
    fun `GIVEN token from-currency WHEN onApproveDone THEN adds contract address to in-progress`() = runTest {
        val model = createModel()
        val token = mockk<CryptoCurrency.Token>(relaxed = true) {
            every { contractAddress } returns "0xContract"
        }
        model.dataState = model.dataState.copy(
            fromSwapCurrencyStatus = swapCurrencyStatus(currency = token),
        )

        model.approvalFullCallback.onApproveDone()

        verify(exactly = 1) { allowPermissionsHandler.addAddressToInProgress("0xContract") }
    }

    @Test
    fun `GIVEN null from-currency WHEN onApproveDone THEN does not add address to in-progress`() = runTest {
        val model = createModel()
        model.dataState = model.dataState.copy(fromSwapCurrencyStatus = null)

        model.approvalFullCallback.onApproveDone()

        verify(exactly = 0) { allowPermissionsHandler.addAddressToInProgress(any()) }
    }

    @Test
    fun `GIVEN failure WHEN onApproveFailed THEN sends alert message`() = runTest {
        val model = createModel()

        model.approvalFullCallback.onApproveFailed()

        verify(exactly = 1) { messageSender.send(any()) }
    }

    @Test
    fun `WHEN onApproveClick THEN no interactions`() = runTest {
        val model = createModel()

        // onApproveClick is intentionally a no-op; assert it does not crash.
        model.approvalFullCallback.onApproveClick()

        verify(exactly = 0) { allowPermissionsHandler.addAddressToInProgress(any()) }
    }
}