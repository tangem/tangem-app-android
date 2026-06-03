package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for [SwapModel.isPermissionNotNeeded].
 *
 * The getter is `true` when the current loaded swap state needs no approval:
 * - always when [PermissionDataState.Empty]
 * - additionally for [PermissionDataState.PermissionSettings] when the integrated-approve toggle is ON
 */
internal class SwapModelIsPermissionNotNeededTest : SwapModelTestBase() {

    @BeforeEach
    fun setUp() {
        setUpBase()
    }

    private fun modelWithPermissionState(
        permissionState: PermissionDataState,
        isIntegratedApproveEnabled: Boolean,
    ): SwapModel {
        every { swapFeatureToggles.isSwapIntegratedApproveEnabled } returns isIntegratedApproveEnabled
        val provider = swapProvider()
        val model = createModel()
        model.dataState = model.dataState.copy(
            selectedProvider = provider,
            lastLoadedSwapStates = mapOf(provider to quotesLoadedState(provider, permissionState)),
        )
        return model
    }

    @Test
    fun `GIVEN toggle OFF and Empty permission THEN permission is not needed`() {
        val model = modelWithPermissionState(PermissionDataState.Empty, isIntegratedApproveEnabled = false)

        assertThat(model.isPermissionNotNeeded).isTrue()
    }

    @Test
    fun `GIVEN toggle OFF and PermissionSettings THEN permission is needed`() {
        val model = modelWithPermissionState(permissionSettings(), isIntegratedApproveEnabled = false)

        assertThat(model.isPermissionNotNeeded).isFalse()
    }

    @Test
    fun `GIVEN toggle OFF and PermissionRequired THEN permission is needed`() {
        val model = modelWithPermissionState(
            PermissionDataState.PermissionRequired(isResetApproval = false, spenderAddress = "0x"),
            isIntegratedApproveEnabled = false,
        )

        assertThat(model.isPermissionNotNeeded).isFalse()
    }

    @Test
    fun `GIVEN toggle ON and Empty permission THEN permission is not needed`() {
        val model = modelWithPermissionState(PermissionDataState.Empty, isIntegratedApproveEnabled = true)

        assertThat(model.isPermissionNotNeeded).isTrue()
    }

    @Test
    fun `GIVEN toggle ON and PermissionSettings THEN permission is not needed`() {
        val model = modelWithPermissionState(permissionSettings(), isIntegratedApproveEnabled = true)

        assertThat(model.isPermissionNotNeeded).isTrue()
    }

    @Test
    fun `GIVEN toggle ON and PermissionRequired THEN permission is needed`() {
        val model = modelWithPermissionState(
            PermissionDataState.PermissionRequired(isResetApproval = false, spenderAddress = "0x"),
            isIntegratedApproveEnabled = true,
        )

        assertThat(model.isPermissionNotNeeded).isFalse()
    }

    @Test
    fun `GIVEN no current loaded state THEN permission is needed`() {
        every { swapFeatureToggles.isSwapIntegratedApproveEnabled } returns true
        val model = createModel()

        assertThat(model.isPermissionNotNeeded).isFalse()
    }
}