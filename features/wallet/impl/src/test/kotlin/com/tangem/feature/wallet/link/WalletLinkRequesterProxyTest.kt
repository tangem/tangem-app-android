package com.tangem.feature.wallet.link

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.registration.WalletLinkRequester
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class WalletLinkRequesterProxyTest {

    private val userWallet: UserWallet = mockk()

    @Test
    fun `GIVEN component attached WHEN requestLink THEN returns the component result`() = runTest {
        // Arrange
        val proxy = WalletLinkRequesterProxy()
        val component = mockk<WalletLinkRequester> {
            coEvery { requestLink(userWallet) } returns WalletLinkRequester.Result.Linked
        }
        proxy.componentRequester.value = component

        // Act
        val actual = proxy.requestLink(userWallet)

        // Assert
        assertThat(actual).isEqualTo(WalletLinkRequester.Result.Linked)
    }

    @Test
    fun `GIVEN no component attached WHEN requestLink THEN times out`() = runTest {
        // Arrange
        val proxy = WalletLinkRequesterProxy()

        // Act
        val actual = try {
            proxy.requestLink(userWallet)
            null
        } catch (e: TimeoutCancellationException) {
            e
        }

        // Assert
        assertThat(actual).isInstanceOf(TimeoutCancellationException::class.java)
    }
}