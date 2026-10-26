package com.tangem.features.tangempay.withdraw.bank.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.model.bankofframp.BankOfframpOptions
import com.tangem.features.tangempay.withdraw.bank.BASE_CHAIN_ID
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount
import com.tangem.features.tangempay.withdraw.bank.POLYGON_CHAIN_ID
import com.tangem.features.tangempay.withdraw.bank.USDC_BASE
import com.tangem.features.tangempay.withdraw.bank.activeMoneyAccount
import com.tangem.features.tangempay.withdraw.bank.availableNetwork
import com.tangem.features.tangempay.withdraw.bank.bankWithdrawOptions
import com.tangem.features.tangempay.withdraw.bank.bankWithdrawSource
import com.tangem.features.tangempay.withdraw.bank.currencyStatus
import com.tangem.features.tangempay.withdraw.bank.moneyAccountBalance
import com.tangem.features.tangempay.withdraw.bank.moneyAccountStatus
import com.tangem.features.tangempay.withdraw.bank.offrampOptions
import com.tangem.features.tangempay.withdraw.bank.offrampSource
import com.tangem.features.tangempay.withdraw.bank.paymentToken
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Currency

internal class BankWithdrawSourcesObserverTest {

    private val userWalletId = UserWalletId("011")
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()
    private val moneyAccountStatusSupplier: MoneyAccountStatusSupplier = mockk()

    private val observer = BankWithdrawSourcesObserver(
        paymentAccountStatusSupplier = paymentAccountStatusSupplier,
        moneyAccountStatusSupplier = moneyAccountStatusSupplier,
    )

