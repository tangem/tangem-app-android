package com.tangem.features.survey.impl.nativesurvey.opener

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.features.survey.SurveyFeatureToggles
import com.tangem.features.survey.impl.nativesurvey.NativeSurveyTestData.SURVEY_ID
import com.tangem.features.survey.impl.nativesurvey.NativeSurveyTestData.npsSurvey
import com.tangem.features.survey.impl.nativesurvey.repository.LoadedNativeSurvey
import com.tangem.features.survey.impl.nativesurvey.repository.NativeSurveyRepository
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class NativeSurveyOpenerTest {

    private val repository: NativeSurveyRepository = mockk()
    private val surveyFeatureToggles: SurveyFeatureToggles = mockk()
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase = mockk()
    private val uiMessageSender: UiMessageSender = mockk()
    private val router: Router = mockk()
    private val userWallet: UserWallet.Cold = mockk()
    private val anotherUserWallet: UserWallet.Cold = mockk()

    private var stack: List<Route> = emptyList()
    private val messages = mutableListOf<UiMessage>()
    private val deferredNavigations = ArrayDeque<() -> Unit>()

    @BeforeEach
    fun setUp() {
        clearMocks(
            repository,
            surveyFeatureToggles,
            getSelectedWalletSyncUseCase,
            uiMessageSender,
            router,
            userWallet,
            anotherUserWallet,
        )
        stack = listOf(AppRoute.Wallet)
        messages.clear()
        deferredNavigations.clear()
        every { userWallet.walletId } returns USER_WALLET_ID
        every { userWallet.isLocked } returns false
        every { anotherUserWallet.walletId } returns ANOTHER_USER_WALLET_ID
        every { anotherUserWallet.isLocked } returns false
        every { getSelectedWalletSyncUseCase() } returns userWallet.right()
        every { surveyFeatureToggles.isNpsSurveyEnabled } returns true
        coEvery { repository.isAnswered(any()) } returns false
        coEvery { repository.load(any()) } returns LoadedNativeSurvey(npsSurvey(), isFromCache = false)
        justRun { uiMessageSender.send(capture(messages)) }
        every { router.navigate(any(), any()) } answers {
            stack = firstArg<(List<Route>) -> List<Route>>().invoke(stack)
            secondArg<(Boolean) -> Unit>().invoke(true)
        }
    }

    @Test
    fun `GIVEN no selected wallet WHEN opened THEN nothing is loaded or shown`() = runTest {
        // Arrange
        every { getSelectedWalletSyncUseCase() } returns GetUserWalletError.UserWalletNotFound.left()

        // Act
        createOpener().open(REQUEST, router)

        // Assert
        assertThat(stack).containsExactly(AppRoute.Wallet)
        assertThat(messages).isEmpty()
        coVerify(exactly = 0) { repository.load(any()) }
    }

    @Test
    fun `GIVEN a locked wallet WHEN opened THEN nothing is loaded or shown`() = runTest {
        // Arrange
        every { userWallet.isLocked } returns true

        // Act
        createOpener().open(REQUEST, router)

        // Assert
        assertThat(stack).containsExactly(AppRoute.Wallet)
        assertThat(messages).isEmpty()
        coVerify(exactly = 0) { repository.load(any()) }
    }

    @Test
    fun `GIVEN a survey answered within its term WHEN opened THEN Done is shown without a request`() = runTest {
        // Arrange
        coEvery { repository.isAnswered(any()) } returns true

        // Act
        createOpener().open(REQUEST, router)

        // Assert
        assertThat(stack).containsExactly(AppRoute.Wallet, route(isAlreadyAnswered = true, isFromCache = true))
            .inOrder()
        coVerify(exactly = 0) { repository.load(any()) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN a loaded survey WHEN opened THEN it is shown with where it came from`(isFromCache: Boolean) =
        runTest {
            // Arrange
            coEvery { repository.load(any()) } returns LoadedNativeSurvey(npsSurvey(), isFromCache = isFromCache)

            // Act
            createOpener().open(REQUEST, router)

            // Assert
            assertThat(stack).containsExactly(AppRoute.Wallet, route(isFromCache = isFromCache)).inOrder()
            assertThat(messages).isEmpty()
        }

    @Test
    fun `GIVEN the survey cannot be loaded WHEN opened THEN the alert is shown instead`() = runTest {
        // Arrange
        coEvery { repository.load(any()) } returns null

        // Act
        createOpener().open(REQUEST, router)

        // Assert
        assertThat(stack).containsExactly(AppRoute.Wallet)
        assertThat(messages.single()).isInstanceOf(DialogMessage::class.java)
    }

    @Test
    fun `GIVEN a disabled survey type WHEN loaded THEN nothing is shown`() = runTest {
        // Arrange
        every { surveyFeatureToggles.isNpsSurveyEnabled } returns false

        // Act
        createOpener().open(REQUEST, router)

        // Assert
        assertThat(stack).containsExactly(AppRoute.Wallet)
        assertThat(messages).isEmpty()
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN the screen changed while loading WHEN loaded THEN neither the survey nor the alert is shown`(
        isLoaded: Boolean,
    ) = runTest {
        // Arrange
        coEvery { repository.load(any()) } coAnswers {
            stack = listOf(AppRoute.Initial)
            LoadedNativeSurvey(npsSurvey(), isFromCache = false).takeIf { isLoaded }
        }

        // Act
        createOpener().open(REQUEST, router)

        // Assert
        assertThat(stack).containsExactly(AppRoute.Initial)
        assertThat(messages).isEmpty()
    }

    @ParameterizedTest
    @EnumSource(WalletChange::class)
    fun `GIVEN the wallet changed while loading WHEN loaded THEN the survey is not shown`(
        change: WalletChange,
    ) = runTest {
        // Arrange
        coEvery { repository.load(any()) } coAnswers {
            when (change) {
                WalletChange.ANOTHER_SELECTED -> {
                    every { getSelectedWalletSyncUseCase() } returns anotherUserWallet.right()
                }
                WalletChange.LOCKED -> every { userWallet.isLocked } returns true
            }
            LoadedNativeSurvey(npsSurvey(), isFromCache = false)
        }

        // Act
        createOpener().open(REQUEST, router)

        // Assert
        assertThat(stack).containsExactly(AppRoute.Wallet)
        assertThat(messages).isEmpty()
    }

    @Test
    fun `GIVEN a navigation in progress WHEN opened THEN the survey is shown after it`() = runTest {
        // Arrange
        every { router.navigate(any(), any()) } answers {
            val transformer = firstArg<(List<Route>) -> List<Route>>()
            val onComplete = secondArg<(Boolean) -> Unit>()
            deferredNavigations.addLast {
                stack = transformer(stack)
                onComplete(true)
            }
        }
        launch { createOpener().open(REQUEST, router) }
        advanceUntilIdle()

        // Act
        while (deferredNavigations.isNotEmpty()) {
            deferredNavigations.removeFirst().invoke()
            advanceUntilIdle()
        }

        // Assert
        assertThat(stack).containsExactly(AppRoute.Wallet, route()).inOrder()
    }

    @Test
    fun `GIVEN a survey already open WHEN opened again THEN it is not shown twice`() = runTest {
        // Arrange
        stack = listOf(AppRoute.Wallet, route())

        // Act
        createOpener().open(REQUEST, router)

        // Assert
        assertThat(stack).containsExactly(AppRoute.Wallet, route()).inOrder()
    }

    @Test
    fun `GIVEN two openings at once WHEN both are loaded THEN the survey is shown once`() = runTest {
        // Arrange
        val loaded = CompletableDeferred<LoadedNativeSurvey?>()
        coEvery { repository.load(any()) } coAnswers { loaded.await() }
        val opener = createOpener()
        repeat(2) { launch { opener.open(REQUEST, router) } }
        advanceUntilIdle()

        // Act
        loaded.complete(LoadedNativeSurvey(npsSurvey(), isFromCache = false))
        advanceUntilIdle()

        // Assert
        assertThat(stack).containsExactly(AppRoute.Wallet, route()).inOrder()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a token and a type toggle WHEN availability is checked THEN both are required`(
        model: AvailabilityModel,
    ) = runTest {
        // Arrange
        every { repository.isAvailable } returns model.hasToken
        every { surveyFeatureToggles.isNpsSurveyEnabled } returns model.isTypeEnabled

        // Act
        val actual = createOpener().isAvailable

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun TestScope.createOpener() = NativeSurveyOpener(
        repository = repository,
        surveyFeatureToggles = surveyFeatureToggles,
        getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
        dispatchers = createTestingCoroutineDispatcherProvider(),
        uiMessageSender = uiMessageSender,
    )

    private fun route(isAlreadyAnswered: Boolean = false, isFromCache: Boolean = false) = AppRoute.NativeSurvey(
        surveyId = SURVEY_ID,
        source = AppRoute.NativeSurvey.Source.LINK,
        stage = STAGE,
        repeatAfterDays = REPEAT_AFTER_DAYS,
        userWalletId = USER_WALLET_ID,
        isAlreadyAnswered = isAlreadyAnswered,
        isFromCache = isFromCache,
    )

    private fun provideTestModels() = listOf(
        AvailabilityModel(hasToken = true, isTypeEnabled = true, expected = true),
        AvailabilityModel(hasToken = true, isTypeEnabled = false, expected = false),
        AvailabilityModel(hasToken = false, isTypeEnabled = true, expected = false),
    )

    data class AvailabilityModel(val hasToken: Boolean, val isTypeEnabled: Boolean, val expected: Boolean)

    enum class WalletChange { ANOTHER_SELECTED, LOCKED }

    private companion object {
        val USER_WALLET_ID = UserWalletId("0A1B2C3D4E5F")
        val ANOTHER_USER_WALLET_ID = UserWalletId("F5E4D3C2B1A0")
        const val STAGE = "funded_30d"
        const val REPEAT_AFTER_DAYS = 180
        val REQUEST = NativeSurveyOpener.Request(
            surveyId = SURVEY_ID,
            source = AppRoute.NativeSurvey.Source.LINK,
            stage = STAGE,
            repeatAfterDays = REPEAT_AFTER_DAYS,
        )
    }
}