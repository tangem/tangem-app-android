package com.tangem.features.details.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.features.details.entity.DetailsFooterUM
import com.tangem.features.details.entity.DetailsItemUM
import io.mockk.coEvery
import io.mockk.every
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class DetailsModelInitTest : DetailsModelTestBase() {

    @Test
    fun `GIVEN walletConnect available WHEN init THEN buildAll receives isWalletConnectAvailable true`() = runTest {
        // Arrange
        coEvery { checkIsWalletConnectAvailableUseCase(userWalletId) } returns true.right()

        // Act
        createModel(this).also { advanceUntilIdle() }.onDestroy()

        // Assert
        assertThat(wcSlot.captured).isTrue()
    }

    @Test
    fun `GIVEN walletConnect unavailable WHEN init THEN buildAll receives isWalletConnectAvailable false`() = runTest {
        coEvery { checkIsWalletConnectAvailableUseCase(userWalletId) } returns false.right()

        createModel(this).also { advanceUntilIdle() }.onDestroy()

        assertThat(wcSlot.captured).isFalse()
    }

    @Test
    fun `GIVEN walletConnect check fails WHEN init THEN isWalletConnectAvailable falls back to false`() = runTest {
        coEvery { checkIsWalletConnectAvailableUseCase(userWalletId) } returns Throwable("boom").left()

        createModel(this).also { advanceUntilIdle() }.onDestroy()

        assertThat(wcSlot.captured).isFalse()
    }

    @Test
    fun `GIVEN addressBook enabled WHEN init THEN buildAll receives isAddressBookAvailable true`() = runTest {
        every { addressBookFeatureToggles.isAddressBookEnabled } returns true

        createModel(this).also { advanceUntilIdle() }.onDestroy()

        assertThat(abSlot.captured).isTrue()
    }

    @Test
    fun `GIVEN addressBook disabled WHEN init THEN buildAll receives isAddressBookAvailable false`() = runTest {
        every { addressBookFeatureToggles.isAddressBookEnabled } returns false

        createModel(this).also { advanceUntilIdle() }.onDestroy()

        assertThat(abSlot.captured).isFalse()
    }

    @Test
    fun `GIVEN usedesk enabled WHEN init THEN buildAll receives isSupportChatAvailable true`() = runTest {
        every { feedbackFeatureToggles.isUsedeskEnabled } returns true

        createModel(this).also { advanceUntilIdle() }.onDestroy()

        assertThat(chatSlot.captured).isTrue()
    }

    @Test
    fun `GIVEN usedesk disabled WHEN init THEN buildAll receives isSupportChatAvailable false`() = runTest {
        every { feedbackFeatureToggles.isUsedeskEnabled } returns false

        createModel(this).also { advanceUntilIdle() }.onDestroy()

        assertThat(chatSlot.captured).isFalse()
    }

    @Test
    fun `GIVEN a hot wallet present WHEN init THEN buildAll receives hasAnyMobileWallet true`() = runTest {
        every { getWalletsUseCase.invokeSync() } returns listOf(hotWallet(wallet1))

        createModel(this).also { advanceUntilIdle() }.onDestroy()

        assertThat(mobileSlot.captured).isTrue()
    }

    @Test
    fun `GIVEN only cold wallets WHEN init THEN buildAll receives hasAnyMobileWallet false`() = runTest {
        every { getWalletsUseCase.invokeSync() } returns listOf(coldWallet(wallet1, isVisa = false))

        createModel(this).also { advanceUntilIdle() }.onDestroy()

        assertThat(mobileSlot.captured).isFalse()
    }

    @Test
    fun `GIVEN params userWalletId WHEN init THEN buildAll receives it`() = runTest {
        createModel(this).also { advanceUntilIdle() }.onDestroy()

        assertThat(walletIdSlot.captured).isEqualTo(userWalletId)
    }

    @Test
    fun `GIVEN app info WHEN init THEN footer appVersion combines version and code`() = runTest {
        val model = createModel(this)
        advanceUntilIdle()

        assertThat(model.state.value.footer.appVersion).isEqualTo("1.2.3 (456)")

        model.onDestroy()
    }

    @Test
    fun `GIVEN socials WHEN init THEN footer socials come from SocialsBuilder`() = runTest {
        val socials = persistentListOf(DetailsFooterUM.Social(id = "tw", iconResId = 0, onClick = {}))
        every { socialsBuilder.buildAll() } returns socials

        val model = createModel(this)
        advanceUntilIdle()

        assertThat(model.state.value.footer.socials).isEqualTo(socials)

        model.onDestroy()
    }

    @Test
    fun `GIVEN popBack WHEN invoked THEN router pop is called`() = runTest {
        val model = createModel(this)
        advanceUntilIdle()

        model.state.value.popBack()

        verify { router.pop(onComplete = any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN buildAll result WHEN not eligible for tangem pay THEN state items equal buildAll result`() = runTest {
        val items = persistentListOf<DetailsItemUM>(DetailsItemUM.UserWalletList)
        stubBuildAllReturns(items)
        coEvery { tangemPayEligibilityManager.getEligibleWallets(any(), any()) } returns emptyList()

        val model = createModel(this)
        advanceUntilIdle()

        assertThat(model.state.value.items).isEqualTo(items)
        model.onDestroy()
    }

    @Test
    fun `GIVEN items flow updates WHEN tangem pay item added THEN new items propagate to state`() = runTest {
        val initial = persistentListOf<DetailsItemUM>()
        val withTangemPay = persistentListOf<DetailsItemUM>(DetailsItemUM.UserWalletList)
        stubBuildAllReturns(initial)
        every { itemsBuilder.addTangemPayItem(any(), any()) } returns withTangemPay
        coEvery { tangemPayEligibilityManager.getEligibleWallets(any(), any()) } returns listOf(hotWallet(wallet1))

        val model = createModel(this)
        advanceUntilIdle()

        assertThat(model.state.value.items).isEqualTo(withTangemPay)
        model.onDestroy()
    }
}