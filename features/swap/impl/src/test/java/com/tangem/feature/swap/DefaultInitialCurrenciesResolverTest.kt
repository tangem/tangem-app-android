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