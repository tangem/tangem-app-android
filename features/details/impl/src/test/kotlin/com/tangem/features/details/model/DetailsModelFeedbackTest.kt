package com.tangem.features.details.model

import arrow.core.left
import arrow.core.right
import com.tangem.core.analytics.models.Basic
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.features.details.entity.SelectContactSupportTypeBS
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class DetailsModelFeedbackTest : DetailsModelTestBase() {

    @Test
    fun `GIVEN all wallets hot WHEN support email clicked THEN DirectUserRequest sent`() = runTest {
        // Arrange
        val wallet = hotWallet(wallet1)
        val meta = metaInfo(wallet1)
        every { getWalletsUseCase.invokeSync() } returns listOf(wallet)
        every { getSelectedWalletSyncUseCase() } returns wallet.right()
        coEvery { getWalletMetaInfoUseCase(wallet1) } returns meta.right()

        // Act
        val model = createModel(this)
        advanceUntilIdle()
        onSupportSlot.captured.invoke()
        model.chooseMailSupport()
        advanceUntilIdle()

        // Assert
        verify { analyticsEventHandler.send(any<Basic.ButtonSupport>()) }
        coVerify { sendFeedbackEmailUseCase(FeedbackEmailType.DirectUserRequest(meta)) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN meta info missing WHEN support email clicked THEN no email sent`() = runTest {
        val wallet = hotWallet(wallet1)
        every { getWalletsUseCase.invokeSync() } returns listOf(wallet)
        every { getSelectedWalletSyncUseCase() } returns wallet.right()
        coEvery { getWalletMetaInfoUseCase(wallet1) } returns Throwable().left()

        val model = createModel(this)
        advanceUntilIdle()
        onSupportSlot.captured.invoke()
        model.chooseMailSupport()
        advanceUntilIdle()

        coVerify(exactly = 0) { sendFeedbackEmailUseCase(any()) }
        model.onDestroy()
    }

    /** Picks the "Mail" option in the contact-support chooser, which is what opens the feedback-email flow. */
    private fun DetailsModel.chooseMailSupport() {
        val chooser = state.value.selectContactSupportTypeBSConfig.content as SelectContactSupportTypeBS
        chooser.onOptionClick(SelectContactSupportTypeBS.Option.Mail)
    }
}