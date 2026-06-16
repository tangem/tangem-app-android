package com.tangem.feature.swap

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.*
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.quote.PriceChange
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.feature.swap.model.InitialCurrenciesResolver
import com.tangem.features.swap.SwapComponent.Params.CurrencyPosition
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultInitialCurrenciesResolverTest {

    private val getUserWalletUseCase = mockk<GetUserWalletUseCase>()
    private val singleAccountStatusListSupplier = mockk<SingleAccountStatusListSupplier>()
    private val rampStateManager = mockk<RampStateManager>()

    private val userWalletId = UserWalletId("0011")
    private val userWallet = mockk<UserWallet> {
        every { walletId } returns userWalletId
    }

    private val resolver = InitialCurrenciesResolver(
        getUserWalletUseCase = getUserWalletUseCase,
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        rampStateManager = rampStateManager,
    )

    private var uniqueIndex = 0

    @BeforeEach
    fun setup() {
        coEvery { getUserWalletUseCase(userWalletId) } returns userWallet.right()
    }

    // region no initial currency

    @Test
    fun `GIVEN available tokens with balance WHEN no initial currency THEN returns available token with max fiat balance`() =
        runTest {
            val currency1 = mockCryptoCurrency()
            val currency2 = mockCryptoCurrency()
            val currency3 = mockCryptoCurrency()

            val status1 = createCurrencyStatus(currency1, fiatAmount = BigDecimal("100"))
            val status2 = createCurrencyStatus(currency2, fiatAmount = BigDecimal("300"))
            val status3 = createCurrencyStatus(currency3, fiatAmount = BigDecimal("500"))

            val accountStatus = createCryptoPortfolioAccountStatus(listOf(status1, status2, status3))
            setupSupplier(listOf(accountStatus))

            setupAvailability(linkedMapOf(currency1 to true, currency2 to true, currency3 to false))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = null,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            assertThat(from?.status).isSameInstanceAs(status2)
            assertThat(to).isNull()
        }

    @Test
    fun `GIVEN available tokens without balance WHEN no initial currency THEN returns first token from first account`() =
        runTest {
            val currency1 = mockCryptoCurrency()
            val currency2 = mockCryptoCurrency()

            val status1 = createCurrencyStatus(currency1, fiatAmount = BigDecimal.ZERO)
            val status2 = createCurrencyStatus(currency2, fiatAmount = null)

            val accountStatus = createCryptoPortfolioAccountStatus(listOf(status1, status2))
            setupSupplier(listOf(accountStatus))

            setupAvailability(linkedMapOf(currency1 to true, currency2 to true))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = null,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            assertThat(from?.status).isSameInstanceAs(status1)
            assertThat(to).isNull()
        }

    @Test
    fun `GIVEN no available tokens with balance WHEN no initial currency THEN returns token with max fiat balance`() =
        runTest {
            val currency1 = mockCryptoCurrency()
            val currency2 = mockCryptoCurrency()

            val status1 = createCurrencyStatus(currency1, fiatAmount = BigDecimal("100"))
            val status2 = createCurrencyStatus(currency2, fiatAmount = BigDecimal("200"))

            val accountStatus = createCryptoPortfolioAccountStatus(listOf(status1, status2))
            setupSupplier(listOf(accountStatus))

            setupAvailability(linkedMapOf(currency1 to false, currency2 to false))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = null,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            assertThat(from?.status).isSameInstanceAs(status2)
            assertThat(to).isNull()
        }

    @Test
    fun `GIVEN no available tokens without balance WHEN no initial currency THEN returns first token from first account`() =
        runTest {
            val currency1 = mockCryptoCurrency()
            val currency2 = mockCryptoCurrency()

            val status1 = createCurrencyStatus(currency1, fiatAmount = BigDecimal.ZERO)
            val status2 = createCurrencyStatus(currency2, fiatAmount = null)

            val accountStatus = createCryptoPortfolioAccountStatus(listOf(status1, status2))
            setupSupplier(listOf(accountStatus))

            setupAvailability(linkedMapOf(currency1 to false, currency2 to false))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = null,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            assertThat(from?.status).isSameInstanceAs(status1)
            assertThat(to).isNull()
        }

    @Test
    fun `GIVEN empty accounts WHEN no initial currency THEN returns null pair`() = runTest {
        setupSupplier(emptyList())

        val (from, to) = resolver.invoke(
            userWalletId,
            initialCryptoCurrency = null,
            swapCurrencyPosition = CurrencyPosition.ANY,
            isPaymentAccount = false,
        )

        assertThat(from).isNull()
        assertThat(to).isNull()
    }

    @Test
    fun `GIVEN multiple accounts WHEN fallback to first THEN returns first token from first account`() =
        runTest {
            val currency1 = mockCryptoCurrency()
            val currency2 = mockCryptoCurrency()

            val status1 = createCurrencyStatus(currency1, fiatAmount = null)
            val status2 = createCurrencyStatus(currency2, fiatAmount = null)

            val account1Status = createCryptoPortfolioAccountStatus(listOf(status1))
            val account2Status = createCryptoPortfolioAccountStatus(listOf(status2))
            setupSupplier(listOf(account1Status, account2Status))

            setupAvailability(linkedMapOf(currency1 to true))
            setupAvailability(linkedMapOf(currency2 to true))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = null,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            assertThat(from?.status).isSameInstanceAs(status1)
            assertThat(to).isNull()
        }

    @Test
    fun `GIVEN mixed availability and balance across accounts WHEN no initial currency THEN returns best available with balance`() =
        runTest {
            val currency1 = mockCryptoCurrency()
            val currency2 = mockCryptoCurrency()
            val currency3 = mockCryptoCurrency()

            val status1 = createCurrencyStatus(currency1, fiatAmount = BigDecimal("50"))
            val status2 = createCurrencyStatus(currency2, fiatAmount = BigDecimal("200"))
            val status3 = createCurrencyStatus(currency3, fiatAmount = BigDecimal("100"))

            val account1Status = createCryptoPortfolioAccountStatus(listOf(status1))
            val account2Status = createCryptoPortfolioAccountStatus(listOf(status2, status3))
            setupSupplier(listOf(account1Status, account2Status))

            setupAvailability(linkedMapOf(currency1 to true))
            setupAvailability(linkedMapOf(currency2 to false, currency3 to true))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = null,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            assertThat(from?.status).isSameInstanceAs(status3)
            assertThat(to).isNull()
        }

    @Test
    fun `GIVEN Token on ETH network with higher fiat vs Coin on BTC network with lower fiat WHEN no initial currency THEN Token is selected as FROM`() =
        runTest {
            val ethTokenId = mockCurrencyId(rawNetworkId = "ethereum", contractAddress = "0xUSDT")
            val btcCoinId = mockCurrencyId(rawNetworkId = "bitcoin", contractAddress = "")

            val ethToken = mockk<CryptoCurrency.Token>(relaxed = true) {
                every { id } returns ethTokenId
            }
            val btcCoin = mockk<CryptoCurrency.Coin>(relaxed = true) {
                every { id } returns btcCoinId
            }

            val tokenStatus = createCurrencyStatus(ethToken, fiatAmount = BigDecimal("500"))
            val coinStatus = createCurrencyStatus(btcCoin, fiatAmount = BigDecimal("100"))

            // currencies list order must match the order passed to setupAvailability
            val accountStatus = createCryptoPortfolioAccountStatus(listOf(coinStatus, tokenStatus))
            setupSupplier(listOf(accountStatus))

            setupAvailability(linkedMapOf(btcCoin to true, ethToken to true))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = null,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            assertThat(from?.status).isSameInstanceAs(tokenStatus)
            assertThat(to).isNull()
        }

    @Test
    fun `GIVEN Token on ETH with highest fiat vs Coin on BTC with mid fiat vs Coin on SOL with lowest fiat WHEN no initial currency THEN Token is selected as FROM`() =
        runTest {
            val usdtId = mockCurrencyId(rawNetworkId = "ethereum", contractAddress = "0xUSDT")
            val btcId = mockCurrencyId(rawNetworkId = "bitcoin", contractAddress = "")
            val solId = mockCurrencyId(rawNetworkId = "solana", contractAddress = "")

            val usdtToken = mockk<CryptoCurrency.Token>(relaxed = true) {
                every { id } returns usdtId
            }
            val btcCoin = mockk<CryptoCurrency.Coin>(relaxed = true) {
                every { id } returns btcId
            }
            val solCoin = mockk<CryptoCurrency.Coin>(relaxed = true) {
                every { id } returns solId
            }

            val usdtStatus = createCurrencyStatus(usdtToken, fiatAmount = BigDecimal("1000"))
            val btcStatus = createCurrencyStatus(btcCoin, fiatAmount = BigDecimal("500"))
            val solStatus = createCurrencyStatus(solCoin, fiatAmount = BigDecimal("100"))

            // list order: btcStatus, solStatus, usdtStatus → setupAvailability must match
            val accountStatus = createCryptoPortfolioAccountStatus(listOf(btcStatus, solStatus, usdtStatus))
            setupSupplier(listOf(accountStatus))

            setupAvailability(linkedMapOf(btcCoin to true, solCoin to true, usdtToken to true))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = null,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            assertThat(from?.status).isSameInstanceAs(usdtStatus)
            assertThat(to).isNull()
        }

    @Test
    fun `GIVEN Coin on BTC with highest fiat vs Token on ETH with mid fiat vs Token on SOL with lowest fiat WHEN no initial currency THEN Coin is selected as FROM`() =
        runTest {
            val btcId = mockCurrencyId(rawNetworkId = "bitcoin", contractAddress = "")
            val usdtId = mockCurrencyId(rawNetworkId = "ethereum", contractAddress = "0xUSDT")
            val usdcId = mockCurrencyId(rawNetworkId = "solana", contractAddress = "EPjFWdd5")

            val btcCoin = mockk<CryptoCurrency.Coin>(relaxed = true) {
                every { id } returns btcId
            }
            val usdtToken = mockk<CryptoCurrency.Token>(relaxed = true) {
                every { id } returns usdtId
            }
            val usdcToken = mockk<CryptoCurrency.Token>(relaxed = true) {
                every { id } returns usdcId
            }

            val btcStatus = createCurrencyStatus(btcCoin, fiatAmount = BigDecimal("2000"))
            val usdtStatus = createCurrencyStatus(usdtToken, fiatAmount = BigDecimal("600"))
            val usdcStatus = createCurrencyStatus(usdcToken, fiatAmount = BigDecimal("150"))

            // list order: usdtStatus, usdcStatus, btcStatus → setupAvailability must match
            val accountStatus = createCryptoPortfolioAccountStatus(listOf(usdtStatus, usdcStatus, btcStatus))
            setupSupplier(listOf(accountStatus))

            setupAvailability(linkedMapOf(usdtToken to true, usdcToken to true, btcCoin to true))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = null,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            assertThat(from?.status).isSameInstanceAs(btcStatus)
            assertThat(to).isNull()
        }

    // endregion

    // region initial currency tests

    @Test
    fun `GIVEN initial currency not found WHEN invoke THEN returns null pair`() = runTest {
        val initialCurrency = mockCryptoCurrency()
        val otherCurrency = mockCryptoCurrency()

        val status = createCurrencyStatus(otherCurrency, fiatAmount = BigDecimal("100"))
        val accountStatus = createCryptoPortfolioAccountStatus(listOf(status))
        setupSupplier(listOf(accountStatus))
        setupAvailability(linkedMapOf(otherCurrency to true))

        val (from, to) = resolver.invoke(
            userWalletId,
            initialCryptoCurrency = initialCurrency,
            swapCurrencyPosition = CurrencyPosition.ANY,
            isPaymentAccount = false,
        )

        assertThat(from).isNull()
        assertThat(to).isNull()
    }

    @Test
    fun `GIVEN initial currency available with balance WHEN invoke THEN returns it as from`() = runTest {
        val sharedId = mockk<CryptoCurrency.ID>(relaxed = true)
        val initialCurrency = mockCryptoCurrency(id = sharedId)
        val accountCurrency = mockCryptoCurrency(id = sharedId)

        val status = createCurrencyStatus(accountCurrency, fiatAmount = BigDecimal("100"))
        val accountStatus = createCryptoPortfolioAccountStatus(listOf(status))
        setupSupplier(listOf(accountStatus))
        setupAvailability(linkedMapOf(accountCurrency to true))

        val (from, to) = resolver.invoke(
            userWalletId,
            initialCryptoCurrency = initialCurrency,
            swapCurrencyPosition = CurrencyPosition.ANY,
            isPaymentAccount = false,
        )

        assertThat(from?.status).isSameInstanceAs(status)
        assertThat(to).isNull()
    }

    @Test
    fun `GIVEN initial currency available without balance WHEN invoke THEN returns it as to and best as from`() =
        runTest {
            val sharedId = mockk<CryptoCurrency.ID>(relaxed = true)
            val initialCurrency = mockCryptoCurrency(id = sharedId)
            val accountCurrency = mockCryptoCurrency(id = sharedId)
            val otherCurrency = mockCryptoCurrency()

            val initialStatus = createCurrencyStatus(accountCurrency, fiatAmount = BigDecimal.ZERO)
            val otherStatus = createCurrencyStatus(otherCurrency, fiatAmount = BigDecimal("200"))
            val accountStatus = createCryptoPortfolioAccountStatus(listOf(initialStatus, otherStatus))
            setupSupplier(listOf(accountStatus))

            setupAvailability(linkedMapOf(accountCurrency to true, otherCurrency to true))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = initialCurrency,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            assertThat(from?.status).isSameInstanceAs(otherStatus)
            assertThat(to?.status).isSameInstanceAs(initialStatus)
        }

    @Test
    fun `GIVEN initial currency available without balance and is only token WHEN invoke THEN returns it as to and from is null`() =
        runTest {
            val sharedId = mockk<CryptoCurrency.ID>(relaxed = true)
            val initialCurrency = mockCryptoCurrency(id = sharedId)
            val accountCurrency = mockCryptoCurrency(id = sharedId)

            val status = createCurrencyStatus(accountCurrency, fiatAmount = BigDecimal.ZERO)
            val accountStatus = createCryptoPortfolioAccountStatus(listOf(status))
            setupSupplier(listOf(accountStatus))
            setupAvailability(linkedMapOf(accountCurrency to true))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = initialCurrency,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            assertThat(from).isNull()
            assertThat(to?.status).isSameInstanceAs(status)
        }

    @Test
    fun `GIVEN initial currency not available with balance WHEN invoke THEN returns it as from`() = runTest {
        val sharedId = mockk<CryptoCurrency.ID>(relaxed = true)
        val initialCurrency = mockCryptoCurrency(id = sharedId)
        val accountCurrency = mockCryptoCurrency(id = sharedId)

        val status = createCurrencyStatus(accountCurrency, fiatAmount = BigDecimal("100"))
        val accountStatus = createCryptoPortfolioAccountStatus(listOf(status))
        setupSupplier(listOf(accountStatus))
        setupAvailability(linkedMapOf(accountCurrency to false))

        val (from, to) = resolver.invoke(
            userWalletId,
            initialCryptoCurrency = initialCurrency,
            swapCurrencyPosition = CurrencyPosition.ANY,
            isPaymentAccount = false,
        )

        assertThat(from?.status).isSameInstanceAs(status)
        assertThat(to).isNull()
    }

    @Test
    fun `GIVEN initial currency not available with balance and other available with higher balance WHEN invoke THEN returns initial as from`() =
        runTest {
            val sharedId = mockk<CryptoCurrency.ID>(relaxed = true)
            val initialCurrency = mockCryptoCurrency(id = sharedId)
            val accountCurrency = mockCryptoCurrency(id = sharedId)
            val otherCurrency = mockCryptoCurrency()

            val initialStatus = createCurrencyStatus(accountCurrency, fiatAmount = BigDecimal("100"))
            val otherStatus = createCurrencyStatus(otherCurrency, fiatAmount = BigDecimal("500"))
            val accountStatus = createCryptoPortfolioAccountStatus(listOf(initialStatus, otherStatus))
            setupSupplier(listOf(accountStatus))

            setupAvailability(linkedMapOf(accountCurrency to false, otherCurrency to true))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = initialCurrency,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            assertThat(from?.status).isSameInstanceAs(initialStatus)
            assertThat(to).isNull()
        }

    @Test
    fun `GIVEN initial currency not available without balance and other available with balance WHEN invoke THEN returns best available as from`() =
        runTest {
            val sharedId = mockk<CryptoCurrency.ID>(relaxed = true)
            val initialCurrency = mockCryptoCurrency(id = sharedId)
            val accountCurrency = mockCryptoCurrency(id = sharedId)
            val other1 = mockCryptoCurrency()
            val other2 = mockCryptoCurrency()

            val initialStatus = createCurrencyStatus(accountCurrency, fiatAmount = null)
            val other1Status = createCurrencyStatus(other1, fiatAmount = BigDecimal("100"))
            val other2Status = createCurrencyStatus(other2, fiatAmount = BigDecimal("300"))
            val accountStatus = createCryptoPortfolioAccountStatus(
                listOf(initialStatus, other1Status, other2Status),
            )
            setupSupplier(listOf(accountStatus))

            setupAvailability(linkedMapOf(accountCurrency to false, other1 to true, other2 to true))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = initialCurrency,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            assertThat(from?.status).isSameInstanceAs(other2Status)
            assertThat(to?.status).isSameInstanceAs(initialStatus)
        }

    @Test
    fun `GIVEN initial currency not available without balance and other available without balance WHEN invoke THEN returns first token as from`() =
        runTest {
            val sharedId = mockk<CryptoCurrency.ID>(relaxed = true)
            val initialCurrency = mockCryptoCurrency(id = sharedId)
            val accountCurrency = mockCryptoCurrency(id = sharedId)
            val otherCurrency = mockCryptoCurrency()

            val initialStatus = createCurrencyStatus(accountCurrency, fiatAmount = null)
            val otherStatus = createCurrencyStatus(otherCurrency, fiatAmount = BigDecimal.ZERO)
            val accountStatus = createCryptoPortfolioAccountStatus(listOf(otherStatus, initialStatus))
            setupSupplier(listOf(accountStatus))

            setupAvailability(linkedMapOf(otherCurrency to true, accountCurrency to false))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = initialCurrency,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            assertThat(from?.status).isSameInstanceAs(otherStatus)
            assertThat(to?.status).isSameInstanceAs(initialStatus)
        }

    @Test
    fun `GIVEN initial currency not available without balance and no available tokens with balance WHEN invoke THEN returns best by balance as from`() =
        runTest {
            val sharedId = mockk<CryptoCurrency.ID>(relaxed = true)
            val initialCurrency = mockCryptoCurrency(id = sharedId)
            val accountCurrency = mockCryptoCurrency(id = sharedId)
            val otherCurrency = mockCryptoCurrency()

            val initialStatus = createCurrencyStatus(accountCurrency, fiatAmount = null)
            val otherStatus = createCurrencyStatus(otherCurrency, fiatAmount = BigDecimal("200"))
            val accountStatus = createCryptoPortfolioAccountStatus(listOf(initialStatus, otherStatus))
            setupSupplier(listOf(accountStatus))

            setupAvailability(linkedMapOf(accountCurrency to false, otherCurrency to false))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = initialCurrency,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            assertThat(from?.status).isSameInstanceAs(otherStatus)
            assertThat(to?.status).isSameInstanceAs(initialStatus)
        }

    @Test
    fun `GIVEN initial currency not available without balance and no available tokens without balance WHEN invoke THEN returns first token as from`() =
        runTest {
            val sharedId = mockk<CryptoCurrency.ID>(relaxed = true)
            val initialCurrency = mockCryptoCurrency(id = sharedId)
            val accountCurrency = mockCryptoCurrency(id = sharedId)
            val otherCurrency = mockCryptoCurrency()

            val initialStatus = createCurrencyStatus(accountCurrency, fiatAmount = null)
            val otherStatus = createCurrencyStatus(otherCurrency, fiatAmount = BigDecimal.ZERO)
            val accountStatus = createCryptoPortfolioAccountStatus(listOf(otherStatus, initialStatus))
            setupSupplier(listOf(accountStatus))

            setupAvailability(linkedMapOf(otherCurrency to false, accountCurrency to false))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = initialCurrency,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            assertThat(from?.status).isSameInstanceAs(otherStatus)
            assertThat(to?.status).isSameInstanceAs(initialStatus)
        }

    @Test
    fun `GIVEN initial currency not available without balance and is only token WHEN invoke THEN returns it as to and from is null`() =
        runTest {
            val sharedId = mockk<CryptoCurrency.ID>(relaxed = true)
            val initialCurrency = mockCryptoCurrency(id = sharedId)
            val accountCurrency = mockCryptoCurrency(id = sharedId)

            val status = createCurrencyStatus(accountCurrency, fiatAmount = null)
            val accountStatus = createCryptoPortfolioAccountStatus(listOf(status))
            setupSupplier(listOf(accountStatus))
            setupAvailability(linkedMapOf(accountCurrency to false))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = initialCurrency,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            assertThat(from).isNull()
            assertThat(to?.status).isSameInstanceAs(status)
        }

    @Test
    fun `GIVEN initial from second account without balance and same token in main with balance WHEN invoke THEN does not pick same token as from`() =
        runTest {
            val sharedNetworkId = "ethereum"
            val sharedContractAddress = "0xUSDT"

            // Same token (same network + contract), different ids (simulates different derivations)
            val idInSecondary = mockCurrencyId(sharedNetworkId, sharedContractAddress)
            val idInMain = mockCurrencyId(sharedNetworkId, sharedContractAddress)
            val initialCurrency = mockCryptoCurrency(id = idInSecondary)
            val usdtInSecondary = mockCryptoCurrency(id = idInSecondary)
            val usdtInMain = mockCryptoCurrency(id = idInMain)

            val statusInSecondary = createCurrencyStatus(usdtInSecondary, fiatAmount = null)
            val statusInMain = createCurrencyStatus(usdtInMain, fiatAmount = BigDecimal("1000"))

            val accountStatus = createCryptoPortfolioAccountStatus(listOf(statusInMain, statusInSecondary))
            setupSupplier(listOf(accountStatus))

            setupAvailability(linkedMapOf(usdtInMain to true, usdtInSecondary to true))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = initialCurrency,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            // Selected goes to TO; FROM must not be the same token from another account
            assertThat(from).isNull()
            assertThat(to?.status).isSameInstanceAs(statusInSecondary)
        }

    @Test
    fun `GIVEN initial from second account with balance WHEN invoke THEN returns it as from`() = runTest {
        val idInSecondary = mockCurrencyId("ethereum", "0xUSDT")
        val initialCurrency = mockCryptoCurrency(id = idInSecondary)
        val usdtInSecondary = mockCryptoCurrency(id = idInSecondary)
        val otherCurrency = mockCryptoCurrency()

        val statusInSecondary = createCurrencyStatus(usdtInSecondary, fiatAmount = BigDecimal("200"))
        val otherStatus = createCurrencyStatus(otherCurrency, fiatAmount = BigDecimal("500"))

        val accountStatus = createCryptoPortfolioAccountStatus(listOf(otherStatus, statusInSecondary))
        setupSupplier(listOf(accountStatus))

        setupAvailability(linkedMapOf(otherCurrency to true, usdtInSecondary to true))

        val (from, to) = resolver.invoke(
            userWalletId,
            initialCryptoCurrency = initialCurrency,
            swapCurrencyPosition = CurrencyPosition.ANY,
            isPaymentAccount = false,
        )

        assertThat(from?.status).isSameInstanceAs(statusInSecondary)
        assertThat(to).isNull()
    }

    @Test
    fun `GIVEN initial in secondary account placed in TO WHEN invoke THEN FROM is picked only from same account`() =
        runTest {
            // Main account has a high-balance available currency.
            val mainOnlyCurrency = mockCryptoCurrency()
            val mainStatus = createCurrencyStatus(mainOnlyCurrency, fiatAmount = BigDecimal("10000"))
            val mainAccount = createCryptoPortfolioAccountStatus(
                currencies = listOf(mainStatus),
                derivationIndexValue = 0,
            )

            // Secondary account holds the initial currency (available, zero balance → TO)
            // plus another available currency with balance.
            val initialId = mockCurrencyId("ethereum", "0xUSDT")
            val initialCurrency = mockCryptoCurrency(id = initialId)
            val initialInSecondary = mockCryptoCurrency(id = initialId)
            val secondaryCompanion = mockCryptoCurrency()

            val initialStatus = createCurrencyStatus(initialInSecondary, fiatAmount = BigDecimal.ZERO)
            val secondaryStatus = createCurrencyStatus(secondaryCompanion, fiatAmount = BigDecimal("50"))

            val secondaryAccount = createCryptoPortfolioAccountStatus(
                currencies = listOf(initialStatus, secondaryStatus),
                derivationIndexValue = 1,
            )

            setupSupplier(listOf(mainAccount, secondaryAccount))

            setupAvailability(linkedMapOf(mainOnlyCurrency to true))
            setupAvailability(linkedMapOf(initialInSecondary to true, secondaryCompanion to true))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = initialCurrency,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            // FROM must come from secondary account only — never the main account's high-balance currency.
            assertThat(from?.status).isSameInstanceAs(secondaryStatus)
            assertThat(to?.status).isSameInstanceAs(initialStatus)
        }

    @Test
    fun `GIVEN initial in secondary account is only token in that account WHEN invoke THEN FROM is null`() =
        runTest {
            // Main account has candidates that must NOT be picked as FROM.
            val mainOnlyCurrency = mockCryptoCurrency()
            val mainStatus = createCurrencyStatus(mainOnlyCurrency, fiatAmount = BigDecimal("500"))
            val mainAccount = createCryptoPortfolioAccountStatus(
                currencies = listOf(mainStatus),
                derivationIndexValue = 0,
            )

            // Secondary account has only the initial currency (available, no balance → TO).
            val initialId = mockCurrencyId("ethereum", "0xUSDT")
            val initialCurrency = mockCryptoCurrency(id = initialId)
            val initialInSecondary = mockCryptoCurrency(id = initialId)

            val initialStatus = createCurrencyStatus(initialInSecondary, fiatAmount = BigDecimal.ZERO)
            val secondaryAccount = createCryptoPortfolioAccountStatus(
                currencies = listOf(initialStatus),
                derivationIndexValue = 1,
            )

            setupSupplier(listOf(mainAccount, secondaryAccount))

            setupAvailability(linkedMapOf(mainOnlyCurrency to true))
            setupAvailability(linkedMapOf(initialInSecondary to true))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = initialCurrency,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            // Secondary account has no other candidates; FROM must be null, not pulled from main.
            assertThat(from).isNull()
            assertThat(to?.status).isSameInstanceAs(initialStatus)
        }

    // endregion

    // region currency position FROM

    @Test
    fun `GIVEN position FROM and available with balance WHEN invoke THEN returns selected as from`() = runTest {
        val sharedId = mockk<CryptoCurrency.ID>(relaxed = true)
        val initialCurrency = mockCryptoCurrency(id = sharedId)
        val accountCurrency = mockCryptoCurrency(id = sharedId)

        val status = createCurrencyStatus(accountCurrency, fiatAmount = BigDecimal("100"))
        val accountStatus = createCryptoPortfolioAccountStatus(listOf(status))
        setupSupplier(listOf(accountStatus))
        setupAvailability(linkedMapOf(accountCurrency to true))

        val (from, to) = resolver.invoke(
            userWalletId,
            initialCryptoCurrency = initialCurrency,
            swapCurrencyPosition = CurrencyPosition.FROM,
            isPaymentAccount = false,
        )

        assertThat(from?.status).isSameInstanceAs(status)
        assertThat(to).isNull()
    }

    @Test
    fun `GIVEN position FROM and not available without balance WHEN invoke THEN still returns selected as from`() =
        runTest {
            val sharedId = mockk<CryptoCurrency.ID>(relaxed = true)
            val initialCurrency = mockCryptoCurrency(id = sharedId)
            val accountCurrency = mockCryptoCurrency(id = sharedId)

            val status = createCurrencyStatus(accountCurrency, fiatAmount = null)
            val accountStatus = createCryptoPortfolioAccountStatus(listOf(status))
            setupSupplier(listOf(accountStatus))
            setupAvailability(linkedMapOf(accountCurrency to false))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = initialCurrency,
                swapCurrencyPosition = CurrencyPosition.FROM,
                isPaymentAccount = false,
            )

            assertThat(from?.status).isSameInstanceAs(status)
            assertThat(to).isNull()
        }

    // endregion

    // region currency position TO

    @Test
    fun `GIVEN position TO and available with balance WHEN invoke THEN returns selected as to`() = runTest {
        val sharedId = mockk<CryptoCurrency.ID>(relaxed = true)
        val initialCurrency = mockCryptoCurrency(id = sharedId)
        val accountCurrency = mockCryptoCurrency(id = sharedId)

        val status = createCurrencyStatus(accountCurrency, fiatAmount = BigDecimal("100"))
        val accountStatus = createCryptoPortfolioAccountStatus(listOf(status))
        setupSupplier(listOf(accountStatus))
        setupAvailability(linkedMapOf(accountCurrency to true))

        val (from, to) = resolver.invoke(
            userWalletId,
            initialCryptoCurrency = initialCurrency,
            swapCurrencyPosition = CurrencyPosition.TO,
            isPaymentAccount = false,
        )

        assertThat(from).isNull()
        assertThat(to?.status).isSameInstanceAs(status)
    }

    @Test
    fun `GIVEN position TO and not available without balance WHEN invoke THEN still returns selected as to`() =
        runTest {
            val sharedId = mockk<CryptoCurrency.ID>(relaxed = true)
            val initialCurrency = mockCryptoCurrency(id = sharedId)
            val accountCurrency = mockCryptoCurrency(id = sharedId)

            val status = createCurrencyStatus(accountCurrency, fiatAmount = null)
            val accountStatus = createCryptoPortfolioAccountStatus(listOf(status))
            setupSupplier(listOf(accountStatus))
            setupAvailability(linkedMapOf(accountCurrency to false))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = initialCurrency,
                swapCurrencyPosition = CurrencyPosition.TO,
                isPaymentAccount = false,
            )

            assertThat(from).isNull()
            assertThat(to?.status).isSameInstanceAs(status)
        }

    // endregion

    // region multi-account balance selection (no initial currency)

    @Test
    fun `GIVEN two accounts where secondary has higher balance WHEN no initial currency THEN picks token from secondary account`() =
        runTest {
            // Main account: currency with low balance.
            val mainCurrency = mockCryptoCurrency()
            val mainStatus = createCurrencyStatus(mainCurrency, fiatAmount = BigDecimal("10"))
            val mainAccount = createCryptoPortfolioAccountStatus(
                currencies = listOf(mainStatus),
                derivationIndexValue = 0,
            )

            // Secondary account: currency with higher balance.
            val secondaryCurrency = mockCryptoCurrency()
            val secondaryStatus = createCurrencyStatus(secondaryCurrency, fiatAmount = BigDecimal("500"))
            val secondaryAccount = createCryptoPortfolioAccountStatus(
                currencies = listOf(secondaryStatus),
                derivationIndexValue = 1,
            )

            setupSupplier(listOf(mainAccount, secondaryAccount))
            setupAvailability(linkedMapOf(mainCurrency to true))
            setupAvailability(linkedMapOf(secondaryCurrency to true))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = null,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            // Global max-balance candidate lives in the secondary account.
            assertThat(from?.status).isSameInstanceAs(secondaryStatus)
            assertThat(to).isNull()
        }

    @Test
    fun `GIVEN two accounts each with several currencies where highest fiat token is in account 2 WHEN no initial currency THEN that token wins`() =
        runTest {
            val c1 = mockCryptoCurrency()
            val c2 = mockCryptoCurrency()
            val c3 = mockCryptoCurrency()
            val c4 = mockCryptoCurrency()

            val s1 = createCurrencyStatus(c1, fiatAmount = BigDecimal("100"))
            val s2 = createCurrencyStatus(c2, fiatAmount = BigDecimal("200"))
            // c3 is a Token in account 2 with the highest fiat.
            val s3 = createCurrencyStatus(c3, fiatAmount = BigDecimal("999"))
            val s4 = createCurrencyStatus(c4, fiatAmount = BigDecimal("50"))

            val account1 = createCryptoPortfolioAccountStatus(
                currencies = listOf(s1, s2),
                derivationIndexValue = 0,
            )
            val account2 = createCryptoPortfolioAccountStatus(
                currencies = listOf(s4, s3),
                derivationIndexValue = 1,
            )

            setupSupplier(listOf(account1, account2))
            setupAvailability(linkedMapOf(c1 to true, c2 to true))
            setupAvailability(linkedMapOf(c4 to true, c3 to true))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = null,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            assertThat(from?.status).isSameInstanceAs(s3)
            assertThat(to).isNull()
        }

    @Test
    fun `GIVEN two accounts all balances are zero or null WHEN no initial currency THEN falls back to first currency of first account`() =
        runTest {
            val c1 = mockCryptoCurrency()
            val c2 = mockCryptoCurrency()

            val s1 = createCurrencyStatus(c1, fiatAmount = BigDecimal.ZERO)
            val s2 = createCurrencyStatus(c2, fiatAmount = null)

            val account1 = createCryptoPortfolioAccountStatus(
                currencies = listOf(s1),
                derivationIndexValue = 0,
            )
            val account2 = createCryptoPortfolioAccountStatus(
                currencies = listOf(s2),
                derivationIndexValue = 1,
            )

            setupSupplier(listOf(account1, account2))
            setupAvailability(linkedMapOf(c1 to true))
            setupAvailability(linkedMapOf(c2 to true))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = null,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            // cryptoPortfolioAccountsMap.entries.firstOrNull()?.value?.firstOrNull() = s1.
            assertThat(from?.status).isSameInstanceAs(s1)
            assertThat(to).isNull()
        }

    @Test
    fun `GIVEN first account is empty and second account has currencies with balance WHEN no initial currency THEN picks highest balance from second account`() =
        runTest {
            val c1 = mockCryptoCurrency()
            val c2 = mockCryptoCurrency()

            val s1 = createCurrencyStatus(c1, fiatAmount = BigDecimal("150"))
            val s2 = createCurrencyStatus(c2, fiatAmount = BigDecimal("300"))

            // account1 has no currencies at all.
            val account1 = createCryptoPortfolioAccountStatus(
                currencies = emptyList(),
                derivationIndexValue = 0,
            )
            val account2 = createCryptoPortfolioAccountStatus(
                currencies = listOf(s1, s2),
                derivationIndexValue = 1,
            )

            setupSupplier(listOf(account1, account2))
            // account1 is empty so rampStateManager is called with an empty list for it.
            coEvery { rampStateManager.availableForSwap(userWalletId, emptyList()) } returns emptyMap()
            setupAvailability(linkedMapOf(c1 to true, c2 to true))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = null,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            assertThat(from?.status).isSameInstanceAs(s2)
            assertThat(to).isNull()
        }

    // endregion

    // region initial currency + CurrencyPosition.ANY going to TO, scoped to same account

    @Test
    fun `GIVEN selected zero-balance currency in account 1 goes to TO WHEN account 2 has higher-balance currency THEN FROM is picked only from account 1`() =
        runTest {
            // Account 1: the initial currency (available, zero balance → TO) + a companion.
            val initialId = mockCurrencyId("ethereum", "0xDAI")
            val initialCurrency = mockCryptoCurrency(id = initialId)
            val initialInAccount1 = mockCryptoCurrency(id = initialId)
            val companion1 = mockCryptoCurrency()

            val initialStatus = createCurrencyStatus(initialInAccount1, fiatAmount = BigDecimal.ZERO)
            val companionStatus = createCurrencyStatus(companion1, fiatAmount = BigDecimal("75"))

            val account1 = createCryptoPortfolioAccountStatus(
                currencies = listOf(initialStatus, companionStatus),
                derivationIndexValue = 0,
            )

            // Account 2: has a currency with much higher balance that must NOT be chosen as FROM.
            val highBalanceCurrency = mockCryptoCurrency()
            val highBalanceStatus = createCurrencyStatus(highBalanceCurrency, fiatAmount = BigDecimal("9999"))
            val account2 = createCryptoPortfolioAccountStatus(
                currencies = listOf(highBalanceStatus),
                derivationIndexValue = 1,
            )

            setupSupplier(listOf(account1, account2))
            setupAvailability(linkedMapOf(initialInAccount1 to true, companion1 to true))
            setupAvailability(linkedMapOf(highBalanceCurrency to true))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = initialCurrency,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            // FROM must be from account1, never the high-balance token from account2.
            assertThat(from?.status).isSameInstanceAs(companionStatus)
            assertThat(to?.status).isSameInstanceAs(initialStatus)
        }

    @Test
    fun `GIVEN selected zero-balance currency goes to TO WHEN same account has multiple currencies THEN FROM is the highest-balance one within same account`() =
        runTest {
            val initialId = mockCurrencyId("solana", "")
            val initialCurrency = mockCryptoCurrency(id = initialId)
            val initialInAccount = mockCryptoCurrency(id = initialId)

            val lowBalance = mockCryptoCurrency()
            val midBalance = mockCryptoCurrency()
            val highBalance = mockCryptoCurrency()

            val initialStatus = createCurrencyStatus(initialInAccount, fiatAmount = BigDecimal.ZERO)
            val lowStatus = createCurrencyStatus(lowBalance, fiatAmount = BigDecimal("10"))
            val midStatus = createCurrencyStatus(midBalance, fiatAmount = BigDecimal("100"))
            val highStatus = createCurrencyStatus(highBalance, fiatAmount = BigDecimal("500"))

            val account1 = createCryptoPortfolioAccountStatus(
                currencies = listOf(initialStatus, lowStatus, midStatus, highStatus),
                derivationIndexValue = 0,
            )

            // Account 2 has an even higher balance that must NOT be picked.
            val outsiderCurrency = mockCryptoCurrency()
            val outsiderStatus = createCurrencyStatus(outsiderCurrency, fiatAmount = BigDecimal("10000"))
            val account2 = createCryptoPortfolioAccountStatus(
                currencies = listOf(outsiderStatus),
                derivationIndexValue = 1,
            )

            setupSupplier(listOf(account1, account2))
            setupAvailability(
                linkedMapOf(
                    initialInAccount to true,
                    lowBalance to true,
                    midBalance to true,
                    highBalance to true
                )
            )
            setupAvailability(linkedMapOf(outsiderCurrency to true))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = initialCurrency,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            // FROM must be the highest-balance token within account 1 only.
            assertThat(from?.status).isSameInstanceAs(highStatus)
            assertThat(to?.status).isSameInstanceAs(initialStatus)
        }

    // endregion

    // region isSameTokenAs dedupe across accounts

    @Test
    fun `GIVEN same token in two accounts where selected is in account 1 WHEN going to TO THEN duplicate in account 2 is not considered and FROM is other account 1 currency`() =
        runTest {
            val sharedNetworkId = "polygon"
            val sharedContract = "0xUSDC"

            // Same token in two accounts (different IDs due to different derivations).
            val idInAccount1 = mockCurrencyId(sharedNetworkId, sharedContract)
            val idInAccount2 = mockCurrencyId(sharedNetworkId, sharedContract)

            val initialCurrency = mockCryptoCurrency(id = idInAccount1)
            val usdcInAccount1 = mockCryptoCurrency(id = idInAccount1)
            val usdcInAccount2 = mockCryptoCurrency(id = idInAccount2)

            // Account 1 also has a distinct companion currency.
            val account1Companion = mockCryptoCurrency()

            val initialStatus = createCurrencyStatus(usdcInAccount1, fiatAmount = BigDecimal.ZERO)
            val companionStatus = createCurrencyStatus(account1Companion, fiatAmount = BigDecimal("200"))

            val account1 = createCryptoPortfolioAccountStatus(
                currencies = listOf(initialStatus, companionStatus),
                derivationIndexValue = 0,
            )

            // Account 2 has the duplicate token with a large balance — must NOT be picked.
            val duplicateStatus = createCurrencyStatus(usdcInAccount2, fiatAmount = BigDecimal("5000"))
            val account2 = createCryptoPortfolioAccountStatus(
                currencies = listOf(duplicateStatus),
                derivationIndexValue = 1,
            )

            setupSupplier(listOf(account1, account2))
            setupAvailability(linkedMapOf(usdcInAccount1 to true, account1Companion to true))
            setupAvailability(linkedMapOf(usdcInAccount2 to true))

            val (from, to) = resolver.invoke(
                userWalletId,
                initialCryptoCurrency = initialCurrency,
                swapCurrencyPosition = CurrencyPosition.ANY,
                isPaymentAccount = false,
            )

            // FROM must be the account1 companion (scoped to account1, duplicate in account2 excluded).
            assertThat(from?.status).isSameInstanceAs(companionStatus)
            assertThat(to?.status).isSameInstanceAs(initialStatus)
        }

    // endregion

    // region helpers

    private fun mockCryptoCurrency(
        id: CryptoCurrency.ID = mockCurrencyId(),
    ): CryptoCurrency = mockk(relaxed = true) {
        every { this@mockk.id } returns id
    }

    private fun mockCurrencyId(
        rawNetworkId: String = "net-${uniqueIndex++}",
        contractAddress: String = "contract-${uniqueIndex++}",
    ): CryptoCurrency.ID = mockk(relaxed = true) {
        every { this@mockk.rawNetworkId } returns rawNetworkId
        every { this@mockk.contractAddress } returns contractAddress
    }

    private fun createCurrencyStatus(
        currency: CryptoCurrency,
        fiatAmount: BigDecimal?,
    ): CryptoCurrencyStatus {
        val value = mockk<CryptoCurrencyStatus.Value> {
            every { this@mockk.fiatAmount } returns fiatAmount
        }
        return CryptoCurrencyStatus(currency = currency, value = value)
    }

    private fun createCryptoPortfolioAccountStatus(
        currencies: List<CryptoCurrencyStatus>,
    ): AccountStatus.CryptoPortfolio {
        val account = Account.CryptoPortfolio.createMainAccount(userWalletId = userWalletId)
        return AccountStatus.CryptoPortfolio(
            account = account,
            tokenList = TokenList.Ungrouped(
                totalFiatBalance = TotalFiatBalance.Loaded(
                    amount = BigDecimal.ZERO,
                    source = StatusSource.ACTUAL,
                ),
                sortedBy = TokensSortType.NONE,
                currencies = currencies,
            ),
            priceChangeLce = Lce.Content(PriceChange(value = BigDecimal.ZERO, source = StatusSource.ACTUAL)),
        )
    }

    private fun createCryptoPortfolioAccountStatus(
        currencies: List<CryptoCurrencyStatus>,
        derivationIndexValue: Int,
    ): AccountStatus.CryptoPortfolio {
        val derivationIndex = requireNotNull(DerivationIndex(value = derivationIndexValue).getOrNull()) {
            "Invalid derivation index for test: $derivationIndexValue"
        }
        val accountId = AccountId.forCryptoPortfolio(
            userWalletId = userWalletId,
            derivationIndex = derivationIndex,
        )
        val accountName = if (derivationIndex.isMain) {
            AccountName.DefaultMain
        } else {
            requireNotNull(AccountName.Custom(value = "Account $derivationIndexValue").getOrNull()) {
                "Invalid account name for test"
            }
        }
        val account = Account.CryptoPortfolio(
            accountId = accountId,
            accountName = accountName,
            icon = CryptoPortfolioIcon.ofMainAccount(userWalletId),
            derivationIndex = derivationIndex,
        )
        return AccountStatus.CryptoPortfolio(
            account = account,
            tokenList = TokenList.Ungrouped(
                totalFiatBalance = TotalFiatBalance.Loaded(
                    amount = BigDecimal.ZERO,
                    source = StatusSource.ACTUAL,
                ),
                sortedBy = TokensSortType.NONE,
                currencies = currencies,
            ),
            priceChangeLce = Lce.Content(PriceChange(value = BigDecimal.ZERO, source = StatusSource.ACTUAL)),
        )
    }

    private fun setupSupplier(accountStatuses: List<AccountStatus>) {
        val accountStatusList = if (accountStatuses.isEmpty()) {
            null
        } else {
            AccountStatusList(
                userWalletId = userWalletId,
                accountStatuses = accountStatuses,
                totalAccounts = accountStatuses.size,
                totalArchivedAccounts = 0,
                totalFiatBalance = TotalFiatBalance.Loaded(
                    amount = BigDecimal.ZERO,
                    source = StatusSource.ACTUAL,
                ),
                sortType = TokensSortType.NONE,
                groupType = TokensGroupType.NONE,
            )
        }
        coEvery {
            singleAccountStatusListSupplier.getSyncOrNull(any<SingleAccountStatusListProducer.Params>(), any())
        } returns accountStatusList
    }

    private fun setupAvailability(currenciesAvailability: LinkedHashMap<CryptoCurrency, Boolean>) {
        val result = currenciesAvailability.map { (currency, available) ->
            val reason = if (available) {
                ScenarioUnavailabilityReason.None
            } else {
                ScenarioUnavailabilityReason.Unreachable
            }
            currency to reason
        }.toMap()
        coEvery { rampStateManager.availableForSwap(userWalletId, currenciesAvailability.keys.toList()) } returns result
    }

    // endregion
}