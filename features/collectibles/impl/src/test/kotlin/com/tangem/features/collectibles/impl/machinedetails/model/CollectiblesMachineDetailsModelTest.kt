package com.tangem.features.collectibles.impl.machinedetails.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.collectibles.CollectibleGachaMachineNftsBatchFlow
import com.tangem.domain.collectibles.CollectibleGachaMachineNftsBatchListState
import com.tangem.domain.collectibles.usecase.GetCollectibleGachaMachineNftsBatchFlowUseCase
import com.tangem.domain.collectibles.usecase.GetCollectibleGachaMachineUseCase
import com.tangem.features.collectibles.impl.CollectiblesRoute
import com.tangem.features.collectibles.impl.machinedetails.CollectiblesMachineDetailsBottomSheetConfig
import com.tangem.features.collectibles.impl.createMachine
import com.tangem.features.collectibles.impl.machinedetails.CollectiblesMachineDetailsComponent
import com.tangem.features.collectibles.impl.machinedetails.ui.state.CollectiblesMachineDetailsUM
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class CollectiblesMachineDetailsModelTest {

    private val getMachineUseCase: GetCollectibleGachaMachineUseCase = mockk()
    private val getPrizePoolBatchFlowUseCase: GetCollectibleGachaMachineNftsBatchFlowUseCase = mockk()
    private val router: Router = mockk(relaxed = true)

    private val prizePoolBatchFlow: CollectibleGachaMachineNftsBatchFlow = mockk(relaxed = true)

    private var model: CollectiblesMachineDetailsModel? = null

    @BeforeEach
    fun setUp() {
        clearMocks(getMachineUseCase, getPrizePoolBatchFlowUseCase, router, prizePoolBatchFlow)
        coEvery { getMachineUseCase(any(), any()) } returns createMachine().right()
        every { getPrizePoolBatchFlowUseCase(any(), any(), any()) } returns prizePoolBatchFlow
        every { prizePoolBatchFlow.state } returns MutableStateFlow(
            CollectibleGachaMachineNftsBatchListState(data = emptyList(), status = PaginationStatus.None),
        )
    }

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN turbo toggled WHEN buy tapped THEN the order is opened with the turbo state`(
        isTurboEnabled: Boolean,
    ) = runModelTest {
        // Arrange
        val model = startModel()
        (model.uiState.value as CollectiblesMachineDetailsUM.Content).footer.onTurboCheckedChange(isTurboEnabled)
        val content = model.uiState.value as CollectiblesMachineDetailsUM.Content

        // Act
        content.footer.onBuyClick()

        // Assert
        verify(exactly = 1) {
            router.push(CollectiblesRoute.ConfirmOrder(externalId = EXTERNAL_ID, isTurboEnabled = isTurboEnabled))
        }
    }

    @Test
    fun `GIVEN the machine is shown WHEN turbo info tapped THEN the turbo explainer opens`() = runModelTest {
        // Arrange
        val model = startModel()
        val content = model.uiState.value as CollectiblesMachineDetailsUM.Content
        var shownSheet: CollectiblesMachineDetailsBottomSheetConfig? = null
        model.bottomSheetNavigation.subscribe { event -> shownSheet = event.transformer(shownSheet) }

        // Act
        content.footer.onTurboInfoClick()

        // Assert
        assertThat(shownSheet).isEqualTo(CollectiblesMachineDetailsBottomSheetConfig.TurboInfo)
    }

    @Test
    fun `GIVEN the turbo explainer is open WHEN dismissed THEN it closes`() = runModelTest {
        // Arrange
        val model = startModel()
        val content = model.uiState.value as CollectiblesMachineDetailsUM.Content
        var shownSheet: CollectiblesMachineDetailsBottomSheetConfig? = null
        model.bottomSheetNavigation.subscribe { event -> shownSheet = event.transformer(shownSheet) }
        content.footer.onTurboInfoClick()

        // Act
        model.onDismissBottomSheet()

        // Assert
        assertThat(shownSheet).isNull()
    }

    @Test
    fun `GIVEN the machine is shown WHEN 10 seconds pass THEN only the expected value is refreshed`() = runModelTest {
        // Arrange
        coEvery { getMachineUseCase(EXTERNAL_ID, true) } returns createMachine(
            expectedValue = BigDecimal("15"),
            price = BigDecimal("99"),
        ).right()
        val model = startModel()
        val before = model.uiState.value as CollectiblesMachineDetailsUM.Content

        // Act
        advanceTimeBy(POLLING_DELAY_MS + 1)

        // Assert
        val expected = before.copy(footer = before.footer.copy(expectedValue = stringReference("$15")))
        assertThat(model.uiState.value).isEqualTo(expected)
    }

    @Test
    fun `GIVEN the machine is shown WHEN less than 10 seconds pass THEN it is not polled`() = runModelTest {
        // Arrange
        startModel()

        // Act
        advanceTimeBy(POLLING_DELAY_MS - 1)

        // Assert
        coVerify(exactly = 0) { getMachineUseCase(EXTERNAL_ID, true) }
    }

    @Test
    fun `GIVEN polling runs WHEN several periods pass THEN the machine is polled once per period`() = runModelTest {
        // Arrange
        startModel()

        // Act
        advanceTimeBy(POLLING_DELAY_MS * 3 + 1)

        // Assert
        coVerify(exactly = 3) { getMachineUseCase(EXTERNAL_ID, true) }
    }

    @Test
    fun `GIVEN the screen is paused WHEN 10 seconds pass THEN it is not polled`() = runModelTest {
        // Arrange
        val model = startModel()

        // Act
        model.onPause()
        advanceTimeBy(POLLING_DELAY_MS * 2 + 1)

        // Assert
        coVerify(exactly = 0) { getMachineUseCase(EXTERNAL_ID, true) }
    }

    @Test
    fun `GIVEN the screen was paused WHEN resumed THEN polling continues`() = runModelTest {
        // Arrange
        val model = startModel()
        model.onPause()
        advanceTimeBy(POLLING_DELAY_MS * 2)

        // Act
        model.onResume()
        advanceTimeBy(POLLING_DELAY_MS + 1)

        // Assert
        coVerify(exactly = 1) { getMachineUseCase(EXTERNAL_ID, true) }
    }

    @Test
    fun `GIVEN the machine loads while the screen is paused WHEN 10 seconds pass THEN it is not polled`() =
        runModelTest {
            // Arrange
            startModel(isResumed = false)

            // Act
            advanceTimeBy(POLLING_DELAY_MS * 2 + 1)

            // Assert
            coVerify(exactly = 0) { getMachineUseCase(EXTERNAL_ID, true) }
        }

    @Test
    fun `GIVEN the machine loaded while paused WHEN resumed THEN polling starts`() = runModelTest {
        // Arrange
        val model = startModel(isResumed = false)

        // Act
        model.onResume()
        advanceTimeBy(POLLING_DELAY_MS + 1)

        // Assert
        coVerify(exactly = 1) { getMachineUseCase(EXTERNAL_ID, true) }
    }

    /** The model is destroyed inside [runTest]: its closing advanceUntilIdle would spin on the never-idle polling. */
    private fun runModelTest(testBody: suspend TestScope.() -> Unit) = runTest {
        try {
            testBody()
        } finally {
            model?.onDestroy()
            model = null
        }
    }

    private fun TestScope.startModel(isResumed: Boolean = true): CollectiblesMachineDetailsModel {
        val model = CollectiblesMachineDetailsModel(
            dispatchers = createTestingCoroutineDispatcherProvider(),
            getMachineUseCase = getMachineUseCase,
            router = router,
            paramsContainer = MutableParamsContainer(
                CollectiblesMachineDetailsComponent.Params(externalId = EXTERNAL_ID),
            ),
            getPrizePoolBatchFlowUseCase = getPrizePoolBatchFlowUseCase,
        ).also { this@CollectiblesMachineDetailsModelTest.model = it }
        if (isResumed) model.onResume()

        // Not advanceUntilIdle: the expected value polling never idles
        runCurrent()

        return model
    }

    private companion object {

        const val EXTERNAL_ID = "pokemon_1"
        const val POLLING_DELAY_MS = 10_000L
    }
}