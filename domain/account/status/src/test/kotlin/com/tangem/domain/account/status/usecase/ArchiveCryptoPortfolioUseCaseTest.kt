package com.tangem.domain.account.status.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.ArchiveCryptoPortfolioUseCase.Error
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.referral.ReferralRepository
import com.tangem.domain.referral.ReferralStatus
import com.tangem.test.mock.MockAccounts.createAccountList
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArchiveCryptoPortfolioUseCaseTest {

    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val crudRepository: AccountsCRUDRepository = mockk(relaxUnitFun = true)
    private val referralRepository: ReferralRepository = mockk(relaxUnitFun = true)

    private val useCase = ArchiveCryptoPortfolioUseCase(
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        crudRepository = crudRepository,
        referralRepository = referralRepository,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(singleAccountStatusListSupplier, referralRepository, crudRepository)
    }

    @Test
    fun `invoke should archive existing crypto portfolio account`() = runTest {
        // Arrange
        val accountList = createAccountList(activeAccounts = 2)
        val archivingAccount = accountList.accounts.last()
        val updatedAccountList = (accountList - archivingAccount).getOrNull()!!

        val params = SingleAccountStatusListProducer.Params(userWalletId)
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(params) } returns accountList.toStatus()

        val referralStatus = ReferralStatus(isActive = false, token = null, address = null)
        coEvery { referralRepository.getReferralStatus(userWalletId.stringValue) } returns referralStatus

        // Act
        val actual = useCase(accountId = archivingAccount.accountId)

        // Assert
        val expected = Unit.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            singleAccountStatusListSupplier.getSyncOrNull(params)
            referralRepository.getReferralStatus(userWalletId.stringValue)
            crudRepository.saveAccounts(updatedAccountList)
        }
    }

    @Test
    fun `invoke should archive if referral status is active but token is null`() = runTest {
        // Arrange
        val accountList = createAccountList(activeAccounts = 2)
        val archivingAccount = accountList.accounts.last()
        val updatedAccountList = (accountList - archivingAccount).getOrNull()!!

        val params = SingleAccountStatusListProducer.Params(userWalletId)
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(params) } returns accountList.toStatus()

        val referralStatus = ReferralStatus(isActive = true, token = null, address = null)
        coEvery { referralRepository.getReferralStatus(userWalletId.stringValue) } returns referralStatus

        // Act
        val actual = useCase(accountId = archivingAccount.accountId)

        // Assert
        val expected = Unit.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            singleAccountStatusListSupplier.getSyncOrNull(params)
            referralRepository.getReferralStatus(userWalletId.stringValue)
            crudRepository.saveAccounts(updatedAccountList)
        }
    }

    @Test
    fun `invoke should archive if referral status is active but token is absent`() = runTest {
        // Arrange
        val accountList = createAccountList(activeAccounts = 2)
        val archivingAccount = accountList.accounts.last()
        val updatedAccountList = (accountList - archivingAccount).getOrNull()!!

        val params = SingleAccountStatusListProducer.Params(userWalletId)
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(params) } returns accountList.toStatus()

        val token = ReferralStatus.Token(
            networkId = "ethereum",
            contractAddress = "0x1",
        )
        val referralStatus = ReferralStatus(isActive = true, token = token, address = "0xABC")
        coEvery { referralRepository.getReferralStatus(userWalletId.stringValue) } returns referralStatus

        // Act
        val actual = useCase(accountId = archivingAccount.accountId)

        // Assert
        val expected = Unit.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            singleAccountStatusListSupplier.getSyncOrNull(params)
            referralRepository.getReferralStatus(userWalletId.stringValue)
            crudRepository.saveAccounts(updatedAccountList)
        }
    }

    @Test
    fun `invoke returns error if account contains referral token `() = runTest {
        // Arrange
        val token = ReferralStatus.Token(
            networkId = "ethereum",
            contractAddress = "0x1",
        )
        val defaultAddress = "0xABC"

        val cryptoCurrency = mockk<CryptoCurrency.Token> {
            every { this@mockk.network.backendId } returns token.networkId
            every { this@mockk.contractAddress } returns token.contractAddress!!
        }

        val networkAddress = NetworkAddress.Single(
            defaultAddress = NetworkAddress.Address(
                value = defaultAddress,
                type = NetworkAddress.Address.Type.Primary,
            ),
        )

        val cryptoCurrencyStatus = mockk<CryptoCurrencyStatus> {
            every { this@mockk.currency } returns cryptoCurrency
            every { this@mockk.value.networkAddress } returns networkAddress
        }

        val accountList = createAccountList(activeAccounts = 2)
        val archivingAccount = accountList.accounts.last()
        val archivingAccountStatus = AccountStatus.CryptoPortfolio(
            account = archivingAccount as Account.CryptoPortfolio,
            tokenList = TokenList.Ungrouped(
                totalFiatBalance = TotalFiatBalance.Failed,
                sortedBy = TokensSortType.NONE,
                currencies = listOf(cryptoCurrencyStatus),
            ),
            priceChangeLce = Unit.lceError(),
        )
        val accountListStatus = with(accountList.toStatus()) {
            copy(
                accountStatuses = accountStatuses.filter { it.accountId != archivingAccount.accountId } +
                    archivingAccountStatus,
            )
        }

        (accountList - archivingAccount).getOrNull()!!

        val params = SingleAccountStatusListProducer.Params(userWalletId)
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(params) } returns accountListStatus

        val referralStatus = ReferralStatus(isActive = true, token = token, address = defaultAddress)
        coEvery { referralRepository.getReferralStatus(userWalletId.stringValue) } returns referralStatus

        // Act
        val actual = useCase(accountId = archivingAccount.accountId)

        // Assert
        val expected = Error.ActiveReferralStatus.left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            singleAccountStatusListSupplier.getSyncOrNull(params)
            referralRepository.getReferralStatus(userWalletId.stringValue)
        }

        coVerify(inverse = true) { crudRepository.saveAccounts(any()) }
    }

    @Test
    fun `invoke should return error if supplier returns null`() = runTest {
        // Arrange
        val accountId = AccountId.forCryptoPortfolio(
            userWalletId = userWalletId,
            derivationIndex = DerivationIndex(1).getOrNull()!!,
        )

        val params = SingleAccountStatusListProducer.Params(userWalletId)
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(params) } returns null

        // Act
        val actual = useCase(accountId)

        // Assert
        val expected = Error.CriticalTechError.AccountsNotCreated(userWalletId).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder { singleAccountStatusListSupplier.getSyncOrNull(params) }
        coVerify(inverse = true) {
            referralRepository.getReferralStatus(any())
            crudRepository.saveAccounts(any())
        }
    }

    @Test
    fun `invoke should return error if account not found`() = runTest {
        // Arrange
        val accountList = AccountList.empty(userWalletId)
        val accountId = AccountId.forCryptoPortfolio(
            userWalletId = userWalletId,
            derivationIndex = DerivationIndex.Companion(1).getOrNull()!!,
        )

        val params = SingleAccountStatusListProducer.Params(userWalletId)
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(params) } returns accountList.toStatus()

        val referralStatus = ReferralStatus(isActive = false, token = null, address = null)
        coEvery { referralRepository.getReferralStatus(userWalletId.stringValue) } returns referralStatus

        // Act
        val actual = useCase(accountId)

        // Assert
        val expected = Error.CriticalTechError.AccountNotFound(accountId).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder { singleAccountStatusListSupplier.getSyncOrNull(params) }

        coVerify(inverse = true) {
            referralRepository.getReferralStatus(any())
            crudRepository.saveAccounts(any())
        }
    }

    @Test
    fun `invoke should return error if saveAccounts throws exception`() = runTest {
        val accountList = createAccountList(activeAccounts = 2)
        val archivingAccount = accountList.accounts.last()
        val updatedAccountList = (accountList - archivingAccount).getOrNull()!!

        val params = SingleAccountStatusListProducer.Params(userWalletId)
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(params) } returns accountList.toStatus()

        val referralStatus = ReferralStatus(isActive = false, token = null, address = null)
        coEvery { referralRepository.getReferralStatus(userWalletId.stringValue) } returns referralStatus

        val exception = IllegalStateException("Save failed")
        coEvery { crudRepository.saveAccounts(updatedAccountList) } throws exception

        // Act
        val actual = useCase(accountId = archivingAccount.accountId)

        // Assert
        val expected = Error.DataOperationFailed(exception).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            singleAccountStatusListSupplier.getSyncOrNull(params)
            referralRepository.getReferralStatus(userWalletId.stringValue)
            crudRepository.saveAccounts(updatedAccountList)
        }
    }

    private fun AccountList.toStatus(): AccountStatusList {
        return AccountStatusList(
            userWalletId = Companion.userWalletId,
            accountStatuses = accounts.map {
                AccountStatus.CryptoPortfolio(
                    account = it as Account.CryptoPortfolio,
                    tokenList = TokenList.Empty,
                    priceChangeLce = Unit.lceError(),
                )
            },
            totalAccounts = totalAccounts,
            totalArchivedAccounts = totalArchivedAccounts,
            totalFiatBalance = TotalFiatBalance.Loading,
            sortType = sortType,
            groupType = groupType,
        )
    }

    private companion object {
        val userWalletId = UserWalletId("011")
    }
}