    private val usdcPolygon = paymentToken()
    private val usdcBase = paymentToken(networkId = "base", networkName = "Base", contractAddress = USDC_BASE)
    private val usdtPolygon = paymentToken(
        contractAddress = "0xc2132D05D31c914a87C6611C10748AEb04B58e8F",
        symbol = "USDT",
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(paymentAccountStatusSupplier, moneyAccountStatusSupplier)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PaymentAccount {

        private val account = BankWithdrawSourceAccount.Payment(userWalletId)

        @Test
        fun `GIVEN tokens with balance WHEN observed THEN accepted ones available and the rest not`() = runTest {
            // Arrange
            val networks = listOf(
                availableNetwork(
                    token = usdtPolygon,
                    fiatBalance = BigDecimal("5"),
                    extraStatuses = listOf(currencyStatus(usdcPolygon, fiatBalance = BigDecimal("100"))),
                ),
                availableNetwork(token = usdcBase, chainId = BASE_CHAIN_ID, fiatBalance = BigDecimal("40")),
            )
            emitPaymentStatus(MockAccounts.createPaymentAccountStatus(userWalletId = userWalletId, networks = networks))

            // Act
            val actual = observer.observe(account, offrampOptions()).toList()

            // Assert
            val expected = bankWithdrawOptions(
                sources = listOf(
                    bankWithdrawSource(token = usdcPolygon, fiatBalance = BigDecimal("100")),
                    bankWithdrawSource(
                        token = usdcBase,
                        chainId = BASE_CHAIN_ID,
                        fiatBalance = BigDecimal("40"),
                        isAvailable = false,
                    ),
                    bankWithdrawSource(token = usdtPolygon, fiatBalance = BigDecimal("5"), isAvailable = false),
                ),
            )
            assertThat(actual).containsExactly(expected)
        }

        @Test
        fun `GIVEN tokens without balance WHEN observed THEN they are dropped whether accepted or not`() = runTest {
            // Arrange
            val networks = listOf(
                availableNetwork(
                    token = usdtPolygon,
                    fiatBalance = BigDecimal.ZERO,
                    extraStatuses = listOf(currencyStatus(usdcPolygon, fiatBalance = BigDecimal("100"))),
                ),
                availableNetwork(token = usdcBase, chainId = BASE_CHAIN_ID, fiatBalance = null),
            )
            val offramp = offrampOptions(
                sources = listOf(offrampSource(), offrampSource("Base", BASE_CHAIN_ID, USDC_BASE)),
            )
            emitPaymentStatus(MockAccounts.createPaymentAccountStatus(userWalletId = userWalletId, networks = networks))

            // Act
            val actual = observer.observe(account, offramp).toList()

            // Assert
            val expected = bankWithdrawOptions(
                sources = listOf(bankWithdrawSource(token = usdcPolygon, fiatBalance = BigDecimal("100"))),
            )
            assertThat(actual).containsExactly(expected)
        }

        @Test
        fun `GIVEN contract address in different case WHEN observed THEN the token is accepted`() = runTest {
            // Arrange
            val token = paymentToken(contractAddress = "0x3C499C542CEF5E3811E1192CE70D8CC03D5C3359")
            val networks = listOf(availableNetwork(token = token))
            emitPaymentStatus(MockAccounts.createPaymentAccountStatus(userWalletId = userWalletId, networks = networks))

            // Act
            val actual = observer.observe(account, offrampOptions()).toList()

            // Assert
            assertThat(actual).containsExactly(bankWithdrawOptions(sources = listOf(bankWithdrawSource(token = token))))
        }

        @Test
        fun `GIVEN account balance in another currency WHEN observed THEN options use that currency`() = runTest {
            // Arrange
            val status = MockAccounts.createPaymentAccountStatus(
                userWalletId = userWalletId,
                balance = MockAccounts.createPaymentAccountBalance(currency = "EUR"),
                networks = listOf(availableNetwork()),
            )
            emitPaymentStatus(status)

            // Act
            val actual = observer.observe(account, offrampOptions()).toList()

            // Assert
            assertThat(actual).containsExactly(bankWithdrawOptions(currency = Currency.getInstance("EUR")))
        }

        @Test
        fun `GIVEN account still loading WHEN observed THEN nothing is emitted`() = runTest {
            // Arrange
            emitPaymentStatus(AccountStatus.Payment(Account.Payment(userWalletId), PaymentAccountStatusValue.Loading))

            // Act
            val actual = observer.observe(account, offrampOptions()).toList()

            // Assert
            assertThat(actual).isEmpty()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN nothing the off-ramp takes WHEN observed THEN no options`(model: PaymentNoOptionsModel) = runTest {
            // Arrange
            emitPaymentStatus(model.status)

            // Act
            val actual = observer.observe(account, offrampOptions()).toList()

            // Assert
            assertThat(actual).containsExactly(null)
        }

        private fun provideTestModels(): List<PaymentNoOptionsModel> = listOf(
            PaymentNoOptionsModel(
                description = "only tokens the off-ramp does not accept",
                status = MockAccounts.createPaymentAccountStatus(
                    userWalletId = userWalletId,
                    networks = listOf(availableNetwork(token = usdtPolygon, fiatBalance = BigDecimal("5"))),
                ),
            ),
            PaymentNoOptionsModel(
                description = "every token without balance",
                status = MockAccounts.createPaymentAccountStatus(
                    userWalletId = userWalletId,
                    networks = listOf(availableNetwork(fiatBalance = BigDecimal.ZERO)),
                ),
            ),
            PaymentNoOptionsModel(
                description = "disabled and not issued networks",
                status = MockAccounts.createPaymentAccountStatus(
                    userWalletId = userWalletId,
                    networks = listOf(
                        PaymentNetworkStatus.Disabled(
                            network = usdcPolygon.network,
                            chainId = POLYGON_CHAIN_ID,
                            cryptoCurrencies = listOf(usdcPolygon),
                        ),
                        PaymentNetworkStatus.NotIssued(network = usdcBase.network, chainId = BASE_CHAIN_ID),
                    ),
                ),
            ),
            PaymentNoOptionsModel(
                description = "account without networks",
                status = AccountStatus.Payment(
                    account = Account.Payment(userWalletId),
                    value = PaymentAccountStatusValue.Error.Unavailable,
                ),
            ),
        )

        private fun emitPaymentStatus(status: AccountStatus.Payment) {
            every { paymentAccountStatusSupplier(userWalletId) } returns flowOf(status)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MoneyAccount {

        private val account = BankWithdrawSourceAccount.Money(userWalletId, productInstanceId = "instance-1")

        @Test
        fun `GIVEN active account with balance WHEN observed THEN its USDC is the only source`() = runTest {
            // Arrange
            emitMoneyStatus(activeMoneyAccount(token = usdcPolygon))

            // Act
            val actual = observer.observe(account, offrampOptions()).toList()

            // Assert
            val expected = bankWithdrawOptions(
                sources = listOf(
                    bankWithdrawSource(
                        token = usdcPolygon,
                        chainId = POLYGON_CHAIN_ID,
                        fiatBalance = BigDecimal("2325.55"),
                    ),
                ),
            )
            assertThat(actual).containsExactly(expected)
        }

        @Test
        fun `GIVEN account still loading WHEN observed THEN nothing is emitted`() = runTest {
            // Arrange
            emitMoneyStatus(MoneyAccountStatusValue.Loading)

            // Act
            val actual = observer.observe(account, offrampOptions()).toList()

            // Assert
            assertThat(actual).isEmpty()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN account the off-ramp cannot take from WHEN observed THEN no options`(model: MoneyNoOptionsModel) =
            runTest {
                // Arrange
                emitMoneyStatus(model.value)

                // Act
                val actual = observer.observe(account, model.offramp).toList()

                // Assert
                assertThat(actual).containsExactly(null)
            }

        private fun provideTestModels(): List<MoneyNoOptionsModel> = listOf(
            MoneyNoOptionsModel(
                description = "closed account with balance",
                value = MoneyAccountStatusValue.Account.Deactivated(
                    source = StatusSource.ACTUAL,
                    cryptoCurrency = usdcPolygon,
                    balance = moneyAccountBalance(),
                    customerId = "cust_1",
                ),
            ),
            MoneyNoOptionsModel(
                description = "bank off-ramp channel not granted",
                value = activeMoneyAccount(token = usdcPolygon, isBankOfframpAvailable = false),
            ),
            MoneyNoOptionsModel(
                description = "bank off-ramp channel unknown",
                value = activeMoneyAccount(token = usdcPolygon, isBankOfframpAvailable = null),
            ),
            MoneyNoOptionsModel(
                description = "balance not loaded",
                value = activeMoneyAccount(token = usdcPolygon, balance = null),
            ),
            MoneyNoOptionsModel(
                description = "zero balance",
                value = activeMoneyAccount(token = usdcPolygon, balance = moneyAccountBalance(BigDecimal.ZERO)),
            ),
            MoneyNoOptionsModel(
                description = "no fiat rate",
                value = activeMoneyAccount(token = usdcPolygon, balance = moneyAccountBalance(fiatRate = null)),
            ),
            MoneyNoOptionsModel(
                description = "off-ramp does not accept the account token",
                value = activeMoneyAccount(token = usdcPolygon),
                offramp = offrampOptions(sources = listOf(offrampSource("Base", BASE_CHAIN_ID, USDC_BASE))),
            ),
            MoneyNoOptionsModel(
                description = "account not created",
                value = MoneyAccountStatusValue.NotCreated,
            ),
            MoneyNoOptionsModel(
                description = "account unavailable",
                value = MoneyAccountStatusValue.Error.Unavailable,
            ),
        )

        private fun emitMoneyStatus(value: MoneyAccountStatusValue) {
            every { moneyAccountStatusSupplier(userWalletId) } returns flowOf(moneyAccountStatus(userWalletId, value))
        }
    }

    internal data class PaymentNoOptionsModel(
        val description: String,
        val status: AccountStatus.Payment,
    )

    internal data class MoneyNoOptionsModel(
        val description: String,
        val value: MoneyAccountStatusValue,
        val offramp: BankOfframpOptions = offrampOptions(),
    )
}