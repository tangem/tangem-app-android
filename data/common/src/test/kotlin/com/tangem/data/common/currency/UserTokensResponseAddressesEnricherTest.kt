package com.tangem.data.common.currency

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.jointaccount.address.JointAccountSafeAddressResolver
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserTokensResponseAddressesEnricherTest {

    private val walletsRepository: WalletsRepository = mockk()
    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val safeAddressResolver: JointAccountSafeAddressResolver = mockk()
    private val enricher: UserTokensResponseAddressesEnricher = UserTokensResponseAddressesEnricher(
        walletsRepository = walletsRepository,
        walletManagersFacade = walletManagersFacade,
        dispatchers = TestingCoroutineDispatcherProvider(),
        jointAccountSafeAddressResolver = { safeAddressResolver },
    )

    private val userWalletId = UserWalletId("1234567890abcdef")

    @AfterEach
    fun tearDown() {
        clearMocks(walletsRepository, walletManagersFacade, safeAddressResolver)
    }

    @Test
    fun `GIVEN notifications are disabled for wallet WHEN invoke THEN return response with empty addresses`() =
        runTest {
            // GIVEN
            val token = createToken()
            val response = createUserTokensResponse(tokens = listOf(token))
            val walletManager = mockk<WalletManager> {
                val wallet = mockk<Wallet> {
                    every { addresses } returns setOf(
                        Address(value = "0x12345", type = AddressType.Default),
                    )
                }

                every { this@mockk.wallet } returns wallet
            }

            coEvery { walletsRepository.isNotificationsEnabled(userWalletId) } returns false

            coEvery {
                walletManagersFacade.getOrCreateWalletManager(
                    userWalletId = userWalletId,
                    blockchain = Blockchain.Ethereum,
                    derivationPath = token.derivationPath,
                )
            } returns walletManager

            // WHEN
            val result = enricher(userWalletId, response)

            // THEN
            assertThat(result.tokens).hasSize(1)
            assertThat(result.tokens[0].addresses).isEmpty()
        }

    @Test
    fun `GIVEN notifications are enabled and addresses available WHEN invoke THEN return enriched response`() =
        runTest {
            // GIVEN
            val token = createToken()
            val response = createUserTokensResponse(tokens = listOf(token))
            val addresses = setOf(
                Address(value = "0x123", type = AddressType.Default),
                Address(value = "0x456", type = AddressType.Legacy),
            )

            val walletManager = mockk<WalletManager> {
                val wallet = mockk<Wallet> {
                    every { this@mockk.addresses } returns addresses
                }

                every { this@mockk.wallet } returns wallet
            }

            coEvery { walletsRepository.isNotificationsEnabled(userWalletId) } returns true
            coEvery {
                walletManagersFacade.getOrCreateWalletManager(
                    userWalletId = userWalletId,
                    blockchain = Blockchain.Ethereum,
                    derivationPath = token.derivationPath,
                )
            } returns walletManager

            // WHEN
            val result = enricher(userWalletId, response)

            // THEN
            assertThat(result.tokens).hasSize(1)
            assertThat(result.tokens[0].addresses).containsExactlyElementsIn(addresses.map { it.value })
        }

    @Test
    fun `GIVEN notifications are enabled but no matching network WHEN invoke THEN return original token`() = runTest {
        // GIVEN
        val token = createToken()
        val response = createUserTokensResponse(tokens = listOf(token))

        coEvery { walletsRepository.isNotificationsEnabled(userWalletId) } returns true
        coEvery {
            walletManagersFacade.getOrCreateWalletManager(
                userWalletId = userWalletId,
                blockchain = Blockchain.Ethereum,
                derivationPath = token.derivationPath,
            )
        } returns null

        // WHEN
        val result = enricher(userWalletId, response)

        // THEN
        assertThat(result.tokens).hasSize(1)
        assertThat(result.tokens[0]).isEqualTo(token)
    }

    @Test
    fun `GIVEN a joint token WHEN invoke THEN it is registered on the Safe address, not on the owner key`() =
        runTest {
            // GIVEN
            val jointToken = createToken(derivationPath = JOINT_OWNER_PATH)
            val response = createUserTokensResponse(tokens = listOf(jointToken))

            coEvery { walletsRepository.isNotificationsEnabled(userWalletId) } returns true
            coEvery { safeAddressResolver.resolveByOwnerPath(userWalletId, setOf(JOINT_OWNER_PATH)) } returns mapOf(
                JOINT_OWNER_PATH to SAFE_ADDRESS,
            )

            // WHEN
            val result = enricher(userWalletId, response)

            // THEN
            assertThat(result.tokens.single().addresses).containsExactly(SAFE_ADDRESS)
            coVerify { walletManagersFacade wasNot Called }
        }

    @Test
    fun `GIVEN a joint token whose Safe address is unknown WHEN invoke THEN the token is left alone`() = runTest {
        // GIVEN
        val jointToken = createToken(derivationPath = JOINT_OWNER_PATH)
        val response = createUserTokensResponse(tokens = listOf(jointToken))

        coEvery { walletsRepository.isNotificationsEnabled(userWalletId) } returns true
        coEvery { safeAddressResolver.resolveByOwnerPath(userWalletId, setOf(JOINT_OWNER_PATH)) } returns emptyMap()

        // WHEN
        val result = enricher(userWalletId, response)

        // THEN
        assertThat(result.tokens.single()).isEqualTo(jointToken)
    }

    /** The resolver waits for the accounts data, so a wallet without joint tokens must not pay for that wait. */
    @Test
    fun `GIVEN no joint token WHEN invoke THEN the Safe address resolver is not asked at all`() = runTest {
        // GIVEN
        val response = createUserTokensResponse(tokens = listOf(createToken()))

        coEvery { walletsRepository.isNotificationsEnabled(userWalletId) } returns true
        coEvery {
            walletManagersFacade.getOrCreateWalletManager(userWalletId, Blockchain.Ethereum, any())
        } returns null

        // WHEN
        enricher(userWalletId, response)

        // THEN
        coVerify { safeAddressResolver wasNot Called }
    }

    /** With notifications off every address is dropped anyway, so the wait for the accounts data is pure cost. */
    @Test
    fun `GIVEN notifications disabled WHEN invoke THEN the Safe address resolver is not asked at all`() = runTest {
        // GIVEN
        val response = createUserTokensResponse(tokens = listOf(createToken(derivationPath = JOINT_OWNER_PATH)))

        coEvery { walletsRepository.isNotificationsEnabled(userWalletId) } returns false

        // WHEN
        val result = enricher(userWalletId, response)

        // THEN
        assertThat(result.tokens.single().addresses).isEmpty()
        coVerify { safeAddressResolver wasNot Called }
    }

    private fun createToken(
        networkId: String = "ethereum",
        derivationPath: String = "m/44'/60'/0'/0/0",
        list: List<String> = emptyList(),
        name: String = "Ethereum",
        symbol: String = "ETH",
        decimals: Int = 18,
        contractAddress: String? = null,
        id: String? = null,
    ) = UserTokensResponse.Token(
        id = id,
        networkId = networkId,
        derivationPath = derivationPath,
        name = name,
        symbol = symbol,
        decimals = decimals,
        contractAddress = contractAddress,
        addresses = list,
    )

    private fun createUserTokensResponse(
        tokens: List<UserTokensResponse.Token> = emptyList(),
        version: Int = 0,
        group: UserTokensResponse.GroupType = UserTokensResponse.GroupType.NETWORK,
        sort: UserTokensResponse.SortType = UserTokensResponse.SortType.BALANCE,
        notifyStatus: Boolean? = null,
    ) = UserTokensResponse(
        version = version,
        group = group,
        sort = sort,
        notifyStatus = notifyStatus,
        tokens = tokens,
    )

    private companion object {
        const val JOINT_OWNER_PATH = "m/44'/60'/888888'/0/3"
        const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"
    }
}