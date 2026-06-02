package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for [SwapModel.approvalSelectorCallback] (the [PermissionDataState.PermissionSettings]
 * approval-type selector).
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SwapModelApprovalSelectorCallbackTest : SwapModelTestBase() {

    @BeforeEach
    fun setUp() {
        setUpBase()
    }

    @Test
    fun `GIVEN no current loaded state WHEN onApproveTypeSelected THEN no state change and no reload`() = runTest {
        val model = createModel()
        val before = model.dataState

        model.approvalSelectorCallback.onApproveTypeSelected("0xSpender", ApproveType.UNLIMITED)

        assertThat(model.dataState).isEqualTo(before)
        coVerify(exactly = 0) { feeSelectorReloadTrigger.triggerLoadingState() }
        coVerify(exactly = 0) { feeSelectorReloadTrigger.triggerUpdate() }
    }

    @Test
    fun `GIVEN permission is not PermissionSettings WHEN onApproveTypeSelected THEN no reload`() = runTest {
        val provider = swapProvider()
        val model = createModel()
        model.dataState = model.dataState.copy(
            selectedProvider = provider,
            lastLoadedSwapStates = mapOf(provider to quotesLoadedState(provider, PermissionDataState.Empty)),
        )

        model.approvalSelectorCallback.onApproveTypeSelected("0xSpender", ApproveType.UNLIMITED)

        coVerify(exactly = 0) { feeSelectorReloadTrigger.triggerLoadingState() }
        coVerify(exactly = 0) { feeSelectorReloadTrigger.triggerUpdate() }
    }

    @Test
    fun `GIVEN same approve type WHEN onApproveTypeSelected THEN no state change and no reload`() = runTest {
        val provider = swapProvider()
        val model = createModel()
        model.dataState = model.dataState.copy(
            selectedProvider = provider,
            lastLoadedSwapStates = mapOf(
                provider to quotesLoadedState(provider, permissionSettings(type = ApproveType.LIMITED)),
            ),
        )
        val before = model.dataState

        model.approvalSelectorCallback.onApproveTypeSelected("0xSpender", ApproveType.LIMITED)

        assertThat(model.dataState).isEqualTo(before)
        coVerify(exactly = 0) { feeSelectorReloadTrigger.triggerLoadingState() }
        coVerify(exactly = 0) { feeSelectorReloadTrigger.triggerUpdate() }
    }

    @Test
    fun `GIVEN different approve type WHEN onApproveTypeSelected THEN updates state and triggers reload`() = runTest {
        val provider = swapProvider()
        val model = createModel()
        model.dataState = model.dataState.copy(
            selectedProvider = provider,
            lastLoadedSwapStates = mapOf(
                provider to quotesLoadedState(provider, permissionSettings(type = ApproveType.LIMITED)),
            ),
        )

        model.approvalSelectorCallback.onApproveTypeSelected("0xSpender", ApproveType.UNLIMITED)

        val updated = model.dataState.getCurrentLoadedSwapState()
        val settings = updated?.permissionState as? PermissionDataState.PermissionSettings
        assertThat(settings).isNotNull()
        assertThat(settings!!.type).isEqualTo(ApproveType.UNLIMITED)
        assertThat(model.feeSelectorRepository.state.value).isEqualTo(FeeSelectorUM.Loading)
        coVerify(exactly = 1) { feeSelectorReloadTrigger.triggerLoadingState() }
        coVerify(exactly = 1) { feeSelectorReloadTrigger.triggerUpdate() }
    }

    @Test
    fun `GIVEN any state WHEN onCancelClick THEN no state change and no reload`() = runTest {
        val provider = swapProvider()
        val model = createModel()
        model.dataState = model.dataState.copy(
            selectedProvider = provider,
            lastLoadedSwapStates = mapOf(
                provider to quotesLoadedState(provider, permissionSettings()),
            ),
        )
        val before = model.dataState

        model.approvalSelectorCallback.onCancelClick()

        assertThat(model.dataState).isEqualTo(before)
        coVerify(exactly = 0) { feeSelectorReloadTrigger.triggerLoadingState() }
        coVerify(exactly = 0) { feeSelectorReloadTrigger.triggerUpdate() }
    }
}