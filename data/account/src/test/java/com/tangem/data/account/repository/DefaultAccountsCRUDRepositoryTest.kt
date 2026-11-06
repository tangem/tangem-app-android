package com.tangem.data.account.repository

import android.content.res.Resources
import arrow.core.None
import arrow.core.toOption
import com.google.common.truth.Truth
import com.tangem.core.local.datastore.RuntimeStateStore
import com.tangem.core.remote.response.ApiResponse
import com.tangem.data.account.api.WalletAccountsApi
import com.tangem.data.account.converter.*
import com.tangem.data.account.store.AccountsResponseStore
import com.tangem.data.account.store.AccountsResponseStoreFactory
import com.tangem.data.account.store.ArchivedAccountsStore
import com.tangem.data.account.store.ArchivedAccountsStoreFactory
import com.tangem.data.common.account.WalletAccountsSaver
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletArchivedAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.SaveWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.models.ArchivedAccount
import com.tangem.domain.models.account.*
import com.tangem.domain.models.account.Account.Personal
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.collectibles.api.CollectiblesFeatureToggles
import com.tangem.features.jointaccount.JointAccountFeatureToggles
import com.tangem.test.core.getEmittedValues
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*

/**
[REDACTED_AUTHOR]
 */
@Suppress("UnusedFlow")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultAccountsCRUDRepositoryTest {

    private val walletAccountsApi: WalletAccountsApi = mockk()
    private val walletAccountsSaver: WalletAccountsSaver = mockk(relaxUnitFun = true)

    private val accountsResponseStoreFactory: AccountsResponseStoreFactory = mockk()
    private val accountsResponseStore: AccountsResponseStore = mockk()
    private val accountsResponseStoreFlow = MutableStateFlow<GetWalletAccountsResponse?>(value = null)

    private val archivedAccountsStoreFactory: ArchivedAccountsStoreFactory = mockk()
    private val archivedAccountsInnerStore = RuntimeStateStore<List<ArchivedAccount>?>(defaultValue = null)
    private val archivedAccountsStore = ArchivedAccountsStore(runtimeStore = archivedAccountsInnerStore)

    private val userTokensSaver: UserTokensSaver = mockk()
    private val archivedAccountsETagStore: RuntimeStateStore<Map<String, String?>> = mockk(relaxUnitFun = true)

    private val convertersContainer: AccountConverterFactoryContainer = mockk()
    private val accountListConverter: AccountListConverter = mockk()
    private val cryptoPortfolioConverter: CryptoPortfolioConverter = mockk()

    private val resources: Resources = mockk()

    private val jointAccountFeatureToggles: JointAccountFeatureToggles = mockk()
    private val collectiblesFeatureToggles: CollectiblesFeatureToggles = mockk()

    private val repository = DefaultAccountsCRUDRepository(
        walletAccountsApi = walletAccountsApi,
        walletAccountsSaver = walletAccountsSaver,
        accountsResponseStoreFactory = accountsResponseStoreFactory,
        archivedAccountsStoreFactory = archivedAccountsStoreFactory,
        userTokensSaver = userTokensSaver,
        archivedAccountsETagStore = archivedAccountsETagStore,
        convertersContainer = convertersContainer,
        resources = resources,
        dispatchers = TestingCoroutineDispatcherProvider(),
        jointAccountFeatureToggles = jointAccountFeatureToggles,
        collectiblesFeatureToggles = collectiblesFeatureToggles,
    )

    private val userWalletId = UserWalletId("011")
    private val archivedETagKey = userWalletId.stringValue

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
        every { jointAccountFeatureToggles.isJointAccountEnabled } returns true
        every { collectiblesFeatureToggles.isCollectiblesEnabled } returns true
        every { walletAccountsApi.isV2Enabled } returns true
    }

    @AfterEach
    fun tearDownEach() {
        accountsResponseStoreFlow.value = null
        archivedAccountsInnerStore.clear()

        clearMocks(
            walletAccountsApi,
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

            val cryptoPortfolio = mockk<Personal>()

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
                type = ArchivedAccount.Type.CRYPTO,
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

            val eTag = "etag123"
            val apiResponse = mockk<GetWalletArchivedAccountsResponse> {
                every { this@mockk.accounts } returns listOf(accountDTO)
            }

            val archivedAccount = ArchivedAccountConverter(userWalletId).convert(accountDTO)

            coEvery { archivedAccountsETagStore.getSyncOrNull() } returns mapOf(archivedETagKey to eTag)

            coEvery {
                walletAccountsApi.getArchivedAccounts(userWalletId.stringValue, eTag)
            } returns ApiResponse.Success(apiResponse)

            // Act
            repository.fetchArchivedAccounts(userWalletId)
            val actual = archivedAccountsStore.getSyncOrNull()

            // Assert
            Truth.assertThat(actual).containsExactly(archivedAccount)

            coVerifyOrder {
                archivedAccountsETagStore.getSyncOrNull()
                archivedAccountsStoreFactory.create(userWalletId)
                walletAccountsApi.getArchivedAccounts(userWalletId.stringValue, eTag)
                archivedAccountsETagStore.update(any())
            }
        }

        @Test
        fun `GIVEN an archived collectibles record WHEN fetchArchivedAccounts THEN it is left out`() = runTest {
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
                every { this@mockk.accounts } returns listOf(accountDTO, collectiblesRecord())
            }
            coEvery { archivedAccountsETagStore.getSyncOrNull() } returns emptyMap()
            coEvery {
                walletAccountsApi.getArchivedAccounts(userWalletId.stringValue, null)
            } returns ApiResponse.Success(apiResponse)

            // Act
            repository.fetchArchivedAccounts(userWalletId)

            // Assert
            Truth.assertThat(archivedAccountsStore.getSyncOrNull())
                .containsExactly(ArchivedAccountConverter(userWalletId).convert(accountDTO))
        }

        @Test
        fun `GIVEN joint accounts disabled WHEN fetchArchivedAccounts THEN an archived joint record is left out`() =
            runTest {
                // Arrange
                every { jointAccountFeatureToggles.isJointAccountEnabled } returns false
                val accountDTO = WalletAccountDTO(
                    id = accountId.value,
                    name = "Archived Account",
                    derivationIndex = 0,
                    icon = CryptoPortfolioIcon.Icon.Wallet.name,
                    iconColor = CryptoPortfolioIcon.Color.DullLavender.name,
                    totalNetworks = 0,
                    totalTokens = 0,
                )
                val jointDTO = accountDTO.copy(
                    id = "4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6",
                    name = "Archived Joint",
                    type = WalletAccountDTO.Type.JOINT,
                )
                val apiResponse = mockk<GetWalletArchivedAccountsResponse> {
                    every { this@mockk.accounts } returns listOf(accountDTO, jointDTO)
                }
                coEvery { archivedAccountsETagStore.getSyncOrNull() } returns emptyMap()
                coEvery {
                    walletAccountsApi.getArchivedAccounts(userWalletId.stringValue, null)
                } returns ApiResponse.Success(apiResponse)

                // Act
                repository.fetchArchivedAccounts(userWalletId)

                // Assert
                Truth.assertThat(archivedAccountsStore.getSyncOrNull())
                    .containsExactly(ArchivedAccountConverter(userWalletId).convert(accountDTO))
            }

        @Test
        fun `fetchArchivedAccounts should throw exception if API returns error`() = runTest {
            // Arrange
            val eTag = "etag123"
            val exception = Exception("API error")

            coEvery { archivedAccountsETagStore.getSyncOrNull() } returns mapOf(archivedETagKey to eTag)
            coEvery { walletAccountsApi.getArchivedAccounts(userWalletId.stringValue, eTag) } throws exception

            // Act
            val actual = runCatching { repository.fetchArchivedAccounts(userWalletId) }.exceptionOrNull()!!

            // Assert
            Truth.assertThat(actual).isSameInstanceAs(exception)
            Truth.assertThat(actual).hasMessageThat().isEqualTo(exception.message)
            Truth.assertThat(archivedAccountsStore.getSyncOrNull()).isNull()

            coVerifyOrder {
                archivedAccountsETagStore.getSyncOrNull()
                walletAccountsApi.getArchivedAccounts(userWalletId.stringValue, eTag)
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SaveAccounts {

        @Test
        fun `saveAccounts should call API and update store`() = runTest {
            // Arrange
            val accountList = AccountList.empty(userWalletId = userWalletId)
            val expectedBody = SaveWalletAccountsResponseConverter.convert(value = accountList)

            val accountsResponse = createGetWalletAccountsResponse(userWalletId)

            coEvery {
                walletAccountsSaver.push(userWalletId = userWalletId, body = expectedBody)
            } returns accountsResponse

            // Act
            repository.saveAccounts(accountList)

            // Assert
            coVerifyOrder {
                walletAccountsSaver.push(userWalletId = userWalletId, body = expectedBody)
                walletAccountsSaver.store(userWalletId = userWalletId, response = accountsResponse)
            }
        }

        /**
         * The backend archives an account whose row is missing from the written document, so a joint account
         * must survive an edit of an ordinary one — renaming, sorting or archiving a crypto account all come
         * through here with the whole list.
         */
        @Test
        fun `GIVEN account list with a joint account WHEN saveAccounts THEN the joint row is pushed too`() = runTest {
            // Arrange
            val accountList = AccountList(
                userWalletId = userWalletId,
                accounts = listOf(Personal.createMainAccount(userWalletId = userWalletId), jointAccount()),
                totalAccounts = 2,
                totalArchivedAccounts = 0,
                totalJointAccounts = 1,
            ).getOrNull()!!

            val accountsResponse = createGetWalletAccountsResponse(userWalletId)
            val bodySlot = slot<SaveWalletAccountsResponse>()

            coEvery {
                walletAccountsSaver.push(userWalletId = userWalletId, body = capture(bodySlot))
            } returns accountsResponse

            // Act
            repository.saveAccounts(accountList)

            // Assert
            Truth.assertThat(bodySlot.captured.accounts.map { it.type })
                .containsExactly(WalletAccountDTO.Type.CRYPTO, WalletAccountDTO.Type.JOINT)
            Truth.assertThat(bodySlot.captured.accounts.map { it.id })
                .containsExactly(
                    Personal.createMainAccount(userWalletId = userWalletId).accountId.value,
                    jointAccount().accountId.value,
                )
        }

        @Test
        fun `GIVEN joint accounts disabled WHEN saveAccounts THEN the stored joint row is pushed back in its place`() =
            runTest {
                // Arrange
                every { jointAccountFeatureToggles.isJointAccountEnabled } returns false
                val mainAccount = Personal.createMainAccount(userWalletId = userWalletId)
                val accountList = AccountList.empty(userWalletId = userWalletId)
                accountsResponseStoreFlow.value = storedResponse(jointRow(), mainRow(mainAccount))
                val bodySlot = slot<SaveWalletAccountsResponse>()
                coEvery {
                    walletAccountsSaver.push(userWalletId = userWalletId, body = capture(bodySlot))
                } returns createGetWalletAccountsResponse(userWalletId)

                // Act
                repository.saveAccounts(accountList)

                // Assert
                Truth.assertThat(bodySlot.captured.accounts.map { it.id to it.type }).containsExactly(
                    JOINT_ROW_ID to WalletAccountDTO.Type.JOINT,
                    mainAccount.accountId.value to WalletAccountDTO.Type.CRYPTO,
                ).inOrder()
            }

        @Test
        fun `GIVEN accounts API v1 WHEN saveAccounts THEN a stored joint row is not pushed`() = runTest {
            // Arrange
            every { walletAccountsApi.isV2Enabled } returns false
            every { jointAccountFeatureToggles.isJointAccountEnabled } returns false
            every { collectiblesFeatureToggles.isCollectiblesEnabled } returns false
            val accountList = AccountList.empty(userWalletId = userWalletId)
            val expectedBody = SaveWalletAccountsResponseConverter.convert(value = accountList)
            accountsResponseStoreFlow.value = storedResponse(
                jointRow(),
                mainRow(Personal.createMainAccount(userWalletId = userWalletId)),
            )
            val bodySlot = slot<SaveWalletAccountsResponse>()
            coEvery {
                walletAccountsSaver.push(userWalletId = userWalletId, body = capture(bodySlot))
            } returns createGetWalletAccountsResponse(userWalletId)

            // Act
            repository.saveAccounts(accountList)

            // Assert
            Truth.assertThat(bodySlot.captured).isEqualTo(expectedBody)
        }

        @Test
        fun `GIVEN collectibles disabled WHEN saveAccounts THEN the stored row keeps its place among reordered ones`() =
            runTest {
                // Arrange
                every { collectiblesFeatureToggles.isCollectiblesEnabled } returns false
                val mainAccount = Personal.createMainAccount(userWalletId = userWalletId)
                val accountList = AccountList(
                    userWalletId = userWalletId,
                    accounts = listOf(jointAccount(), mainAccount),
                    totalAccounts = 1,
                    totalArchivedAccounts = 0,
                    totalJointAccounts = 1,
                ).getOrNull()!!
                accountsResponseStoreFlow.value = storedResponse(mainRow(mainAccount), collectiblesRecord(), jointRow())
                val bodySlot = slot<SaveWalletAccountsResponse>()
                coEvery {
                    walletAccountsSaver.push(userWalletId = userWalletId, body = capture(bodySlot))
                } returns createGetWalletAccountsResponse(userWalletId)

                // Act
                repository.saveAccounts(accountList)

                // Assert
                Truth.assertThat(bodySlot.captured.accounts.map { it.type }).containsExactly(
                    WalletAccountDTO.Type.JOINT,
                    WalletAccountDTO.Type.COLLECTIBLES,
                    WalletAccountDTO.Type.CRYPTO,
                ).inOrder()
            }

        @Test
        fun `saveAccounts if API request is failed`() = runTest {
            // Arrange
            val accountList = AccountList.empty(userWalletId = userWalletId)
            val expectedBody = SaveWalletAccountsResponseConverter.convert(value = accountList)

            val exception = Exception("Test error")

            coEvery { walletAccountsSaver.push(userWalletId = userWalletId, body = expectedBody) } throws exception

            // Act
            val actual = runCatching { repository.saveAccounts(accountList) }.exceptionOrNull()!!

            // Assert
            Truth.assertThat(actual).isInstanceOf(exception::class.java)
            Truth.assertThat(actual).hasMessageThat().isEqualTo(exception.message)

            coVerify(exactly = 0) { walletAccountsSaver.store(userWalletId = any(), response = any()) }
        }

        private fun jointAccount(): Account.Joint {
            return Account.Joint(
                accountId = AccountId.forJointAccount(userWalletId = userWalletId, value = JOINT_ROW_ID)
                    .getOrNull()!!,
                accountName = AccountName.Custom(value = "Family").getOrNull()!!,
                icon = CryptoPortfolioIcon.ofCustomAccount(
                    value = CryptoPortfolioIcon.Icon.Star,
                    color = CryptoPortfolioIcon.Color.Azure,
                ),
                ownerKeyIndex = OwnerKeyIndex(value = 1).getOrNull()!!,
                cryptoCurrencies = emptyList(),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetTotalActiveAccountsCountSync {

        @Test
        fun `getTotalActiveAccountsCountSync returns None if account list response is null`() = runTest {
            // Arrange
            accountsResponseStoreFlow.value = null

            // Act
            val actual = repository.getTotalActiveAccountsCountSync(userWalletId)

            // Assert
            Truth.assertThat(actual).isEqualTo(None)

            verifyOrder {
                accountsResponseStoreFactory.create(userWalletId)
                accountsResponseStore.data
            }
        }

        @Test
        fun `getTotalActiveAccountsCountSync returns Some with totalAccounts when response is valid`() = runTest {
            // Arrange
            val totalAccounts = 5
            val response = createGetWalletAccountsResponse(userWalletId).let {
                it.copy(
                    wallet = it.wallet.copy(totalAccounts = totalAccounts),
                )
            }

            accountsResponseStoreFlow.value = response

            // Act
            val actual = repository.getTotalActiveAccountsCountSync(userWalletId)

            // Assert
            val expected = 1.toOption()
            Truth.assertThat(actual).isEqualTo(expected)

            verifyOrder {
                accountsResponseStoreFactory.create(userWalletId)
                accountsResponseStore.data
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetTotalActiveAccountsCount {

        @Test
        fun `getTotalActiveAccountsCount emits 0 when account list response is null`() = runTest {
            // Arrange
            accountsResponseStoreFlow.value = null

            // Act
            val flow = repository.getTotalActiveAccountsCount(userWalletId)
            val actual = getEmittedValues(flow)

            // Assert
            Truth.assertThat(actual).containsExactly(None)

            verifyOrder {
                accountsResponseStoreFactory.create(userWalletId)
                accountsResponseStore.data
            }
        }

        @Test
        fun `getTotalActiveAccountsCount emits correct value when response is valid`() = runTest {
            // Arrange
            val totalAccounts = 7
            val response = createGetWalletAccountsResponse(userWalletId).let {
                it.copy(
                    wallet = it.wallet.copy(totalAccounts = totalAccounts),
                )
            }

            accountsResponseStoreFlow.value = response

            // Act
            val flow = repository.getTotalActiveAccountsCount(userWalletId)
            val actual = getEmittedValues(flow)

            // Assert
            Truth.assertThat(actual).containsExactly(1.toOption())
            verifyOrder {
                accountsResponseStoreFactory.create(userWalletId)
                accountsResponseStore.data
            }
        }
    }

    private fun collectiblesRecord(): WalletAccountDTO {
        return WalletAccountDTO(
            id = AccountId.forCollectiblesAccount(userWalletId = userWalletId).value,
            name = "Collectibles",
            derivationIndex = DerivationIndex.Collectibles.value,
            icon = CryptoPortfolioIcon.Icon.Package.name,
            iconColor = CryptoPortfolioIcon.Color.Azure.name,
            type = WalletAccountDTO.Type.COLLECTIBLES,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SaveAccountsLocally {

        private val responseConverter: GetWalletAccountsResponseConverter = mockk()

        @BeforeEach
        fun setupConverter() {
            every { convertersContainer.createWalletAccountsResponseConverter(userWalletId) } returns responseConverter
        }

        @Test
        fun `GIVEN joint accounts disabled WHEN saveAccountsLocally THEN the joint row and its counter are kept`() =
            runTest {
                // Arrange
                every { jointAccountFeatureToggles.isJointAccountEnabled } returns false
                val mainAccount = Personal.createMainAccount(userWalletId = userWalletId)
                val accountList = AccountList.empty(userWalletId = userWalletId)
                val stored = storedResponse(mainRow(mainAccount), jointRow(), totalJointAccounts = 1)
                val converted = storedResponse(mainRow(mainAccount))
                accountsResponseStoreFlow.value = stored
                every { responseConverter.convert(accountList) } returns converted

                // Act
                repository.saveAccountsLocally(accountList)

                // Assert
                coVerify(exactly = 1) {
                    walletAccountsSaver.store(
                        userWalletId = userWalletId,
                        response = converted.copy(
                            wallet = converted.wallet.copy(totalJointAccounts = 1),
                            accounts = stored.accounts,
                        ),
                    )
                }
            }

        @Test
        fun `GIVEN all features enabled WHEN saveAccountsLocally THEN the list is stored as converted`() = runTest {
            // Arrange
            val mainAccount = Personal.createMainAccount(userWalletId = userWalletId)
            val accountList = AccountList.empty(userWalletId = userWalletId)
            val converted = storedResponse(mainRow(mainAccount))
            accountsResponseStoreFlow.value = storedResponse(mainRow(mainAccount), jointRow(), totalJointAccounts = 1)
            every { responseConverter.convert(accountList) } returns converted

            // Act
            repository.saveAccountsLocally(accountList)

            // Assert
            coVerify(exactly = 1) { walletAccountsSaver.store(userWalletId = userWalletId, response = converted) }
        }
    }

    private fun storedResponse(
        vararg rows: WalletAccountDTO,
        totalJointAccounts: Int? = null,
    ): GetWalletAccountsResponse = GetWalletAccountsResponse(
        wallet = GetWalletAccountsResponse.Wallet(
            group = null,
            sort = null,
            totalAccounts = 1,
            totalArchivedAccounts = 0,
            totalJointAccounts = totalJointAccounts,
        ),
        accounts = rows.toList(),
        unassignedTokens = emptyList(),
    )

    private fun mainRow(mainAccount: Personal): WalletAccountDTO = WalletAccountDTO(
        id = mainAccount.accountId.value,
        name = null,
        derivationIndex = 0,
        icon = CryptoPortfolioIcon.Icon.Wallet.name,
        iconColor = CryptoPortfolioIcon.Color.Azure.name,
        type = WalletAccountDTO.Type.CRYPTO,
    )

    private fun jointRow(): WalletAccountDTO = WalletAccountDTO(
        id = JOINT_ROW_ID,
        name = "Family",
        derivationIndex = 1,
        icon = CryptoPortfolioIcon.Icon.Star.name,
        iconColor = CryptoPortfolioIcon.Color.Azure.name,
        type = WalletAccountDTO.Type.JOINT,
        tokens = emptyList(),
    )

    private companion object {
        const val JOINT_ROW_ID = "4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6"
    }
}