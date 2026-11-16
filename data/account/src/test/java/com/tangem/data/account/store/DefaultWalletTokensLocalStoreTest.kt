package com.tangem.data.account.store

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.datastore.MockStateDataStore
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultWalletTokensLocalStoreTest {

    private val accountsResponseStoreFactory: AccountsResponseStoreFactory = mockk()

    private val store = DefaultWalletTokensLocalStore(accountsResponseStoreFactory = accountsResponseStoreFactory)

    private val savedToken = createToken(index = 0)
    private val peerToken = createToken(index = 1)

    @BeforeEach
    fun resetMocks() {
        clearMocks(accountsResponseStoreFactory)
    }

    @Test
    fun `GIVEN saved document WHEN addRemoteTokens THEN peer token is appended to its account`() = runTest {
        // Arrange
        val dataStore = stubSaved(createResponse(createAccount(MAIN_ACCOUNT_ID, listOf(savedToken))))

        // Act
        store.addRemoteTokens(userWalletId = userWalletId, tokens = listOf(peerToken))

        // Assert
        val expected = createResponse(createAccount(MAIN_ACCOUNT_ID, listOf(savedToken, peerToken)))
        assertThat(dataStore.data.first()).isEqualTo(expected)
    }

    @Test
    fun `GIVEN tokens of several accounts WHEN addRemoteTokens THEN each lands in its own account`() = runTest {
        // Arrange
        val secondAccountToken = createToken(index = 2, accountId = SECOND_ACCOUNT_ID)
        val dataStore = stubSaved(
            createResponse(
                createAccount(MAIN_ACCOUNT_ID, listOf(savedToken)),
                createAccount(SECOND_ACCOUNT_ID, emptyList()),
            ),
        )

        // Act
        store.addRemoteTokens(userWalletId = userWalletId, tokens = listOf(secondAccountToken, peerToken))

        // Assert
        val expected = createResponse(
            createAccount(MAIN_ACCOUNT_ID, listOf(savedToken, peerToken)),
            createAccount(SECOND_ACCOUNT_ID, listOf(secondAccountToken)),
        )
        assertThat(dataStore.data.first()).isEqualTo(expected)
    }

    @Test
    fun `GIVEN token already saved WHEN addRemoteTokens THEN document is unchanged`() = runTest {
        // Arrange
        val saved = createResponse(createAccount(MAIN_ACCOUNT_ID, listOf(savedToken, peerToken)))
        val dataStore = stubSaved(saved)

        // Act
        store.addRemoteTokens(userWalletId = userWalletId, tokens = listOf(peerToken))

        // Assert
        assertThat(dataStore.data.first()).isEqualTo(saved)
    }

    @Test
    fun `GIVEN token of an account missing locally WHEN addRemoteTokens THEN it fails and document is unchanged`() =
        runTest {
            // Arrange
            val saved = createResponse(createAccount(MAIN_ACCOUNT_ID, listOf(savedToken)))
            val dataStore = stubSaved(saved)
            val tokens = listOf(peerToken, createToken(index = 2, accountId = "unknown"))

            // Act
            val error = runCatching { store.addRemoteTokens(userWalletId = userWalletId, tokens = tokens) }
                .exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(IllegalStateException::class.java)
            assertThat(dataStore.data.first()).isEqualTo(saved)
        }

    @Test
    fun `GIVEN unassigned token WHEN addRemoteTokens THEN it is assigned to the main account`() = runTest {
        // Arrange
        val dataStore = stubSaved(createResponse(createAccount(MAIN_ACCOUNT_ID, listOf(savedToken))))
        val unassignedToken = createToken(index = 2, accountId = null)

        // Act
        store.addRemoteTokens(userWalletId = userWalletId, tokens = listOf(unassignedToken, peerToken))

        // Assert
        val expected = createResponse(
            createAccount(
                id = MAIN_ACCOUNT_ID,
                tokens = listOf(savedToken, peerToken, unassignedToken.copy(accountId = MAIN_ACCOUNT_ID)),
            ),
        )
        val actual = dataStore.data.first()
        assertThat(actual).isEqualTo(expected)
        assertThat(actual?.accounts?.single()?.tokens?.map { it.accountId })
            .containsExactly(MAIN_ACCOUNT_ID, MAIN_ACCOUNT_ID, MAIN_ACCOUNT_ID)
    }

    @Test
    fun `GIVEN unassigned token already saved WHEN addRemoteTokens THEN document is unchanged`() = runTest {
        // Arrange
        val saved = createResponse(createAccount(MAIN_ACCOUNT_ID, listOf(savedToken)))
        val dataStore = stubSaved(saved)

        // Act
        store.addRemoteTokens(userWalletId = userWalletId, tokens = listOf(savedToken.copy(accountId = null)))

        // Assert
        assertThat(dataStore.data.first()).isEqualTo(saved)
    }

    @Test
    fun `GIVEN no saved document WHEN addRemoteTokens THEN it fails and nothing is stored`() = runTest {
        // Arrange
        val dataStore = stubSaved(saved = null)

        // Act
        val error = runCatching { store.addRemoteTokens(userWalletId = userWalletId, tokens = listOf(peerToken)) }
            .exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
        assertThat(dataStore.data.first()).isNull()
    }

    private fun stubSaved(saved: GetWalletAccountsResponse?): MockStateDataStore<GetWalletAccountsResponse?> {
        val dataStore = MockStateDataStore(default = saved)
        every { accountsResponseStoreFactory.create(userWalletId) } returns dataStore
        return dataStore
    }

    private fun createResponse(vararg accounts: WalletAccountDTO) = GetWalletAccountsResponse(
        wallet = GetWalletAccountsResponse.Wallet(
            version = 1,
            group = UserTokensResponse.GroupType.NONE,
            sort = UserTokensResponse.SortType.MANUAL,
            totalAccounts = accounts.size,
            totalArchivedAccounts = 0,
        ),
        accounts = accounts.toList(),
        unassignedTokens = emptyList(),
    )

    private fun createAccount(id: String, tokens: List<UserTokensResponse.Token>) = WalletAccountDTO(
        id = id,
        name = "Account $id",
        derivationIndex = 0,
        icon = "icon",
        iconColor = "color",
        type = null,
        tokens = tokens,
        totalTokens = 1,
        totalNetworks = 1,
    )

    private fun createToken(index: Int, accountId: String? = MAIN_ACCOUNT_ID) = UserTokensResponse.Token(
        id = "token-$index",
        accountId = accountId,
        networkId = "ethereum",
        derivationPath = "m/44'/60'/0'/0/0",
        name = "Token $index",
        symbol = "T$index",
        contractAddress = "0x$index",
        decimals = 18,
    )

    private companion object {
        val userWalletId = UserWalletId("011")
        const val MAIN_ACCOUNT_ID = "main_account_id"
        const val SECOND_ACCOUNT_ID = "second_account_id"
    }
}