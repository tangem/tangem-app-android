package com.tangem.data.account.token

import com.tangem.data.account.converter.createGetWalletAccountsResponse
import com.tangem.data.account.converter.createWalletAccountDTO
import com.tangem.data.account.store.AccountsResponseStore
import com.tangem.data.account.store.AccountsResponseStoreFactory
import com.tangem.data.account.tokens.DefaultMainAccountTokensMigration
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.toUserTokensResponse
import com.tangem.datasource.local.accounts.AccountTokenMigrationStore
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.assertEitherLeft
import com.tangem.test.core.assertEitherRight
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@Suppress("UnusedFlow")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultMainAccountTokensMigrationTest {

    private val accountsResponseStoreFactory = mockk<AccountsResponseStoreFactory>()
    private val accountsResponseStore = mockk<AccountsResponseStore>()
    private val accountsResponseStoreFlow = MutableStateFlow<GetWalletAccountsResponse?>(value = null)
    private val accountTokenMigrationStore = mockk<AccountTokenMigrationStore>(relaxUnitFun = true)
    private val userTokensSaver = mockk<UserTokensSaver>(relaxUnitFun = true)

    private val migration = DefaultMainAccountTokensMigration(
        accountsResponseStoreFactory = accountsResponseStoreFactory,
        accountTokenMigrationStore = accountTokenMigrationStore,
        userTokensSaver = userTokensSaver,
    )

    private val userWalletId = UserWalletId("011")
    private val derivationIndex = DerivationIndex(1).getOrNull()!!

    @BeforeEach
    fun setupAll() {
        every { accountsResponseStoreFactory.create(userWalletId) } returns accountsResponseStore
        every { accountsResponseStore.data } returns accountsResponseStoreFlow
    }

    @AfterEach
    fun tearDown() {
        clearMocks(accountsResponseStoreFactory, accountsResponseStore, userTokensSaver)
        accountsResponseStoreFlow.value = null
    }

    @Test
    fun `migrate skips when derivation index is Main`() = runTest {
        // Act
        val actual = migration.migrate(userWalletId, DerivationIndex.Main)

        // Assert
        assertEitherRight(actual)

        coVerify(inverse = true) {
            accountsResponseStoreFactory.create(any())
            accountsResponseStore.data
            userTokensSaver.pushWithRetryer(userWalletId = any(), response = any(), onFailSend = any())
        }
    }

    @Test
    fun `migrate fails when no cached accounts response`() = runTest {
        // Act
        val actual = migration.migrate(userWalletId, derivationIndex)

        // Assert
        val expected = IllegalStateException("No cached accounts response found")
        assertEitherLeft(actual, expected)

        coVerifySequence {
            accountsResponseStoreFactory.create(userWalletId)
            accountsResponseStore.data
        }

        coVerify(inverse = true) {
            userTokensSaver.pushWithRetryer(userWalletId = any(), response = any(), onFailSend = any())
        }
    }

    @Test
    fun `migrate fails when selected account DTO not found`() = runTest {
        // Arrange
        val response = createGetWalletAccountsResponse(
            userWalletId = userWalletId,
            tokens = listOf(
                createBitcoin(accountIndex = DerivationIndex.Main.value),
            ),
        )

        accountsResponseStoreFlow.value = response

        // Act
        val actual = migration.migrate(userWalletId, derivationIndex)

        // Assert
        val expected = IllegalStateException("No account found with derivation index: $derivationIndex")
        assertEitherLeft(actual, expected)

        coVerifySequence {
            accountsResponseStoreFactory.create(userWalletId)
            accountsResponseStore.data
        }

        coVerify(inverse = true) {
            userTokensSaver.pushWithRetryer(userWalletId = any(), response = any(), onFailSend = any())
        }
    }

    @Test
    fun `migrate skips when no unassigned tokens`() = runTest {
        // Arrange
        val response = createGetWalletAccountsResponse(
            userWalletId = userWalletId,
            tokens = listOf(
                createBitcoin(accountIndex = DerivationIndex.Main.value),
            ),
        )

        val selectedAccount = createWalletAccountDTO(
            userWalletId = userWalletId,
            accountId = AccountId.forCryptoPortfolio(userWalletId, derivationIndex).value,
            derivationIndex = derivationIndex.value,
            tokens = emptyList(),
        )

        accountsResponseStoreFlow.value = response.copy(accounts = response.accounts + selectedAccount)

        // Act
        val actual = migration.migrate(userWalletId, derivationIndex)

        // Assert
        assertEitherRight(actual)

        coVerifySequence {
            accountsResponseStoreFactory.create(userWalletId)
            accountsResponseStore.data
        }

        coVerify(inverse = true) {
            userTokensSaver.pushWithRetryer(userWalletId = any(), response = any(), onFailSend = any())
        }
    }

    @Test
    fun `migrate updates tokens for selected account`() = runTest {
        // Arrange
        val unassignedToken = createBitcoin(accountIndex = 1)

        val mainAccount = createWalletAccountDTO(
            userWalletId = userWalletId,
            accountId = AccountId.forCryptoPortfolio(userWalletId, DerivationIndex.Main).value,
            derivationIndex = DerivationIndex.Main.value,
            tokens = listOf(
                createBitcoin(accountIndex = 0),
                unassignedToken,
            ),
        )

        val selectedAccount = createWalletAccountDTO(
            userWalletId = userWalletId,
            accountId = AccountId.forCryptoPortfolio(userWalletId, derivationIndex).value,
            derivationIndex = derivationIndex.value,
            tokens = emptyList(),
        )

        val response = GetWalletAccountsResponse(
            wallet = GetWalletAccountsResponse.Wallet(
                group = UserTokensResponse.GroupType.NONE,
                sort = UserTokensResponse.SortType.MANUAL,
                totalAccounts = 2,
            ),
            accounts = listOf(mainAccount, selectedAccount),
            unassignedTokens = emptyList(),
        )

        accountsResponseStoreFlow.value = response

        coEvery { accountsResponseStore.updateData(any()) } returns mockk()

        // Act
        val actual = migration.migrate(userWalletId, derivationIndex)

        // Assert
        assertEitherRight(actual)

        val migratedResponse = response.copy(
            accounts = listOf(
                mainAccount.copy(tokens = mainAccount.tokens!! - unassignedToken),
                selectedAccount.copy(tokens = listOf(unassignedToken)),
            ),
        )

        coVerifySequence {
            accountsResponseStoreFactory.create(userWalletId)
            accountsResponseStore.data
            accountsResponseStore.updateData(any())
            userTokensSaver.pushWithRetryer(
                userWalletId = userWalletId,
                response = migratedResponse.toUserTokensResponse(),
                onFailSend = any(),
            )
        }
    }

    private fun createBitcoin(accountIndex: Int): UserTokensResponse.Token {
        return UserTokensResponse.Token(
            id = "ne",
            accountId = AccountId.forCryptoPortfolio(
                userWalletId = userWalletId,
                derivationIndex = DerivationIndex(accountIndex).getOrNull()!!,
            ).value,
            networkId = "bitcoin",
            derivationPath = "m/44'/60'/$accountIndex'/0/0",
            name = "Phil Hinton",
            symbol = "graeci",
            decimals = 6487,
            contractAddress = "vim",
            addresses = listOf(),
        )
    }
}