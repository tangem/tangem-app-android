package com.tangem.features.details.model

import arrow.core.left
import arrow.core.right
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.models.Basic
import com.tangem.domain.wallets.usecase.GenerateBuyTangemCardLinkUseCase
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
internal class DetailsModelNavigationTest : DetailsModelTestBase() {

    @Test
    fun `GIVEN usedesk enabled WHEN chat option selected THEN router pushes Usedesk`() = runTest {
        // Arrange
        val wallet = hotWallet(wallet1)
        val meta = metaInfo(wallet1)
        every { feedbackFeatureToggles.isUsedeskEnabled } returns true
        every { getSelectedWalletSyncUseCase() } returns wallet.right()
        coEvery { getWalletMetaInfoUseCase(wallet1) } returns meta.right()

        // Act
        val model = createModel(this)
        advanceUntilIdle()
        onSupportSlot.captured.invoke()
        selectContactSupportOption(model, SelectContactSupportTypeBS.Option.Chat)
        advanceUntilIdle()

        // Assert
        verify { router.push(route = AppRoute.Usedesk(meta), onComplete = any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN meta info missing WHEN chat option selected THEN no navigation`() = runTest {
        val wallet = hotWallet(wallet1)
        every { feedbackFeatureToggles.isUsedeskEnabled } returns true
        every { getSelectedWalletSyncUseCase() } returns wallet.right()
        coEvery { getWalletMetaInfoUseCase(wallet1) } returns Throwable().left()

        val model = createModel(this)
        advanceUntilIdle()
        onSupportSlot.captured.invoke()
        selectContactSupportOption(model, SelectContactSupportTypeBS.Option.Chat)
        advanceUntilIdle()

        verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN usedesk enabled WHEN mail option selected THEN sends email and does not open Usedesk`() = runTest {
        // Arrange
        val wallet = hotWallet(wallet1)
        val meta = metaInfo(wallet1)
        every { feedbackFeatureToggles.isUsedeskEnabled } returns true
        every { getWalletsUseCase.invokeSync() } returns listOf(wallet)
        every { getSelectedWalletSyncUseCase() } returns wallet.right()
        coEvery { getWalletMetaInfoUseCase(wallet1) } returns meta.right()
        every { getTangemPayCustomerIdUseCase(wallet1) } returns customerId.right()

        // Act
        val model = createModel(this)
        advanceUntilIdle()
        onSupportSlot.captured.invoke()
        selectContactSupportOption(model, SelectContactSupportTypeBS.Option.Mail)
        advanceUntilIdle()

        // Assert
        coVerify { sendFeedbackEmailUseCase(any()) }
        verify(exactly = 0) { router.push(route = AppRoute.Usedesk(meta), onComplete = any()) }
        model.onDestroy()
    }

    private fun selectContactSupportOption(model: DetailsModel, option: SelectContactSupportTypeBS.Option) {
        val content = model.state.value.selectContactSupportTypeBSConfig.content as SelectContactSupportTypeBS
        content.onOptionClick(option)
    }

    @Test
    fun `GIVEN buy link WHEN buy clicked THEN opens url and sends analytics`() = runTest {
        coEvery {
            generateBuyTangemCardLinkUseCase(GenerateBuyTangemCardLinkUseCase.Source.Settings)
        } returns buyUrl

        val model = createModel(this)
        advanceUntilIdle()
        onBuySlot.captured.invoke()
        advanceUntilIdle()

        verify { analyticsEventHandler.send(any<Basic.ButtonBuy>()) }
        verify { urlOpener.openUrl(buyUrl) }
        model.onDestroy()
    }
}