package com.tangem.domain.account.status.utils

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.single.SingleStakingBalanceProducer
import com.tangem.domain.staking.single.SingleStakingBalanceSupplier
import com.tangem.test.core.getEmittedValues
import io.mockk.*
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
@Suppress("UnusedFlow")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CryptoCurrencyStatusesFlowFactoryTest {

    private val singleNetworkStatusSupplier: SingleNetworkStatusSupplier = mockk()
    private val singleQuoteStatusSupplier: SingleQuoteStatusSupplier = mockk()
    private val singleStakingBalanceSupplier: SingleStakingBalanceSupplier = mockk()
    private val stakingIdFactory: StakingIdFactory = mockk()

    private val factory = CryptoCurrencyStatusesFlowFactory(
        singleNetworkStatusSupplier = singleNetworkStatusSupplier,
        singleQuoteStatusSupplier = singleQuoteStatusSupplier,
        singleStakingBalanceSupplier = singleStakingBalanceSupplier,
        stakingIdFactory = stakingIdFactory,
    )

    private val userWalletId = UserWalletId("011")
    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()

    private val networkAddress = NetworkAddress.Single(
        defaultAddress = NetworkAddress.Address(value = "0x1", type = NetworkAddress.Address.Type.Primary),
    )

    @AfterEach
    fun tearDown() {
        clearMocks(
            singleNetworkStatusSupplier,
            singleQuoteStatusSupplier,
            singleStakingBalanceSupplier,
            stakingIdFactory,
        )
    }

    @Test
    fun `if rawCurrencyId is null, there will be no subscription to the quote status`() = runTest {
        // Arrange
        val userWallet = mockk<UserWallet> {
            every { this@mockk.walletId } returns userWalletId
            every { this@mockk.isMultiCurrency } returns true
        }

        val currency = cryptoCurrencyFactory.ethereum.copy(
            id = cryptoCurrencyFactory.ethereum.id.copy(
                suffix = CryptoCurrency.ID.Suffix.ContractAddress(contractAddress = "0x12345"),
            ),
        )

        val networkStatus = NetworkStatus(
            network = currency.network,
            value = NetworkStatus.Unreachable(address = networkAddress),
        )
        val networkStatusFlow = flowOf(networkStatus)
        every {
            singleNetworkStatusSupplier(params = SingleNetworkStatusProducer.Params(userWalletId, currency.network))
        } returns networkStatusFlow

        val stakingId = StakingID(integrationId = "id", address = networkAddress.defaultAddress.value)
        coEvery {
            stakingIdFactory.create(currencyId = currency.id, defaultAddress = networkAddress.defaultAddress.value)
        } returns stakingId.right()

        val stakingBalance = StakingBalance.Empty(stakingId = stakingId, source = StatusSource.ACTUAL)
        val stakingBalanceFlow = flowOf(stakingBalance)
        every {
            singleStakingBalanceSupplier(
                params = SingleStakingBalanceProducer.Params(userWalletId = userWalletId, stakingId = stakingId),
            )
        } returns stakingBalanceFlow

        // Act
        val actual = factory.create(userWallet = userWallet, currency = currency).let(::getEmittedValues)

        // Assert
        val expected = CryptoCurrencyStatus(
            currency = currency,
            value = CryptoCurrencyStatus.Unreachable(
                priceChange = null,
                fiatRate = null,
                networkAddress = networkAddress,
            ),
        )
        Truth.assertThat(actual).containsExactly(expected)

        coVerify(ordering = Ordering.SEQUENCE) {
            singleNetworkStatusSupplier(params = SingleNetworkStatusProducer.Params(userWalletId, currency.network))
            stakingIdFactory.create(currencyId = currency.id, defaultAddress = networkAddress.defaultAddress.value)
            singleStakingBalanceSupplier(params = SingleStakingBalanceProducer.Params(userWalletId, stakingId))
        }
    }

    @Test
    fun `if userWallet is not multi-currency, there will be no subscription to the yield balance`() = runTest {
        // Arrange
        val userWallet = mockk<UserWallet> {
            every { this@mockk.walletId } returns userWalletId
            every { this@mockk.isMultiCurrency } returns false
        }

        val currency = cryptoCurrencyFactory.ethereum

        val networkStatus = NetworkStatus(
            network = currency.network,
            value = NetworkStatus.Unreachable(address = networkAddress),
        )
        val networkStatusFlow = flowOf(networkStatus)
        every {
            singleNetworkStatusSupplier(params = SingleNetworkStatusProducer.Params(userWalletId, currency.network))
        } returns networkStatusFlow

        val quoteStatus = QuoteStatus(
            rawCurrencyId = currency.id.rawCurrencyId!!,
            value = QuoteStatus.Data(
                source = StatusSource.ACTUAL,
                fiatRate = BigDecimal.ONE,
                priceChange = BigDecimal.ONE,
            ),
        )
        val quoteStatusFlow = flowOf(quoteStatus)
        every {
            singleQuoteStatusSupplier(params = SingleQuoteStatusProducer.Params(currency.id.rawCurrencyId!!))
        } returns quoteStatusFlow

        // Act
        val actual = factory.create(userWallet = userWallet, currency = currency).let(::getEmittedValues)

        // Assert
        val expected = CryptoCurrencyStatus(
            currency = currency,
            value = CryptoCurrencyStatus.Unreachable(
                priceChange = BigDecimal.ONE,
                fiatRate = BigDecimal.ONE,
                networkAddress = networkAddress,
            ),
        )
        Truth.assertThat(actual).containsExactly(expected)

        coVerify(ordering = Ordering.SEQUENCE) {
            singleNetworkStatusSupplier(params = SingleNetworkStatusProducer.Params(userWalletId, currency.network))
            singleQuoteStatusSupplier(params = SingleQuoteStatusProducer.Params(currency.id.rawCurrencyId!!))
        }
    }

    @Test
    fun `no subscription to the quote status and yield balance`() = runTest {
        // Arrange
        val userWallet = mockk<UserWallet> {
            every { this@mockk.walletId } returns userWalletId
            every { this@mockk.isMultiCurrency } returns false
        }

        val currency = cryptoCurrencyFactory.ethereum.copy(
            id = cryptoCurrencyFactory.ethereum.id.copy(
                suffix = CryptoCurrency.ID.Suffix.ContractAddress(contractAddress = "0x12345"),
            ),
        )

        val networkStatus = NetworkStatus(
            network = currency.network,
            value = NetworkStatus.Unreachable(address = networkAddress),
        )
        val networkStatusFlow = flowOf(networkStatus)
        every {
            singleNetworkStatusSupplier(params = SingleNetworkStatusProducer.Params(userWalletId, currency.network))
        } returns networkStatusFlow

        // Act
        val actual = factory.create(userWallet = userWallet, currency = currency).let(::getEmittedValues)

        // Assert
        val expected = CryptoCurrencyStatus(
            currency = currency,
            value = CryptoCurrencyStatus.Unreachable(
                priceChange = null,
                fiatRate = null,
                networkAddress = networkAddress,
            ),
        )
        Truth.assertThat(actual).containsExactly(expected)

        coVerify(ordering = Ordering.SEQUENCE) {
            singleNetworkStatusSupplier(params = SingleNetworkStatusProducer.Params(userWalletId, currency.network))
        }
    }

    @Test
    fun `if stakingId is not supported, yield balance will be null`() = runTest {
        // Arrange
        val userWallet = mockk<UserWallet> {
            every { this@mockk.walletId } returns userWalletId
            every { this@mockk.isMultiCurrency } returns true
        }

        val currency = cryptoCurrencyFactory.ethereum

        val networkStatus = NetworkStatus(
            network = currency.network,
            value = NetworkStatus.Unreachable(address = networkAddress),
        )
        val networkStatusFlow = flowOf(networkStatus)
        every {
            singleNetworkStatusSupplier(params = SingleNetworkStatusProducer.Params(userWalletId, currency.network))
        } returns networkStatusFlow

        val quoteStatus = QuoteStatus(
            rawCurrencyId = currency.id.rawCurrencyId!!,
            value = QuoteStatus.Data(
                source = StatusSource.ACTUAL,
                fiatRate = BigDecimal.ONE,
                priceChange = BigDecimal.ONE,
            ),
        )
        val quoteStatusFlow = flowOf(quoteStatus)
        every {
            singleQuoteStatusSupplier(params = SingleQuoteStatusProducer.Params(currency.id.rawCurrencyId!!))
        } returns quoteStatusFlow

        coEvery {
            stakingIdFactory.create(currencyId = currency.id, defaultAddress = networkAddress.defaultAddress.value)
        } returns StakingIdFactory.Error.UnsupportedCurrency.left()

        // Act
        val actual = factory.create(userWallet = userWallet, currency = currency).let(::getEmittedValues)

        // Assert
        val expected = CryptoCurrencyStatus(
            currency = currency,
            value = CryptoCurrencyStatus.Unreachable(
                priceChange = BigDecimal.ONE,
                fiatRate = BigDecimal.ONE,
                networkAddress = networkAddress,
            ),
        )
        Truth.assertThat(actual).containsExactly(expected)

        coVerify(ordering = Ordering.SEQUENCE) {
            singleNetworkStatusSupplier(params = SingleNetworkStatusProducer.Params(userWalletId, currency.network))
            singleQuoteStatusSupplier(params = SingleQuoteStatusProducer.Params(currency.id.rawCurrencyId!!))
        }
    }

    @Test
    fun `all sources are empty`() = runTest {
        // Arrange
        val userWallet = mockk<UserWallet> {
            every { this@mockk.walletId } returns userWalletId
            every { this@mockk.isMultiCurrency } returns true
        }

        val currency = cryptoCurrencyFactory.ethereum

        every {
            singleNetworkStatusSupplier(params = SingleNetworkStatusProducer.Params(userWalletId, currency.network))
        } returns emptyFlow()

        every {
            singleQuoteStatusSupplier(params = SingleQuoteStatusProducer.Params(currency.id.rawCurrencyId!!))
        } returns emptyFlow()

        // Act
        val actual = factory.create(userWallet = userWallet, currency = currency).let(::getEmittedValues)

        // Assert
        val expected = CryptoCurrencyStatus(currency = currency, value = CryptoCurrencyStatus.Loading)
        Truth.assertThat(actual).containsExactly(expected)

        coVerify(ordering = Ordering.SEQUENCE) {
            singleNetworkStatusSupplier(params = SingleNetworkStatusProducer.Params(userWalletId, currency.network))
            singleQuoteStatusSupplier(params = SingleQuoteStatusProducer.Params(currency.id.rawCurrencyId!!))
        }
    }
}