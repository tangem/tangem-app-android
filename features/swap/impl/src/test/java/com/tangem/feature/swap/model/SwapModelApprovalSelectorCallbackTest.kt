package com.tangem.feature.swap.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
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
    fun `GIVEN no current loaded state WHEN onApproveTypeSelected THEN type remembered and no reload`() = runTest {
        // Arrange
        val model = createModel()
        val slot = SlotTracker(model.approvalSlotNavigation)
        val statesBefore = model.dataState.lastLoadedSwapStates

        // Act
        model.approvalSelectorCallback.onApproveTypeSelected("0xSpender", ApproveType.UNLIMITED)

        // Assert
        assertThat(model.dataState.selectedApproveTypes).containsExactly("0xSpender", ApproveType.UNLIMITED)
        assertThat(model.dataState.lastLoadedSwapStates).isEqualTo(statesBefore)
        assertThat(slot.results).containsExactly(null)
        coVerify(exactly = 0) { feeSelectorReloadTrigger.triggerLoadingState() }
        coVerify(exactly = 0) { feeSelectorReloadTrigger.triggerUpdate() }
    }

    @Test
    fun `GIVEN permission is not PermissionSettings WHEN onApproveTypeSelected THEN dismissed and no reload`() =
        runTest {
            // Arrange
            val provider = swapProvider()
            val model = createModel()
            val slot = SlotTracker(model.approvalSlotNavigation)
            model.dataState = model.dataState.copy(
                selectedProvider = provider,
                lastLoadedSwapStates = mapOf(provider to quotesLoadedState(provider, PermissionDataState.Empty)),
            )

            // Act
            model.approvalSelectorCallback.onApproveTypeSelected("0xSpender", ApproveType.UNLIMITED)

            // Assert
            assertThat(model.dataState.selectedApproveTypes).containsExactly("0xSpender", ApproveType.UNLIMITED)
            assertThat(slot.results).containsExactly(null)
            coVerify(exactly = 0) { feeSelectorReloadTrigger.triggerLoadingState() }
            coVerify(exactly = 0) { feeSelectorReloadTrigger.triggerUpdate() }
        }

    @Test
    fun `GIVEN same approve type WHEN onApproveTypeSelected THEN quotes untouched and no reload`() = runTest {
        // Arrange
        val provider = swapProvider()
        val model = createModel()
        val slot = SlotTracker(model.approvalSlotNavigation)
        model.dataState = model.dataState.copy(
            selectedProvider = provider,
            lastLoadedSwapStates = mapOf(
                provider to quotesLoadedState(provider, permissionSettings(type = ApproveType.LIMITED)),
            ),
        )
        val statesBefore = model.dataState.lastLoadedSwapStates

        // Act
        model.approvalSelectorCallback.onApproveTypeSelected("0xSpender", ApproveType.LIMITED)

        // Assert
        assertThat(model.dataState.lastLoadedSwapStates).isEqualTo(statesBefore)
        assertThat(model.dataState.selectedApproveTypes).containsExactly("0xSpender", ApproveType.LIMITED)
        assertThat(slot.results).containsExactly(null)
        coVerify(exactly = 0) { feeSelectorReloadTrigger.triggerLoadingState() }
        coVerify(exactly = 0) { feeSelectorReloadTrigger.triggerUpdate() }
    }

    @Test
    fun `GIVEN different approve type WHEN onApproveTypeSelected THEN updates state and triggers reload`() = runTest {
        // Arrange
        val provider = swapProvider()
        val model = createModel()
        val slot = SlotTracker(model.approvalSlotNavigation)
        model.dataState = model.dataState.copy(
            selectedProvider = provider,
            lastLoadedSwapStates = mapOf(
                provider to quotesLoadedState(provider, permissionSettings(type = ApproveType.LIMITED)),
            ),
        )

        // Act
        model.approvalSelectorCallback.onApproveTypeSelected("0xSpender", ApproveType.UNLIMITED)

        // Assert
        val updated = model.dataState.getCurrentLoadedSwapState()
        val settings = updated?.permissionState as? PermissionDataState.PermissionSettings
        assertThat(settings).isNotNull()
        assertThat(settings!!.type).isEqualTo(ApproveType.UNLIMITED)
        assertThat(model.dataState.selectedApproveTypes).containsExactly("0xSpender", ApproveType.UNLIMITED)
        assertThat(slot.results).containsExactly(null)
        assertThat(model.feeSelectorRepository.state.value).isEqualTo(FeeSelectorUM.Loading)
        coVerify(exactly = 1) { feeSelectorReloadTrigger.triggerLoadingState() }
        coVerify(exactly = 1) { feeSelectorReloadTrigger.triggerUpdate() }
    }

    @Test
    fun `GIVEN any state WHEN onCancelClick THEN no state change and no reload`() = runTest {
        // Arrange
        val provider = swapProvider()
        val model = createModel()
        model.dataState = model.dataState.copy(
            selectedProvider = provider,
            lastLoadedSwapStates = mapOf(
                provider to quotesLoadedState(provider, permissionSettings()),
            ),
        )
        val before = model.dataState

        // Act
        model.approvalSelectorCallback.onCancelClick()

        // Assert
        assertThat(model.dataState).isEqualTo(before)
        coVerify(exactly = 0) { feeSelectorReloadTrigger.triggerLoadingState() }
        coVerify(exactly = 0) { feeSelectorReloadTrigger.triggerUpdate() }
    }

    /**
     * Minimal stand-in for `childSlot`: tracks the active configuration and invokes each navigation
     * event's completion callback, which `DefaultSlotNavigation` leaves to its host. [results] holds
     * one entry per navigation event, so a `dismiss()` shows up as a single `null`.
     */
    private class SlotTracker<C : Any>(navigation: SlotNavigation<C>) {

        val results = mutableListOf<C?>()

        private var configuration: C? = null

        init {
            navigation.subscribe { event ->
                val oldConfiguration = configuration
                configuration = event.transformer(oldConfiguration)
                results += configuration
                event.onComplete(configuration, oldConfiguration)
            }
        }
    }
}