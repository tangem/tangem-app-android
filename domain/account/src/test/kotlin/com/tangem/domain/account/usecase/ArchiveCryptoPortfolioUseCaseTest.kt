package com.tangem.domain.account.usecase

import arrow.core.None
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import com.google.common.truth.Truth
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.usecase.ArchiveCryptoPortfolioUseCase.Error
import com.tangem.domain.account.utils.createAccount
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArchiveCryptoPortfolioUseCaseTest {

    private val crudRepository: AccountsCRUDRepository = mockk(relaxUnitFun = true)
    private val useCase = ArchiveCryptoPortfolioUseCase(crudRepository)
    private val userWallet = mockk<UserWallet>()

    @BeforeEach
    fun resetMocks() {
        clearMocks(crudRepository, userWallet)
        every { userWallet.walletId } returns userWalletId
    }

    @Test
    fun `invoke should archive existing crypto portfolio account`() = runTest {
        // Arrange
        val account = createAccount(userWalletId)
        val accountList = (AccountList.empty(userWallet) + account).getOrNull()!!
        val accountId = account.accountId

        val updatedAccountList = (accountList - account).getOrNull()!!

        coEvery { crudRepository.getAccounts(userWalletId) } returns accountList.toOption()

        // Act
        val actual = useCase(accountId)

        // Assert
        val expected = Unit.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            crudRepository.getAccounts(userWalletId)
            crudRepository.saveAccounts(updatedAccountList)
        }
    }

    @Test
    fun `invoke should return error if getAccounts returns None`() = runTest {
        // Arrange
        val accountId = AccountId.forCryptoPortfolio(
            userWalletId = userWalletId,
            derivationIndex = DerivationIndex.Main,
        )

        coEvery { crudRepository.getAccounts(userWalletId) } returns None

        // Act
        val actual = useCase(accountId)

        // Assert
        val expected = Error.CriticalTechError.AccountsNotCreated(userWalletId).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder { crudRepository.getAccounts(userWalletId) }
        coVerify(inverse = true) { crudRepository.saveAccounts(any()) }
    }

    @Test
    fun `invoke should return error if getAccounts throws exception`() = runTest {
        // Arrange
        val accountId = AccountId.forCryptoPortfolio(
            userWalletId = userWalletId,
            derivationIndex = DerivationIndex.Main,
        )

        val exception = IllegalStateException("Test error")

        coEvery { crudRepository.getAccounts(userWalletId) } throws exception

        // Act
        val actual = useCase(accountId)

        // Assert
        val expected = Error.DataOperationFailed(exception).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder { crudRepository.getAccounts(userWalletId) }
        coVerify(inverse = true) { crudRepository.saveAccounts(any()) }
    }

    @Test
    fun `invoke should return error if account not found`() = runTest {
        // Arrange
        val accountList = AccountList.empty(userWallet)
        val accountId = AccountId.forCryptoPortfolio(
            userWalletId = userWalletId,
            derivationIndex = DerivationIndex(1).getOrNull()!!,
        )

        coEvery { crudRepository.getAccounts(userWalletId) } returns accountList.toOption()

        // Act
        val actual = useCase(accountId)

        // Assert
        val expected = Error.CriticalTechError.AccountNotFound(accountId).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder { crudRepository.getAccounts(userWalletId) }
        coVerify(inverse = true) { crudRepository.saveAccounts(any()) }
    }

    @Test
    fun `invoke should return error if saveAccounts throws exception`() = runTest {
        // Arrange
        val account = createAccount(userWalletId)
        val accountList = (AccountList.empty(userWallet) + account).getOrNull()!!
        val accountId = account.accountId

        val updatedAccountList = (accountList - account).getOrNull()!!

        val exception = IllegalStateException("Save failed")

        coEvery { crudRepository.getAccounts(userWalletId) } returns accountList.toOption()
        coEvery { crudRepository.saveAccounts(updatedAccountList) } throws exception

        // Act
        val actual = useCase(accountId)

        // Assert
        val expected = Error.DataOperationFailed(exception).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            crudRepository.getAccounts(userWalletId)
            crudRepository.saveAccounts(updatedAccountList)
        }
    }

    private companion object {
        val userWalletId = UserWalletId("011")
    }
}