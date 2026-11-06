package com.tangem.data.collectibles.account

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.collectibles.account.converter.CollectiblesAccountStatusValueDTOConverter
import com.tangem.data.collectibles.account.store.CollectiblesAccountStore
import com.tangem.data.collectibles.account.store.WalletIdWithCollectiblesAccountDTO
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.common.account.WalletAccountsSaver
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.account.CollectiblesVaultCurrencyFactory
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.mock.collectibles.MockCollectibles
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

internal class DefaultCollectiblesAccountRepositoryTest {

    private val walletAccountsFetcher: WalletAccountsFetcher = mockk()
    private val walletAccountsSaver: WalletAccountsSaver = mockk(relaxUnitFun = true)
    private val timeSource = TestTimeSource()

    @BeforeEach
    fun resetMocks() {
        clearMocks(walletAccountsFetcher, walletAccountsSaver)
    }

    @Test
    fun `GIVEN no collectibles record WHEN registerAccount THEN it is pushed with the others and stored`() =
        runTest {
            // Arrange
            val synced = accountsResponse(PORTFOLIO_RECORD, COLLECTIBLES_RECORD)
            coEvery { walletAccountsFetcher.fetch(WALLET) } returns accountsResponse(PORTFOLIO_RECORD)
            coEvery { walletAccountsSaver.push(userWalletId = WALLET, accounts = any()) } returns synced
            val repository = createRepository(testScope = this)

            // Act
            val actual = repository.registerAccount(WALLET)

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            coVerify(exactly = 1) {
                walletAccountsSaver.push(userWalletId = WALLET, accounts = listOf(PORTFOLIO_RECORD, COLLECTIBLES_RECORD))
                walletAccountsSaver.store(userWalletId = WALLET, response = synced)
            }
        }

