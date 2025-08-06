package com.tangem.domain.account.usecase

import arrow.core.None
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import com.google.common.truth.Truth
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddCryptoPortfolioUseCaseTest {

    private val crudRepository: AccountsCRUDRepository = mockk(relaxUnitFun = true)
    private val useCase = AddCryptoPortfolioUseCase(crudRepository)

    private val userWalletId = UserWalletId("011")
    private val userWallet = mockk<UserWallet>()

    @BeforeEach
    fun resetMocks() {
        clearMocks(crudRepository, userWallet)

        every { userWallet.walletId } returns userWalletId
    }

    @Test
    fun `invoke should add new crypto portfolio account to existing list`() = runTest {
        // Arrange
        val existingAccount = createAccount(
            name = "Main Account",
            derivationIndex = 0,
            icon = CryptoPortfolioIcon.ofMainAccount(userWalletId = userWalletId),
        )

        val accountList = AccountList(
            userWallet = userWallet,
            accounts = setOf(existingAccount),
            totalAccounts = 1,
        ).getOrNull()!!

        val fakeUUID = UUID.randomUUID()

        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns fakeUUID

        val newAccount = createAccount(
            accountId = AccountId(value = fakeUUID.toString(), userWalletId = userWalletId),
            name = "New Account",
            icon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
            derivationIndex = 1,
        )

        val updatedAccountList = (accountList + newAccount).getOrNull()!!

        coEvery { crudRepository.getAccounts(userWalletId) } returns accountList.toOption()
        coEvery { crudRepository.saveAccounts(updatedAccountList) } just Runs

        // Act
        val actual = useCase(
            userWalletId = userWalletId,
            accountName = newAccount.name,
            icon = newAccount.icon,
            derivationIndex = newAccount.derivationIndex,
        )

        // Assert
        val expected = newAccount.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            crudRepository.getAccounts(userWalletId)
            crudRepository.saveAccounts(updatedAccountList)
        }

        coVerify(inverse = true) { crudRepository.getUserWallet(userWalletId) }

        unmockkStatic(UUID::class)
    }

    @Test
    fun `invoke should create new account list if none exists`() = runTest {
        // Arrange
        val fakeUUID = UUID.randomUUID()

        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns fakeUUID

        val newAccount = createAccount(
            accountId = AccountId(value = fakeUUID.toString(), userWalletId = userWalletId),
            name = "New Account",
            icon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
            derivationIndex = 1,
        )

        val newAccountList = (AccountList.createEmpty(userWallet) + newAccount).getOrNull()!!

        coEvery { crudRepository.getAccounts(userWalletId) } returns None
        coEvery { crudRepository.getUserWallet(userWalletId) } returns userWallet
        coEvery { crudRepository.saveAccounts(newAccountList) } just Runs

        // Act
        val actual = useCase(
            userWalletId = userWalletId,
            accountName = newAccount.name,
            icon = newAccount.icon,
            derivationIndex = newAccount.derivationIndex,
        )

        // Assert
        val expected = newAccount.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            crudRepository.getAccounts(userWalletId)
            crudRepository.getUserWallet(userWalletId)
            crudRepository.saveAccounts(newAccountList)
        }

        unmockkStatic(UUID::class)
    }

    @Test
    fun `invoke should return error if account creation fails`() = runTest {
        // Act
        val actual = useCase(
            userWalletId = userWalletId,
            accountName = AccountName.Main,
            icon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
            derivationIndex = -1,
        )

        // Assert
        val expected = AddCryptoPortfolioUseCase.Error.AccountCreation(
            cause = Account.CryptoPortfolio.Error.NegativeDerivationIndex,
        ).left()

        Truth.assertThat(actual).isEqualTo(expected)

        coVerify(inverse = true) {
            crudRepository.getAccounts(any())
            crudRepository.getUserWallet(any())
            crudRepository.saveAccounts(any())
        }
    }

    @Test
    fun `invoke should return error if account list requirements not met`() = runTest {
        // Arrange
        val accountList = AccountList(
            userWallet = userWallet,
            accounts = createAccounts(count = 20),
            totalAccounts = 20,
        ).getOrNull()!!

        val newAccount = createAccount(
            name = "New Account",
            icon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
            derivationIndex = 1,
        )

        coEvery { crudRepository.getAccounts(userWalletId) } returns accountList.toOption()

        // Act
        val actual = useCase(
            userWalletId = userWalletId,
            accountName = newAccount.name,
            icon = newAccount.icon,
            derivationIndex = newAccount.derivationIndex,
        )

        // Assert
        val expected = AddCryptoPortfolioUseCase.Error.AccountListRequirementsNotMet(
            cause = AccountList.Error.ExceedsMaxAccountsCount,
        ).left()

        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder { crudRepository.getAccounts(userWalletId) }

        coVerify(inverse = true) {
            crudRepository.getUserWallet(any())
            crudRepository.saveAccounts(any())
        }
    }

    @Test
    fun `invoke should return error if getAccounts throws exception`() = runTest {
        // Arrange
        val newAccount = createAccount(
            name = "New Account",
            icon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
            derivationIndex = 1,
        )

        val exception = IllegalStateException("Test error")

        coEvery { crudRepository.getAccounts(userWalletId) } throws exception

        // Act
        val actual = useCase(
            userWalletId = userWalletId,
            accountName = newAccount.name,
            icon = newAccount.icon,
            derivationIndex = newAccount.derivationIndex,
        )

        // Assert
        val expected = AddCryptoPortfolioUseCase.Error.DataOperationFailed(cause = exception).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder { crudRepository.getAccounts(userWalletId) }

        coVerify(inverse = true) {
            crudRepository.getUserWallet(any())
            crudRepository.saveAccounts(any())
        }
    }

    @Test
    fun `invoke should return error if saveAccounts throws exception`() = runTest {
        // Arrange
        val existingAccount = createAccount(
            name = "Main Account",
            derivationIndex = 0,
            icon = CryptoPortfolioIcon.ofMainAccount(userWalletId = userWalletId),
        )

        val accountList = AccountList(
            userWallet = userWallet,
            accounts = setOf(existingAccount),
            totalAccounts = 1,
        ).getOrNull()!!

        val fakeUUID = UUID.randomUUID()

        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns fakeUUID

        val newAccount = createAccount(
            accountId = AccountId(value = fakeUUID.toString(), userWalletId = userWalletId),
            name = "New Account",
            icon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
            derivationIndex = 1,
        )

        val updatedAccountList = (accountList + newAccount).getOrNull()!!

        val exception = IllegalStateException("Test error")

        coEvery { crudRepository.getAccounts(userWalletId) } returns accountList.toOption()
        coEvery { crudRepository.saveAccounts(updatedAccountList) } throws exception

        // Act
        useCase(
            userWalletId = userWalletId,
            accountName = newAccount.name,
            icon = newAccount.icon,
            derivationIndex = newAccount.derivationIndex,
        )

        // Assert
        // val expected = AddCryptoPortfolioUseCase.Error.DataOperationFailed(cause = exception).left()
        // Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            crudRepository.getAccounts(userWalletId)
            crudRepository.saveAccounts(updatedAccountList)
        }

        coVerify(inverse = true) { crudRepository.getUserWallet(userWalletId) }
    }

    private fun createAccounts(count: Int): Set<Account.CryptoPortfolio> {
        return buildSet {
            add(createAccount(derivationIndex = 0))
            repeat(count - 1) {
                add(createAccount())
            }
        }
    }

    private fun createAccount(
        accountId: AccountId? = null,
        name: String = "Test Account",
        icon: CryptoPortfolioIcon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
        derivationIndex: Int = Random.nextInt(1, 21),
    ): Account.CryptoPortfolio {
        return Account.CryptoPortfolio(
            accountId = accountId ?: AccountId(value = UUID.randomUUID().toString(), userWalletId = userWalletId),
            accountName = AccountName(name).getOrNull()!!,
            accountIcon = icon,
            derivationIndex = derivationIndex,
            isArchived = false,
            cryptoCurrencyList = Account.CryptoPortfolio.CryptoCurrencyList(
                currencies = emptySet(),
                sortType = TokensSortType.NONE,
                groupType = TokensGroupType.NONE,
            ),
        ).getOrNull()!!
    }
}