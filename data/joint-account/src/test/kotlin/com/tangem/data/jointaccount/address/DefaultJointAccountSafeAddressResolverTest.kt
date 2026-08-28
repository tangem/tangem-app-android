package com.tangem.data.jointaccount.address

import com.google.common.truth.Truth.assertThat
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.jointaccount.store.JointAccountsStore
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultJointAccountSafeAddressResolverTest {

    private val walletAccountsFetcher: WalletAccountsFetcher = mockk()
    private val jointAccountsStore: JointAccountsStore = mockk()

    private val resolver = DefaultJointAccountSafeAddressResolver(
        walletAccountsFetcher = walletAccountsFetcher,
        jointAccountsStore = jointAccountsStore,
    )

    private val userWalletId = UserWalletId("011")
    private val personalNetwork = network(Network.DerivationPath.Card("m/44'/60'/0'/0/0"))
    private val jointNetwork0 = network(Network.DerivationPath.Joint("m/44'/60'/888888'/0/0"))
    private val jointNetwork1 = network(Network.DerivationPath.Joint("m/44'/60'/888888'/0/1"))

    @BeforeEach
    fun resetMocks() {
        clearMocks(walletAccountsFetcher, jointAccountsStore)
    }

    @Test
    fun `GIVEN an active and a pending joint account WHEN resolve THEN only the active network gets an address`() =
        runTest {
            // Arrange
            coEvery { walletAccountsFetcher.getSaved(userWalletId) } returns accountsResponse(
                jointAccount(id = "A0", ownerKeyIndex = 0),
                jointAccount(id = "A1", ownerKeyIndex = 1),
                WalletAccountDTO(id = "P", name = "Main", derivationIndex = 0, icon = "Wallet", iconColor = "Azure"),
            )
            coEvery { jointAccountsStore.getSyncOrNull(userWalletId) } returns listOf(
                jointAccount(cryptoAccountId = "A0", safeAddress = "0x5afe0", status = JointAccount.Status.ACTIVE),
                jointAccount(cryptoAccountId = "A1", safeAddress = null, status = JointAccount.Status.PENDING),
            )

            // Act
            val actual = resolver.resolve(userWalletId, setOf(personalNetwork, jointNetwork0, jointNetwork1))

            // Assert
            assertThat(actual).containsExactly(jointNetwork0, "0x5afe0")
        }

    @Test
    fun `GIVEN no joint network WHEN resolve THEN nothing is read`() = runTest {
        // Act
        val actual = resolver.resolve(userWalletId, setOf(personalNetwork))

        // Assert
        assertThat(actual).isEmpty()
        verify {
            walletAccountsFetcher wasNot Called
            jointAccountsStore wasNot Called
        }
    }

    @Test
    fun `GIVEN the joint accounts store is empty WHEN resolve THEN the accounts data is not read`() = runTest {
        // Arrange
        coEvery { jointAccountsStore.getSyncOrNull(userWalletId) } returns null

        // Act
        val actual = resolver.resolve(userWalletId, setOf(jointNetwork0))

        // Assert
        assertThat(actual).isEmpty()
        verify { walletAccountsFetcher wasNot Called }
    }

    @Test
    fun `GIVEN a Safe known but no matching account in the accounts data WHEN resolve THEN no address`() = runTest {
        // Arrange
        coEvery { walletAccountsFetcher.getSaved(userWalletId) } returns accountsResponse(
            jointAccount(id = "OTHER", ownerKeyIndex = 5),
        )
        coEvery { jointAccountsStore.getSyncOrNull(userWalletId) } returns listOf(
            jointAccount(cryptoAccountId = "A0", safeAddress = "0x5afe0", status = JointAccount.Status.ACTIVE),
        )

        // Act
        val actual = resolver.resolve(userWalletId, setOf(jointNetwork0))

        // Assert
        assertThat(actual).isEmpty()
    }

    private fun network(derivationPath: Network.DerivationPath): Network {
        return Network(
            id = Network.ID(rawId = Network.RawID("ethereum"), derivationPath = derivationPath),
            name = "Ethereum",
            currencySymbol = "ETH",
            derivationPath = derivationPath,
            isTestnet = false,
            standardType = Network.StandardType.ERC20,
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
    }

    private fun jointAccount(id: String, ownerKeyIndex: Int): WalletAccountDTO {
        return WalletAccountDTO(
            id = id,
            name = "Family",
            derivationIndex = ownerKeyIndex,
            icon = "Family",
            iconColor = "Azure",
            type = WalletAccountDTO.Type.JOINT,
            tokens = emptyList<UserTokensResponse.Token>(),
        )
    }

    private fun jointAccount(cryptoAccountId: String, safeAddress: String?, status: JointAccount.Status): JointAccount {
        return JointAccount(
            cryptoAccountId = cryptoAccountId,
            membersCount = 2,
            threshold = 2,
            safeAddress = safeAddress,
            status = status,
            members = emptyList(),
            source = StatusSource.ACTUAL,
        )
    }

    private fun accountsResponse(vararg accounts: WalletAccountDTO): GetWalletAccountsResponse {
        return GetWalletAccountsResponse(
            wallet = GetWalletAccountsResponse.Wallet(
                version = 0,
                group = UserTokensResponse.GroupType.NONE,
                sort = UserTokensResponse.SortType.MANUAL,
                totalAccounts = accounts.size,
                totalArchivedAccounts = 0,
            ),
            accounts = accounts.toList(),
            unassignedTokens = emptyList(),
        )
    }
}