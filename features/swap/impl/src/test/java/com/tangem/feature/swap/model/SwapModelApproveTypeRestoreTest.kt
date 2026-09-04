package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.feature.swap.domain.models.ui.SwapState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for [SwapModel.withRememberedApproveTypes] — [REDACTED_TASK_KEY].
 *
 * The domain rebuilds every [PermissionDataState.PermissionSettings] as [ApproveType.LIMITED] on
 * each quote refresh, so the model has to re-apply the type the user picked.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SwapModelApproveTypeRestoreTest : SwapModelTestBase() {

    @BeforeEach
    fun setUp() {
        setUpBase()
    }

    @Test
    fun `GIVEN unlimited remembered WHEN quotes reloaded as limited THEN unlimited is restored`() = runTest {
        // Arrange
        val provider = swapProvider()
        val model = createModel()
        model.dataState = model.dataState.copy(
            selectedApproveTypes = mapOf(SPENDER to ApproveType.UNLIMITED),
        )
        val freshQuotes = mapOf(
            provider to quotesLoadedState(provider, permissionSettings(type = ApproveType.LIMITED)),
        )

        // Act
        val restored = with(model) { freshQuotes.withRememberedApproveTypes() }

        // Assert
        assertThat(restored.approveTypeOf(provider)).isEqualTo(ApproveType.UNLIMITED)
    }

    @Test
    fun `GIVEN type remembered for another spender WHEN quotes reloaded THEN the quote keeps its default`() = runTest {
        // Arrange
        val provider = swapProvider()
        val model = createModel()
        model.dataState = model.dataState.copy(
            selectedApproveTypes = mapOf("0xOtherSpender" to ApproveType.UNLIMITED),
        )
        val freshQuotes = mapOf(
            provider to quotesLoadedState(provider, permissionSettings(type = ApproveType.LIMITED)),
        )

        // Act
        val restored = with(model) { freshQuotes.withRememberedApproveTypes() }

        // Assert
        assertThat(restored.approveTypeOf(provider)).isEqualTo(ApproveType.LIMITED)
    }

    @Test
    fun `GIVEN two spenders and one remembered WHEN quotes reloaded THEN only the matching quote changes`() = runTest {
        // Arrange
        val remembered = swapProvider(id = "provider-remembered")
        val untouched = swapProvider(id = "provider-untouched")
        val untouchedState = quotesLoadedState(
            provider = untouched,
            permissionState = permissionSettings(type = ApproveType.LIMITED, spender = "0xOtherSpender"),
        )
        val model = createModel()
        model.dataState = model.dataState.copy(
            selectedApproveTypes = mapOf(SPENDER to ApproveType.UNLIMITED),
        )
        val freshQuotes = mapOf(
            remembered to quotesLoadedState(remembered, permissionSettings(type = ApproveType.LIMITED)),
            untouched to untouchedState,
        )

        // Act
        val restored = with(model) { freshQuotes.withRememberedApproveTypes() }

        // Assert
        assertThat(restored.approveTypeOf(remembered)).isEqualTo(ApproveType.UNLIMITED)
        assertThat(restored[untouched]).isSameInstanceAs(untouchedState)
    }

    @Test
    fun `GIVEN nothing remembered WHEN quotes reloaded THEN the map is returned unchanged`() = runTest {
        // Arrange
        val provider = swapProvider()
        val model = createModel()
        val freshQuotes = mapOf(
            provider to quotesLoadedState(provider, permissionSettings(type = ApproveType.LIMITED)),
        )

        // Act
        val restored = with(model) { freshQuotes.withRememberedApproveTypes() }

        // Assert
        assertThat(restored).isEqualTo(freshQuotes)
    }

    @Test
    fun `GIVEN remembered type equals loaded type WHEN quotes reloaded THEN the state is not rebuilt`() = runTest {
        // Arrange
        val provider = swapProvider()
        val state = quotesLoadedState(provider, permissionSettings(type = ApproveType.UNLIMITED))
        val model = createModel()
        model.dataState = model.dataState.copy(
            selectedApproveTypes = mapOf(SPENDER to ApproveType.UNLIMITED),
        )

        // Act
        val restored = with(model) { mapOf(provider to state).withRememberedApproveTypes() }

        // Assert
        assertThat(restored[provider]).isSameInstanceAs(state)
    }

    @Test
    fun `GIVEN a non-PermissionSettings quote WHEN quotes reloaded THEN it is passed through untouched`() = runTest {
        // Arrange
        val provider = swapProvider()
        val state = quotesLoadedState(
            provider = provider,
            permissionState = PermissionDataState.PermissionRequired(isResetApproval = false, spenderAddress = SPENDER),
        )
        val model = createModel()
        model.dataState = model.dataState.copy(
            selectedApproveTypes = mapOf(SPENDER to ApproveType.UNLIMITED),
        )

        // Act
        val restored = with(model) { mapOf(provider to state).withRememberedApproveTypes() }

        // Assert
        assertThat(restored[provider]).isSameInstanceAs(state)
    }

    private fun Map<SwapProvider, SwapState>.approveTypeOf(provider: SwapProvider): ApproveType? {
        val state = this[provider] as? SwapState.QuotesLoadedState
        return (state?.permissionState as? PermissionDataState.PermissionSettings)?.type
    }

    private companion object {
        const val SPENDER = "0xSpender"
    }
}