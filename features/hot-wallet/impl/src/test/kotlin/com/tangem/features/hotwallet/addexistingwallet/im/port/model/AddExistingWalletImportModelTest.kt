package com.tangem.features.hotwallet.addexistingwallet.im.port.model

import androidx.compose.ui.text.input.TextFieldValue
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.dialog.Dialogs
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.analytics.Settings.NoticeMobileWalletLimit
import com.tangem.features.hotwallet.MnemonicRepository
import com.tangem.features.hotwallet.addexistingwallet.im.port.AddExistingWalletImportComponent
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AddExistingWalletImportModelTest {

    private val mnemonicRepository: MnemonicRepository = mockk()
    private val hotWalletImporter: HotWalletImporter = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val callbacks: AddExistingWalletImportComponent.ModelCallbacks = mockk(relaxed = true)
    private val mnemonic: Mnemonic = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(mnemonicRepository, hotWalletImporter, uiMessageSender, analyticsEventHandler, callbacks)
        every { mnemonicRepository.words } returns WORDS
        every { mnemonicRepository.generateMnemonic(PHRASE) } returns mnemonic
        coEvery { hotWalletImporter.import(any(), any(), any(), any(), any(), any(), any()) } returns WALLET_ID.right()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN params with a source WHEN the phrase imported THEN the import reports that source`(
        model: SourceModel,
    ) = runTest {
        // Arrange
        val importModel = createModel(model.params(callbacks))

        // Act
        importPhrase(importModel)

        // Assert
        coVerify(exactly = 1) {
            hotWalletImporter.import(
                scope = any(),
                mnemonic = mnemonic,
                passphrase = null,
                name = any(),
                isSeedPhraseBackedUp = true,
                creationType = any(),
                analyticsSource = model.expectedSource,
            )
        }
        verify(exactly = 1) { callbacks.onWalletImported(WALLET_ID) }
        importModel.onDestroy()
    }

    @Test
    fun `GIVEN the hot wallets limit reached WHEN the phrase imported THEN the limit is reported and the dialog shows`() = runTest {
        // Arrange
        coEvery {
            hotWalletImporter.import(any(), any(), any(), any(), any(), any(), any())
        } returns HotWalletImportError.LimitReached.left()
        val importModel = createModel(AddExistingWalletImportComponent.Params(callbacks))

        // Act
        importPhrase(importModel)

        // Assert
        val message = slot<DialogMessage>()
        verify(exactly = 1) { uiMessageSender.send(capture(message)) }
        assertThat(message.captured.message).isEqualTo(Dialogs.hotWalletsLimitReachedDialog().message)
        verify(exactly = 1) {
            analyticsEventHandler.send(
                NoticeMobileWalletLimit(
                    type = NoticeMobileWalletLimit.Type.Import,
                    limit = NoticeMobileWalletLimit.Limit.MobileWallets,
                ),
            )
        }
        assertThat(importModel.uiState.value.isImportWalletInProgress).isFalse()
        verify(exactly = 0) { callbacks.onWalletImported(any()) }
        importModel.onDestroy()
    }

    @Test
    fun `GIVEN the import succeeds WHEN the phrase imported THEN no limit notice is sent`() = runTest {
        // Arrange
        val importModel = createModel(AddExistingWalletImportComponent.Params(callbacks))

        // Act
        importPhrase(importModel)

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(ofType<NoticeMobileWalletLimit>()) }
        importModel.onDestroy()
    }

    @Test
    fun `GIVEN the import fails for another reason WHEN the phrase imported THEN no limit notice is sent`() = runTest {
        // Arrange
        coEvery {
            hotWalletImporter.import(any(), any(), any(), any(), any(), any(), any())
        } returns HotWalletImportError.Unknown().left()
        val importModel = createModel(AddExistingWalletImportComponent.Params(callbacks))

        // Act
        importPhrase(importModel)

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(ofType<NoticeMobileWalletLimit>()) }
        importModel.onDestroy()
    }

    private fun provideTestModels() = listOf(
        SourceModel(
            name = "default source",
            params = { AddExistingWalletImportComponent.Params(it) },
            expectedSource = AnalyticsParam.ScreensSources.ImportWallet,
        ),
        SourceModel(
            name = "add new wallet source",
            params = { AddExistingWalletImportComponent.Params(it, AnalyticsParam.ScreensSources.AddNewWallet) },
            expectedSource = AnalyticsParam.ScreensSources.AddNewWallet,
        ),
    )

    internal data class SourceModel(
        val name: String,
        val params: (AddExistingWalletImportComponent.ModelCallbacks) -> AddExistingWalletImportComponent.Params,
        val expectedSource: AnalyticsParam.ScreensSources,
    ) {
        override fun toString(): String = name
    }

    private fun TestScope.createModel(params: AddExistingWalletImportComponent.Params): AddExistingWalletImportModel {
        val model = AddExistingWalletImportModel(
            paramsContainer = MutableParamsContainer(params),
            dispatchers = createTestingCoroutineDispatcherProvider(),
            mnemonicRepository = mnemonicRepository,
            hotWalletImporter = hotWalletImporter,
            uiMessageSender = uiMessageSender,
            analyticsEventHandler = analyticsEventHandler,
        )
        advanceUntilIdle()
        return model
    }

    private fun TestScope.importPhrase(model: AddExistingWalletImportModel) {
        model.uiState.value.wordsChange(TextFieldValue(PHRASE))
        advanceUntilIdle()
        model.uiState.value.importWalletClick()
        advanceUntilIdle()
    }

    private companion object {
        const val PHRASE = "word1 word2"
        val WORDS = setOf("word1", "word2")
        val WALLET_ID = UserWalletId("0A0B")
    }
}