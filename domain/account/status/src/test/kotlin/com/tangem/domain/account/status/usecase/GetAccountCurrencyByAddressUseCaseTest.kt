package com.tangem.domain.account.status.usecase

import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.utils.assertNone
import com.tangem.common.test.utils.assertSome
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.producer.SingleAccountListProducer
import com.tangem.domain.account.status.model.AccountCryptoCurrency
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetAccountCurrencyByAddressUseCaseTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val multiNetworkStatusSupplier: MultiNetworkStatusSupplier = mockk()
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()

    private val useCase = GetAccountCurrencyByAddressUseCase(
        userWalletsListRepository = userWalletsListRepository,
        multiNetworkStatusSupplier = multiNetworkStatusSupplier,
        singleAccountListSupplier = singleAccountListSupplier,
    )

    @AfterEach
    fun tearDown() {
        clearMocks(userWalletsListRepository, multiNetworkStatusSupplier, singleAccountListSupplier)
    }

    @Test
    fun `returns None if address is empty`() = runTest {
        // Arrange
        val address = ""

        // Act
        val actual = useCase(address)

        // Assert
        assertNone(actual)
    }

    @Test
    fun `returns None if userWalletIds is null`() = runTest {
        // Arrange
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(null)

        // Act
        val actual = useCase(validAddress)

        // Assert
        assertNone(actual)

        coVerifySequence {
            userWalletsListRepository.userWallets
        }
    }

    @Test
    fun `returns None if userWalletIds is empty list`() = runTest {
        // Arrange
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(emptyList())

        // Act
        val actual = useCase(validAddress)

        // Assert
        assertNone(actual)

        coVerifySequence {
            userWalletsListRepository.userWallets
        }
    }

    @Test
    fun `returns None if userWalletIds contain only single wallet`() = runTest {
        // Arrange
        val singleWallet = mockk<UserWallet> {
            every { isMultiCurrency } returns false
        }

        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(singleWallet))

        // Act
        val actual = useCase(validAddress)

        // Assert
        assertNone(actual)

        coVerifySequence {
            userWalletsListRepository.userWallets
        }
    }

    @Test
    fun `returns None if multiNetworkStatusSupplier returns null`() = runTest {
        // Arrange
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(multiUserWallet))
        coEvery {
            multiNetworkStatusSupplier.getSyncOrNull(params = MultiNetworkStatusProducer.Params(userWalletId))
        } returns null

        // Act
        val actual = useCase(validAddress)

        // Assert
        assertNone(actual)

        coVerifySequence {
            userWalletsListRepository.userWallets
            multiNetworkStatusSupplier.getSyncOrNull(params = MultiNetworkStatusProducer.Params(userWalletId))
        }
    }

    @Test
    fun `returns None if multiNetworkStatusSupplier returns empty list`() = runTest {
        // Arrange
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(multiUserWallet))
        coEvery {
            multiNetworkStatusSupplier.getSyncOrNull(params = MultiNetworkStatusProducer.Params(userWalletId))
        } returns emptySet()

        // Act
        val actual = useCase(validAddress)

        // Assert
        assertNone(actual)

        coVerifySequence {
            userWalletsListRepository.userWallets
            multiNetworkStatusSupplier.getSyncOrNull(params = MultiNetworkStatusProducer.Params(userWalletId))
        }
    }

    @Test
    fun `returns None if network status not found`() = runTest {
        // Arrange
        val networkStatus = NetworkStatus(
            network = mockk(),
            value = NetworkStatus.Unreachable(address = null),
        )

        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(multiUserWallet))
        coEvery {
            multiNetworkStatusSupplier.getSyncOrNull(params = MultiNetworkStatusProducer.Params(userWalletId))
        } returns setOf(networkStatus)

        // Act
        val actual = useCase(validAddress)

        // Assert
        assertNone(actual)

        coVerifySequence {
            userWalletsListRepository.userWallets
            multiNetworkStatusSupplier.getSyncOrNull(params = MultiNetworkStatusProducer.Params(userWalletId))
        }
    }

    @Test
    fun `returns None if singleAccountListSupplier returns null`() = runTest {
        // Arrange
        val networkStatus = NetworkStatus(
            network = mockk {
                every { id } returns Network.ID(value = "bitcoin", derivationPath = Network.DerivationPath.None)
            },
            value = NetworkStatus.Unreachable(address = validNetworkAddress),
        )

        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(multiUserWallet))
        coEvery {
            multiNetworkStatusSupplier.getSyncOrNull(params = MultiNetworkStatusProducer.Params(userWalletId))
        } returns setOf(networkStatus)
        coEvery {
            singleAccountListSupplier.getSyncOrNull(
                params = SingleAccountListProducer.Params(userWalletId = userWalletId),
            )
        } returns null

        // Act
        val actual = useCase(validAddress)

        // Assert
        assertNone(actual)

        coVerifySequence {
            userWalletsListRepository.userWallets
            multiNetworkStatusSupplier.getSyncOrNull(params = MultiNetworkStatusProducer.Params(userWalletId))
            singleAccountListSupplier.getSyncOrNull(
                params = SingleAccountListProducer.Params(userWalletId = userWalletId),
            )
        }
    }

    @Test
    fun `returns None if crypto currencies is empty list`() = runTest {
        // Arrange
        val networkId = Network.ID(value = "bitcoin", derivationPath = Network.DerivationPath.None)
        val networkStatus = NetworkStatus(
            network = mockk {
                every { id } returns networkId
            },
            value = NetworkStatus.Unreachable(address = validNetworkAddress),
        )
        val accountList = AccountList.empty(userWalletId)

        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(multiUserWallet))
        coEvery {
            multiNetworkStatusSupplier.getSyncOrNull(params = MultiNetworkStatusProducer.Params(userWalletId))
        } returns setOf(networkStatus)
        coEvery {
            singleAccountListSupplier.getSyncOrNull(
                params = SingleAccountListProducer.Params(userWalletId = userWalletId),
            )
        } returns accountList

        // Act
        val actual = useCase(validAddress)

        // Assert
        assertNone(actual)

        coVerifySequence {
            userWalletsListRepository.userWallets
            multiNetworkStatusSupplier.getSyncOrNull(params = MultiNetworkStatusProducer.Params(userWalletId))
            singleAccountListSupplier.getSyncOrNull(
                params = SingleAccountListProducer.Params(userWalletId = userWalletId),
            )
        }
    }

    @Test
    fun `returns Some if all data is valid`() = runTest {
        // Arrange
        val currency = MockCryptoCurrencyFactory().ethereum
        val networkStatus = NetworkStatus(
            network = currency.network,
            value = NetworkStatus.Unreachable(address = validNetworkAddress),
        )
        val accountList = AccountList.empty(userWalletId = userWalletId, cryptoCurrencies = setOf(currency))

        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(multiUserWallet))
        coEvery {
            multiNetworkStatusSupplier.getSyncOrNull(params = MultiNetworkStatusProducer.Params(userWalletId))
        } returns setOf(networkStatus)
        coEvery {
            singleAccountListSupplier.getSyncOrNull(
                params = SingleAccountListProducer.Params(userWalletId = userWalletId),
            )
        } returns accountList

        // Act
        val actual = useCase(validAddress)

        // Assert
        val expected = AccountCryptoCurrency(account = accountList.mainAccount, cryptoCurrency = currency)
        assertSome(actual, expected)

        coVerifySequence {
            userWalletsListRepository.userWallets
            multiNetworkStatusSupplier.getSyncOrNull(params = MultiNetworkStatusProducer.Params(userWalletId))
            singleAccountListSupplier.getSyncOrNull(
                params = SingleAccountListProducer.Params(userWalletId = userWalletId),
            )
        }
    }

    private companion object Companion {
        const val validAddress = "0x1234567890abcdef"
        val validNetworkAddress = NetworkAddress.Single(
            defaultAddress = NetworkAddress.Address(
                value = validAddress,
                type = NetworkAddress.Address.Type.Primary,
            ),
        )

        val userWalletId = UserWalletId("011")
        val multiUserWallet = mockk<UserWallet> {
            every { this@mockk.walletId } returns userWalletId
            every { this@mockk.isMultiCurrency } returns true
        }
    }
}