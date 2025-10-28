package com.tangem.domain.account.status.usecase

import arrow.core.None
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import com.google.common.truth.Truth
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.models.ArchivedAccount
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.status.usecase.RecoverCryptoPortfolioUseCase.Error.DataOperationFailed
import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.account.tokens.MainAccountTokensMigration
import com.tangem.domain.models.account.*
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.random.Random

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecoverCryptoPortfolioUseCaseTest {

    private val crudRepository: AccountsCRUDRepository = mockk(relaxUnitFun = true)
    private val mainAccountTokensMigration: MainAccountTokensMigration = mockk()
    private val cryptoCurrencyBalanceFetcher: CryptoCurrencyBalanceFetcher = mockk(relaxUnitFun = true)
    private val useCase = RecoverCryptoPortfolioUseCase(
        crudRepository = crudRepository,
        mainAccountTokensMigration = mainAccountTokensMigration,
        cryptoCurrencyBalanceFetcher = cryptoCurrencyBalanceFetcher,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(crudRepository, mainAccountTokensMigration, cryptoCurrencyBalanceFetcher)
    }

    @Test
    fun `invoke should recover archived crypto portfolio account`() = runTest {
        // Arrange
        val account = createAccount(userWalletId)
        val accountList = AccountList.empty(userWalletId)
        val archivedAccount = ArchivedAccount(
            accountId = account.accountId,
            name = account.accountName,
            icon = account.icon,
            derivationIndex = account.derivationIndex,
            tokensCount = 1,
            networksCount = 1,
        )

        val updatedAccountList = (accountList + account).getOrNull()!!

        coEvery { crudRepository.getAccountListSync(userWalletId) } returns accountList.toOption()
        coEvery { crudRepository.getArchivedAccountSync(account.accountId) } returns archivedAccount.toOption()
        coEvery { mainAccountTokensMigration.migrate(userWalletId, account.derivationIndex) } returns Unit.right()
        coEvery { crudRepository.getAccountSync(account.accountId) } returns account.toOption()

        // Act
        val actual = useCase(account.accountId)

        // Assert
        val expected = account.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifySequence {
            crudRepository.getAccountListSync(userWalletId)
            crudRepository.getArchivedAccountSync(account.accountId)
            crudRepository.saveAccounts(updatedAccountList)
            crudRepository.getAccountSync(account.accountId)
            cryptoCurrencyBalanceFetcher(userWalletId, account.cryptoCurrencies.toList())
        }
    }

    @Test
    fun `invoke should return error if getAccounts returns None`() = runTest {
        // Arrange
        val accountId = AccountId.Companion.forCryptoPortfolio(
            userWalletId = userWalletId,
            derivationIndex = DerivationIndex.Companion.Main,
        )

        coEvery { crudRepository.getAccountListSync(userWalletId) } returns None

        // Act
        val actual = useCase(accountId).leftOrNull() as DataOperationFailed

        // Assert
        val expected = IllegalStateException("Account list not found for wallet $userWalletId")
        Truth.assertThat(actual.cause).isInstanceOf(expected::class.java)
        Truth.assertThat(actual.cause).hasMessageThat().isEqualTo(expected.message)

        coVerifySequence { crudRepository.getAccountListSync(userWalletId) }
        coVerify(inverse = true) {
            crudRepository.getArchivedAccountSync(any())
            crudRepository.saveAccounts(any())
        }
    }

    @Test
    fun `invoke should return error if getAccounts throws exception`() = runTest {
        // Arrange
        val accountId = AccountId.Companion.forCryptoPortfolio(
            userWalletId = userWalletId,
            derivationIndex = DerivationIndex.Companion.Main,
        )
        val exception = IllegalStateException("Test error")

        coEvery { crudRepository.getAccountListSync(userWalletId) } throws exception

        // Act
        val actual = useCase(accountId)

        // Assert
        val expected = DataOperationFailed(exception).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifySequence { crudRepository.getAccountListSync(userWalletId) }
        coVerify(inverse = true) {
            crudRepository.getArchivedAccountSync(any())
            crudRepository.saveAccounts(any())
        }
    }

    @Test
    fun `invoke should return error if getArchivedAccount throws exception`() = runTest {
        // Arrange
        val account = createAccount(userWalletId)
        val accountList = AccountList.Companion.empty(userWalletId)
        val exception = IllegalStateException("Test error")

        coEvery { crudRepository.getAccountListSync(userWalletId) } returns accountList.toOption()
        coEvery { crudRepository.getArchivedAccountSync(account.accountId) } throws exception

        // Act
        val actual = useCase(account.accountId)

        // Assert
        val expected = DataOperationFailed(exception).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifySequence {
            crudRepository.getAccountListSync(userWalletId)
            crudRepository.getArchivedAccountSync(account.accountId)
        }
        coVerify(inverse = true) { crudRepository.saveAccounts(any()) }
    }

    @Test
    fun `invoke should return error if getArchivedAccount returns null`() = runTest {
        // Arrange
        val account = createAccount(userWalletId)
        val accountList = AccountList.Companion.empty(userWalletId)

        coEvery { crudRepository.getAccountListSync(userWalletId) } returns accountList.toOption()
        coEvery { crudRepository.getArchivedAccountSync(account.accountId) } returns None

        // Act
        val actual = useCase(account.accountId).leftOrNull() as DataOperationFailed

        // Assert
        val expected = IllegalStateException("Account not found: ${account.accountId}")
        Truth.assertThat(actual.cause).isInstanceOf(expected::class.java)
        Truth.assertThat(actual.cause).hasMessageThat().isEqualTo(expected.message)

        coVerifySequence {
            crudRepository.getAccountListSync(userWalletId)
            crudRepository.getArchivedAccountSync(account.accountId)
        }
        coVerify(inverse = true) { crudRepository.saveAccounts(any()) }
    }

    @Test
    fun `invoke should return error if saveAccounts throws exception`() = runTest {
        // Arrange
        val account = createAccount(userWalletId)
        val accountList = AccountList.Companion.empty(userWalletId)
        val archivedAccount = ArchivedAccount(
            accountId = account.accountId,
            name = account.accountName,
            icon = account.icon,
            derivationIndex = account.derivationIndex,
            tokensCount = 1,
            networksCount = 1,
        )

        val updatedAccountList = (accountList + account).getOrNull()!!
        val exception = IllegalStateException("Save failed")

        coEvery { crudRepository.getAccountListSync(userWalletId) } returns accountList.toOption()
        coEvery { crudRepository.getArchivedAccountSync(account.accountId) } returns archivedAccount.toOption()
        coEvery { crudRepository.saveAccounts(updatedAccountList) } throws exception

        // Act
        val actual = useCase(account.accountId)

        // Assert
        val expected = DataOperationFailed(exception).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifySequence {
            crudRepository.getAccountListSync(userWalletId)
            crudRepository.getArchivedAccountSync(account.accountId)
            crudRepository.saveAccounts(updatedAccountList)
        }
    }

    private fun createAccount(
        userWalletId: UserWalletId,
        name: String = "Test Account",
        icon: CryptoPortfolioIcon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
        derivationIndex: Int = Random.nextInt(1, 21),
    ): Account.CryptoPortfolio {
        val derivationIndex = DerivationIndex(derivationIndex).getOrNull()!!

        return Account.CryptoPortfolio(
            accountId = AccountId.forCryptoPortfolio(userWalletId = userWalletId, derivationIndex = derivationIndex),
            accountName = AccountName(name).getOrNull()!!,
            icon = icon,
            derivationIndex = derivationIndex,
            cryptoCurrencies = emptySet(),
        )
    }

    private companion object {
        val userWalletId = UserWalletId("011")
    }
}