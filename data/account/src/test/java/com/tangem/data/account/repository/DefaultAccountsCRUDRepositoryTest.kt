package com.tangem.data.account.repository

import arrow.core.None
import arrow.core.toOption
import com.google.common.truth.Truth
import com.tangem.common.test.utils.getEmittedValues
import com.tangem.data.account.converter.*
import com.tangem.data.account.store.AccountsResponseStore
import com.tangem.data.account.store.AccountsResponseStoreFactory
import com.tangem.data.account.store.ArchivedAccountsStore
import com.tangem.data.account.store.ArchivedAccountsStoreFactory
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletArchivedAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.datasource.local.datastore.RuntimeStateStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.models.ArchivedAccount
import com.tangem.domain.models.account.Account.CryptoPortfolio
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import kotlin.time.Duration.Companion.minutes

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultAccountsCRUDRepositoryTest {

    private val tangemTechApi: TangemTechApi = mockk()

    private val accountsResponseStoreFactory: AccountsResponseStoreFactory = mockk()
    private val accountsResponseStore: AccountsResponseStore = mockk()
    private val accountsResponseStoreFlow = MutableStateFlow<GetWalletAccountsResponse?>(value = null)

    private val archivedAccountsStoreFactory: ArchivedAccountsStoreFactory = mockk()
    private val archivedAccountsInnerStore = RuntimeStateStore<List<ArchivedAccount>?>(defaultValue = null)
    private val archivedAccountsStore = ArchivedAccountsStore(runtimeStore = archivedAccountsInnerStore)

    private val userWalletsStore: UserWalletsStore = mockk()

    private val convertersContainer: AccountConverterFactoryContainer = mockk()
    private val accountListConverter: AccountListConverter = mockk()
    private val cryptoPortfolioConverter: CryptoPortfolioConverter = mockk()

    private val repository = DefaultAccountsCRUDRepository(
        tangemTechApi = tangemTechApi,
        accountsResponseStoreFactory = accountsResponseStoreFactory,
        archivedAccountsStoreFactory = archivedAccountsStoreFactory,
        userWalletsStore = userWalletsStore,
        convertersContainer = convertersContainer,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val userWalletId = UserWalletId("011")

    @BeforeAll
    fun setup() {
        every { accountsResponseStoreFactory.create(userWalletId) } returns accountsResponseStore
        every { accountsResponseStore.data } returns accountsResponseStoreFlow

        every { convertersContainer.createAccountListConverter(userWalletId) } returns accountListConverter
        every { convertersContainer.createCryptoPortfolioConverter(userWalletId) } returns cryptoPortfolioConverter
    }

    @BeforeEach
    fun setupEach() {
        every { archivedAccountsStoreFactory.create(userWalletId) } returns archivedAccountsStore
    }

    @AfterEach
    fun tearDownEach() {
        accountsResponseStoreFlow.value = null
        archivedAccountsInnerStore.clear()

        clearMocks(
            tangemTechApi,
            archivedAccountsStoreFactory,
            accountListConverter,
            cryptoPortfolioConverter,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetAccountListSync {

        @Test
        fun `getAccounts should return None when account list response is null`() = runTest {
            // Arrange
            accountsResponseStoreFlow.value = null

            // Act
            val actual = repository.getAccountListSync(userWalletId)

            // Assert
            Truth.assertThat(actual).isEqualTo(None)

            verifyOrder {
                accountsResponseStoreFactory.create(userWalletId)
                accountsResponseStore.data
            }

            verify(inverse = true) { accountListConverter.convert(value = any()) }
        }

        @Test
        fun `getAccounts should return AccountList when account list response is not null`() = runTest {
            // Arrange
            val response = mockk<GetWalletAccountsResponse>()
            val accountList = mockk<AccountList>()

            accountsResponseStoreFlow.value = response

            every { accountListConverter.convert(response) } returns accountList

            // Act
            val actual = repository.getAccountListSync(userWalletId)

            // Assert
            val expected = accountList.toOption()
            Truth.assertThat(actual).isEqualTo(expected)

            verifyOrder {
                accountsResponseStoreFactory.create(userWalletId)
                accountsResponseStore.data
                convertersContainer.createAccountListConverter(userWalletId = userWalletId)
                accountListConverter.convert(response)
            }
        }

        @Test
        fun `getAccounts should throw exception if converter throws exception`() = runTest {
            // Arrange
            val response = mockk<GetWalletAccountsResponse>()
            mockk<AccountList>()

            accountsResponseStoreFlow.value = response

            val exception = Exception("Test error")

            every { accountListConverter.convert(response) } throws exception

            // Act
            val actual = runCatching { repository.getAccountListSync(userWalletId) }.exceptionOrNull()!!

            // Assert
            val expected = exception
            Truth.assertThat(actual).isSameInstanceAs(expected)
            Truth.assertThat(actual).hasMessageThat().isEqualTo(expected.message)

            verifyOrder {
                accountsResponseStoreFactory.create(userWalletId)
                accountsResponseStore.data
                convertersContainer.createAccountListConverter(userWalletId = userWalletId)
                accountListConverter.convert(response)
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetAccountSync {

        private val accountId = AccountId.forCryptoPortfolio(userWalletId, DerivationIndex.Main)

        @Test
        fun `getAccount should return None when account response is null`() = runTest {
            // Arrange
            val response = null

            accountsResponseStoreFlow.value = response

            // Act
            val actual = repository.getAccountSync(accountId)

            // Assert
            Truth.assertThat(actual).isEqualTo(None)

            verifyOrder {
                accountsResponseStoreFactory.create(userWalletId)
                accountsResponseStore.data
            }

            verify(inverse = true) { convertersContainer.createCryptoPortfolioConverter(userWalletId = any()) }
        }

        @Test
        fun `getAccount should return None when accountDto is not found`() = runTest {
            // Arrange
            val response = mockk<GetWalletAccountsResponse> {
                every { this@mockk.accounts } returns emptyList()
            }

            accountsResponseStoreFlow.value = response

            // Act
            val actual = repository.getAccountSync(accountId)

            // Assert
            Truth.assertThat(actual).isEqualTo(None)

            verifyOrder {
                accountsResponseStoreFactory.create(userWalletId)
                accountsResponseStore.data
            }

            verify(inverse = true) { convertersContainer.createCryptoPortfolioConverter(userWalletId = any()) }
        }

        @Test
        fun `getAccount should return Account_CryptoPortfolio when account response is not null`() = runTest {
            // Arrange
            val accountDTO = mockk<WalletAccountDTO> {
                every { this@mockk.id } returns accountId.value
            }

            val response = mockk<GetWalletAccountsResponse> {
                every { this@mockk.accounts } returns listOf(accountDTO)
            }

            accountsResponseStoreFlow.value = response

            val cryptoPortfolio = mockk<CryptoPortfolio>()

            every { cryptoPortfolioConverter.convert(accountDTO) } returns cryptoPortfolio

            // Act
            val actual = repository.getAccountSync(accountId)

            // Assert
            val expected = cryptoPortfolio.toOption()
            Truth.assertThat(actual).isEqualTo(expected)

            verifyOrder {
                accountsResponseStoreFactory.create(userWalletId)
                accountsResponseStore.data
                convertersContainer.createCryptoPortfolioConverter(userWalletId)
                cryptoPortfolioConverter.convert(accountDTO)
            }
        }

        @Test
        fun `getAccount should throw exception if converter throws exception`() = runTest {
            // Arrange
            val accountDTO = mockk<WalletAccountDTO> {
                every { this@mockk.id } returns accountId.value
            }

            val response = mockk<GetWalletAccountsResponse> {
                every { this@mockk.accounts } returns listOf(accountDTO)
            }

            accountsResponseStoreFlow.value = response

            val exception = Exception("Test error")

            every { cryptoPortfolioConverter.convert(accountDTO) } throws exception

            // Act
            val actual = runCatching { repository.getAccountSync(accountId) }.exceptionOrNull()!!

            // Assert
            val expected = exception
            Truth.assertThat(actual).isSameInstanceAs(expected)
            Truth.assertThat(actual).hasMessageThat().isEqualTo(expected.message)

            verifyOrder {
                accountsResponseStoreFactory.create(userWalletId)
                accountsResponseStore.data
                convertersContainer.createCryptoPortfolioConverter(userWalletId)
                cryptoPortfolioConverter.convert(accountDTO)
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetArchivedAccountSync {

        private val accountId = AccountId.forCryptoPortfolio(userWalletId, DerivationIndex.Main)

        @Test
        fun `getArchivedAccount should return None when archived accounts are null`() = runTest {
            // Arrange
            archivedAccountsInnerStore.store(value = null)

            // Act
            val actual = repository.getArchivedAccountSync(accountId)

            // Assert
            Truth.assertThat(actual).isEqualTo(None)

            coVerifyOrder {
                archivedAccountsStoreFactory.create(userWalletId)
                archivedAccountsStore.getSyncOrNull()
            }
        }

        @Test
        fun `getArchivedAccount should return None when archived account not found`() = runTest {
            // Arrange
            archivedAccountsInnerStore.store(value = listOf())
            archivedAccountsStore.setTimestamp(time = System.currentTimeMillis() + 3.minutes.inWholeMicroseconds)

            // Act
            val actual = repository.getArchivedAccountSync(accountId)

            // Assert
            Truth.assertThat(actual).isEqualTo(None)

            coVerifyOrder {
                archivedAccountsStoreFactory.create(userWalletId)
                archivedAccountsStore.getSyncOrNull()
            }
        }

        @Test
        fun `getArchivedAccount should return ArchivedAccount when found`() = runTest {
            // Arrange
            val archivedAccount = ArchivedAccount(
                accountId = accountId,
                name = AccountName("Archived Account").getOrNull()!!,
                icon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
                derivationIndex = DerivationIndex.Main,
                tokensCount = 0,
                networksCount = 0,
            )

            archivedAccountsInnerStore.store(value = listOf(archivedAccount))
            archivedAccountsStore.setTimestamp(time = System.currentTimeMillis() + 3.minutes.inWholeMicroseconds)

            // Act
            val actual = repository.getArchivedAccountSync(accountId)

            // Assert
            val expected = archivedAccount.toOption()
            Truth.assertThat(actual).isEqualTo(expected)

            coVerifyOrder {
                archivedAccountsStoreFactory.create(userWalletId)
                archivedAccountsStore.getSyncOrNull()
            }
        }

        @Test
        fun `getArchivedAccount should throws exception when store throws exception`() = runTest {
            // Arrange
            val exception = Exception("Test error")

            coEvery { archivedAccountsStoreFactory.create(userWalletId) } throws exception

            // Act
            val actual = runCatching { repository.getArchivedAccountSync(accountId) }.exceptionOrNull()!!

            // Assert
            val expected = exception
            Truth.assertThat(actual).isSameInstanceAs(expected)
            Truth.assertThat(actual).hasMessageThat().isEqualTo(expected.message)

            coVerifyOrder { archivedAccountsStoreFactory.create(userWalletId) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetArchivedAccountListSync {

        @Test
        fun `getArchivedAccountListSync should return None when archived accounts are null`() = runTest {
            // Arrange
            archivedAccountsInnerStore.store(value = null)

            // Act
            val actual = repository.getArchivedAccountListSync(userWalletId)

            // Assert
            Truth.assertThat(actual).isEqualTo(None)

            coVerifyOrder {
                archivedAccountsStoreFactory.create(userWalletId)
                archivedAccountsStore.getSyncOrNull()
            }
        }

        @Test
        fun `getArchivedAccountListSync should return Option with list when archived accounts exist`() = runTest {
            // Arrange
            val archivedAccount1 = mockk<ArchivedAccount>()
            val archivedAccount2 = mockk<ArchivedAccount>()
            val archivedAccounts = listOf(archivedAccount1, archivedAccount2)
            archivedAccountsInnerStore.store(value = archivedAccounts)
            archivedAccountsStore.setTimestamp(time = System.currentTimeMillis() + 3.minutes.inWholeMicroseconds)

            // Act
            val actual = repository.getArchivedAccountListSync(userWalletId)

            // Assert
            val expected = archivedAccounts.toOption()
            Truth.assertThat(actual).isEqualTo(expected)

            coVerifyOrder {
                archivedAccountsStoreFactory.create(userWalletId)
                archivedAccountsStore.getSyncOrNull()
            }
        }

        @Test
        fun `getArchivedAccountListSync should throw exception when store throws exception`() = runTest {
            // Arrange
            val exception = Exception("Test error")
            coEvery { archivedAccountsStoreFactory.create(userWalletId) } throws exception

            // Act
            val actual = runCatching { repository.getArchivedAccountListSync(userWalletId) }.exceptionOrNull()!!

            // Assert
            Truth.assertThat(actual).isSameInstanceAs(exception)
            Truth.assertThat(actual).hasMessageThat().isEqualTo(exception.message)

            coVerifyOrder { archivedAccountsStoreFactory.create(userWalletId) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetArchivedAccounts {

        @Test
        fun `getArchivedAccounts should emit empty list when no archived accounts`() = runTest {
            // Arrange
            archivedAccountsInnerStore.store(value = null)

            val archivedAccountsFlow = repository.getArchivedAccounts(userWalletId)

            // Act
            val actual = getEmittedValues(archivedAccountsFlow)

            // Assert
            Truth.assertThat(actual).isEmpty()

            coVerifyOrder {
                archivedAccountsStoreFactory.create(userWalletId)
                archivedAccountsStore.get()
            }
        }

        @Test
        fun `getArchivedAccounts should emit list of archived accounts when present`() = runTest {
            // Arrange
            val archivedAccount1 = mockk<ArchivedAccount>()
            val archivedAccount2 = mockk<ArchivedAccount>()
            val archivedAccounts = listOf(archivedAccount1, archivedAccount2)

            archivedAccountsInnerStore.store(value = archivedAccounts)
            archivedAccountsStore.setTimestamp(time = System.currentTimeMillis() + 3.minutes.inWholeMicroseconds)

            val archivedAccountsFlow = repository.getArchivedAccounts(userWalletId)

            // Act
            val actual = getEmittedValues(archivedAccountsFlow)

            // Assert
            val expected = listOf(archivedAccounts)
            Truth.assertThat(actual).containsExactlyElementsIn(expected)
            coVerifyOrder {
                archivedAccountsStoreFactory.create(userWalletId)
                archivedAccountsStore.get()
            }
        }

        @Test
        fun `getArchivedAccounts should throw exception when store throws exception`() = runTest {
            // Arrange
            val exception = Exception("Test error")
            coEvery { archivedAccountsStoreFactory.create(userWalletId) } throws exception

            // Act
            val actual = runCatching { repository.getArchivedAccounts(userWalletId) }.exceptionOrNull()!!

            // Assert
            Truth.assertThat(actual).isSameInstanceAs(exception)
            Truth.assertThat(actual).hasMessageThat().isEqualTo(exception.message)

            coVerifyOrder { archivedAccountsStoreFactory.create(userWalletId) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FetchArchivedAccounts {

        private val accountId = AccountId.forCryptoPortfolio(userWalletId, DerivationIndex.Main)

        @Test
        fun `fetchArchivedAccounts should store archived accounts in store`() = runTest {
            // Arrange
            val accountDTO = WalletAccountDTO(
                id = accountId.value,
                name = "Archived Account",
                derivationIndex = 0,
                icon = CryptoPortfolioIcon.Icon.Wallet.name,
                iconColor = CryptoPortfolioIcon.Color.DullLavender.name,
                totalNetworks = 0,
                totalTokens = 0,
            )

            val apiResponse = mockk<GetWalletArchivedAccountsResponse> {
                every { this@mockk.accounts } returns listOf(accountDTO)
            }

            val archivedAccount = ArchivedAccountConverter(userWalletId).convert(accountDTO)

            coEvery {
                tangemTechApi.getWalletArchivedAccounts(userWalletId.stringValue)
            } returns ApiResponse.Success(apiResponse)

            // Act
            repository.fetchArchivedAccounts(userWalletId)
            val actual = archivedAccountsStore.getSyncOrNull()

            // Assert
            Truth.assertThat(actual).containsExactly(archivedAccount)

            coVerifyOrder {
                tangemTechApi.getWalletArchivedAccounts(userWalletId.stringValue)
                archivedAccountsStoreFactory.create(userWalletId)
            }
        }

        @Test
        fun `fetchArchivedAccounts should throw exception if API returns error`() = runTest { // Arrange
            val exception = Exception("API error")
            coEvery { tangemTechApi.getWalletArchivedAccounts(userWalletId.stringValue) } throws exception

            // Act
            val actual = runCatching { repository.fetchArchivedAccounts(userWalletId) }.exceptionOrNull()!!

            // Assert
            Truth.assertThat(actual).isSameInstanceAs(exception)
            Truth.assertThat(actual).hasMessageThat().isEqualTo(exception.message)
            Truth.assertThat(archivedAccountsStore.getSyncOrNull()).isNull()

            coVerify { tangemTechApi.getWalletArchivedAccounts(userWalletId.stringValue) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SaveAccounts {

        private val version = 1

        @Test
        fun `saveAccounts should call API and update store`() = runTest {
            // Arrange
            val userWallet = mockk<UserWallet> {
                every { this@mockk.walletId } returns userWalletId
            }

            val accountList = AccountList.empty(userWallet = userWallet)

            val accountsResponse = mockk<GetWalletAccountsResponse> {
                every { this@mockk.wallet.version } returns version
            }

            accountsResponseStoreFlow.value = accountsResponse

            val body = SaveWalletAccountsResponseConverter.convert(value = accountList)

            val apiResponse = ApiResponse.Success(Unit)

            coEvery {
                tangemTechApi.saveWalletAccounts(
                    walletId = userWalletId.stringValue,
                    ifMatch = version.toString(),
                    body = body,
                )
            } returns apiResponse

            val converter = mockk<GetWalletAccountsResponseConverter> {
                every { this@mockk.convert(accountList) } returns accountsResponse
            }

            every {
                convertersContainer.getWalletAccountsResponseCF.create(userWallet = userWallet, version = version)
            } returns converter

            coEvery { accountsResponseStore.updateData(transform = any()) } returns accountsResponse

            // Act
            repository.saveAccounts(accountList)

            // Assert
            Truth.assertThat(accountsResponseStoreFlow.value).isEqualTo(accountsResponse)

            coVerifyOrder {
                accountsResponseStoreFactory.create(userWalletId)
                accountsResponseStore.data
                tangemTechApi.saveWalletAccounts(userWalletId.stringValue, version.toString(), body)
                convertersContainer.getWalletAccountsResponseCF.create(userWallet, version)
                converter.convert(accountList)
                accountsResponseStore.updateData(any())
            }
        }

        @Test
        fun `saveAccounts if API request is failed`() = runTest {
            // Arrange
            val userWallet = mockk<UserWallet> {
                every { this@mockk.walletId } returns userWalletId
            }

            val accountList = AccountList.empty(userWallet = userWallet)

            val accountsResponse = mockk<GetWalletAccountsResponse> {
                every { this@mockk.wallet.version } returns version
            }

            accountsResponseStoreFlow.value = accountsResponse

            val body = SaveWalletAccountsResponseConverter.convert(value = accountList)

            val apiResponse = ApiResponse.Error(cause = ApiResponseError.NetworkException) as ApiResponse<Unit>

            coEvery {
                tangemTechApi.saveWalletAccounts(
                    walletId = userWalletId.stringValue,
                    ifMatch = version.toString(),
                    body = body,
                )
            } returns apiResponse

            // Act
            val actual = runCatching { repository.saveAccounts(accountList) }.exceptionOrNull()!!

            // Assert
            Truth.assertThat(actual).isEqualTo(ApiResponseError.NetworkException)

            coVerifyOrder {
                accountsResponseStoreFactory.create(userWalletId)
                accountsResponseStore.data
                tangemTechApi.saveWalletAccounts(userWalletId.stringValue, version.toString(), body)
            }

            coVerify(inverse = true) {
                convertersContainer.getWalletAccountsResponseCF.create(any(), any())
                accountsResponseStore.updateData(any())
            }
        }
    }
}