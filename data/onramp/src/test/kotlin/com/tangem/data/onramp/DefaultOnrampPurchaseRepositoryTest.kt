package com.tangem.data.onramp

import android.net.Uri
import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.common.txhistory.ExpressHistoryRepository
import com.tangem.datasource.api.common.response.map
import com.tangem.datasource.local.onramp.countries.DefaultOnrampCountriesStore
import com.tangem.datasource.local.onramp.country.DefaultOnrampCurrentCountryByIPStore
import com.tangem.datasource.local.onramp.currencies.DefaultOnrampCurrenciesStore
import com.tangem.datasource.local.onramp.pairs.DefaultOnrampPairsStore
import com.tangem.datasource.local.onramp.paymentmethods.DefaultOnrampPaymentMethodsStore
import com.tangem.datasource.local.onramp.quotes.DefaultOnrampQuotesStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onramp.model.*
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.onramp.model.error.OnrampRedirectError
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.grow.datasource.crypto.DataSignatureVerifier
import com.tangem.grow.datasource.express.ExpressUtils
import com.tangem.grow.datasource.onramp.OnrampApi
import com.tangem.grow.datasource.onramp.models.response.OnrampDataResponse
import com.tangem.grow.datasource.preferences.GrowPreferencesKeys
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.core.datastore.createAppPreferencesStore
import com.tangem.test.core.datastore.legacyDataStoreMoshi
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.joda.time.DateTimeUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

internal class DefaultOnrampPurchaseRepositoryTest {

    private val api: OnrampApi = mockk()
    private val verifier: DataSignatureVerifier = mockk()
    private val wallets: WalletManagersFacade = mockk()
    private val history: ExpressHistoryRepository = mockk(relaxUnitFun = true)
    private val walletId = UserWalletId("0011")
    private val wallet: UserWallet = mockk { every { walletId } returns this@DefaultOnrampPurchaseRepositoryTest.walletId }
    private val crypto: CryptoCurrency.Coin = mockk {
        every { id } returns CryptoCurrency.ID.fromValue("coin⟨ethereum⟩ethereum")
        every { network.rawId } returns "ethereum"
        every { decimals } returns 18
    }
    private val currency = OnrampCurrency("Dollar", "USD", null, 2, "$")
    private val country = OnrampCountry("USA-US", "US", "US", "", "USA", "NA", currency, true)
    private val payment = OnrampPaymentMethod("card", "Card", "", PaymentMethodType.CARD)
    private val quote = OnrampProviderWithQuote.Data(
        OnrampProvider("provider", OnrampProviderInfo("Provider", "image", null, null), listOf(payment)),
        payment, OnrampAmount("ETH", BigDecimal("0.01"), 18), OnrampAmount("USD", BigDecimal("12.34"), 2),
    )
    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val repository = DefaultOnrampRepository(
        onrampApi = api,
        expressApi = mockk(),
        dispatchers = dispatchers,
        appPreferencesStore = createAppPreferencesStore(
            legacyDataStoreMoshi, dispatchers, MockStateDataStore(emptyPreferences()),
        ),
        growPreferencesKeys = GrowPreferencesKeys(),
        expressUtils = mockk<ExpressUtils> { coEvery { getRefCode(wallet) } returns null },
        paymentMethodsStore = DefaultOnrampPaymentMethodsStore(RuntimeSharedMapStore()),
        onrampCurrentCountryByIPStore = DefaultOnrampCurrentCountryByIPStore(RuntimeSharedStore()),
        pairsStore = DefaultOnrampPairsStore(RuntimeSharedMapStore()),
        quotesStore = DefaultOnrampQuotesStore(RuntimeSharedMapStore()),
        countriesStore = DefaultOnrampCountriesStore(RuntimeSharedMapStore()),
        currenciesStore = DefaultOnrampCurrenciesStore(RuntimeSharedMapStore()),
        walletManagersFacade = wallets,
        dataSignatureVerifier = verifier,
        expressHistoryDao = mockk(relaxUnitFun = true),
        expressHistoryRepository = history,
        moshi = legacyDataStoreMoshi,
    )

