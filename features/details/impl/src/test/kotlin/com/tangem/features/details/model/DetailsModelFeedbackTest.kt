package com.tangem.features.details.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.Basic
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.details.entity.SelectEmailFeedbackTypeBS
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class DetailsModelFeedbackTest : DetailsModelTestBase() {

    @Test
    fun `GIVEN all wallets hot WHEN support email clicked THEN DirectUserRequest sent`() = runTest {
        // Arrange
        val wallet = hotWallet(wallet1)
        val meta = metaInfo(wallet1, isVisa = false)
        every { getWalletsUseCase.invokeSync() } returns listOf(wallet)
        every { getSelectedWalletSyncUseCase() } returns wallet.right()
        coEvery { getWalletMetaInfoUseCase(wallet1) } returns meta.right()
        every { getTangemPayCustomerIdUseCase(wallet1) } returns "".right()

        // Act
        val model = createModel(this)
        advanceUntilIdle()
        onSupportSlot.captured.invoke()
        advanceUntilIdle()

        // Assert
        verify { analyticsEventHandler.send(any<Basic.ButtonSupport>()) }
        coVerify { sendFeedbackEmailUseCase(FeedbackEmailType.DirectUserRequest(meta)) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN all wallets cold visa with customerId WHEN support email clicked THEN Visa request sent`() = runTest {
        val wallet = coldWallet(wallet1, isVisa = true)
        val meta = metaInfo(wallet1, isVisa = true)
        every { getWalletsUseCase.invokeSync() } returns listOf(wallet)
        every { getSelectedWalletSyncUseCase() } returns wallet.right()
        coEvery { getWalletMetaInfoUseCase(wallet1) } returns meta.right()
        every { getTangemPayCustomerIdUseCase(wallet1) } returns customerId.right()

        val model = createModel(this)
        advanceUntilIdle()
        onSupportSlot.captured.invoke()
        advanceUntilIdle()

        verify { analyticsEventHandler.send(any<Basic.ButtonSupport>()) }
        coVerify { sendFeedbackEmailUseCase(FeedbackEmailType.Visa.DirectUserRequest(meta, customerId)) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN mixed wallets WHEN support email clicked THEN bottom sheet shown and no email sent`() = runTest {
        val selected = hotWallet(wallet1)
        val meta = metaInfo(wallet1, isVisa = false)
        every { getWalletsUseCase.invokeSync() } returns listOf(selected, coldWallet(wallet2, isVisa = true))
        every { getSelectedWalletSyncUseCase() } returns selected.right()
        coEvery { getWalletMetaInfoUseCase(wallet1) } returns meta.right()
        every { getTangemPayCustomerIdUseCase(wallet1) } returns customerId.right()

        val model = createModel(this)
        advanceUntilIdle()
        onSupportSlot.captured.invoke()
        advanceUntilIdle()

        val bsConfig = model.state.value.selectFeedbackEmailTypeBSConfig
        assertThat(bsConfig.isShown).isTrue()
        assertThat(bsConfig.content).isInstanceOf(SelectEmailFeedbackTypeBS::class.java)
        coVerify(exactly = 0) { sendFeedbackEmailUseCase(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN meta info missing WHEN support email clicked THEN no email sent`() = runTest {
        val wallet = hotWallet(wallet1)
        every { getWalletsUseCase.invokeSync() } returns listOf(wallet)
        every { getSelectedWalletSyncUseCase() } returns wallet.right()
        coEvery { getWalletMetaInfoUseCase(wallet1) } returns Throwable().left()
        every { getTangemPayCustomerIdUseCase(wallet1) } returns "".right()

        val model = createModel(this)
        advanceUntilIdle()
        onSupportSlot.captured.invoke()
        advanceUntilIdle()

        coVerify(exactly = 0) { sendFeedbackEmailUseCase(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN General option AND selected meta not visa WHEN selected THEN DirectUserRequest with selected meta`() =
        runTest {
            val selectedMeta = metaInfo(wallet1, isVisa = false)
            val content = openBottomSheet(selectedMeta = selectedMeta)

            content.onOptionClick(SelectEmailFeedbackTypeBS.Option.General)
            advanceUntilIdle()

            coVerify { sendFeedbackEmailUseCase(FeedbackEmailType.DirectUserRequest(selectedMeta)) }
            assertThat(currentModel.state.value.selectFeedbackEmailTypeBSConfig.isShown).isFalse()
            verify { analyticsEventHandler.send(any<Basic.ButtonSupport>()) }
            currentModel.onDestroy()
        }

    @Test
    fun `GIVEN General option AND selected meta visa WHEN selected THEN picks non-visa wallet meta`() = runTest {
        val selectedMeta = metaInfo(wallet1, isVisa = true)
        val nonVisaMeta = metaInfo(wallet2, isVisa = false)
        val content = openBottomSheet(
            selectedMeta = selectedMeta,
            wallets = listOf(coldWallet(wallet1, isVisa = true), hotWallet(wallet2)),
        )
        coEvery { getWalletMetaInfoUseCase(wallet2) } returns nonVisaMeta.right()

        content.onOptionClick(SelectEmailFeedbackTypeBS.Option.General)
        advanceUntilIdle()

        coVerify { sendFeedbackEmailUseCase(FeedbackEmailType.DirectUserRequest(nonVisaMeta)) }
        currentModel.onDestroy()
    }

    @Test
    fun `GIVEN Visa option AND selected meta visa with customerId WHEN selected THEN Visa request with selected`() =
        runTest {
            val selectedMeta = metaInfo(wallet1, isVisa = true)
            val content = openBottomSheet(selectedMeta = selectedMeta, customerId = customerId)

            content.onOptionClick(SelectEmailFeedbackTypeBS.Option.Visa)
            advanceUntilIdle()

            coVerify {
                sendFeedbackEmailUseCase(FeedbackEmailType.Visa.DirectUserRequest(selectedMeta, customerId))
            }
            currentModel.onDestroy()
        }

    @Test
    fun `GIVEN Visa option AND selected meta not visa WHEN selected THEN picks cold visa wallet meta`() = runTest {
        val selectedMeta = metaInfo(wallet1, isVisa = false)
        val visaMeta = metaInfo(wallet2, isVisa = true)
        val content = openBottomSheet(
            selectedMeta = selectedMeta,
            wallets = listOf(hotWallet(wallet1), coldWallet(wallet2, isVisa = true)),
        )
        coEvery { getWalletMetaInfoUseCase(wallet2) } returns visaMeta.right()
        every { getTangemPayCustomerIdUseCase(wallet2) } returns customerId.right()

        content.onOptionClick(SelectEmailFeedbackTypeBS.Option.Visa)
        advanceUntilIdle()

        coVerify { sendFeedbackEmailUseCase(FeedbackEmailType.Visa.DirectUserRequest(visaMeta, customerId)) }
        currentModel.onDestroy()
    }

    private lateinit var currentModel: DetailsModel

    /**
     * Drives the model into the "mixed wallets" state so the bottom sheet is shown, then returns its content.
     * [selectedMeta] is what [getWalletMetaInfoUseCase] returns for the selected wallet ([wallet1]).
     */
    private fun TestScope.openBottomSheet(
        selectedMeta: WalletMetaInfo,
        wallets: List<UserWallet> = listOf(hotWallet(wallet1), coldWallet(wallet2, isVisa = true)),
        customerId: String = "",
    ): SelectEmailFeedbackTypeBS {
        every { getWalletsUseCase.invokeSync() } returns wallets
        every { getSelectedWalletSyncUseCase() } returns wallets.first().right()
        coEvery { getWalletMetaInfoUseCase(wallet1) } returns selectedMeta.right()
        every { getTangemPayCustomerIdUseCase(wallet1) } returns customerId.right()

        currentModel = createModel(this)
        advanceUntilIdle()
        onSupportSlot.captured.invoke()
        advanceUntilIdle()

        return currentModel.state.value.selectFeedbackEmailTypeBSConfig.content as SelectEmailFeedbackTypeBS
    }
}