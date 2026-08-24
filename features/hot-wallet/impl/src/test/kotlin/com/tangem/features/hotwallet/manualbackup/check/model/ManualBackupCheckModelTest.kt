package com.tangem.features.hotwallet.manualbackup.check.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.core.TangemSdkError
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.ExportSeedPhraseUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.UpdateWalletUseCase
import com.tangem.features.hotwallet.manualbackup.check.ManualBackupCheckComponent
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.hot.sdk.model.SeedPhrasePrivateInfo
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
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
internal class ManualBackupCheckModelTest {

    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val updateWalletUseCase: UpdateWalletUseCase = mockk()
    private val exportSeedPhraseUseCase: ExportSeedPhraseUseCase = mockk()
    private val paramsContainer: ParamsContainer = mockk()
    private val callbacks: ManualBackupCheckComponent.ModelCallbacks = mockk(relaxUnitFun = true)

    private val walletId = UserWalletId("011")
    private val hotWalletId: HotWalletId = mockk()
    private val hotWallet: UserWallet.Hot = mockk {
        every { walletId } returns this@ManualBackupCheckModelTest.walletId
        every { hotWalletId } returns this@ManualBackupCheckModelTest.hotWalletId
    }

    // Fixed fake word list — never a real secret.
    private val words = List(size = 12) { index -> "word${index + 1}" }
    private val mnemonic: Mnemonic = mockk {
        every { mnemonicComponents } returns words
    }
    private val privateInfo = SeedPhrasePrivateInfo(mnemonic = mnemonic, passphrase = null)

    @BeforeEach
    fun setUp() {
        clearMocks(getUserWalletUseCase, updateWalletUseCase, exportSeedPhraseUseCase, paramsContainer)
        every { paramsContainer.require<ManualBackupCheckComponent.Params>() } returns
            ManualBackupCheckComponent.Params(userWalletId = walletId, callbacks = callbacks)
        every { getUserWalletUseCase(walletId) } returns hotWallet.right()
    }

    @Test
    fun `GIVEN export succeeds WHEN model created THEN check words populated from the phrase`() = runTest {
        // Arrange
        coEvery { exportSeedPhraseUseCase.invoke(hotWalletId) } returns privateInfo.right()

        // Act
        val model = createModel(this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.words).containsExactly("word2", "word7", "word11").inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN export fails WHEN model created THEN words stay empty AND complete stays disabled`() = runTest {
        // Arrange
        coEvery { exportSeedPhraseUseCase.invoke(hotWalletId) } returns TangemSdkError.UserCancelled().left()

        // Act
        val model = createModel(this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.words).isEmpty()
        assertThat(model.uiState.value.completeButtonEnabled).isFalse()
        model.onDestroy()
    }

    private fun createModel(testScope: TestScope): ManualBackupCheckModel {
        return ManualBackupCheckModel(
            paramsContainer = paramsContainer,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            getUserWalletUseCase = getUserWalletUseCase,
            updateWalletUseCase = updateWalletUseCase,
            exportSeedPhraseUseCase = exportSeedPhraseUseCase,
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