    @Test
    fun `GIVEN the record is listed already WHEN registerAccount THEN nothing is pushed`() = runTest {
        // Arrange
        coEvery { walletAccountsFetcher.fetch(WALLET) } returns accountsResponse(PORTFOLIO_RECORD, COLLECTIBLES_RECORD)
        val repository = createRepository(testScope = this)

        // Act
        val actual = repository.registerAccount(WALLET)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 0) { walletAccountsSaver.push(userWalletId = any(), accounts = any()) }
    }

    @Test
    fun `GIVEN the backend refuses the record WHEN registerAccount THEN the failure is returned`() = runTest {
        // Arrange
        val cause = IllegalStateException("PUT /accounts failed")
        coEvery { walletAccountsFetcher.fetch(WALLET) } returns accountsResponse(PORTFOLIO_RECORD)
        coEvery { walletAccountsSaver.push(userWalletId = WALLET, accounts = any()) } throws cause
        val repository = createRepository(testScope = this)

        // Act
        val actual = repository.registerAccount(WALLET)

        // Assert
        assertThat(actual).isEqualTo(CollectiblesAccountError.RegistrationFailed(cause).left())
        coVerify(exactly = 0) { walletAccountsSaver.store(userWalletId = any(), response = any()) }
    }

    @Test
    fun `GIVEN the push returns nothing WHEN registerAccount THEN it fails and stores nothing`() = runTest {
        // Arrange
        coEvery { walletAccountsFetcher.fetch(WALLET) } returns accountsResponse(PORTFOLIO_RECORD)
        coEvery { walletAccountsSaver.push(userWalletId = WALLET, accounts = any()) } returns null
        val repository = createRepository(testScope = this)

        // Act
        val actual = repository.registerAccount(WALLET)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(CollectiblesAccountError.RegistrationFailed::class.java)
        coVerify(exactly = 0) { walletAccountsSaver.store(userWalletId = any(), response = any()) }
    }

    @Test
    fun `GIVEN an account status WHEN saveAccount THEN it is recorded locally only`() = runTest {
        // Arrange
        val repository = createRepository(testScope = this)

        // Act
        repository.saveAccount(userWalletId = WALLET, status = ONBOARDED)

        // Assert
        assertThat(repository.getStatusSync(WALLET)).isEqualTo(ONBOARDED)
        coVerify(exactly = 0) { walletAccountsSaver.push(userWalletId = any(), accounts = any()) }
    }

    @Test
    fun `GIVEN a collectibles record WHEN unregisterAccount THEN the others are pushed without it and stored`() =
        runTest {
            // Arrange
            val synced = accountsResponse(PORTFOLIO_RECORD)
            coEvery { walletAccountsFetcher.fetch(WALLET) } returns
                accountsResponse(PORTFOLIO_RECORD, COLLECTIBLES_RECORD)
            coEvery { walletAccountsSaver.push(userWalletId = WALLET, accounts = any()) } returns synced
            val repository = createRepository(testScope = this)

            // Act
            repository.unregisterAccount(WALLET)

            // Assert
            coVerify(exactly = 1) {
                walletAccountsSaver.push(userWalletId = WALLET, accounts = listOf(PORTFOLIO_RECORD))
                walletAccountsSaver.store(userWalletId = WALLET, response = synced)
            }
        }

    @Test
    fun `GIVEN no collectibles record WHEN unregisterAccount THEN nothing is pushed`() = runTest {
        // Arrange
        coEvery { walletAccountsFetcher.fetch(WALLET) } returns accountsResponse(PORTFOLIO_RECORD)
        val repository = createRepository(testScope = this)

        // Act
        repository.unregisterAccount(WALLET)

        // Assert
        coVerify(exactly = 0) { walletAccountsSaver.push(userWalletId = any(), accounts = any()) }
    }

    @Test
    fun `GIVEN the backend fails WHEN unregisterAccount THEN it does not throw and stores nothing`() = runTest {
        // Arrange
        coEvery { walletAccountsFetcher.fetch(WALLET) } throws IllegalStateException("GET /accounts failed")
        val repository = createRepository(testScope = this)

        // Act
        val actual = runCatching { repository.unregisterAccount(WALLET) }.exceptionOrNull()

        // Assert
        assertThat(actual).isNull()
        coVerify(exactly = 0) { walletAccountsSaver.store(userWalletId = any(), response = any()) }
    }

    @Test
    fun `GIVEN a relink just sent WHEN isRelinkPending THEN it is pending for this wallet only`() = runTest {
        // Arrange
        val repository = createRepository(testScope = this)

        // Act
        repository.markRelinkSent(WALLET)

        // Assert
        assertThat(repository.isRelinkPending(WALLET)).isTrue()
        assertThat(repository.isRelinkPending(OTHER_WALLET)).isFalse()
    }

    @Test
    fun `GIVEN a relink sent two minutes ago WHEN isRelinkPending THEN it can no longer land`() = runTest {
        // Arrange
        val repository = createRepository(testScope = this)
        repository.markRelinkSent(WALLET)

        // Act
        timeSource += 120.seconds

        // Assert
        assertThat(repository.isRelinkPending(WALLET)).isFalse()
    }

    @Test
    fun `GIVEN a relink sent WHEN clearAccount THEN it is no longer pending`() = runTest {
        // Arrange
        val repository = createRepository(testScope = this)
        repository.markRelinkSent(WALLET)

        // Act
        repository.clearAccount(WALLET)

        // Assert
        assertThat(repository.isRelinkPending(WALLET)).isFalse()
    }

    private fun accountsResponse(vararg records: WalletAccountDTO): GetWalletAccountsResponse = mockk {
        every { accounts } returns records.toList()
    }

    private fun createRepository(testScope: TestScope): DefaultCollectiblesAccountRepository {
        val store = CollectiblesAccountStore(
            runtimeStore = RuntimeSharedStore(),
            persistenceDataStore = MockStateDataStore<WalletIdWithCollectiblesAccountDTO>(default = emptyMap()),
            converter = CollectiblesAccountStatusValueDTOConverter(
                currencyFactory = CollectiblesVaultCurrencyFactory(MockCollectibles::usdc),
            ),
            scope = TestAppCoroutineScope(testScope),
        )

        return DefaultCollectiblesAccountRepository(
            collectiblesAccountStore = store,
            walletAccountsFetcher = walletAccountsFetcher,
            walletAccountsSaver = walletAccountsSaver,
            timeSource = timeSource,
        )
    }

    private companion object {
        val WALLET = UserWalletId("011")
        val OTHER_WALLET = UserWalletId("022")
        val ONBOARDED = CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ACTUAL)

        val PORTFOLIO_RECORD = WalletAccountDTO(
            id = "portfolio",
            name = null,
            derivationIndex = 0,
            icon = "Letter",
            iconColor = "Azure",
        )

        val COLLECTIBLES_RECORD = WalletAccountDTO(
            id = AccountId.forCollectiblesAccount(userWalletId = WALLET).value,
            name = "Collectibles",
            derivationIndex = DerivationIndex.Collectibles.value,
            icon = "Package",
            iconColor = "Azure",
            type = WalletAccountDTO.Type.COLLECTIBLES,
        )
    }
}