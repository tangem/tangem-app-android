package com.tangem.features.collectibles.impl.confirmorder.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusSupplier
import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.domain.collectibles.models.CollectiblesError
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.collectibles.usecase.GetCollectibleGachaMachineUseCase
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.collectibles.impl.createMachine
import com.tangem.features.collectibles.impl.confirmorder.CollectiblesConfirmOrderComponent
import com.tangem.features.collectibles.impl.confirmorder.ui.state.CollectiblesConfirmOrderUM
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.mock.collectibles.MockCollectibles
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class CollectiblesConfirmOrderModelTest {

    private val getMachineUseCase: GetCollectibleGachaMachineUseCase = mockk()
    private val router: Router = mockk(relaxed = true)
    private val accountStatusSupplier: CollectiblesAccountStatusSupplier = mockk()

    private val statusFlow = MutableStateFlow<CollectiblesAccountStatusValue>(onboarded(usdcAmount = "1000"))

    private var model: CollectiblesConfirmOrderModel? = null

    @BeforeEach
    fun setUp() {
        clearMocks(getMachineUseCase, router, accountStatusSupplier)
        statusFlow.value = onboarded(usdcAmount = "1000")
        coEvery { getMachineUseCase(any(), any()) } returns createMachine().right()
        every { accountStatusSupplier(USER_WALLET_ID) } returns statusFlow
    }

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN the machine loads WHEN model created THEN the order is rendered`() = runTest {
        // Act
        val model = startModel()

        // Assert
        val state = model.uiState.value
        assertThat(state).isInstanceOf(CollectiblesConfirmOrderUM.Content::class.java)

        val content = state as CollectiblesConfirmOrderUM.Content
        assertThat(content.order.rows).hasSize(3)
        coVerify(exactly = 1) { getMachineUseCase(externalId = EXTERNAL_ID, skipCache = false) }
    }

    @Test
    fun `GIVEN the machine fails WHEN model created THEN the screen reports the error`() = runTest {
        // Arrange
        coEvery { getMachineUseCase(any(), any()) } returns CollectiblesError.Api(IllegalStateException()).left()

        // Act
        val model = startModel()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(CollectiblesConfirmOrderUM.Error::class.java)
    }

    @Test
    fun `GIVEN the machine failed WHEN reload tapped twice THEN one cache-bypassing retry runs with a spinner`() =
        runTest {
            // Arrange
            coEvery { getMachineUseCase(EXTERNAL_ID, false) } returns CollectiblesError.Api(IllegalStateException()).left()
            coEvery { getMachineUseCase(EXTERNAL_ID, true) } coAnswers { CompletableDeferred<MachineResult>().await() }
            val model = startModel()
            val error = model.uiState.value as CollectiblesConfirmOrderUM.Error

            // Act
            error.onReloadClick()
            error.onReloadClick()
            runCurrent()

            // Assert
            assertThat(model.uiState.value).isEqualTo(error.copy(isReloading = true))
            coVerify(exactly = 1) { getMachineUseCase(EXTERNAL_ID, true) }
        }

    @Test
    fun `GIVEN a retry in flight WHEN it succeeds THEN the order is rendered`() = runTest {
        // Arrange
        val retry = CompletableDeferred<MachineResult>()
        coEvery { getMachineUseCase(EXTERNAL_ID, false) } returns CollectiblesError.Api(IllegalStateException()).left()
        coEvery { getMachineUseCase(EXTERNAL_ID, true) } coAnswers { retry.await() }
        val model = startModel()
        (model.uiState.value as CollectiblesConfirmOrderUM.Error).onReloadClick()
        runCurrent()

        // Act
        retry.complete(createMachine().right())
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(CollectiblesConfirmOrderUM.Content::class.java)
    }

    @Test
    fun `GIVEN a retry in flight WHEN it fails again THEN the error is shown without the spinner`() = runTest {
        // Arrange
        val retry = CompletableDeferred<MachineResult>()
        coEvery { getMachineUseCase(EXTERNAL_ID, false) } returns CollectiblesError.Api(IllegalStateException()).left()
        coEvery { getMachineUseCase(EXTERNAL_ID, true) } coAnswers { retry.await() }
        val model = startModel()
        val error = model.uiState.value as CollectiblesConfirmOrderUM.Error
        error.onReloadClick()
        runCurrent()

        // Act
        retry.complete(CollectiblesError.Api(IllegalStateException()).left())
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isEqualTo(error)
    }

    @Test
    fun `GIVEN the pack is open WHEN back tapped THEN the flow is left`() = runTest {
        // Arrange
        val model = startModel()

        // Act
        model.uiState.value.onBackClick()

        // Assert
        coVerify(exactly = 1) { router.pop() }
    }

    /** Approving cannot submit anything yet, so the model must not reach for the machine again. */
    @Test
    fun `GIVEN the pack is open WHEN approve tapped THEN no further request is made`() = runTest {
        // Arrange
        val model = startModel()
        val content = model.uiState.value as CollectiblesConfirmOrderUM.Content

        // Act
        content.onApproveClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { getMachineUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN the pack is open WHEN approve tapped THEN the button turns into a disabled progress label`() = runTest {
        // Arrange
        val model = startModel()
        val content = model.uiState.value as CollectiblesConfirmOrderUM.Content

        // Act
        content.onApproveClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.order().button)
            .isEqualTo(
                CollectiblesConfirmOrderUM.ButtonUM(textRef = stringReference("Opening pack…"), isEnabled = false),
            )
    }

    @Test
    fun `GIVEN the vault covers the total WHEN model created THEN approve is available`() = runTest {
        // Arrange
        coEvery { getMachineUseCase(any(), any()) } returns createMachine(
            price = BigDecimal("50"),
            feeAmount = BigDecimal("1"),
        ).right()
        statusFlow.value = onboarded(usdcAmount = "51")

        // Act
        val model = startModel()

        // Assert
        assertThat(model.order().button.isEnabled).isTrue()
    }

    @Test
    fun `GIVEN the vault is short of the total WHEN model created THEN approve is blocked`() = runTest {
        // Arrange
        coEvery { getMachineUseCase(any(), any()) } returns createMachine(
            price = BigDecimal("50"),
            feeAmount = BigDecimal("1"),
        ).right()
        statusFlow.value = onboarded(usdcAmount = "50.99")

        // Act
        val model = startModel()

        // Assert
        assertThat(model.order().button.isEnabled).isFalse()
    }

    @Test
    fun `GIVEN approve is blocked WHEN the vault is topped up THEN approve becomes available`() = runTest {
        // Arrange
        statusFlow.value = onboarded(usdcAmount = "0")
        val model = startModel()

        // Act
        statusFlow.value = onboarded(usdcAmount = "1000")
        advanceUntilIdle()

        // Assert
        assertThat(model.order().button.isEnabled).isTrue()
    }

    private fun CollectiblesConfirmOrderModel.order() = (uiState.value as CollectiblesConfirmOrderUM.Content).order

    private fun onboarded(usdcAmount: String) = CollectiblesAccountStatusValue.Onboarded(
        source = StatusSource.ACTUAL,
        vaultBalance = MockCollectibles.vaultBalance(amount = BigDecimal(usdcAmount), fiatRate = BigDecimal.ONE),
    )

    private fun TestScope.startModel(): CollectiblesConfirmOrderModel {
        val model = CollectiblesConfirmOrderModel(
            dispatchers = createTestingCoroutineDispatcherProvider(),
            getMachineUseCase = getMachineUseCase,
            accountStatusSupplier = accountStatusSupplier,
            router = router,
            paramsContainer = MutableParamsContainer(
                CollectiblesConfirmOrderComponent.Params(
                    userWalletId = USER_WALLET_ID,
                    externalId = EXTERNAL_ID,
                    isTurboEnabled = false,
                ),
            ),
        ).also { this@CollectiblesConfirmOrderModelTest.model = it }

        advanceUntilIdle()

        return model
    }

    private companion object {

        const val EXTERNAL_ID = "pokemon_1"
        val USER_WALLET_ID = UserWalletId("011")
    }
}

private typealias MachineResult = Either<CollectiblesError, CollectibleGachaMachine>