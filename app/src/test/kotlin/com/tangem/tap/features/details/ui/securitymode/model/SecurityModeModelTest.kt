package com.tangem.tap.features.details.ui.securitymode.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.CompletionResult
import com.tangem.common.SuccessResponse
import com.tangem.common.core.TangemSdkError
import com.tangem.common.routing.AppRouter
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.core.analytics.api.AnalyticsErrorHandler
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.card.configs.GenericCardConfig
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.features.details.redux.SecurityOption
import com.tangem.tap.features.details.ui.cardsettings.domain.CardSettingsInteractor
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SecurityModeModelTest {

    private val tangemSdkManager: TangemSdkManager = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val analyticsErrorHandler: AnalyticsErrorHandler = mockk(relaxed = true)
    private val appRouter: AppRouter = mockk(relaxed = true)

    private val cardSettingsInteractor = CardSettingsInteractor()

    @BeforeEach
    fun resetMocks() {
        clearMocks(tangemSdkManager, analyticsEventHandler, analyticsErrorHandler, appRouter)
        cardSettingsInteractor.clear()
    }

    @Test
    fun `GIVEN long tap card WHEN model created THEN long tap is selected and save changes disabled`() = runTest {
        // Arrange
        cardSettingsInteractor.initialize(createTwinScanResponse())

        // Act
        val model = createModel()

        // Assert
        val state = model.screenState.value
        assertThat(state.selectedSecurityMode).isEqualTo(SecurityOption.LongTap)
        assertThat(state.isSaveChangesEnabled).isFalse()
        assertThat(state.availableOptions).containsExactly(SecurityOption.LongTap, SecurityOption.PassCode)

        model.onDestroy()
    }

    @Test
    fun `GIVEN passcode selected WHEN save changes succeeds THEN scan response updated and screen popped`() = runTest {
        // Arrange
        cardSettingsInteractor.initialize(createTwinScanResponse())
        coEvery { tangemSdkManager.setPasscode(any()) } returns CompletionResult.Success(
            SuccessResponse(cardId = CARD_ID),
        )

        val model = createModel()

        // Act
        model.screenState.value.onNewModeSelected(SecurityOption.PassCode)
        model.screenState.value.onSaveChangesClicked()
        advanceUntilIdle()

        // Assert
        val card = cardSettingsInteractor.scannedScanResponse.value?.card
        assertThat(card?.isPasscodeSet).isTrue()
        assertThat(card?.isAccessCodeSet).isFalse()
        coVerify(exactly = 1) { tangemSdkManager.setPasscode(any()) }
        verify(exactly = 1) { appRouter.pop(any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN passcode selected WHEN user cancels save changes THEN scan response is not updated`() = runTest {
        // Arrange
        cardSettingsInteractor.initialize(createTwinScanResponse())
        coEvery { tangemSdkManager.setPasscode(any()) } returns CompletionResult.Failure(TangemSdkError.UserCancelled())

        val model = createModel()

        // Act
        model.screenState.value.onNewModeSelected(SecurityOption.PassCode)
        model.screenState.value.onSaveChangesClicked()
        advanceUntilIdle()

        // Assert
        val card = cardSettingsInteractor.scannedScanResponse.value?.card
        assertThat(card?.isPasscodeSet).isFalse()
        assertThat(card?.isAccessCodeSet).isFalse()
        verify(exactly = 0) { appRouter.pop(any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN passcode selected WHEN user cancels save changes THEN selection rolled back to long tap`() = runTest {
        // Arrange
        cardSettingsInteractor.initialize(createTwinScanResponse())
        coEvery { tangemSdkManager.setPasscode(any()) } returns CompletionResult.Failure(TangemSdkError.UserCancelled())

        val model = createModel()

        // Act
        model.screenState.value.onNewModeSelected(SecurityOption.PassCode)
        model.screenState.value.onSaveChangesClicked()
        advanceUntilIdle()

        // Assert
        val state = model.screenState.value
        assertThat(state.selectedSecurityMode).isEqualTo(SecurityOption.LongTap)
        assertThat(state.isSaveChangesEnabled).isFalse()

        model.onDestroy()
    }

    @Test
    fun `GIVEN passcode card WHEN long tap change is cancelled THEN passcode stays set`() = runTest {
        // Arrange
        cardSettingsInteractor.initialize(createTwinScanResponse(isPasscodeSet = true))
        coEvery { tangemSdkManager.setLongTap(any()) } returns CompletionResult.Failure(TangemSdkError.UserCancelled())

        val model = createModel()

        // Act
        model.screenState.value.onNewModeSelected(SecurityOption.LongTap)
        model.screenState.value.onSaveChangesClicked()
        advanceUntilIdle()

        // Assert
        assertThat(cardSettingsInteractor.scannedScanResponse.value?.card?.isPasscodeSet).isTrue()
        assertThat(model.screenState.value.selectedSecurityMode).isEqualTo(SecurityOption.PassCode)

        model.onDestroy()
    }

    private fun TestScope.createModel(): SecurityModeModel {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        return SecurityModeModel(
            dispatchers = TestingCoroutineDispatcherProvider(
                main = testDispatcher,
                mainImmediate = testDispatcher,
                io = testDispatcher,
                default = testDispatcher,
                single = testDispatcher,
            ),
            tangemSdkManager = tangemSdkManager,
            cardSettingsInteractor = cardSettingsInteractor,
            analyticsEventHandler = analyticsEventHandler,
            analyticsErrorHandler = analyticsErrorHandler,
            appRouter = appRouter,
        )
    }

    /** Twin card, the only product where [SecurityOption.PassCode] can be chosen */
    private fun createTwinScanResponse(isAccessCodeSet: Boolean = false, isPasscodeSet: Boolean = false): ScanResponse {
        val scanResponse = MockScanResponseFactory.create(
            cardConfig = GenericCardConfig(maxWalletCount = 1),
            derivedKeys = emptyMap(),
        )

        return scanResponse.copy(
            productType = ProductType.Twins,
            card = scanResponse.card.copy(
                cardId = CARD_ID,
                isAccessCodeSet = isAccessCodeSet,
                isPasscodeSet = isPasscodeSet,
            ),
        )
    }

    private companion object {
        const val CARD_ID = "CB79000000000000"
    }
}