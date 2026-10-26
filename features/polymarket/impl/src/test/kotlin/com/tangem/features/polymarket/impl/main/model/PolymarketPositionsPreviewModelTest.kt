package com.tangem.features.polymarket.impl.main.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionResult
import com.tangem.domain.polymarket.model.PolymarketPositionsTab
import com.tangem.domain.polymarket.model.PolymarketResolutionStatus
import com.tangem.features.polymarket.impl.main.PolymarketPositionsPreviewComponent
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketPositionsPreviewUM
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.positions.model.createPolymarketPosition
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PolymarketPositionsPreviewModelTest {

    private val router: Router = mockk(relaxed = true)
    private val loader: PolymarketPositionsPreviewLoader = mockk(relaxed = true)
    private val loaderState = MutableStateFlow<PolymarketPositionsPreviewUM>(PolymarketPositionsPreviewUM.Loading)
    private var blocksRefresh = PolymarketMainBlocksRefresh()

    private var model: PolymarketPositionsPreviewModel? = null

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, loader)
        every { loader.state } returns loaderState
        blocksRefresh = PolymarketMainBlocksRefresh()
    }

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN the preview WHEN see all tapped THEN opens Active for the settled wallet`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        model.onActiveClick()

        // Assert
        verify { router.push(PolymarketRoute.Positions(USER_WALLET_ID, PolymarketPositionsTab.ACTIVE)) }
    }

    @Test
    fun `GIVEN an empty preview WHEN Finished tapped THEN history remains reachable`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        model.onFinishedClick()

        // Assert
        verify { router.push(PolymarketRoute.Positions(USER_WALLET_ID, PolymarketPositionsTab.FINISHED)) }
    }

    @Test
    fun `GIVEN loader state WHEN the block resumes and pauses THEN it exposes the state and forwards lifecycle`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            val positions = PolymarketPositionsPreviewUM.Error(onRetry = {})

            // Act
            loaderState.value = positions
            model.onResume()
            model.onPause()

            // Assert
            assertThat(model.uiState.value).isEqualTo(positions)
            verify(exactly = 1) { loader.resume(any(), USER_WALLET_ID, any()) }
            verify(exactly = 1) { loader.pause() }
        }

    @Test
    fun `GIVEN the block is created WHEN the host refreshes its blocks THEN the preview is re-read`() = runTest {
        // Arrange
        createModel(testScope = this)

        // Act
        blocksRefresh.refreshAll()

        // Assert
        coVerify(exactly = 1) { loader.refresh(any(), USER_WALLET_ID, any()) }
    }

    @Test
    fun `GIVEN an open position WHEN tapped THEN details receives resolved wallet and outcome`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        val position = createPolymarketPosition()

        // Act
        capturePositionClick(model).invoke(position)

        // Assert
        verify(exactly = 1) { router.push(PolymarketRoute.PositionDetails(USER_WALLET_ID, position.id), any()) }
    }

    @Test
    fun `GIVEN a claimable position WHEN tapped THEN result receives wallet and real position`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        val position = createPolymarketPosition(
            result = PolymarketPositionResult.SPLIT,
            resolutionStatus = PolymarketResolutionStatus.RESOLVED,
        ).copy(isRedeemable = true)

        // Act
        capturePositionClick(model).invoke(position)

        // Assert
        verify(exactly = 1) { router.push(PolymarketRoute.PositionResult(USER_WALLET_ID, position), any()) }
        verify(exactly = 0) { router.push(ofType<PolymarketRoute.PositionDetails>(), any()) }
    }

    private fun capturePositionClick(model: PolymarketPositionsPreviewModel): (PolymarketPosition.Open) -> Unit {
        val click = slot<(PolymarketPosition.Open) -> Unit>()
        model.onResume()
        verify { loader.resume(any(), USER_WALLET_ID, capture(click)) }
        return click.captured
    }

    private fun createModel(testScope: TestScope): PolymarketPositionsPreviewModel = PolymarketPositionsPreviewModel(
        paramsContainer = MutableParamsContainer(
            value = PolymarketPositionsPreviewComponent.Params(
                userWalletId = USER_WALLET_ID,
                blocksRefresh = blocksRefresh,
            ),
        ),
        router = router,
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        loader = loader,
    ).also { model = it }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
    }
}