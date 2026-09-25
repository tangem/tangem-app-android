package com.tangem.data.pay.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.network.NetworkFactory
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Locale

internal class DefaultTangemPayCurrencyFactoryNetworksTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val networkFactory: NetworkFactory = mockk()
    private val userWallet: UserWallet = mockk()
    private val network: Network = mockk()

    private val factory = DefaultTangemPayCurrencyFactory(
        excludedBlockchains = ExcludedBlockchains(),
        userWalletsListRepository = userWalletsListRepository,
        networkFactory = networkFactory,
    )

    @BeforeEach
    fun setUp() {
        mockkStatic("com.tangem.domain.common.wallets.UserWalletsListRepositoryExtKt")
        every { userWalletsListRepository.getSyncStrict(any()) } returns userWallet
        every { network.rawId } returns "polygon"
        every { network.derivationPath } returns Network.DerivationPath.None
        every {
            networkFactory.create(
                blockchain = any<Blockchain>(),
                extraDerivationPath = any(),
                userWallet = any(),
                accountIndex = any(),
            )
        } returns network
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `GIVEN enabled network without deposit address WHEN createNetworkStatuses THEN it is skipped`() {
        // Arrange
        val networks = listOf(networkInfo(depositAddress = null))

        // Act
        val statuses = factory.createNetworkStatuses(USER_WALLET_ID, networks, fiatRate = null)

        // Assert
        assertThat(statuses).isEmpty()
    }

    @Test
    fun `GIVEN enabled network with blank deposit address WHEN createNetworkStatuses THEN it is skipped`() {
        // Arrange
        val networks = listOf(networkInfo(depositAddress = ""))

        // Act
        val statuses = factory.createNetworkStatuses(USER_WALLET_ID, networks, fiatRate = null)

        // Assert
        assertThat(statuses).isEmpty()
    }

    @Test
    fun `GIVEN enabled network with deposit address WHEN createNetworkStatuses THEN it maps to Available`() {
        // Arrange
        val networks = listOf(networkInfo(depositAddress = "0xDEPOSIT"))

        // Act
        val statuses = factory.createNetworkStatuses(USER_WALLET_ID, networks, fiatRate = null)

        // Assert
        val available = statuses.single() as PaymentNetworkStatus.Available
        assertThat(available.depositAddress).isEqualTo("0xDEPOSIT")
        assertThat(available.chainId).isEqualTo(POLYGON_CHAIN_ID)
    }

    @Test
    fun `GIVEN not issued network without deposit address WHEN createNetworkStatuses THEN it survives`() {
        // Arrange
        val networks = listOf(
            networkInfo(depositAddress = null, status = CustomerInfo.NetworkInfo.Status.NOT_ISSUED),
        )

        // Act
        val statuses = factory.createNetworkStatuses(USER_WALLET_ID, networks, fiatRate = null)

        // Assert
        assertThat(statuses.single()).isInstanceOf(PaymentNetworkStatus.NotIssued::class.java)
    }

    @Test
    fun `GIVEN checksum cased contract WHEN createNetworkStatuses THEN the token carries it lowercased`() {
        // Arrange — the backend returns EIP-55 checksummed addresses, while Express and the rest of the app
        // compare contracts as plain lowercase strings.
        val networks = listOf(
            networkInfo(
                depositAddress = "0xDEPOSIT",
                tokens = listOf(
                    CustomerInfo.NetworkInfo.Token(
                        symbol = "USDC",
                        contractAddress = CHECKSUM_CONTRACT,
                        availableForWithdrawal = null,
                    ),
                ),
            ),
        )

        // Act
        val statuses = factory.createNetworkStatuses(USER_WALLET_ID, networks, fiatRate = null)

        // Assert
        val available = statuses.single() as PaymentNetworkStatus.Available
        val token = available.cryptoCurrencyStatuses.single().currency as CryptoCurrency.Token
        assertThat(token.contractAddress).isEqualTo(CHECKSUM_CONTRACT.lowercase(Locale.US))
    }

    @Test
    fun `GIVEN Tron contract WHEN createNetworkStatuses THEN the token keeps the Base58Check case`() {
        // Arrange — Tron addresses are Base58Check, i.e. case-sensitive: lowercasing would corrupt them.
        every { network.rawId } returns "tron"
        val networks = listOf(
            networkInfo(
                name = "tron",
                chainId = TRON_CHAIN_ID,
                depositAddress = "TDEPOSIT",
                tokens = listOf(
                    CustomerInfo.NetworkInfo.Token(
                        symbol = "USDT",
                        contractAddress = TRON_USDT_CONTRACT,
                        availableForWithdrawal = null,
                    ),
                ),
            ),
        )

        // Act
        val statuses = factory.createNetworkStatuses(USER_WALLET_ID, networks, fiatRate = null)

        // Assert
        val available = statuses.single() as PaymentNetworkStatus.Available
        val token = available.cryptoCurrencyStatuses.single().currency as CryptoCurrency.Token
        assertThat(token.contractAddress).isEqualTo(TRON_USDT_CONTRACT)
    }

    private fun networkInfo(
        depositAddress: String?,
        status: CustomerInfo.NetworkInfo.Status = CustomerInfo.NetworkInfo.Status.ENABLED,
        tokens: List<CustomerInfo.NetworkInfo.Token> = emptyList(),
        name: String = "polygon",
        chainId: Long = POLYGON_CHAIN_ID,
    ) = CustomerInfo.NetworkInfo(
        name = name,
        chainId = chainId,
        isTestnet = false,
        status = status,
        depositAddress = depositAddress,
        tokens = tokens,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val POLYGON_CHAIN_ID = 137L
        const val CHECKSUM_CONTRACT = "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359"
        const val TRON_CHAIN_ID = 728126428L
        const val TRON_USDT_CONTRACT = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
    }
}