    @BeforeEach
    fun setUp() {
        DateTimeUtils.setCurrentMillisFixed(123L)
        mockkConstructor(Uri.Builder::class)
        every { anyConstructed<Uri.Builder>().path(any()) } answers { self as Uri.Builder }
        every { anyConstructed<Uri.Builder>().appendPath(any()) } answers { self as Uri.Builder }
        every { anyConstructed<Uri.Builder>().build() } returns mockk {
            every { path } returns "https://tangem.com/onramp/provider"
        }
        coEvery { wallets.getDefaultAddress(walletId, crypto.network) } returns "address"
        every { verifier.verifySignature("signature", any()) } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkConstructor(Uri.Builder::class)
        DateTimeUtils.setCurrentMillisSystem()
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN valid signed data WHEN purchase requested THEN request and complete transaction match`(dark: Boolean) =
        runTest {
            // Arrange
            repository.saveDefaultCountry(country)
            coEvery {
                api.getData(
                    "0011", null, "USD", 2, "0", "ethereum", "card", "US", "1234", 18,
                    "provider", "address", "https://tangem.com/onramp/provider", null,
                    if (dark) "dark" else "light", any(),
                )
            } answers { response(requestId = arg(15)) }

            // Act
            val actual = repository.getOnrampData(wallet, crypto, quote, dark)

            // Assert
            assertThat(actual).isEqualTo(
                OnrampTransaction(
                    txId = "tx", userWalletId = walletId, fromAmount = BigDecimal("12.34"),
                    fromCurrency = currency, toAmount = BigDecimal("0.01"), toCurrencyId = crypto.id.value,
                    status = OnrampStatus.Status.Created, externalTxUrl = "external-url", externalTxId = "external",
                    timestamp = 123L, providerName = "Provider", providerImageUrl = "image", providerType = "ONRAMP",
                    redirectUrl = "https://widget", paymentMethod = "Card", residency = "US",
                ),
            )
            verify(exactly = 1) { verifier.verifySignature("signature", any()) }
        }

    @ParameterizedTest
    @ValueSource(strings = ["invalidSignature", "wrongRequestId", "malformedJson"])
    fun `GIVEN invalid redirect data WHEN purchase requested THEN specific validation failure returned`(kind: String) =
        runTest {
            // Arrange
            repository.saveDefaultCountry(country)
            stubData {
                when (kind) {
                    "malformedJson" -> response(json = "not-json")
                    else -> response(requestId = "mismatch")
                }
            }
            every { verifier.verifySignature("signature", any()) } returns (kind != "invalidSignature")

            // Act
            val actual = runCatching { repository.getOnrampData(wallet, crypto, quote, false) }.exceptionOrNull()

            // Assert
            val expectedType = when (kind) {
                "invalidSignature" -> OnrampRedirectError.VerificationFailed::class.java
                "wrongRequestId" -> OnrampRedirectError.WrongRequestId::class.java
                else -> com.squareup.moshi.JsonEncodingException::class.java
            }
            assertThat(actual).isInstanceOf(expectedType)
            verify(exactly = 1) { verifier.verifySignature("signature", any()) }
        }

    @Test
    fun `GIVEN missing address WHEN purchase requested THEN no data request or signature verification`() = runTest {
        // Arrange
        coEvery { wallets.getDefaultAddress(walletId, crypto.network) } returns null

        // Act
        val actual = runCatching { repository.getOnrampData(wallet, crypto, quote, false) }.exceptionOrNull()

        // Assert
        assertThat(actual).hasMessageThat().isEqualTo("Address must not be null")
        coVerify(exactly = 0) {
            api.getData(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any())
        }
        verify { verifier wasNot Called }
    }

    @Test
    fun `GIVEN data API error WHEN purchase requested THEN error propagated without signature verification`() = runTest {
        // Arrange
        repository.saveDefaultCountry(country)
        val error = ApiResponseError.HttpException(ApiResponseError.HttpException.Code.BAD_REQUEST, "failed", null)
        stubData { ApiResponse.Error(error).map<Nothing, OnrampDataResponse> { it } }

        // Act
        val actual = runCatching { repository.getOnrampData(wallet, crypto, quote, false) }.exceptionOrNull()

        // Assert
        assertThat(actual).isInstanceOf(ApiResponseError.HttpException::class.java)
        assertThat(actual).hasMessageThat().isEqualTo(error.message)
        verify { verifier wasNot Called }
    }

    private fun stubData(response: () -> ApiResponse<OnrampDataResponse>) {
        coEvery {
            api.getData(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any())
        } answers { response() }
    }

    private fun response(
        requestId: String = "request",
        json: String = """{"widgetUrl":"https://widget","requestId":"$requestId","externalTxId":"external","externalTxUrl":"external-url"}""",
    ) = ApiResponse.Success(OnrampDataResponse("tx", json, "signature"))
}