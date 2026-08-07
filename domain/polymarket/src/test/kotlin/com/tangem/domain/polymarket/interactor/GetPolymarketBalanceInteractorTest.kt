package com.tangem.domain.polymarket.interactor

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketCredentialsStore
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketBalanceAllowance
import com.tangem.domain.polymarket.usecase.GetPolymarketApiCredentialsUseCase
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class GetPolymarketBalanceInteractorTest {

    private val repository: PolymarketRepository = mockk()
    private val credentialsStore: PolymarketCredentialsStore = mockk()

    private val interactor = GetPolymarketBalanceInteractor(
        polymarketRepository = repository,
        getApiCredentials = GetPolymarketApiCredentialsUseCase(credentialsStore = credentialsStore),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository, credentialsStore)
    }

    @Test
    fun `GIVEN credentials are stored WHEN invoke THEN reads the balance of the owner address`() = runTest {
        // Arrange
        coEvery { credentialsStore.get(USER_WALLET_ID) } returns CREDENTIALS
        coEvery { repository.getBalanceAllowance(OWNER, CREDENTIALS) } returns BALANCE.right()

        // Act
        val actual = interactor(addresses = ADDRESSES)

        // Assert
        assertThat(actual).isEqualTo(BALANCE.right())
    }

    @Test
    fun `GIVEN nothing is stored WHEN invoke THEN fails with KeyNotFound without reaching the network`() = runTest {
        // Arrange
        coEvery { credentialsStore.get(USER_WALLET_ID) } returns null

        // Act
        val actual = interactor(addresses = ADDRESSES)

        // Assert
        assertThat(actual).isEqualTo(PolymarketAuthError.KeyNotFound.left())
        coVerify(exactly = 0) { repository.getBalanceAllowance(any(), any()) }
    }

    @Test
    fun `GIVEN the request is rejected WHEN invoke THEN propagates the auth error`() = runTest {
        // Arrange
        coEvery { credentialsStore.get(USER_WALLET_ID) } returns CREDENTIALS
        coEvery { repository.getBalanceAllowance(OWNER, CREDENTIALS) } returns
            PolymarketAuthError.InvalidSignature.left()

        // Act
        val actual = interactor(addresses = ADDRESSES)

        // Assert
        assertThat(actual).isEqualTo(PolymarketAuthError.InvalidSignature.left())
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val OWNER = "0x1111111111111111111111111111111111111111"
        val ADDRESSES = PolymarketAddresses(
            ownerAddress = OWNER,
            depositWalletAddress = "0x2222222222222222222222222222222222222222",
            userWalletId = USER_WALLET_ID,
        )
        val CREDENTIALS = PolymarketApiCredentials(apiKey = "key", secret = "secret", passphrase = "pass")
        val BALANCE = PolymarketBalanceAllowance(balance = BigDecimal("12.34"), allowance = BigDecimal("1000"))
    }
}