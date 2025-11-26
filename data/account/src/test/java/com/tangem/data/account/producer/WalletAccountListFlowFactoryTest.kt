package com.tangem.data.account.producer

import com.google.common.truth.Truth
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.data.account.converter.AccountListConverter
import com.tangem.data.account.converter.createGetWalletAccountsResponse
import com.tangem.data.account.store.AccountsResponseStore
import com.tangem.data.account.store.AccountsResponseStoreFactory
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.test.core.getEmittedValues
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@Suppress("UnusedFlow")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WalletAccountListFlowFactoryTest {

    private val userWalletsStore: UserWalletsStore = mockk()
    private val accountsResponseStoreFactory: AccountsResponseStoreFactory = mockk()
    private val accountsResponseStore: AccountsResponseStore = mockk()
    private val accountsResponseStoreFlow = MutableStateFlow<GetWalletAccountsResponse?>(value = null)

    private val accountListConverterFactory: AccountListConverter.Factory = mockk()
    private val accountListConverter: AccountListConverter = mockk()

    private val cardCryptoCurrencyFactory: CardCryptoCurrencyFactory = mockk()

    private val factory = WalletAccountListFlowFactory(
        userWalletsStore = userWalletsStore,
        accountsResponseStoreFactory = accountsResponseStoreFactory,
        accountListConverterFactory = accountListConverterFactory,
        cardCryptoCurrencyFactory = cardCryptoCurrencyFactory,
    )

    private val userWalletId = UserWalletId("011")
    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()

    @AfterEach
    fun tearDownEach() {
        clearMocks(
            userWalletsStore,
            accountsResponseStoreFactory,
            accountsResponseStore,
            accountListConverterFactory,
            accountListConverter,
            cardCryptoCurrencyFactory,
        )
        accountsResponseStoreFlow.value = null
    }

    @Test
    fun `create for multi wallet`() = runTest {
        // Arrange
        val userWallet = mockk<UserWallet> {
            every { this@mockk.walletId } returns userWalletId
            every { this@mockk.isMultiCurrency } returns true
        }

        every { userWalletsStore.getSyncStrict(userWalletId) } returns userWallet

        val accountsResponse = createGetWalletAccountsResponse(userWalletId)
        every { accountsResponseStoreFactory.create(userWalletId) } returns accountsResponseStore
        every { accountsResponseStore.data } returns accountsResponseStoreFlow
        accountsResponseStoreFlow.value = accountsResponse

        val accountList = AccountList.empty(userWalletId)
        every { accountListConverterFactory.create(userWallet) } returns accountListConverter
        every { accountListConverter.convert(accountsResponse) } returns accountList

        // Act
        val actual = factory.create(userWalletId).let(::getEmittedValues)

        // Assert
        val expected = accountList
        Truth.assertThat(actual).containsExactly(expected)

        coVerify(ordering = Ordering.SEQUENCE) {
            accountsResponseStoreFactory.create(userWalletId)
            accountsResponseStore.data
            accountListConverterFactory.create(userWallet)
            accountListConverter.convert(accountsResponse)
        }

        coVerify(inverse = true) {
            cardCryptoCurrencyFactory.createPrimaryCurrencyForSingleCurrencyCard(any())
            cardCryptoCurrencyFactory.createCurrenciesForSingleCurrencyCardWithToken(any())
        }
    }

    @Test
    fun `create for single wallet`() = runTest {
        val userWallet = MockUserWalletFactory.create().copy(isMultiCurrency = false)

        every { userWalletsStore.getSyncStrict(userWallet.walletId) } returns userWallet

        val currency = cryptoCurrencyFactory.ethereum
        every { cardCryptoCurrencyFactory.createPrimaryCurrencyForSingleCurrencyCard(userWallet) } returns currency

        // Act
        val actual = factory.create(userWallet.walletId).let(::getEmittedValues)

        // Assert
        val expected = AccountList.empty(userWalletId = userWallet.walletId, cryptoCurrencies = setOf(currency))
        Truth.assertThat(actual).containsExactly(expected)

        coVerify(ordering = Ordering.SEQUENCE) {
            cardCryptoCurrencyFactory.createPrimaryCurrencyForSingleCurrencyCard(userWallet)
        }

        coVerify(inverse = true) {
            cardCryptoCurrencyFactory.createCurrenciesForSingleCurrencyCardWithToken(userWallet = any())
            accountsResponseStoreFactory.create(any())
            accountsResponseStore.data
            accountListConverterFactory.create(any())
            accountListConverter.convert(any())
        }
    }

    @Test
    fun `flow is created for single wallet with token`() = runTest {
        val nodl = MockUserWalletFactory.createSingleWalletWithToken()

        every { userWalletsStore.getSyncStrict(nodl.walletId) } returns nodl

        val currencies = cryptoCurrencyFactory.ethereumAndStellar.toSet()
        every {
            cardCryptoCurrencyFactory.createCurrenciesForSingleCurrencyCardWithToken(userWallet = nodl)
        } returns currencies.toList()

        // Act
        val actual = factory.create(nodl.walletId).let(::getEmittedValues)

        // Assert
        val expected = AccountList.empty(userWalletId = nodl.walletId, cryptoCurrencies = currencies)
        Truth.assertThat(actual).containsExactly(expected)

        coVerify(ordering = Ordering.SEQUENCE) {
            cardCryptoCurrencyFactory.createCurrenciesForSingleCurrencyCardWithToken(userWallet = nodl)
        }

        coVerify(inverse = true) {
            cardCryptoCurrencyFactory.createPrimaryCurrencyForSingleCurrencyCard(any())
            accountsResponseStoreFactory.create(any())
            accountsResponseStore.data
            accountListConverterFactory.create(any())
            accountListConverter.convert(any())
        }
    }
}