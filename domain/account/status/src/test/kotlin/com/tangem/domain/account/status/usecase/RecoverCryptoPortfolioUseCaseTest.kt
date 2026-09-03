package com.tangem.domain.account.status.usecase

import arrow.core.None
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import com.google.common.truth.Truth
import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.models.ArchivedAccount
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.status.usecase.RecoverCryptoPortfolioUseCase.Error.DataOperationFailed
import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.account.tokens.MainAccountTokensMigration
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
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
    private val singleAccountListFetcher: SingleAccountListFetcher = mockk()
    private val mainAccountTokensMigration: MainAccountTokensMigration = mockk()
    private val cryptoCurrencyBalanceFetcher: CryptoCurrencyBalanceFetcher = mockk(relaxUnitFun = true)
    private val singleJointAccountListFetcher: SingleJointAccountListFetcher = mockk()
    private val useCase = RecoverCryptoPortfolioUseCase(
        crudRepository = crudRepository,
        mainAccountTokensMigration = mainAccountTokensMigration,
        cryptoCurrencyBalanceFetcher = cryptoCurrencyBalanceFetcher,
        singleAccountListFetcher = singleAccountListFetcher,
        singleJointAccountListFetcher = singleJointAccountListFetcher,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            crudRepository,
            mainAccountTokensMigration,
            cryptoCurrencyBalanceFetcher,
            singleAccountListFetcher,
            singleJointAccountListFetcher,
        )
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
            type = ArchivedAccount.Type.CRYPTO,
        )

        val updatedAccountList = (accountList + account).getOrNull()!!

        coEvery { singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId)) } returns Unit.right()
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
            singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId))
            crudRepository.getAccountListSync(userWalletId)
            crudRepository.getArchivedAccountSync(account.accountId)
            crudRepository.saveAccounts(updatedAccountList)
            crudRepository.getAccountSync(account.accountId)
            cryptoCurrencyBalanceFetcher(userWalletId, account.cryptoCurrencies.toList())
        }
    }

    @Test
    fun `invoke should recover archived joint account and refresh joint list instead of migrating tokens`() = runTest {
        // Arrange
        val account = createJointAccount(userWalletId)
        val accountList = AccountList.empty(userWalletId)
        val archivedAccount = ArchivedAccount(
            accountId = account.accountId,
            name = account.accountName,
            icon = account.icon,
            derivationIndex = DerivationIndex(account.ownerKeyIndex.value).getOrNull()!!,
            tokensCount = 0,
            networksCount = 0,
            type = ArchivedAccount.Type.JOINT,
        )

        val updatedAccountList = (accountList + account).getOrNull()!!

        coEvery { singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId)) } returns Unit.right()
        coEvery { crudRepository.getAccountListSync(userWalletId) } returns accountList.toOption()
        coEvery { crudRepository.getArchivedAccountSync(account.accountId) } returns archivedAccount.toOption()
        coEvery {
            singleJointAccountListFetcher(params = SingleJointAccountListFetcher.Params(userWalletId))
        } returns Unit.right()
        coEvery { crudRepository.getAccountSync(account.accountId) } returns account.toOption()

        // Act
        val actual = useCase(account.accountId)

        // Assert
        val expected = account.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifySequence {
            singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId))
            crudRepository.getAccountListSync(userWalletId)
            crudRepository.getArchivedAccountSync(account.accountId)
            crudRepository.saveAccounts(updatedAccountList)
            singleJointAccountListFetcher(params = SingleJointAccountListFetcher.Params(userWalletId))
            crudRepository.getAccountSync(account.accountId)
            cryptoCurrencyBalanceFetcher(userWalletId, account.cryptoCurrencies.toList())
        }
        coVerify(inverse = true) { mainAccountTokensMigration.migrate(any(), any()) }
    }

    @Test
    fun `GIVEN personal cap reached WHEN recovering joint account THEN it is recovered`() = runTest {
        // Arrange
        val account = createJointAccount(userWalletId)
        val accountList = buildAccountListWithPersonalAccounts(userWalletId, count = AccountList.MAX_CRYPTO_PORTFOLIO_ACCOUNTS_COUNT)
        val archivedAccount = ArchivedAccount(
            accountId = account.accountId,
            name = account.accountName,
            icon = account.icon,
            derivationIndex = DerivationIndex(account.ownerKeyIndex.value).getOrNull()!!,
            tokensCount = 0,
            networksCount = 0,
            type = ArchivedAccount.Type.JOINT,
        )

        val updatedAccountList = (accountList + account).getOrNull()!!

        coEvery { singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId)) } returns Unit.right()
        coEvery { crudRepository.getAccountListSync(userWalletId) } returns accountList.toOption()
        coEvery { crudRepository.getArchivedAccountSync(account.accountId) } returns archivedAccount.toOption()
        coEvery {
            singleJointAccountListFetcher(params = SingleJointAccountListFetcher.Params(userWalletId))
        } returns Unit.right()
        coEvery { crudRepository.getAccountSync(account.accountId) } returns account.toOption()

        // Act
        val actual = useCase(account.accountId)

        // Assert
        val expected = account.right()
        Truth.assertThat(actual).isEqualTo(expected)
        coVerify(exactly = 1) { crudRepository.saveAccounts(updatedAccountList) }
    }

    @Test
    fun `GIVEN personal cap reached WHEN recovering personal account THEN error returned`() = runTest {
        // Arrange
        val account = createAccount(userWalletId, derivationIndex = 20)
        val accountList = buildAccountListWithPersonalAccounts(userWalletId, count = AccountList.MAX_CRYPTO_PORTFOLIO_ACCOUNTS_COUNT)
        val archivedAccount = ArchivedAccount(
            accountId = account.accountId,
            name = account.accountName,
            icon = account.icon,
            derivationIndex = account.derivationIndex,
            tokensCount = 1,
            networksCount = 1,
            type = ArchivedAccount.Type.CRYPTO,
        )

        coEvery { singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId)) } returns Unit.right()
        coEvery { crudRepository.getAccountListSync(userWalletId) } returns accountList.toOption()
        coEvery { crudRepository.getArchivedAccountSync(account.accountId) } returns archivedAccount.toOption()

        // Act
        val actual = useCase(account.accountId)

        // Assert
        val expected = RecoverCryptoPortfolioUseCase.Error.AccountListRequirementsNotMet(
            cause = AccountList.Error.ExceedsMaxAccountsCount,
        ).left()
        Truth.assertThat(actual).isEqualTo(expected)
        coVerify(inverse = true) { crudRepository.saveAccounts(any()) }
    }

    @Test
    fun `GIVEN joint cap reached WHEN recovering joint account THEN error returned`() = runTest {
        // Arrange
        val account = createJointAccount(userWalletId, ownerKeyIndex = 20)
        val accountList = buildAccountListWithJointAccounts(userWalletId, count = AccountList.MAX_JOINT_ACCOUNTS_COUNT)
        val archivedAccount = ArchivedAccount(
            accountId = account.accountId,
            name = account.accountName,
            icon = account.icon,
            derivationIndex = DerivationIndex(account.ownerKeyIndex.value).getOrNull()!!,
            tokensCount = 0,
            networksCount = 0,
            type = ArchivedAccount.Type.JOINT,
        )

        coEvery { singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId)) } returns Unit.right()
        coEvery { crudRepository.getAccountListSync(userWalletId) } returns accountList.toOption()
        coEvery { crudRepository.getArchivedAccountSync(account.accountId) } returns archivedAccount.toOption()

        // Act
        val actual = useCase(account.accountId)

        // Assert
        val expected = RecoverCryptoPortfolioUseCase.Error.AccountListRequirementsNotMet(
            cause = AccountList.Error.ExceedsMaxJointAccountsCount,
        ).left()
        Truth.assertThat(actual).isEqualTo(expected)
        coVerify(inverse = true) { crudRepository.saveAccounts(any()) }
    }

    @Test
    fun `invoke should return error if getAccounts returns None`() = runTest {
        // Arrange
        val accountId = AccountId.Companion.forCryptoPortfolio(
            userWalletId = userWalletId,
            derivationIndex = DerivationIndex.Companion.Main,
        )

        coEvery { singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId)) } returns Unit.right()
        coEvery { crudRepository.getAccountListSync(userWalletId) } returns None

        // Act
        val actual = useCase(accountId).leftOrNull() as DataOperationFailed

        // Assert
        val expected = IllegalStateException("Account list not found for wallet $userWalletId")
        Truth.assertThat(actual.cause).isInstanceOf(expected::class.java)
        Truth.assertThat(actual.cause).hasMessageThat().isEqualTo(expected.message)

        coVerifySequence { singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId))
            crudRepository.getAccountListSync(userWalletId) }
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

        coEvery { singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId)) } returns Unit.right()
        coEvery { crudRepository.getAccountListSync(userWalletId) } throws exception

        // Act
        val actual = useCase(accountId)

        // Assert
        val expected = DataOperationFailed(exception).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifySequence { singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId))
            crudRepository.getAccountListSync(userWalletId) }
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

        coEvery { singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId)) } returns Unit.right()
        coEvery { crudRepository.getAccountListSync(userWalletId) } returns accountList.toOption()
        coEvery { crudRepository.getArchivedAccountSync(account.accountId) } throws exception

        // Act
        val actual = useCase(account.accountId)

        // Assert
        val expected = DataOperationFailed(exception).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifySequence {
            singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId))
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

        coEvery { singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId)) } returns Unit.right()
        coEvery { crudRepository.getAccountListSync(userWalletId) } returns accountList.toOption()
        coEvery { crudRepository.getArchivedAccountSync(account.accountId) } returns None

        // Act
        val actual = useCase(account.accountId).leftOrNull() as DataOperationFailed

        // Assert
        val expected = IllegalStateException("Account not found: ${account.accountId}")
        Truth.assertThat(actual.cause).isInstanceOf(expected::class.java)
        Truth.assertThat(actual.cause).hasMessageThat().isEqualTo(expected.message)

        coVerifySequence {
            singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId))
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
            type = ArchivedAccount.Type.CRYPTO,
        )

        val updatedAccountList = (accountList + account).getOrNull()!!
        val exception = IllegalStateException("Save failed")

        coEvery { singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId)) } returns Unit.right()
        coEvery { crudRepository.getAccountListSync(userWalletId) } returns accountList.toOption()
        coEvery { crudRepository.getArchivedAccountSync(account.accountId) } returns archivedAccount.toOption()
        coEvery { crudRepository.saveAccounts(updatedAccountList) } throws exception

        // Act
        val actual = useCase(account.accountId)

        // Assert
        val expected = DataOperationFailed(exception).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifySequence {
            singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId))
            crudRepository.getAccountListSync(userWalletId)
            crudRepository.getArchivedAccountSync(account.accountId)
            crudRepository.saveAccounts(updatedAccountList)
        }
    }

    @Test
    fun `invoke should return error if fetch is failed`() = runTest {
        // Arrange
        val accountId = AccountId.Companion.forCryptoPortfolio(
            userWalletId = userWalletId,
            derivationIndex = DerivationIndex.Companion.Main,
        )
        val exception = IllegalStateException("Test error")

        coEvery { singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId)) } returns exception.left()

        // Act
        val actual = useCase(accountId)

        // Assert
        val expected = DataOperationFailed(exception).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifySequence {
            singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId))
        }
        coVerify(inverse = true) {
            crudRepository.getAccountListSync(any())
            crudRepository.getArchivedAccountSync(any())
            crudRepository.saveAccounts(any())
        }
    }

    private fun createAccount(
        userWalletId: UserWalletId,
        name: String = "Test Account",
        icon: CryptoPortfolioIcon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
        derivationIndex: Int = Random.nextInt(1, 21),
    ): Account.Personal {
        val derivationIndex = DerivationIndex(derivationIndex).getOrNull()!!

        return Account.Personal(
            accountId = AccountId.forCryptoPortfolio(userWalletId = userWalletId, derivationIndex = derivationIndex),
            accountName = AccountName(name).getOrNull()!!,
            icon = icon,
            derivationIndex = derivationIndex,
            cryptoCurrencies = emptyList(),
        )
    }

    private fun createJointAccount(
        userWalletId: UserWalletId,
        name: String = "Test Joint Account",
        icon: CryptoPortfolioIcon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
        ownerKeyIndex: Int = Random.nextInt(0, 20),
    ): Account.Joint {
        val backendId = ownerKeyIndex.toString(radix = 16).padStart(length = 64, padChar = '0')

        return Account.Joint(
            accountId = AccountId.forJointAccount(userWalletId = userWalletId, value = backendId).getOrNull()!!,
            accountName = AccountName(name).getOrNull()!!,
            icon = icon,
            ownerKeyIndex = OwnerKeyIndex(ownerKeyIndex).getOrNull()!!,
            cryptoCurrencies = emptyList(),
        )
    }

    private fun buildAccountListWithPersonalAccounts(userWalletId: UserWalletId, count: Int): AccountList {
        return (1 until count).fold(AccountList.empty(userWalletId)) { list, derivationIndex ->
            val account = createAccount(userWalletId, name = "Test Account $derivationIndex", derivationIndex = derivationIndex)
            (list + account).getOrNull()!!
        }
    }

    private fun buildAccountListWithJointAccounts(userWalletId: UserWalletId, count: Int): AccountList {
        return (0 until count).fold(AccountList.empty(userWalletId)) { list, ownerKeyIndex ->
            (list + createJointAccount(userWalletId, ownerKeyIndex = ownerKeyIndex)).getOrNull()!!
        }
    }

    private companion object {
        val userWalletId = UserWalletId("011")
    }
}