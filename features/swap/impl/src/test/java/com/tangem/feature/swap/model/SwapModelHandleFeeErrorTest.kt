package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.feature.swap.analytics.SwapEvents
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import io.mockk.coVerify
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for [SwapModel]'s integrated-approval fallback trigger
 * (`SwapModel.FeeSelectorRepository.onResult` → private `handleFeeError`).
 *
 * Driven through the public `feeSelectorRepository.onResult(FeeSelectorUM.Error(...))` path:
 *
 *  (a) `EstimateOverrideError` + `PermissionSettings` → `swapInteractor.integratedApprovalFallback`
 *      is called once with the matching spender, and the loaded state is rewritten to
 *      `PermissionRequired(isResetApproval = false)` with `integratedApprovalData == null`.
 *  (b) `EstimateOverrideError` + non-`PermissionSettings` permission → no fallback call, state
 *      left as-is (permission stays Empty).
 *  (c) non-`EstimateOverrideError` (plain fee error) → no fallback call (plain fee-error path).
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SwapModelHandleFeeErrorTest : SwapModelTestBase() {

    @BeforeEach
    fun setUp() {
        setUpBase()
    }

    @Test
    fun `GIVEN EstimateOverrideError and PermissionSettings THEN fallback is triggered and state becomes PermissionRequired`() =
        runTest {
            val provider = swapProvider()
            val fromStatus = swapCurrencyStatus()
            val toStatus = swapCurrencyStatus()
            val model = createModel()
            model.dataState = model.dataState.copy(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                selectedProvider = provider,
                lastLoadedSwapStates = mapOf(
                    provider to quotesLoadedState(
                        provider = provider,
                        permissionState = permissionSettings(type = ApproveType.LIMITED, spender = SPENDER),
                    ),
                ),
            )

            model.feeSelectorRepository.onResult(
                FeeSelectorUM.Error(error = estimateOverrideError(), isHidden = false),
            )

            coVerify(exactly = 1) {
                swapInteractor.integratedApprovalFallback(
                    fromSwapCurrencyStatus = fromStatus,
                    spenderAddress = SPENDER,
                )
            }
            // The gas-override analytics event must be reported once, carrying the error fields.
            verify(exactly = 1) {
                analyticsEventHandler.send(ofType(SwapEvents.ApproveGasOverrideError::class))
            }
            val sentEvents = mutableListOf<AnalyticsEvent>()
            verify { analyticsEventHandler.send(capture(sentEvents)) }
            val overrideEvent = sentEvents.filterIsInstance<SwapEvents.ApproveGasOverrideError>().single()
            assertThat(overrideEvent.params).isEqualTo(
                mapOf(
                    "Token" to "USDT",
                    "Blockchain" to "ethereum",
                    "RPC Provider" to "infura",
                    "Error Message" to "execution reverted",
                ),
            )
            val updated = model.dataState.getCurrentLoadedSwapState()
            val permission = updated?.permissionState as? PermissionDataState.PermissionRequired
            assertThat(permission).isNotNull()
            assertThat(permission!!.isResetApproval).isFalse()
            assertThat(permission.spenderAddress).isEqualTo(SPENDER)
            assertThat(updated.integratedApprovalData).isNull()
        }

    @Test
    fun `GIVEN EstimateOverrideError and non-PermissionSettings THEN no fallback call`() = runTest {
        val provider = swapProvider()
        val fromStatus = swapCurrencyStatus()
        val toStatus = swapCurrencyStatus()
        val model = createModel()
        model.dataState = model.dataState.copy(
            fromSwapCurrencyStatus = fromStatus,
            toSwapCurrencyStatus = toStatus,
            selectedProvider = provider,
            lastLoadedSwapStates = mapOf(
                provider to quotesLoadedState(provider = provider, permissionState = PermissionDataState.Empty),
            ),
        )

        model.feeSelectorRepository.onResult(
            FeeSelectorUM.Error(error = estimateOverrideError(), isHidden = false),
        )

        coVerify(exactly = 0) {
            swapInteractor.integratedApprovalFallback(fromSwapCurrencyStatus = any(), spenderAddress = any())
        }
        // Permission untouched.
        assertThat(model.dataState.getCurrentLoadedSwapState()?.permissionState)
            .isEqualTo(PermissionDataState.Empty)
    }

    @Test
    fun `GIVEN non-EstimateOverrideError THEN no fallback call (plain fee-error path)`() = runTest {
        val provider = swapProvider()
        val fromStatus = swapCurrencyStatus()
        val toStatus = swapCurrencyStatus()
        val model = createModel()
        // The plain path runs the model's StateBuilder/refresh; relaxed mocks cover it.
        // We assert only the absence of the fallback call.
        model.dataState = model.dataState.copy(
            fromSwapCurrencyStatus = fromStatus,
            toSwapCurrencyStatus = toStatus,
            selectedProvider = provider,
            lastLoadedSwapStates = mapOf(
                provider to quotesLoadedState(
                    provider = provider,
                    permissionState = permissionSettings(type = ApproveType.LIMITED, spender = SPENDER),
                ),
            ),
        )

        model.feeSelectorRepository.onResult(
            FeeSelectorUM.Error(error = GetFeeError.UnknownError, isHidden = false),
        )

        coVerify(exactly = 0) {
            swapInteractor.integratedApprovalFallback(fromSwapCurrencyStatus = any(), spenderAddress = any())
        }
        // The gas-override analytics event belongs only to the EstimateOverrideError branch.
        verify(exactly = 0) {
            analyticsEventHandler.send(ofType(SwapEvents.ApproveGasOverrideError::class))
        }
    }

    private fun estimateOverrideError() = GetFeeError.EstimateOverrideError(
        blockchain = "ethereum",
        tokenSymbol = "USDT",
        rpcProvider = "infura",
        error = "execution reverted",
    )

    private companion object {
        const val SPENDER = "0xSpender"
    }
}