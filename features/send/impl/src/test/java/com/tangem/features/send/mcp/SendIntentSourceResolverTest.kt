package com.tangem.features.send.mcp

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.models.SendIntent
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SendIntentSourceResolverTest {

    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val resolver = SendIntentSourceResolver(singleAccountStatusListSupplier)

    private val walletId = UserWalletId("011")
    private val intent = createSendIntent()

    private val eth = createCoin()
    private val usdt = createToken()
    private val polygonNetwork = createNetwork(rawId = "polygon-pos", name = "Polygon")

    @BeforeEach
    fun resetMocks() {
        clearMocks(singleAccountStatusListSupplier)
    }

    @Test
    fun `GIVEN native coin at the sender address WHEN resolve THEN the coin is found`() = runTest {
        // Arrange
        val accountId = mockk<AccountId>()
        val ethStatus = status(eth)
        stubAccounts(listOf(personal(id = accountId, statuses = listOf(status(usdt, OTHER_ADDRESS), ethStatus))))
        val coinIntent = createSendIntent(tokenId = "ethereum")

        // Act
        val actual = resolver.resolve(walletId, coinIntent)

        // Assert
        assertThat(actual).isEqualTo(SendIntentSource.Found(accountId = accountId, status = ethStatus))
    }

    @Test
    fun `GIVEN coin raw id differs from its network id WHEN resolve THEN the coin is found by its raw id`() = runTest {
        // Arrange
        val accountId = mockk<AccountId>()
        val pol = createCoin(network = polygonNetwork, rawId = "polygon-ecosystem-token", symbol = "POL")
        val polStatus = status(pol)
        stubAccounts(listOf(personal(id = accountId, statuses = listOf(polStatus))))
        val polIntent = createSendIntent(networkId = "polygon-pos", tokenId = "polygon-ecosystem-token")

        // Act
        val actual = resolver.resolve(walletId, polIntent)

        // Assert
        assertThat(actual).isEqualTo(SendIntentSource.Found(accountId = accountId, status = polStatus))
    }

    @Test
    fun `GIVEN token at the sender address WHEN resolve THEN the token is found`() = runTest {
        // Arrange
        val accountId = mockk<AccountId>()
        val usdtStatus = status(usdt)
        stubAccounts(listOf(personal(id = accountId, statuses = listOf(status(eth), usdtStatus))))

        // Act
        val actual = resolver.resolve(walletId, intent)

        // Assert
        assertThat(actual).isEqualTo(SendIntentSource.Found(accountId = accountId, status = usdtStatus))
    }

    @Test
    fun `GIVEN token in two accounts at different addresses WHEN resolve THEN the account owning the address is found`() =
        runTest {
            // Arrange
            val firstAccountId = mockk<AccountId>()
            val secondAccountId = mockk<AccountId>()
            val usdtOfSecond = status(createToken())
            stubAccounts(
                listOf(
                    personal(id = firstAccountId, statuses = listOf(status(createToken(), OTHER_ADDRESS))),
                    personal(id = secondAccountId, statuses = listOf(usdtOfSecond)),
                ),
            )

            // Act
            val actual = resolver.resolve(walletId, intent)

            // Assert
            assertThat(actual).isEqualTo(SendIntentSource.Found(accountId = secondAccountId, status = usdtOfSecond))
        }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN no currency matching the intent WHEN resolve THEN source is not found`(model: NotFoundModel) =
        runTest {
            // Arrange
            stubAccounts(listOf(personal(statuses = model.held.map { (currency, address) -> status(currency, address) })))

            // Act
            val actual = resolver.resolve(walletId, model.intent)

            // Assert
            assertThat(actual).isEqualTo(SendIntentSource.NotFound)
        }

    data class NotFoundModel(
        val case: String,
        val intent: SendIntent,
        val held: List<Pair<CryptoCurrency, String>>,
    ) {
        override fun toString(): String = case
    }

    @Suppress("unused")
    private fun provideTestModels() = listOf(
        NotFoundModel(
            case = "address not in the wallet",
            intent = intent,
            held = listOf(usdt to OTHER_ADDRESS, eth to OTHER_ADDRESS),
        ),
        NotFoundModel(
            case = "same token on another network",
            intent = intent,
            held = listOf(createToken(network = polygonNetwork) to SENDER_ADDRESS),
        ),
        NotFoundModel(
            case = "raw id mismatch",
            intent = createSendIntent(tokenId = "usd-coin"),
            held = listOf(usdt to SENDER_ADDRESS, eth to SENDER_ADDRESS),
        ),
        NotFoundModel(
            case = "custom token without raw id",
            intent = intent,
            held = listOf(createToken(rawId = null) to SENDER_ADDRESS),
        ),
        NotFoundModel(
            case = "no currencies",
            intent = intent,
            held = emptyList(),
        ),
    )

    @Test
    fun `GIVEN EVM sender address in another case WHEN resolve THEN the token is found`() = runTest {
        // Arrange
        val accountId = mockk<AccountId>()
        val usdtStatus = status(usdt)
        stubAccounts(listOf(personal(id = accountId, statuses = listOf(usdtStatus))))
        val shouting = intent.copy(fromAddress = SENDER_ADDRESS.uppercase())

        // Act
        val actual = resolver.resolve(walletId, shouting)

        // Assert
        assertThat(actual).isEqualTo(SendIntentSource.Found(accountId = accountId, status = usdtStatus))
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CaseSensitiveNetwork {

        private val solana = createNetwork(rawId = "solana", name = "Solana")
        private val sol = createCoin(network = solana, symbol = "SOL")

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN Solana sender address WHEN resolve THEN only the exact address is the source`(
            model: SolanaAddressModel,
        ) = runTest {
            // Arrange
            val accountId = mockk<AccountId>()
            val solStatus = status(sol, SOLANA_ADDRESS)
            stubAccounts(listOf(personal(id = accountId, statuses = listOf(solStatus))))
            val solIntent = createSendIntent(networkId = "solana", tokenId = "solana", fromAddress = model.fromAddress)

            // Act
            val actual = resolver.resolve(walletId, solIntent)

            // Assert
            val expected = if (model.isFound) {
                SendIntentSource.Found(accountId = accountId, status = solStatus)
            } else {
                SendIntentSource.NotFound
            }
            assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            SolanaAddressModel(fromAddress = SOLANA_ADDRESS, isFound = true),
            SolanaAddressModel(fromAddress = SOLANA_ADDRESS.lowercase(), isFound = false),
            SolanaAddressModel(fromAddress = SOLANA_ADDRESS.uppercase(), isFound = false),
        )
    }

    data class SolanaAddressModel(val fromAddress: String, val isFound: Boolean)

    @Test
    fun `GIVEN sender address among non-default selectable addresses WHEN resolve THEN the token is found`() = runTest {
        // Arrange
        val accountId = mockk<AccountId>()
        val defaultAddress = NetworkAddress.Address(value = OTHER_ADDRESS, type = NetworkAddress.Address.Type.Primary)
        val selectable = NetworkAddress.Selectable(
            defaultAddress = defaultAddress,
            availableAddresses = setOf(
                defaultAddress,
                NetworkAddress.Address(value = SENDER_ADDRESS, type = NetworkAddress.Address.Type.Secondary),
            ),
        )
        val usdtStatus = status(usdt, networkAddress = selectable)
        stubAccounts(listOf(personal(id = accountId, statuses = listOf(usdtStatus))))

        // Act
        val actual = resolver.resolve(walletId, intent)

        // Assert
        assertThat(actual).isEqualTo(SendIntentSource.Found(accountId = accountId, status = usdtStatus))
    }

    @Test
    fun `GIVEN accounts cannot be read WHEN resolve THEN nothing is resolved`() = runTest {
        // Arrange
        every { singleAccountStatusListSupplier(walletId) } returns emptyFlow()

        // Act
        val actual = resolver.resolve(walletId, intent)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN accounts never emit WHEN resolve THEN nothing is resolved after the wait`() = runTest {
        // Arrange
        every { singleAccountStatusListSupplier(walletId) } returns flow { awaitCancellation() }

        // Act
        val actual = resolver.resolve(walletId, intent)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN token held only in a joint account WHEN resolve THEN source is not found`() = runTest {
        // Arrange
        val joint = mockk<AccountStatus.Joint> {
            every { accountId } returns mockk()
            every { flattenCurrencies() } returns listOf(status(usdt))
        }
        stubAccounts(listOf(joint, personal(statuses = listOf(status(eth)))))

        // Act
        val actual = resolver.resolve(walletId, intent)

        // Assert
        assertThat(actual).isEqualTo(SendIntentSource.NotFound)
    }

    @Test
    fun `GIVEN token still loading in the first emission WHEN resolve THEN it is found in the next one`() = runTest {
        // Arrange
        val accountId = mockk<AccountId>()
        val loadedUsdt = status(usdt)
        stubAccounts(
            listOf(personal(id = accountId, statuses = listOf(loadingStatus(usdt)))),
            listOf(personal(id = accountId, statuses = listOf(loadedUsdt))),
        )

        // Act
        val actual = resolver.resolve(walletId, intent)

        // Assert
        assertThat(actual).isEqualTo(SendIntentSource.Found(accountId = accountId, status = loadedUsdt))
    }

    @Test
    fun `GIVEN token loading until the wait ends WHEN resolve THEN the latest emission is used and source is not found`() =
        runTest {
            // Arrange
            val loading = accountStatusList(listOf(personal(statuses = listOf(loadingStatus(usdt)))))
            every { singleAccountStatusListSupplier(walletId) } returns flow {
                emit(loading)
                awaitCancellation()
            }

            // Act
            val actual = resolver.resolve(walletId, intent)

            // Assert
            assertThat(actual).isEqualTo(SendIntentSource.NotFound)
        }

    @Test
    fun `GIVEN only an unrelated token still loading WHEN resolve THEN it is not waited for`() = runTest {
        // Arrange
        val accountId = mockk<AccountId>()
        val usdtStatus = status(usdt)
        val unrelated = createCoin(network = createNetwork(rawId = "bsc", name = "BNB Smart Chain"), symbol = "BNB")
        val settled = accountStatusList(
            listOf(personal(id = accountId, statuses = listOf(usdtStatus, loadingStatus(unrelated)))),
        )
        every { singleAccountStatusListSupplier(walletId) } returns flow {
            emit(settled)
            awaitCancellation()
        }

        // Act
        val actual = withTimeoutOrNull(timeMillis = 1) { resolver.resolve(walletId, intent) }

        // Assert
        assertThat(actual).isEqualTo(SendIntentSource.Found(accountId = accountId, status = usdtStatus))
    }

    private fun stubAccounts(vararg emissions: List<AccountStatus>) {
        val lists = emissions.map { accountStatusList(it) }
        every { singleAccountStatusListSupplier(walletId) } returns lists.asFlow()
    }

    private fun accountStatusList(accountStatuses: List<AccountStatus>): AccountStatusList =
        mockk { every { this@mockk.accountStatuses } returns accountStatuses }

    private fun personal(
        id: AccountId = mockk(),
        statuses: List<CryptoCurrencyStatus>,
    ): AccountStatus.Personal = mockk {
        every { accountId } returns id
        every { flattenCurrencies() } returns statuses
    }

    private fun status(
        currency: CryptoCurrency,
        address: String = SENDER_ADDRESS,
        networkAddress: NetworkAddress = NetworkAddress.Single(
            defaultAddress = NetworkAddress.Address(value = address, type = NetworkAddress.Address.Type.Primary),
        ),
    ): CryptoCurrencyStatus = mockk {
        every { this@mockk.currency } returns currency
        every { value } returns mockk { every { this@mockk.networkAddress } returns networkAddress }
    }

    private fun loadingStatus(currency: CryptoCurrency): CryptoCurrencyStatus = mockk {
        every { this@mockk.currency } returns currency
        every { value } returns CryptoCurrencyStatus.Loading
    }

    private companion object {
        const val OTHER_ADDRESS = "0x9b8a7c6d5e4f3a2b1c0d9e8f7a6b5c4d3e2f1a0b"
        const val SOLANA_ADDRESS = "7EcDhSYGxXyscszYEp35KHN8vvw3svAuLKTzXwCFLtV"
    }
}