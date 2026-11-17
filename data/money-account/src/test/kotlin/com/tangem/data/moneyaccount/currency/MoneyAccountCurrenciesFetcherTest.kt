package com.tangem.data.moneyaccount.currency

import arrow.core.right
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MoneyAccountCurrenciesFetcherTest {

    private val moneyAccountStatusSupplier: MoneyAccountStatusSupplier = mockk()
    private val moneyAccountStatusFetcher: MoneyAccountStatusFetcher = mockk()

    private val sut = MoneyAccountCurrenciesFetcher(
        moneyAccountStatusSupplier = moneyAccountStatusSupplier,
        moneyAccountStatusFetcher = moneyAccountStatusFetcher,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(moneyAccountStatusSupplier, moneyAccountStatusFetcher)
        coEvery { moneyAccountStatusFetcher(MoneyAccountStatusFetcher.Params(USER_WALLET_ID)) } returns Unit.right()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN account status WHEN fetch THEN only its own network is refetched`(model: RefreshModel) = runTest {
        // Arrange
        every { moneyAccountStatusSupplier(USER_WALLET_ID) } returns flowOf(
            AccountStatus.Money(account = Account.Money(userWalletId = USER_WALLET_ID), value = model.value),
        )

        // Act
        sut.fetch(USER_WALLET_ID, model.network)

        // Assert
        coVerify(exactly = model.expectedFetches) {
            moneyAccountStatusFetcher(MoneyAccountStatusFetcher.Params(USER_WALLET_ID))
        }
    }

    private fun provideTestModels(): List<RefreshModel> = listOf(
        RefreshModel(value = activeAccount(), network = NETWORK, expectedFetches = 1),
        RefreshModel(value = activeAccount(), network = ETHEREUM_NETWORK, expectedFetches = 0),
        RefreshModel(value = MoneyAccountStatusValue.Empty, network = NETWORK, expectedFetches = 0),
    )

    data class RefreshModel(val value: MoneyAccountStatusValue, val network: Network, val expectedFetches: Int)

    private fun activeAccount() = MoneyAccountStatusValue.Account.Active(
        source = StatusSource.ACTUAL,
        cryptoCurrency = TOKEN,
        balance = null,
        customerId = "customer",
        productInstanceId = "instance",
        error = null,
        isKycAdditionalInfoRequired = false,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val CURRENCY_FACTORY = MockCryptoCurrencyFactory()
        val TOKEN = CURRENCY_FACTORY.createToken(
            blockchain = Blockchain.Polygon,
            id = "usd-coin",
            contractAddress = "0x3c499c542cef5e3811e1192ce70d8cc03d5c3359",
            symbol = "USDC",
            decimals = 6,
        )
        val NETWORK = TOKEN.network
        val ETHEREUM_NETWORK = CURRENCY_FACTORY.ethereum.network
    }
}