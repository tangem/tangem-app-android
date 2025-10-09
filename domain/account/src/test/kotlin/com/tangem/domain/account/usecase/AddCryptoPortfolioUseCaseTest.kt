package com.tangem.domain.account.usecase

import arrow.core.None
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import com.google.common.truth.Truth
import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.tokens.MainAccountTokensMigration
import com.tangem.domain.account.usecase.AddCryptoPortfolioUseCase.Error
import com.tangem.domain.account.utils.createAccount
import com.tangem.domain.account.utils.createAccounts
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddCryptoPortfolioUseCaseTest {

    private val crudRepository: AccountsCRUDRepository = mockk(relaxUnitFun = true)
    private val singleAccountListFetcher = mockk<SingleAccountListFetcher>()
    private val mainAccountTokensMigration = mockk<MainAccountTokensMigration>()

    private val useCase = AddCryptoPortfolioUseCase(
        crudRepository = crudRepository,
        singleAccountListFetcher = singleAccountListFetcher,
        mainAccountTokensMigration = mainAccountTokensMigration,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(crudRepository, singleAccountListFetcher, mainAccountTokensMigration)
    }

    @Test
    fun `invoke should add new crypto portfolio account to existing list`() = runTest {
        // Arrange
        val newAccount = createNewAccount()
        val accountList = AccountList.empty(userWalletId)
        val updatedAccountList = (accountList + newAccount).getOrNull()!!

        coEvery {
            singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId))
        } returns Unit.right()
        coEvery { crudRepository.getAccountListSync(userWalletId) } returns accountList.toOption()
        coEvery {
            mainAccountTokensMigration.migrate(userWalletId, newAccount.derivationIndex)
        } returns Unit.right()

        // Act
        val actual = useCase(
            userWalletId = userWalletId,
            accountName = newAccount.accountName,
            icon = newAccount.icon,
            derivationIndex = newAccount.derivationIndex,
        )

        // Assert
        val expected = newAccount.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifySequence {
            singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId))
            crudRepository.getAccountListSync(userWalletId)
            crudRepository.saveAccounts(updatedAccountList)
            mainAccountTokensMigration.migrate(userWalletId, newAccount.derivationIndex)
        }
    }

    @Test
    fun `invoke should return error if fetch is failed`() = runTest {
        // Arrange
        val newAccount = createNewAccount()
        val exception = Exception("Fetch error")

        coEvery {
            singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId))
        } returns exception.left()

        // Act
        val actual = useCase(
            userWalletId = userWalletId,
            accountName = newAccount.accountName,
            icon = newAccount.icon,
            derivationIndex = newAccount.derivationIndex,
        )

        // Assert
        val expected = exception
        Truth.assertThat((actual.leftOrNull() as Error.DataOperationFailed).cause).isInstanceOf(expected::class.java)
        Truth.assertThat((actual.leftOrNull() as Error.DataOperationFailed).cause).hasMessageThat()
            .isEqualTo(expected.message)

        coVerifySequence {
            singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId))
        }

        coVerify(inverse = true) {
            crudRepository.getAccountListSync(any())
            crudRepository.saveAccounts(any())
            mainAccountTokensMigration.migrate(any(), any())
        }
    }

    @Test
    fun `invoke should return error if account list none exists`() = runTest {
        // Arrange
        val newAccount = createNewAccount()

        coEvery {
            singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId))
        } returns Unit.right()
        coEvery { crudRepository.getAccountListSync(userWalletId) } returns None

        // Act
        val actual = useCase(
            userWalletId = userWalletId,
            accountName = newAccount.accountName,
            icon = newAccount.icon,
            derivationIndex = newAccount.derivationIndex,
        ).leftOrNull() as Error.DataOperationFailed

        // Assert
        val expected = IllegalStateException("Account list not found for wallet $userWalletId")
        Truth.assertThat(actual.cause).isInstanceOf(expected::class.java)
        Truth.assertThat(actual.cause).hasMessageThat().isEqualTo(expected.message)

        coVerifySequence {
            singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId))
            crudRepository.getAccountListSync(userWalletId)
        }

        coVerify(inverse = true) {
            crudRepository.saveAccounts(any())
            mainAccountTokensMigration.migrate(any(), any())
        }
    }

    @Test
    fun `invoke should return error if account list requirements not met`() = runTest {
        // Arrange
        val accountList = AccountList(
            userWalletId = userWalletId,
            accounts = createAccounts(userWalletId = userWalletId, count = 20),
            totalAccounts = 20,
        ).getOrNull()!!

        val newAccount = createNewAccount(derivationIndex = 21)

        coEvery {
            singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId))
        } returns Unit.right()
        coEvery { crudRepository.getAccountListSync(userWalletId) } returns accountList.toOption()

        // Act
        val actual = useCase(
            userWalletId = userWalletId,
            accountName = newAccount.accountName,
            icon = newAccount.icon,
            derivationIndex = newAccount.derivationIndex,
        )

        // Assert
        val expected = Error.AccountListRequirementsNotMet(
            cause = AccountList.Error.ExceedsMaxAccountsCount,
        ).left()

        Truth.assertThat(actual).isEqualTo(expected)

        coVerifySequence {
            singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId))
            crudRepository.getAccountListSync(userWalletId)
        }

        coVerify(inverse = true) {
            crudRepository.saveAccounts(any())
            mainAccountTokensMigration.migrate(any(), any())
        }
    }

    @Test
    fun `invoke should return error if getAccounts throws exception`() = runTest {
        // Arrange
        val newAccount = createNewAccount()
        val exception = IllegalStateException("Test error")

        coEvery {
            singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId))
        } returns Unit.right()
        coEvery { crudRepository.getAccountListSync(userWalletId) } throws exception

        // Act
        val actual = useCase(
            userWalletId = userWalletId,
            accountName = newAccount.accountName,
            icon = newAccount.icon,
            derivationIndex = newAccount.derivationIndex,
        )

        // Assert
        val expected = Error.DataOperationFailed(cause = exception).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifySequence {
            singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId))
            crudRepository.getAccountListSync(userWalletId)
        }

        coVerify(inverse = true) {
            crudRepository.saveAccounts(any())
            mainAccountTokensMigration.migrate(any(), any())
        }
    }

    @Test
    fun `invoke should return error if saveAccounts throws exception`() = runTest {
        // Arrange
        val newAccount = createNewAccount()
        val accountList = AccountList.empty(userWalletId)
        val updatedAccountList = (accountList + newAccount).getOrNull()!!

        val exception = IllegalStateException("Test error")

        coEvery {
            singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId))
        } returns Unit.right()
        coEvery { crudRepository.getAccountListSync(userWalletId) } returns accountList.toOption()
        coEvery { crudRepository.saveAccounts(updatedAccountList) } throws exception

        // Act
        val actual = useCase(
            userWalletId = userWalletId,
            accountName = newAccount.accountName,
            icon = newAccount.icon,
            derivationIndex = newAccount.derivationIndex,
        )

        // Assert
        val expected = Error.DataOperationFailed(cause = exception).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifySequence {
            singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId))
            crudRepository.getAccountListSync(userWalletId)
            crudRepository.saveAccounts(updatedAccountList)
        }

        coVerify(inverse = true) {
            mainAccountTokensMigration.migrate(any(), any())
        }
    }

    @Test
    fun `invoke should return new account if migrate returns error`() = runTest {
        // Arrange
        val newAccount = createNewAccount()
        val accountList = AccountList.empty(userWalletId)
        val updatedAccountList = (accountList + newAccount).getOrNull()!!

        val exception = Exception("Migration error")
        coEvery {
            singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId))
        } returns Unit.right()
        coEvery { crudRepository.getAccountListSync(userWalletId) } returns accountList.toOption()
        coEvery {
            mainAccountTokensMigration.migrate(userWalletId, newAccount.derivationIndex)
        } returns exception.left()

        // Act
        val actual = useCase(
            userWalletId = userWalletId,
            accountName = newAccount.accountName,
            icon = newAccount.icon,
            derivationIndex = newAccount.derivationIndex,
        )

        // Assert
        val expected = newAccount.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifySequence {
            singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId))
            crudRepository.getAccountListSync(userWalletId)
            crudRepository.saveAccounts(updatedAccountList)
            mainAccountTokensMigration.migrate(userWalletId, newAccount.derivationIndex)
        }
    }

    private companion object {

        val userWalletId = UserWalletId("011")

        fun createNewAccount(derivationIndex: Int = 1): Account.CryptoPortfolio {
            return createAccount(
                userWalletId = userWalletId,
                name = "New Account",
                icon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
                derivationIndex = derivationIndex,
            )
        }
    }
}