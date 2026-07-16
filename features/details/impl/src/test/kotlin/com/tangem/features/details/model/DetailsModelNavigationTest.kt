package com.tangem.features.details.model

import arrow.core.left
import arrow.core.right
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.models.Basic
import com.tangem.domain.wallets.usecase.GenerateBuyTangemCardLinkUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class DetailsModelNavigationTest : DetailsModelTestBase() {

    @Test
    fun `GIVEN selected wallet and meta WHEN support chat clicked THEN router pushes Usedesk`() = runTest {
        // Arrange
        val wallet = hotWallet(wallet1)
        val meta = metaInfo(wallet1)
        every { getSelectedWalletSyncUseCase() } returns wallet.right()
        coEvery { getWalletMetaInfoUseCase(wallet1) } returns meta.right()

        // Act
        val model = createModel(this)
        advanceUntilIdle()
        onChatSlot.captured.invoke()
        advanceUntilIdle()

        // Assert
        verify { router.push(route = AppRoute.Usedesk(meta), onComplete = any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN meta info missing WHEN support chat clicked THEN no navigation`() = runTest {
        val wallet = hotWallet(wallet1)
        every { getSelectedWalletSyncUseCase() } returns wallet.right()
        coEvery { getWalletMetaInfoUseCase(wallet1) } returns Throwable().left()

        val model = createModel(this)
        advanceUntilIdle()
        onChatSlot.captured.invoke()
        advanceUntilIdle()

        verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
        model.onDestroy()
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