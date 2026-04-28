package com.tangem.tap.domain

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.TestAppCoroutineScope
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.domain.card.configs.GenericCardConfig
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.sdk.api.TangemSdkManager
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultUserWalletSelectedHandlerTest {

    private val trackingContextProxy = mockk<TrackingContextProxy>(relaxed = true)
    private val tangemSdkManager = mockk<TangemSdkManager>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>()
    private val cardSdkConfigRepository = mockk<CardSdkConfigRepository>(relaxed = true)
    private val appScope = TestAppCoroutineScope()

    private lateinit var handler: DefaultUserWalletSelectedHandler

    @BeforeEach
    fun setup() {
        clearMocks(trackingContextProxy, tangemSdkManager, settingsRepository, cardSdkConfigRepository)
        handler = DefaultUserWalletSelectedHandler(
            trackingContextProxy = trackingContextProxy,
            tangemSdkManager = tangemSdkManager,
            settingsRepository = settingsRepository,
            cardSdkConfigRepository = cardSdkConfigRepository,
            appCoroutineScope = appScope,
        )
    }

    @Test
    fun `cold wallet with access code and save-codes enabled applies biometric policy`() = runTest {
        val userWallet = coldWalletWith(isAccessCodeSet = true)
        coEvery { settingsRepository.shouldSaveAccessCodes() } returns true

        handler(userWallet)

        verify(exactly = 1) { trackingContextProxy.setContext(userWallet) }
        verify(exactly = 1) { tangemSdkManager.changeDisplayedCardIdNumbersCount(userWallet.scanResponse) }
        verify(exactly = 1) { cardSdkConfigRepository.setAccessCodeRequestPolicy(isBiometricsRequestPolicy = true) }
    }

    @Test
    fun `cold wallet without access code keeps biometric policy off even when save-codes enabled`() = runTest {
        assertThat(capturePolicyFor(shouldSaveAccessCodes = true, isAccessCodeSet = false)).isFalse()
    }

    @Test
    fun `cold wallet with access code keeps biometric policy off when save-codes disabled`() = runTest {
        assertThat(capturePolicyFor(shouldSaveAccessCodes = false, isAccessCodeSet = true)).isFalse()
    }

    @Test
    fun `cold wallet without access code and save-codes disabled keeps biometric policy off`() = runTest {
        assertThat(capturePolicyFor(shouldSaveAccessCodes = false, isAccessCodeSet = false)).isFalse()
    }

    @Test
    fun `hot wallet only updates tracking context`() = runTest {
        val hotWallet = mockk<UserWallet.Hot>()

        handler(hotWallet)

        verify(exactly = 1) { trackingContextProxy.setContext(hotWallet) }
        verify(exactly = 0) { tangemSdkManager.changeDisplayedCardIdNumbersCount(any()) }
        coVerify(exactly = 0) { settingsRepository.shouldSaveAccessCodes() }
        verify(exactly = 0) { cardSdkConfigRepository.setAccessCodeRequestPolicy(any()) }
    }

    @Test
    fun `consecutive invocations both run policy update`() = runTest {
        val firstWallet = coldWalletWith(isAccessCodeSet = true)
        val secondWallet = coldWalletWith(isAccessCodeSet = false)
        coEvery { settingsRepository.shouldSaveAccessCodes() } returns true

        handler(firstWallet)
        handler(secondWallet)

        verify(exactly = 1) { cardSdkConfigRepository.setAccessCodeRequestPolicy(isBiometricsRequestPolicy = true) }
        verify(exactly = 1) { cardSdkConfigRepository.setAccessCodeRequestPolicy(isBiometricsRequestPolicy = false) }
    }

    @Test
    fun `new invocation cancels in-flight job so only latest side effects are applied`() = runTest {
        val firstWallet = coldWalletWith(isAccessCodeSet = true)
        val secondWallet = coldWalletWith(isAccessCodeSet = false)

        val firstCallGate = CompletableDeferred<Boolean>()
        var callIndex = 0
        coEvery { settingsRepository.shouldSaveAccessCodes() } coAnswers {
            callIndex++
            if (callIndex == 1) firstCallGate.await() else true
        }

        val firstHandlerJob = launch { handler(firstWallet) }
        runCurrent()

        handler(secondWallet)

        firstCallGate.complete(true)
        firstHandlerJob.join()

        verify(exactly = 0) { cardSdkConfigRepository.setAccessCodeRequestPolicy(isBiometricsRequestPolicy = true) }
        verify(exactly = 1) { cardSdkConfigRepository.setAccessCodeRequestPolicy(isBiometricsRequestPolicy = false) }
        verify(exactly = 1) { trackingContextProxy.setContext(secondWallet) }
    }

    private suspend fun capturePolicyFor(shouldSaveAccessCodes: Boolean, isAccessCodeSet: Boolean): Boolean {
        val userWallet = coldWalletWith(isAccessCodeSet = isAccessCodeSet)
        coEvery { settingsRepository.shouldSaveAccessCodes() } returns shouldSaveAccessCodes
        val captured = slot<Boolean>()

        handler(userWallet)

        verify { cardSdkConfigRepository.setAccessCodeRequestPolicy(isBiometricsRequestPolicy = capture(captured)) }
        return captured.captured
    }

    private fun coldWalletWith(isAccessCodeSet: Boolean): UserWallet.Cold {
        val baseScanResponse = MockScanResponseFactory.create(
            cardConfig = GenericCardConfig(maxWalletCount = 2),
            derivedKeys = emptyMap(),
        )
        val scanResponse: ScanResponse = baseScanResponse.copy(
            card = baseScanResponse.card.copy(
                cardId = if (isAccessCodeSet) "CARD-WITH-CODE" else "CARD-NO-CODE",
                isAccessCodeSet = isAccessCodeSet,
            ),
        )
        return MockUserWalletFactory.create(scanResponse = scanResponse)
    }
}