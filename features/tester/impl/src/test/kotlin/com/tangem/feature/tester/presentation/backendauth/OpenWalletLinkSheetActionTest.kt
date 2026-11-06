package com.tangem.feature.tester.presentation.backendauth

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.registration.WalletLinkRequester
import com.tangem.utils.coroutines.AppCoroutineScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class OpenWalletLinkSheetActionTest {

    private val walletLinkRequester: WalletLinkRequester = mockk()

    @Test
    fun `GIVEN wallet WHEN invoke THEN requests link for it`() = runTest {
        // Arrange
        val userWallet: UserWallet = mockk()
        coEvery { walletLinkRequester.requestLink(userWallet) } returns WalletLinkRequester.Result.Dismissed
        val action = OpenWalletLinkSheetAction(
            walletLinkRequester = walletLinkRequester,
            appCoroutineScope = object : AppCoroutineScope, CoroutineScope by backgroundScope {},
        )

        // Act
        action(userWallet)
        runCurrent()

        // Assert
        coVerify(exactly = 1) { walletLinkRequester.requestLink(userWallet) }
    }
}