package com.tangem.data.common.currency

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.domain.notifications.toggles.NotificationsFeatureToggles
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import com.google.common.truth.Truth.assertThat
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import org.junit.Before
import org.junit.Test
import com.tangem.domain.models.StatusSource
import com.tangem.domain.tokens.model.NetworkAddress

class UserTokensResponseAddressesEnricherTest {

    private lateinit var notificationsFeatureToggles: NotificationsFeatureToggles
    private lateinit var walletsRepository: WalletsRepository
    private val dispatchers: CoroutineDispatcherProvider = TestingCoroutineDispatcherProvider()
    private lateinit var multiNetworkStatusProducerFactory: MultiNetworkStatusProducer.Factory
    private lateinit var multiNetworkStatusProducer: MultiNetworkStatusProducer
    private lateinit var enricher: UserTokensResponseAddressesEnricher

    @Before
    fun setup() {
        notificationsFeatureToggles = mockk()
        walletsRepository = mockk()
        multiNetworkStatusProducer = mockk()
        multiNetworkStatusProducerFactory = mockk()

        every { multiNetworkStatusProducerFactory.create(any()) } returns multiNetworkStatusProducer

        enricher = UserTokensResponseAddressesEnricher(
            notificationsFeatureToggles = notificationsFeatureToggles,
            walletsRepository = walletsRepository,
            dispatchers = dispatchers,
            multiNetworkStatusProducerFactory = multiNetworkStatusProducerFactory,
        )
    }

    @Test
    fun `GIVEN notifications are disabled globally WHEN invoke THEN return original response`() = runTest {
        // GIVEN
        val userWalletId = UserWalletId("1234567890abcdef")
        val token = createToken()
        val response = createUserTokensResponse(tokens = listOf(token))
        every { notificationsFeatureToggles.isNotificationsEnabled } returns false

        // WHEN
        val result = enricher(userWalletId, response)

        // THEN
        assertThat(result).isEqualTo(response)
    }

    @Test
    fun `GIVEN notifications are disabled for wallet WHEN invoke THEN return response with empty addresses`() =
        runTest {
            // GIVEN
            val userWalletId = UserWalletId("1234567890abcdef")
            val token = createToken()
            val response = createUserTokensResponse(tokens = listOf(token))
            every { notificationsFeatureToggles.isNotificationsEnabled } returns true
            coEvery { walletsRepository.isNotificationsEnabled(userWalletId, false) } returns false
            coEvery { multiNetworkStatusProducer.produce() } returns flowOf(
                setOf(
                    NetworkStatus(
                        network = mockk {
                            every { backendId } returns "ethereum"
                            every { derivationPath.value } returns "m/44'/60'/0'/0/0"
                        },
                        value = NetworkStatus.Verified(
                            address = mockk {
                                every { availableAddresses } returns emptySet()
                            },
                            amounts = emptyMap(),
                            pendingTransactions = emptyMap(),
                            source = StatusSource.ACTUAL,
                        ),
                    ),
                ),
            )

            // WHEN
            val result = enricher(userWalletId, response)

            // THEN
            assertThat(result.tokens).hasSize(1)
            assertThat(result.tokens[0].list).isEmpty()
        }

    @Test
    fun `GIVEN notifications are enabled and addresses available WHEN invoke THEN return enriched response`() =
        runTest {
            // GIVEN
            val userWalletId = UserWalletId("1234567890abcdef")
            val token = createToken()
            val response = createUserTokensResponse(tokens = listOf(token))
            val addresses = listOf("0x123", "0x456")

            every { notificationsFeatureToggles.isNotificationsEnabled } returns true
            coEvery { walletsRepository.isNotificationsEnabled(userWalletId, false) } returns true
            coEvery { multiNetworkStatusProducer.produce() } returns flowOf(
                setOf(
                    NetworkStatus(
                        network = mockk {
                            every { backendId } returns "ethereum"
                            every { derivationPath.value } returns "m/44'/60'/0'/0/0"
                        },
                        value = NetworkStatus.Verified(
                            address = mockk {
                                every { availableAddresses } returns addresses.map { address ->
                                    mockk<NetworkAddress.Address> {
                                        every { value } returns address
                                    }
                                }.toSet()
                            },
                            amounts = emptyMap(),
                            pendingTransactions = emptyMap(),
                            source = StatusSource.ACTUAL,
                        ),
                    ),
                ),
            )

            // WHEN
            val result = enricher(userWalletId, response)

            // THEN
            assertThat(result.tokens).hasSize(1)
            assertThat(result.tokens[0].list).containsExactlyElementsIn(addresses)
        }

    @Test
    fun `GIVEN notifications are enabled but no matching network WHEN invoke THEN return original token`() = runTest {
        // GIVEN
        val userWalletId = UserWalletId("1234567890abcdef")
        val token = createToken()
        val response = createUserTokensResponse(tokens = listOf(token))

        every { notificationsFeatureToggles.isNotificationsEnabled } returns true
        coEvery { walletsRepository.isNotificationsEnabled(userWalletId, false) } returns true
        coEvery { multiNetworkStatusProducer.produce() } returns flowOf(
            setOf(
                NetworkStatus(
                    network = mockk {
                        every { backendId } returns "bitcoin"
                        every { derivationPath.value } returns "m/44'/0'/0'/0/0"
                    },
                    value = NetworkStatus.Verified(
                        address = mockk {
                            every { availableAddresses } returns emptySet()
                        },
                        amounts = emptyMap(),
                        pendingTransactions = emptyMap(),
                        source = StatusSource.ACTUAL,
                    ),
                ),
            ),
        )

        // WHEN
        val result = enricher(userWalletId, response)

        // THEN
        assertThat(result.tokens).hasSize(1)
        assertThat(result.tokens[0]).isEqualTo(token)
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
        list = list,
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
}