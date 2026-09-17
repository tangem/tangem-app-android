package com.tangem.tap.domain.walletregistration

import android.util.Base64
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.lib.auth.session.WalletSigner
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Base64 as JavaBase64

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultWalletSignerProviderTest {

    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val mobileSigner: MobileWalletRegistrationSigner = mockk()

    private val provider = DefaultWalletSignerProvider(
        getUserWalletUseCase = getUserWalletUseCase,
        mobileSigner = mobileSigner,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(getUserWalletUseCase, mobileSigner)
        mockkStatic(Base64::class)
        every { Base64.decode(any<String>(), any()) } answers {
            JavaBase64.getDecoder().decode(firstArg<String>())
        }
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `GIVEN a hot wallet WHEN signerFor THEN the mobile signer is returned`() = runTest {
        // Arrange
        val userWallet: UserWallet.Hot = mockk()
        val signer = WalletSigner { mockk() }
        every { getUserWalletUseCase(WALLET_ID) } returns userWallet.right()
        every { mobileSigner.signerFor(userWallet) } returns signer

        // Act
        val actual = provider.signerFor(BACKEND_WALLET_ID)

        // Assert
        assertThat(actual).isSameInstanceAs(signer)
    }

    @Test
    fun `GIVEN a cold wallet WHEN signerFor THEN no signer is offered`() = runTest {
        // Arrange — signing a cold wallet needs an open card session, which this provider cannot open
        every { getUserWalletUseCase(WALLET_ID) } returns mockk<UserWallet.Cold>().right()

        // Act
        val actual = provider.signerFor(BACKEND_WALLET_ID)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN the wallet is unknown WHEN signerFor THEN no signer is offered`() = runTest {
        // Arrange
        every { getUserWalletUseCase(WALLET_ID) } returns GetUserWalletError.UserWalletNotFound.left()

        // Act
        val actual = provider.signerFor(BACKEND_WALLET_ID)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN the wallet list is still loading WHEN signerFor THEN no signer is offered`() = runTest {
        // Arrange — the use case throws instead of answering while the list is not in memory yet
        every { getUserWalletUseCase(WALLET_ID) } throws IllegalStateException("User wallets list is not loaded")

        // Act
        val actual = provider.signerFor(BACKEND_WALLET_ID)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN an id that is not base64 WHEN signerFor THEN no wallet is looked up`() = runTest {
        // Act
        val actual = provider.signerFor("not base64")

        // Assert
        assertThat(actual).isNull()
    }

    private companion object {
        val WALLET_ID = UserWalletId(value = ByteArray(size = 32) { it.toByte() })
        val BACKEND_WALLET_ID: String = JavaBase64.getEncoder().encodeToString(WALLET_ID.value)
    }
}