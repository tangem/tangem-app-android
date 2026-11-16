package com.tangem.domain.collectibles.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerKeysRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class GetMissingCollectiblesOwnerKeysCountUseCaseTest {

    private val repository: CollectiblesOwnerKeysRepository = mockk()
    private val userWallet: UserWallet = mockk()

    private val useCase = GetMissingCollectiblesOwnerKeysCountUseCase(repository)

    @Test
    fun `GIVEN the repository counts a missing key WHEN invoke THEN the count is returned`() = runTest {
        // Arrange
        coEvery { repository.missingOwnerKeysCount(listOf(COLLECTIBLES_ACCOUNT), userWallet) } returns 1

        // Act
        val actual = useCase(listOf(COLLECTIBLES_ACCOUNT), userWallet)

        // Assert
        assertThat(actual).isEqualTo(1)
    }

    private companion object {
        val COLLECTIBLES_ACCOUNT = Account.Collectibles(UserWalletId("011"))
    }
}