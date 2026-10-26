package com.tangem.domain.moneyaccount.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.derivation.MONEY_ACCOUNT_DERIVATION_PATH
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetMoneyAccountCryptoCurrencyStatusUseCaseTest {

    private val moneyAccountStatusSupplier: MoneyAccountStatusSupplier = mockk()

    private val useCase = GetMoneyAccountCryptoCurrencyStatusUseCase(
        moneyAccountStatusSupplier = moneyAccountStatusSupplier,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(moneyAccountStatusSupplier)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN account status WHEN invoke THEN emits matching token status`(model: StatusModel) = runTest {
        // Arrange
        every { moneyAccountStatusSupplier(USER_WALLET_ID) } returns flowOf(moneyAccountStatus(model.value))

        // Act
        val emitted = useCase(USER_WALLET_ID, model.currency).toList()

        // Assert
        assertThat(emitted).isEqualTo(listOfNotNull(model.expected))
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN account status WHEN invokeSync THEN returns matching token status`(model: StatusModel) = runTest {
        // Arrange
        every { moneyAccountStatusSupplier(USER_WALLET_ID) } returns flowOf(moneyAccountStatus(model.value))

        // Act
        val actual = useCase.invokeSync(USER_WALLET_ID, model.currency)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(model.expected)
    }

    private fun provideTestModels(): List<StatusModel> {
        val active = activeAccount(balance = BALANCE)
        val deactivated = MoneyAccountStatusValue.Account.Deactivated(
            source = StatusSource.ACTUAL,
            cryptoCurrency = ACCOUNT_TOKEN,
            balance = BALANCE,
            customerId = CUSTOMER_ID,
        )
        return listOf(
            StatusModel(
                name = "active account",
                value = active,
                currency = ACCOUNT_TOKEN,
                expected = ACCOUNT to requireNotNull(active.cryptoCurrencyStatus),
            ),
            StatusModel(
                name = "deactivated account",
                value = deactivated,
                currency = ACCOUNT_TOKEN,
                expected = ACCOUNT to requireNotNull(deactivated.cryptoCurrencyStatus),
            ),
            StatusModel(
                name = "account without balance",
                value = activeAccount(balance = null),
                currency = ACCOUNT_TOKEN,
                expected = null,
            ),
            StatusModel(
                name = "portfolio token of the same contract",
                value = active,
                currency = PORTFOLIO_TOKEN,
                expected = null,
            ),
            StatusModel(
                name = "account not created",
                value = MoneyAccountStatusValue.NotCreated,
                currency = ACCOUNT_TOKEN,
                expected = null,
            ),
            StatusModel(
                name = "account activating",
                value = MoneyAccountStatusValue.Onboarding.Activating(
                    source = StatusSource.ACTUAL,
                    customerId = CUSTOMER_ID,
                ),
                currency = ACCOUNT_TOKEN,
                expected = null,
            ),
            StatusModel(
                name = "status unavailable",
                value = MoneyAccountStatusValue.Error.Unavailable,
                currency = ACCOUNT_TOKEN,
                expected = null,
            ),
        )
    }

    private fun activeAccount(balance: MoneyAccountStatusValue.Balance?) = MoneyAccountStatusValue.Account.Active(
        source = StatusSource.ACTUAL,
        cryptoCurrency = ACCOUNT_TOKEN,
        balance = balance,
        customerId = CUSTOMER_ID,
    )

    private fun moneyAccountStatus(value: MoneyAccountStatusValue) = AccountStatus.Money(
        account = ACCOUNT,
        value = value,
    )

    internal data class StatusModel(
        val name: String,
        val value: MoneyAccountStatusValue,
        val currency: CryptoCurrency,
        val expected: Pair<Account.Money, CryptoCurrencyStatus>?,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        const val CUSTOMER_ID = "customer"
        const val USDC_CONTRACT = "0x3c499c542cef5e3811e1192ce70d8cc03d5c3359"

        val USER_WALLET_ID = UserWalletId("011")
        val ACCOUNT = Account.Money(userWalletId = USER_WALLET_ID)
        val ACCOUNT_TOKEN = polygonUsdc(Network.DerivationPath.Custom(MONEY_ACCOUNT_DERIVATION_PATH.rawPath))
        val PORTFOLIO_TOKEN = polygonUsdc(Network.DerivationPath.Card("m/44'/60'/0'/0/0"))
        val BALANCE = MoneyAccountStatusValue.Balance(
            cryptoAmount = BigDecimal("25"),
            depositAddress = "0xdeposit",
            currencyCode = "USD",
            fiatRate = BigDecimal.ONE,
        )

        fun polygonUsdc(derivationPath: Network.DerivationPath): CryptoCurrency.Token = mockk {
            every { id } returns CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.create(rawId = "polygon-pos", derivationPath = derivationPath),
                suffix = CryptoCurrency.ID.Suffix.RawID(rawId = "usd-coin", contractAddress = USDC_CONTRACT),
            )
        }
    }
}