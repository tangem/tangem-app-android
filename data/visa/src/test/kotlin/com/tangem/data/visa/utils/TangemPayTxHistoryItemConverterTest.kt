package com.tangem.data.visa.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.spend.datasource.pay.models.response.TangemPayTxHistoryResponse
import com.tangem.spend.datasource.pay.models.response.TangemPayTxHistoryResponse.Transaction
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Currency

internal class TangemPayTxHistoryItemConverterTest {

    private val json = NetworkJsonFactory.create()
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)

    private val converter = TangemPayTxHistoryItemConverter(json = json, analytics = analytics)

    @BeforeEach
    fun resetMocks() {
        clearMocks(analytics)
    }

    @Test
    fun `GIVEN refund transaction WHEN convert THEN spend item with negative amounts`() {
        // Arrange
        val refund = refund()
        val transaction = Transaction.Refund(id = TX_ID, refund = refund)
        val expected = TangemPayTxHistoryItem.Spend(
            id = TX_ID,
            jsonRepresentation = json.encodeToString(TangemPayTxHistoryResponse.RefundData.serializer(), refund),
            date = POSTED_AT.withZone(DateTimeZone.getDefault()),
            amount = BigDecimal("-15.00"),
            currency = Currency.getInstance("USD"),
            authorizedAmount = BigDecimal.ZERO,
            localAmount = BigDecimal("-13.50"),
            localCurrency = Currency.getInstance("EUR"),
            enrichedMerchantName = "Spotify",
            merchantName = "Spotify Premium Renewal",
            enrichedMerchantCategory = "Music",
            merchantCategoryCode = "5812",
            merchantCategory = "Entertainment",
            status = TangemPayTxHistoryItem.Status.COMPLETED,
            enrichedMerchantIconUrl = "https://example.com/icon.png",
            declinedReason = null,
            cardName = "Plus Card 1",
            cardNumberLast4 = "6460",
            cashback = TangemPayTxHistoryItem.Cashback(
                status = TangemPayTxHistoryItem.Cashback.Status.CONFIRMED,
                amount = BigDecimal("-7.50"),
                currency = Currency.getInstance("USD"),
                isCapTrimmed = false,
                exclusionReason = null,
            ),
        )

        // Act
        val actual = converter.convert(transaction)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN refund with positive amounts and no posted date WHEN convert THEN sign forced and authorized date used`() {
        // Arrange
        val refund = refund(
            amount = BigDecimal("15.00"),
            localAmount = BigDecimal("13.50"),
            cashback = BigDecimal("7.50"),
            postedAt = null,
        )
        val transaction = Transaction.Refund(id = TX_ID, refund = refund)

        // Act
        val actual = converter.convert(transaction) as TangemPayTxHistoryItem.Spend

        // Assert
        assertThat(actual.amount).isEqualTo(BigDecimal("-15.00"))
        assertThat(actual.localAmount).isEqualTo(BigDecimal("-13.50"))
        assertThat(actual.cashback?.amount).isEqualTo(BigDecimal("-7.50"))
        assertThat(actual.date).isEqualTo(AUTHORIZED_AT.withZone(DateTimeZone.getDefault()))
    }

    @Test
    fun `GIVEN transaction of unknown type WHEN convert THEN null and the type is reported once`() {
        // Arrange
        val transaction = Transaction.Unknown(id = TX_ID, type = "transfer")

        // Act
        val first = converter.convert(transaction)
        val second = converter.convert(transaction.copy(id = "other"))

        // Assert
        assertThat(first).isNull()
        assertThat(second).isNull()
        verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.UnknownTransactionType(type = "transfer")) }
    }

    @Test
    fun `GIVEN transactions of two unknown types WHEN convert THEN each type is reported`() {
        // Act
        converter.convert(Transaction.Unknown(id = TX_ID, type = "transfer"))
        converter.convert(Transaction.Unknown(id = "other", type = null))

        // Assert
        verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.UnknownTransactionType(type = "transfer")) }
        verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.UnknownTransactionType(type = null)) }
    }

    @Test
    fun `GIVEN known type without payload WHEN convert THEN nothing is reported`() {
        // Act
        converter.convert(Transaction.Payment(id = TX_ID, payment = null))

        // Assert
        verify(exactly = 0) { analytics.send(any()) }
    }

    @Test
    fun `GIVEN known type without payload WHEN convert THEN null`() {
        // Arrange
        val transaction = Transaction.Refund(id = TX_ID, refund = null)

        // Act
        val actual = converter.convert(transaction)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN local currency that is not an ISO code WHEN convert THEN the local currency is dropped`() {
        // Arrange
        val transaction = Transaction.Refund(id = TX_ID, refund = refund(localCurrency = UNSUPPORTED_CURRENCY))

        // Act
        val actual = converter.convert(transaction) as TangemPayTxHistoryItem.Spend

        // Assert
        assertThat(actual.localCurrency).isNull()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class UnsupportedCurrency {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN currency that is not an ISO code WHEN convert THEN the transaction is kept in USD`(
            model: CurrencyModel,
        ) {
            // Act
            val actual = converter.convert(model.transaction)

            // Assert
            assertThat(actual?.currency).isEqualTo(Currency.getInstance("USD"))
        }

        private fun provideTestModels() = listOf(
            CurrencyModel(
                name = "spend",
                transaction = Transaction.Spend(
                    id = TX_ID,
                    spend = TangemPayTxHistoryResponse.SpendData(
                        amount = BigDecimal("-15.00"),
                        currency = UNSUPPORTED_CURRENCY,
                        receipt = false,
                        merchantName = "Spotify",
                        status = "COMPLETED",
                        authorizedAt = AUTHORIZED_AT,
                        postedAt = POSTED_AT,
                    ),
                ),
            ),
            CurrencyModel(
                name = "refund",
                transaction = Transaction.Refund(id = TX_ID, refund = refund(currency = UNSUPPORTED_CURRENCY)),
            ),
            CurrencyModel(
                name = "payment",
                transaction = Transaction.Payment(
                    id = TX_ID,
                    payment = TangemPayTxHistoryResponse.PaymentData(
                        amount = BigDecimal("50"),
                        currency = UNSUPPORTED_CURRENCY,
                        status = "COMPLETED",
                        postedAt = POSTED_AT,
                    ),
                ),
            ),
            CurrencyModel(
                name = "fee",
                transaction = Transaction.Fee(
                    id = TX_ID,
                    fee = TangemPayTxHistoryResponse.FeeData(
                        amount = BigDecimal("2"),
                        currency = UNSUPPORTED_CURRENCY,
                        postedAt = POSTED_AT,
                    ),
                ),
            ),
        )
    }

    internal data class CurrencyModel(val name: String, val transaction: Transaction) {
        override fun toString(): String = name
    }

    private fun refund(
        amount: BigDecimal = BigDecimal("-15.00"),
        currency: String = "USD",
        localAmount: BigDecimal? = BigDecimal("-13.50"),
        localCurrency: String? = "EUR",
        cashback: BigDecimal? = BigDecimal("-7.50"),
        postedAt: DateTime? = POSTED_AT,
    ) = TangemPayTxHistoryResponse.RefundData(
        amount = amount,
        currency = currency,
        localAmount = localAmount,
        localCurrency = localCurrency,
        sourceTransactionId = "fd5861d7-6e9d-42b6-ab7e-9687a861b18e",
        merchantName = "Spotify Premium Renewal",
        merchantCategory = "Entertainment",
        merchantCategoryCode = "5812",
        merchantId = "bb7baadb-9f6d-59d0-af03-00f42243e917",
        enrichedMerchantIcon = "https://example.com/icon.png",
        enrichedMerchantName = "Spotify",
        enrichedMerchantCategory = "Music",
        cardId = "21c88a66-ddc7-4d4c-9bd7-bf5342bf9efc",
        cardType = "virtual",
        cardDisplayName = "Plus Card 1",
        cardNumberEnd = "6460",
        status = "COMPLETED",
        authorizedAt = AUTHORIZED_AT,
        postedAt = postedAt,
        cashback = cashback,
        cashbackStatus = "confirmed",
        cashbackCurrencyCode = "USD",
    )

    private companion object {
        const val TX_ID = "2257c149-33f8-4439-b9b8-57ed85ef35a3"
        const val UNSUPPORTED_CURRENCY = "USDC"
        val AUTHORIZED_AT: DateTime = DateTime.parse("2026-08-28T12:08:53.939Z")
        val POSTED_AT: DateTime = DateTime.parse("2026-08-28T12:08:54.013Z")
    }
}