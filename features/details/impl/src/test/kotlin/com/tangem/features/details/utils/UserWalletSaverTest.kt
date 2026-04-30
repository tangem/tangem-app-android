package com.tangem.features.details.utils

import arrow.core.Either
import com.tangem.common.core.TangemError
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.common.wallets.error.SaveWalletError
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.builder.ColdUserWalletBuilder
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.features.details.impl.R
import com.tangem.features.onboarding.v2.OnboardingV2FeatureToggles
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class UserWalletSaverTest {

    private val scanCardProcessor: ScanCardProcessor = mockk()
    private val saveWalletUseCase: SaveWalletUseCase = mockk()
    private val coldUserWalletBuilderFactory: ColdUserWalletBuilder.Factory = mockk()
    private val messageSender: UiMessageSender = mockk(relaxUnitFun = true)
    private val router: Router = mockk(relaxUnitFun = true)
    private val onboardingV2FeatureToggles: OnboardingV2FeatureToggles = mockk()

    private val scanResponse: ScanResponse = mockk()
    private val userWalletId: UserWalletId = UserWalletId("011")
    private val userWallet: UserWallet.Cold = mockk {
        every { walletId } returns userWalletId
    }

    @BeforeEach
    fun setUp() {
        every { onboardingV2FeatureToggles.isAddressSyncEnabled } returns false
    }

    @Test
    fun `GIVEN onWalletNotCreated WHEN scanAndSaveUserWallet THEN no message AND no save`() = runTest {
        mockScanCallback(callbackName = ON_WALLET_NOT_CREATED)

        createSaver().scanAndSaveUserWallet(this)

        verify(exactly = 0) { messageSender.send(any()) }
        coVerify(exactly = 0) { saveWalletUseCase.invoke(any(), any(), any()) }
    }

    @Test
    fun `GIVEN onCancel WHEN scanAndSaveUserWallet THEN no message AND no save`() = runTest {
        mockScanCallback(callbackName = ON_CANCEL)

        createSaver().scanAndSaveUserWallet(this)

        verify(exactly = 0) { messageSender.send(any()) }
        coVerify(exactly = 0) { saveWalletUseCase.invoke(any(), any(), any()) }
    }

    @Test
    fun `GIVEN onFailure with messageResId WHEN scanAndSaveUserWallet THEN SnackbarMessage with resource is sent`() =
        runTest {
            val tangemError = mockk<TangemError> {
                every { silent } returns false
                every { messageResId } returns R.string.common_unknown_error
                every { customMessage } returns "any"
            }
            mockScanFailure(tangemError)

            createSaver().scanAndSaveUserWallet(this)

            verify {
                messageSender.send(
                    message = SnackbarMessage(message = resourceReference(R.string.common_unknown_error)),
                )
            }
            coVerify(exactly = 0) { saveWalletUseCase.invoke(any(), any(), any()) }
        }

    @Test
    fun `GIVEN onFailure without messageResId WHEN scanAndSaveUserWallet THEN SnackbarMessage with custom message`() =
        runTest {
            val tangemError = mockk<TangemError> {
                every { silent } returns false
                every { messageResId } returns null
                every { customMessage } returns "Custom error"
            }
            mockScanFailure(tangemError)

            createSaver().scanAndSaveUserWallet(this)

            verify {
                messageSender.send(
                    message = SnackbarMessage(message = stringReference("Custom error")),
                )
            }
            coVerify(exactly = 0) { saveWalletUseCase.invoke(any(), any(), any()) }
        }

    @Test
    fun `GIVEN onFailure silent WHEN scanAndSaveUserWallet THEN no message is sent`() = runTest {
        val tangemError = mockk<TangemError> {
            every { silent } returns true
            every { messageResId } returns null
            every { customMessage } returns "any"
        }
        mockScanFailure(tangemError)

        createSaver().scanAndSaveUserWallet(this)

        verify(exactly = 0) { messageSender.send(any()) }
        coVerify(exactly = 0) { saveWalletUseCase.invoke(any(), any(), any()) }
    }

    @Test
    fun `GIVEN onSuccess AND save success AND addressSync disabled WHEN scanAndSaveUserWallet THEN popTo Wallet`() =
        runTest {
            every { onboardingV2FeatureToggles.isAddressSyncEnabled } returns false
            mockScanSuccess(scanResponse)
            mockBuilderReturns(userWallet)
            coEvery { saveWalletUseCase.invoke(userWallet, false, any()) } returns Either.Right(Unit)

            createSaver().scanAndSaveUserWallet(this)

            verify { router.popTo(routeClass = AppRoute.Wallet::class, onComplete = any()) }
            verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
            verify(exactly = 0) { messageSender.send(any()) }
        }

    @Test
    fun `GIVEN onSuccess AND save success AND addressSync enabled WHEN scanAndSaveUserWallet THEN push Onboarding`() =
        runTest {
            every { onboardingV2FeatureToggles.isAddressSyncEnabled } returns true
            mockScanSuccess(scanResponse)
            mockBuilderReturns(userWallet)
            coEvery { saveWalletUseCase.invoke(userWallet, false, any()) } returns Either.Right(Unit)

            createSaver().scanAndSaveUserWallet(this)

            verify {
                router.push(
                    route = AppRoute.Onboarding(
                        scanResponse = scanResponse,
                        mode = AppRoute.Onboarding.Mode.AddressSync(
                            userWalletId = userWalletId,
                            isWalletStarted = true,
                        ),
                    ),
                    onComplete = any(),
                )
            }
            verify(exactly = 0) { router.popTo(routeClass = any(), onComplete = any()) }
        }

    @Test
    fun `GIVEN onSuccess AND createUserWallet returns null WHEN scanAndSaveUserWallet THEN unknown error message`() =
        runTest {
            mockScanSuccess(scanResponse)
            mockBuilderReturns(null)

            createSaver().scanAndSaveUserWallet(this)

            verify {
                messageSender.send(
                    message = SnackbarMessage(message = resourceReference(R.string.common_unknown_error)),
                )
            }
            coVerify(exactly = 0) { saveWalletUseCase.invoke(any(), any(), any()) }
        }

    @Test
    fun `GIVEN onSuccess AND save WalletAlreadySaved WHEN scanAndSaveUserWallet THEN DialogMessage is sent`() =
        runTest {
            mockScanSuccess(scanResponse)
            mockBuilderReturns(userWallet)
            coEvery { saveWalletUseCase.invoke(userWallet, false, any()) } returns Either.Left(
                SaveWalletError.WalletAlreadySaved(messageId = R.string.user_wallet_list_error_wallet_already_saved),
            )

            createSaver().scanAndSaveUserWallet(this)

            verify {
                messageSender.send(
                    message = DialogMessage(
                        message = resourceReference(R.string.user_wallet_list_error_wallet_already_saved),
                    ),
                )
            }
            verify(exactly = 0) { router.popTo(routeClass = any(), onComplete = any()) }
            verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
        }

    @Test
    fun `GIVEN onSuccess AND save DataError with messageId WHEN scanAndSaveUserWallet THEN snackbar with resource`() =
        runTest {
            mockScanSuccess(scanResponse)
            mockBuilderReturns(userWallet)
            coEvery { saveWalletUseCase.invoke(userWallet, false, any()) } returns Either.Left(
                SaveWalletError.DataError(messageId = R.string.common_unknown_error),
            )

            createSaver().scanAndSaveUserWallet(this)

            verify {
                messageSender.send(
                    message = SnackbarMessage(message = resourceReference(R.string.common_unknown_error)),
                )
            }
            verify(exactly = 0) { router.popTo(routeClass = any(), onComplete = any()) }
        }

    @Test
    fun `GIVEN onSuccess AND save DataError without messageId WHEN scanAndSaveUserWallet THEN unknown error snackbar`() =
        runTest {
            mockScanSuccess(scanResponse)
            mockBuilderReturns(userWallet)
            coEvery { saveWalletUseCase.invoke(userWallet, false, any()) } returns Either.Left(
                SaveWalletError.DataError(messageId = null),
            )

            createSaver().scanAndSaveUserWallet(this)

            verify {
                messageSender.send(
                    message = SnackbarMessage(message = resourceReference(R.string.common_unknown_error)),
                )
            }
            verify(exactly = 0) { router.popTo(routeClass = any(), onComplete = any()) }
        }

    private fun mockScanCallback(callbackName: String) {
        coEvery {
            scanCardProcessor.scan(
                analyticsSource = any(),
                shouldCheckIsAlreadyActivated = any(),
                cardId = any(),
                onProgressStateChange = any(),
                onWalletNotCreated = any(),
                onCancel = any(),
                onFailure = any(),
                onSuccess = any(),
            )
        } coAnswers {
            when (callbackName) {
                ON_WALLET_NOT_CREATED -> arg<suspend () -> Unit>(ON_WALLET_NOT_CREATED_INDEX).invoke()
                ON_CANCEL -> arg<suspend () -> Unit>(ON_CANCEL_INDEX).invoke()
            }
        }
    }

    private fun mockScanFailure(tangemError: TangemError) {
        coEvery {
            scanCardProcessor.scan(
                analyticsSource = any(),
                shouldCheckIsAlreadyActivated = any(),
                cardId = any(),
                onProgressStateChange = any(),
                onWalletNotCreated = any(),
                onCancel = any(),
                onFailure = any(),
                onSuccess = any(),
            )
        } coAnswers {
            arg<suspend (TangemError) -> Unit>(ON_FAILURE_INDEX).invoke(tangemError)
        }
    }

    private fun mockScanSuccess(scanResponse: ScanResponse) {
        coEvery {
            scanCardProcessor.scan(
                analyticsSource = any(),
                shouldCheckIsAlreadyActivated = any(),
                cardId = any(),
                onProgressStateChange = any(),
                onWalletNotCreated = any(),
                onCancel = any(),
                onFailure = any(),
                onSuccess = any(),
            )
        } coAnswers {
            arg<suspend (ScanResponse) -> Unit>(ON_SUCCESS_INDEX).invoke(scanResponse)
        }
    }

    private fun mockBuilderReturns(userWallet: UserWallet.Cold?) {
        val builder: ColdUserWalletBuilder = mockk {
            every { build() } returns userWallet
        }
        every { coldUserWalletBuilderFactory.create(scanResponse = any()) } returns builder
    }

    private fun createSaver(): UserWalletSaver {
        return UserWalletSaver(
            scanCardProcessor = scanCardProcessor,
            saveWalletUseCase = saveWalletUseCase,
            coldUserWalletBuilderFactory = coldUserWalletBuilderFactory,
            messageSender = messageSender,
            router = router,
            onboardingV2FeatureToggles = onboardingV2FeatureToggles,
        )
    }

    private companion object {
        const val ON_WALLET_NOT_CREATED = "onWalletNotCreated"
        const val ON_CANCEL = "onCancel"

        const val ON_WALLET_NOT_CREATED_INDEX = 4
        const val ON_CANCEL_INDEX = 5
        const val ON_FAILURE_INDEX = 6
        const val ON_SUCCESS_INDEX = 7
    }
}