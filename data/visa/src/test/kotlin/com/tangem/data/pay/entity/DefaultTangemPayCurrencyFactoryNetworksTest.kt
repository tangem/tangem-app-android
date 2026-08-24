package com.tangem.data.pay.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.network.NetworkFactory
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.models.account.PaymentNetworkStatus
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

    private fun networkInfo(
        depositAddress: String?,
        status: CustomerInfo.NetworkInfo.Status = CustomerInfo.NetworkInfo.Status.ENABLED,
    ) = CustomerInfo.NetworkInfo(
        name = "polygon",
        chainId = POLYGON_CHAIN_ID,
        isTestnet = false,
        status = status,
        depositAddress = depositAddress,
        tokens = emptyList(),
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val POLYGON_CHAIN_ID = 137L
    }
}