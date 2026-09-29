package com.tangem.features.hotwallet.accesscode

import arrow.core.right
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.hotwallet.IsAccessCodeSimpleUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.settings.CanUseBiometryUseCase
import com.tangem.domain.settings.ShouldShowAskBiometryUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * The PIN field keeps invoking the `onAccessCodeChange` it got in the last composition until the model's
 * `onAccessCodeChange = {}` reaches it, so one completed code can arrive several times through the same stale lambda.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class AccessCodeModelTest {

    private val paramsContainer: ParamsContainer = mockk()
    private val callbacks: AccessCodeComponent.ModelCallbacks = mockk(relaxUnitFun = true)
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val canUseBiometryUseCase: CanUseBiometryUseCase = mockk()
    private val shouldShowAskBiometryUseCase: ShouldShowAskBiometryUseCase = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxUnitFun = true)

    private val walletId = UserWalletId("011")

    @BeforeEach
    fun setUp() {
        clearMocks(
            paramsContainer,
            callbacks,
            getUserWalletUseCase,
            canUseBiometryUseCase,
            shouldShowAskBiometryUseCase,
            uiMessageSender,
        )
        every { getUserWalletUseCase(walletId) } returns mockk<UserWallet.Hot>().right()
        coEvery { canUseBiometryUseCase() } returns true
        coEvery { shouldShowAskBiometryUseCase() } returns true
    }

    @Test
    fun `GIVEN full code entered once WHEN success delay passes THEN onNewAccessCodeInput called with the code`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this, accessCodeToConfirm = null)

            // Act
            model.uiState.value.onAccessCodeChange(NEW_CODE)
            advanceTimeBy(SUCCESS_DELAY_PASSED_MS)
            runCurrent()

            // Assert
            verify(exactly = 1) { callbacks.onNewAccessCodeInput(walletId, NEW_CODE) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN full code delivered 3 times before recomposition WHEN success delay passes THEN onNewAccessCodeInput called once with the code`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this, accessCodeToConfirm = null)
            val staleOnAccessCodeChange = model.uiState.value.onAccessCodeChange

            // Act
            repeat(times = 3) { staleOnAccessCodeChange(NEW_CODE) }
            advanceTimeBy(SUCCESS_DELAY_PASSED_MS)
            runCurrent()

            // Assert
            verify(exactly = 1) { callbacks.onNewAccessCodeInput(walletId, NEW_CODE) }
            verify(exactly = 0) { callbacks.onNewAccessCodeInput(walletId, "") }
            model.onDestroy()
        }

    @Test
    fun `GIVEN simple code delivered twice before recomposition WHEN success delay passes THEN validation dialog shown once`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this, accessCodeToConfirm = null)
            val staleOnAccessCodeChange = model.uiState.value.onAccessCodeChange

            // Act
            repeat(times = 2) { staleOnAccessCodeChange(SIMPLE_CODE) }
            advanceTimeBy(SUCCESS_DELAY_PASSED_MS)
            runCurrent()

            // Assert
            verify(exactly = 1) { uiMessageSender.send(any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN confirm code delivered twice before recomposition WHEN biometry prompt pending THEN access code update started once`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this, accessCodeToConfirm = NEW_CODE)
            val staleOnAccessCodeChange = model.uiState.value.onAccessCodeChange

            // Act
            repeat(times = 2) { staleOnAccessCodeChange(NEW_CODE) }
            runCurrent()

            // Assert
            verify(exactly = 1) { callbacks.onAccessCodeUpdateStarted(walletId) }
            verify(exactly = 1) { uiMessageSender.send(any()) }
            model.onDestroy()
        }

    private fun createModel(testScope: TestScope, accessCodeToConfirm: String?): AccessCodeModel {
        every { paramsContainer.require<AccessCodeComponent.Params>() } returns AccessCodeComponent.Params(
            accessCodeToConfirm = accessCodeToConfirm,
            userWalletId = walletId,
            callbacks = callbacks,
        )
        return AccessCodeModel(
            paramsContainer = paramsContainer,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            getUserWalletUseCase = getUserWalletUseCase,
            getHotWalletContextualUnlockUseCase = mockk(relaxed = true),
            clearHotWalletContextualUnlockUseCase = mockk(relaxed = true),
            hotWalletAccessor = mockk(relaxed = true),
            userWalletsListRepository = mockk(relaxed = true),
            walletsRepository = mockk(relaxed = true),
            tangemHotSdk = mockk(relaxed = true),
            shouldShowAskBiometryUseCase = shouldShowAskBiometryUseCase,
            setAskBiometryShownUseCase = mockk(relaxed = true),
            canUseBiometryUseCase = canUseBiometryUseCase,
            isAccessCodeSimpleUseCase = IsAccessCodeSimpleUseCase(),
            uiMessageSender = uiMessageSender,
        )
    }

    private companion object {
        const val NEW_CODE = "730491"
        const val SIMPLE_CODE = "111111"
        const val SUCCESS_DELAY_PASSED_MS = 401L
    }
}