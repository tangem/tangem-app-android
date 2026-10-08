package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import com.tangem.feature.swap.component.slippage.SwapSlippageComponent
import com.tangem.features.approval.api.GiveApprovalComponent
import com.tangem.features.approval.api.SelectApprovalTypeComponent
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for the swap sheet slots: the flat provider sheet and the glass approval sheets live in separate slots, so a
 * glass sheet opens above an open provider sheet and closing it leaves the provider sheet in place.
 */
internal class SwapModelBottomSheetNavigationTest : SwapModelTestBase() {

    private val selectApprovalRoute = SwapBottomSheetRoute.SelectApprovalType(
        params = mockk<SelectApprovalTypeComponent.Params>(),
    )
    private val giveApprovalRoute = SwapBottomSheetRoute.GiveApproval(params = mockk<GiveApprovalComponent.Params>())
    private val slippageRoute = SwapBottomSheetRoute.Slippage(params = mockk<SwapSlippageComponent.Params>())

    @BeforeEach
    fun setUp() {
        setUpBase()
    }

    @Test
    fun `GIVEN no sheet shown WHEN provider sheet opened THEN flat slot activated and glass untouched`() {
        // Arrange
        val model = createModel()
        val flat = SlotTracker(model.flatSheetSlotNavigation)
        val glass = SlotTracker(model.glassSheetSlotNavigation)

        // Act
        model.openBottomSheet(SwapBottomSheetRoute.ChooseProvider)

        // Assert
        assertThat(flat.results).containsExactly(SwapBottomSheetRoute.ChooseProvider)
        assertThat(glass.results).isEmpty()
    }

    @Test
    fun `GIVEN no sheet shown WHEN give approval opened THEN glass slot activated and flat untouched`() {
        // Arrange
        val model = createModel()
        val flat = SlotTracker(model.flatSheetSlotNavigation)
        val glass = SlotTracker(model.glassSheetSlotNavigation)

        // Act
        model.openBottomSheet(giveApprovalRoute)

        // Assert
        assertThat(glass.results).containsExactly(giveApprovalRoute)
        assertThat(flat.results).isEmpty()
    }

    @Test
    fun `GIVEN no sheet shown WHEN slippage opened THEN glass slot activated and flat untouched`() {
        // Arrange
        val model = createModel()
        val flat = SlotTracker(model.flatSheetSlotNavigation)
        val glass = SlotTracker(model.glassSheetSlotNavigation)

        // Act
        model.openBottomSheet(slippageRoute)

        // Assert
        assertThat(glass.results).containsExactly(slippageRoute)
        assertThat(flat.results).isEmpty()
    }

    @Test
    fun `GIVEN provider sheet open WHEN approve type selected THEN selector opens above providers`() {
        // Arrange
        val model = createModel()
        val flat = SlotTracker(model.flatSheetSlotNavigation)
        val glass = SlotTracker(model.glassSheetSlotNavigation)
        model.openBottomSheet(SwapBottomSheetRoute.ChooseProvider)

        // Act
        model.openBottomSheet(selectApprovalRoute)

        // Assert
        assertThat(flat.results).containsExactly(SwapBottomSheetRoute.ChooseProvider)
        assertThat(glass.results).containsExactly(selectApprovalRoute)
    }

    @Test
    fun `GIVEN selector above providers WHEN selector cancelled THEN only selector closed`() {
        // Arrange
        val model = createModel()
        val flat = SlotTracker(model.flatSheetSlotNavigation)
        val glass = SlotTracker(model.glassSheetSlotNavigation)
        model.openBottomSheet(SwapBottomSheetRoute.ChooseProvider)
        model.openBottomSheet(selectApprovalRoute)

        // Act
        model.approvalSelectorCallback.onCancelClick()

        // Assert
        assertThat(glass.results).containsExactly(selectApprovalRoute, null).inOrder()
        assertThat(flat.results).containsExactly(SwapBottomSheetRoute.ChooseProvider)
    }

    @Test
    fun `GIVEN selector above providers WHEN selector swiped away THEN provider sheet stays open`() {
        // Arrange
        val model = createModel()
        val flat = SlotTracker(model.flatSheetSlotNavigation)
        val glass = SlotTracker(model.glassSheetSlotNavigation)
        model.openBottomSheet(SwapBottomSheetRoute.ChooseProvider)
        model.openBottomSheet(selectApprovalRoute)

        // Act
        model.dismissGlassSheet()

        // Assert
        assertThat(glass.results.last()).isNull()
        assertThat(flat.results).containsExactly(SwapBottomSheetRoute.ChooseProvider)
    }

    @Test
    fun `GIVEN provider sheet open WHEN dismissed THEN flat slot cleared`() {
        // Arrange
        val model = createModel()
        val flat = SlotTracker(model.flatSheetSlotNavigation)
        model.openBottomSheet(SwapBottomSheetRoute.ChooseProvider)

        // Act
        model.dismissFlatSheet()

        // Assert
        assertThat(flat.results).containsExactly(SwapBottomSheetRoute.ChooseProvider, null).inOrder()
    }

    @Test
    fun `GIVEN give approval sheet open WHEN swiped away THEN glass slot cleared`() {
        // Arrange
        val model = createModel()
        val glass = SlotTracker(model.glassSheetSlotNavigation)
        model.openBottomSheet(giveApprovalRoute)

        // Act
        model.dismissGlassSheet()

        // Assert
        assertThat(glass.results).containsExactly(giveApprovalRoute, null).inOrder()
    }

    @Test
    fun `GIVEN sheet dismissed WHEN opened again THEN slot activated again`() {
        // Arrange
        val model = createModel()
        val flat = SlotTracker(model.flatSheetSlotNavigation)
        model.openBottomSheet(SwapBottomSheetRoute.ChooseProvider)
        model.dismissFlatSheet()

        // Act
        model.openBottomSheet(SwapBottomSheetRoute.ChooseProvider)

        // Assert
        assertThat(flat.results)
            .containsExactly(SwapBottomSheetRoute.ChooseProvider, null, SwapBottomSheetRoute.ChooseProvider)
            .inOrder()
    }
}