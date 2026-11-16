package com.tangem.domain.collectibles.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.vault.repository.GachaVaultRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class WithdrawFromCollectiblesVaultUseCaseTest {

    private val gachaVaultRepository: GachaVaultRepository = mockk()

    private val userWallet: UserWallet = mockk()
    private val vaultToken: CryptoCurrency.Token = mockk()

    private val useCase = WithdrawFromCollectiblesVaultUseCase(gachaVaultRepository = gachaVaultRepository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(gachaVaultRepository)
    }

    @Test
    fun `GIVEN the withdrawal is sent WHEN invoke THEN its hash is returned`() = runTest {
        // Arrange
        coEvery { gachaVaultRepository.withdraw(userWallet, vaultToken, RECIPIENT, AMOUNT) } returns
            TX_HASH.right()

        // Act
        val actual = withdraw()

        // Assert
        assertThat(actual).isEqualTo(TX_HASH.right())
    }

    @Test
    fun `GIVEN it may be sent with a known signature WHEN invoke THEN it counts as sent`() = runTest {
        // Arrange
        coEvery { gachaVaultRepository.withdraw(any(), any(), any(), any()) } returns
            CollectiblesAccountError.MaybeSent(txHash = TX_HASH).left()

        // Act
        val actual = withdraw()

        // Assert
        assertThat(actual).isEqualTo(TX_HASH.right())
    }

    @Test
    fun `GIVEN the outcome is unknown WHEN invoke THEN it is passed on`() = runTest {
        // Arrange
        val maybeSent = CollectiblesAccountError.MaybeSent(txHash = null)
        coEvery { gachaVaultRepository.withdraw(any(), any(), any(), any()) } returns maybeSent.left()

        // Act
        val actual = withdraw()

        // Assert
        assertThat(actual).isEqualTo(maybeSent.left())
    }

    private suspend fun withdraw() = useCase(
        userWallet = userWallet,
        token = vaultToken,
        recipientAddress = RECIPIENT,
        amount = AMOUNT,
    )

    private companion object {

        val AMOUNT = BigDecimal("12.5")
        const val RECIPIENT = "Hx6LbkMHe69DYawhPyVNs8Apa6tyfogfzQV6a7XkwBUU"
        const val TX_HASH = "hash"
    }
}