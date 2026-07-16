package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for [SwapModel.isPermissionNotNeeded].
 *
 * The getter is `true` when the current loaded swap state needs no approval:
 * - always when [PermissionDataState.Empty]
 * - additionally for [PermissionDataState.PermissionSettings]
 */
internal class SwapModelIsPermissionNotNeededTest : SwapModelTestBase() {

    @BeforeEach
    fun setUp() {
        setUpBase()
    }

    private fun modelWithPermissionState(permissionState: PermissionDataState): SwapModel {
        val provider = swapProvider()
        val model = createModel()
        model.dataState = model.dataState.copy(
            selectedProvider = provider,
            lastLoadedSwapStates = mapOf(provider to quotesLoadedState(provider, permissionState)),
        )
        return model
    }

    @Test
    fun `GIVEN Empty permission THEN permission is not needed`() {
        val model = modelWithPermissionState(PermissionDataState.Empty)

        assertThat(model.isPermissionNotNeeded).isTrue()
    }

    @Test
    fun `GIVEN PermissionSettings THEN permission is not needed`() {
        val model = modelWithPermissionState(permissionSettings())

        assertThat(model.isPermissionNotNeeded).isTrue()
    }

    @Test
    fun `GIVEN PermissionRequired THEN permission is needed`() {
        val model = modelWithPermissionState(
            PermissionDataState.PermissionRequired(isResetApproval = false, spenderAddress = "0x"),
        )

        assertThat(model.isPermissionNotNeeded).isFalse()
    }

    @Test
    fun `GIVEN no current loaded state THEN permission is needed`() {
        val model = createModel()

        assertThat(model.isPermissionNotNeeded).isFalse()
    }
}