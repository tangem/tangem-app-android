package com.tangem.features.hotwallet.manualbackup.phrase.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.core.TangemSdkError
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.ExportSeedPhraseUseCase
import com.tangem.domain.wallets.usecase.GetHotWalletContextualUnlockUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.UnlockHotWalletContextualUseCase
import com.tangem.features.hotwallet.manualbackup.phrase.ManualBackupPhraseComponent
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.hot.sdk.model.SeedPhrasePrivateInfo
import com.tangem.hot.sdk.model.UnlockHotWallet
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ManualBackupPhraseModelTest {

    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val exportSeedPhraseUseCase: ExportSeedPhraseUseCase = mockk()
    private val getHotWalletContextualUnlockUseCase: GetHotWalletContextualUnlockUseCase = mockk()
    private val unlockHotWalletContextualUseCase: UnlockHotWalletContextualUseCase = mockk()
    private val paramsContainer: ParamsContainer = mockk()
    private val callbacks: ManualBackupPhraseComponent.ModelCallbacks = mockk(relaxUnitFun = true)

    private val walletId = UserWalletId("011")
    private val hotWalletId: HotWalletId = mockk()
    private val unlockHotWallet: UnlockHotWallet = mockk()
    private val hotWallet: UserWallet.Hot = mockk {
        every { walletId } returns this@ManualBackupPhraseModelTest.walletId
        every { hotWalletId } returns this@ManualBackupPhraseModelTest.hotWalletId
    }

    // Fixed fake word list — never a real secret.
    private val mnemonic: Mnemonic = mockk {
        every { mnemonicComponents } returns listOf("alpha", "bravo", "charlie")
    }
    private val privateInfo = SeedPhrasePrivateInfo(mnemonic = mnemonic, passphrase = null)

    @BeforeEach
    fun setUp() {
        clearMocks(
            getUserWalletUseCase,
            exportSeedPhraseUseCase,
            getHotWalletContextualUnlockUseCase,
            unlockHotWalletContextualUseCase,
            paramsContainer,
        )
        every { paramsContainer.require<ManualBackupPhraseComponent.Params>() } returns
            ManualBackupPhraseComponent.Params(userWalletId = walletId, callbacks = callbacks)
        every { getUserWalletUseCase(walletId) } returns hotWallet.right()
        coEvery { exportSeedPhraseUseCase.invoke(hotWalletId) } returns privateInfo.right()
    }

    @Test
    fun `GIVEN wallet without held unlock WHEN model created THEN wallet unlocked AND words populated`() = runTest {
        // Arrange
        coEvery { getHotWalletContextualUnlockUseCase(hotWalletId) } returns null.right()
        coEvery { unlockHotWalletContextualUseCase(hotWalletId) } returns unlockHotWallet.right()

        // Act
        val model = createModel(this)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { unlockHotWalletContextualUseCase(hotWalletId) }
        assertThat(model.uiState.value.words.map { it.mnemonic })
            .containsExactly("alpha", "bravo", "charlie")
            .inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN wallet with held unlock WHEN model created THEN no unlock prompt AND words populated`() = runTest {
        // Arrange
        coEvery { getHotWalletContextualUnlockUseCase(hotWalletId) } returns unlockHotWallet.right()

        // Act
        val model = createModel(this)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { unlockHotWalletContextualUseCase(any()) }
        assertThat(model.uiState.value.words).hasSize(3)
        model.onDestroy()
    }

    @Test
    fun `GIVEN unlock dismissed WHEN model created THEN words stay empty AND export not called`() = runTest {
        // Arrange
        coEvery { getHotWalletContextualUnlockUseCase(hotWalletId) } returns null.right()
        coEvery { unlockHotWalletContextualUseCase(hotWalletId) } returns TangemSdkError.UserCancelled().left()

        // Act
        val model = createModel(this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.words).isEmpty()
        coVerify(exactly = 0) { exportSeedPhraseUseCase.invoke(any()) }
        model.onDestroy()
    }

    private fun createModel(testScope: TestScope): ManualBackupPhraseModel {
        return ManualBackupPhraseModel(
            paramsContainer = paramsContainer,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            getUserWalletUseCase = getUserWalletUseCase,
            exportSeedPhraseUseCase = exportSeedPhraseUseCase,
            getHotWalletContextualUnlockUseCase = getHotWalletContextualUnlockUseCase,
            unlockHotWalletContextualUseCase = unlockHotWalletContextualUseCase,
        )
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }
}