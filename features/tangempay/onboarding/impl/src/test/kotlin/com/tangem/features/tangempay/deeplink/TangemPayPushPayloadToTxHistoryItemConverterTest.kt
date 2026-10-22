package com.tangem.features.tangempay.deeplink

import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.util.Currency

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TangemPayPushPayloadToTxHistoryItemConverterTest {

    @Test
    fun `convertSpend returns Spend tx with all fields when payload is complete`() {
        val payload = mapOf(
            "transaction_id" to "txn-123",
            "amount" to "5.24",
            "currency" to "usd",
            "local_amount" to "5.24",
            "local_currency" to "usd",
            "authorized_amount" to "5.24",
            "merchant_name" to "PLAYSTATION NETWORK",
            "enriched_merchant_name" to "Playstation",
            "enriched_merchant_icon" to "https://example.com/icon.png",
            "enriched_merchant_category" to "Gaming",
            "merchant_category" to "Digital Goods",
            "merchant_category_code" to "5818",
            "status" to "completed",
            "declined_reason" to "",
            "authorized_at" to "2025-10-24T10:32:24.496Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        assertThat(result).isNotNull()
        assertThat(result!!.id).isEqualTo("txn-123")
        assertThat(result.amount).isEqualTo(BigDecimal("5.24"))
        assertThat(result.currency.currencyCode).isEqualTo("USD")
        assertThat(result.localAmount).isEqualTo(BigDecimal("5.24"))
        assertThat(result.localCurrency?.currencyCode).isEqualTo("USD")
        assertThat(result.authorizedAmount).isEqualTo(BigDecimal("5.24"))
        assertThat(result.merchantName).isEqualTo("PLAYSTATION NETWORK")
        assertThat(result.enrichedMerchantName).isEqualTo("Playstation")
        assertThat(result.enrichedMerchantIconUrl).isEqualTo("https://example.com/icon.png")
        assertThat(result.enrichedMerchantCategory).isEqualTo("Gaming")
        assertThat(result.merchantCategory).isEqualTo("Digital Goods")
        assertThat(result.merchantCategoryCode).isEqualTo("5818")
        assertThat(result.status).isEqualTo(TangemPayTxHistoryItem.Status.COMPLETED)
    }

    @Test
    fun `convertSpend returns null when transaction_id is missing`() {
        val payload = mapOf(
            "amount" to "5.24",
            "currency" to "usd",
            "merchant_name" to "Test",
            "status" to "completed",
            "authorized_at" to "2025-10-24T10:32:24.496Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        assertThat(result).isNull()
    }

    @Test
    fun `convertSpend returns null when amount is missing`() {
        val payload = mapOf(
            "transaction_id" to "txn-123",
            "currency" to "usd",
            "merchant_name" to "Test",
            "status" to "completed",
            "authorized_at" to "2025-10-24T10:32:24.496Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        assertThat(result).isNull()
    }

    @Test
    fun `convertSpend returns null when currency is missing`() {
        val payload = mapOf(
            "transaction_id" to "txn-123",
            "amount" to "5.24",
            "merchant_name" to "Test",
            "status" to "completed",
            "authorized_at" to "2025-10-24T10:32:24.496Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        assertThat(result).isNull()
    }

    @Test
    fun `convertSpend returns null when merchant_name is missing`() {
        val payload = mapOf(
            "transaction_id" to "txn-123",
            "amount" to "5.24",
            "currency" to "usd",
            "status" to "completed",
            "authorized_at" to "2025-10-24T10:32:24.496Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        assertThat(result).isNull()
    }

    @Test
    fun `convertSpend falls back to enriched_merchant_name when merchant_name is absent`() {
        val payload = mapOf(
            "transaction_id" to "txn-123",
            "amount" to "5.24",
            "currency" to "usd",
            "enriched_merchant_name" to "Playstation",
            "status" to "completed",
            "authorized_at" to "2025-10-24T10:32:24.496Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        assertThat(result).isNotNull()
        assertThat(result!!.merchantName).isEqualTo("Playstation")
    }

    @Test
    fun `convertSpend returns null when status is missing`() {
        val payload = mapOf(
            "transaction_id" to "txn-123",
            "amount" to "5.24",
            "currency" to "usd",
            "merchant_name" to "Test",
            "authorized_at" to "2025-10-24T10:32:24.496Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        assertThat(result).isNull()
    }

    @Test
    fun `convertSpend returns null when authorized_at is missing`() {
        val payload = mapOf(
            "transaction_id" to "txn-123",
            "amount" to "5.24",
            "currency" to "usd",
            "merchant_name" to "Test",
            "status" to "completed",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        assertThat(result).isNull()
    }

    @Test
    fun `convertSpend returns null when amount is not a number`() {
        val payload = mapOf(
            "transaction_id" to "txn-123",
            "amount" to "abc",
            "currency" to "usd",
            "merchant_name" to "Test",
            "status" to "completed",
            "authorized_at" to "2025-10-24T10:32:24.496Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        assertThat(result).isNull()
    }

    @Test
    fun `convertSpend returns null when currency is invalid`() {
        val payload = mapOf(
            "transaction_id" to "txn-123",
            "amount" to "5.24",
            "currency" to "INVALID",
            "merchant_name" to "Test",
            "status" to "completed",
            "authorized_at" to "2025-10-24T10:32:24.496Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        assertThat(result).isNull()
    }

    @Test
    fun `convertSpend maps all statuses correctly`() {
        fun spendPayloadWithStatus(status: String) = mapOf(
            "transaction_id" to "txn-123",
            "amount" to "1.00",
            "currency" to "usd",
            "merchant_name" to "Test",
            "status" to status,
            "authorized_at" to "2025-10-24T10:32:24.496Z",
        )

        assertThat(TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(spendPayloadWithStatus("pending"))!!.status)
            .isEqualTo(TangemPayTxHistoryItem.Status.PENDING)
        assertThat(TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(spendPayloadWithStatus("reserved"))!!.status)
            .isEqualTo(TangemPayTxHistoryItem.Status.RESERVED)
        assertThat(TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(spendPayloadWithStatus("completed"))!!.status)
            .isEqualTo(TangemPayTxHistoryItem.Status.COMPLETED)
        assertThat(TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(spendPayloadWithStatus("declined"))!!.status)
            .isEqualTo(TangemPayTxHistoryItem.Status.DECLINED)
        assertThat(TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(spendPayloadWithStatus("reversed"))!!.status)
            .isEqualTo(TangemPayTxHistoryItem.Status.REVERSED)
        assertThat(TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(spendPayloadWithStatus("unknown_value"))!!.status)
            .isEqualTo(TangemPayTxHistoryItem.Status.UNKNOWN)
    }

    @Test
    fun `convertSpend returns null when transaction_id is empty`() {
        val payload = mapOf(
            "transaction_id" to "",
            "amount" to "5.24",
            "currency" to "usd",
            "merchant_name" to "Test",
            "status" to "completed",
            "authorized_at" to "2025-10-24T10:32:24.496Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        assertThat(result).isNull()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN collateral push type WHEN convertCollateral THEN type and amount sign follow the push type`(
        model: CollateralTypeModel,
    ) {
        // Arrange
        val payload = collateralPayload(type = model.type, amount = model.amount)
        val expected = expectedCollateral(payload = payload, amount = model.expectedAmount, type = model.expectedType)

        // Act
        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertCollateral(payload)

        // Assert
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `GIVEN cashback topup in usdc WHEN convertCollateral THEN Cashback in USD`() {
        // Arrange
        val payload = mapOf(
            "customer_id" to "customer-123",
            "customer_wallet_id" to "wallet-123",
            "type" to "cashback_topup",
            "transaction_id" to "col-321",
            "amount" to "0.30",
            "balance" to "85.18",
            "currency" to "usdc",
            "transaction_hash" to "0xcashback",
            "posted_at" to "2026-10-21T13:21:51.055",
        )
        val expected = TangemPayTxHistoryItem.Collateral(
            id = "col-321",
            jsonRepresentation = payload.toString(),
            date = DateTime.parse("2026-10-21T13:21:51.055").withZone(DateTimeZone.getDefault()),
            amount = BigDecimal("0.30"),
            currency = Currency.getInstance("USD"),
            transactionHash = "0xcashback",
            type = TangemPayTxHistoryItem.Type.Cashback,
        )

        // Act
        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertCollateral(payload)

        // Assert
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `GIVEN no currency WHEN convertCollateral THEN Collateral in USD`() {
        // Arrange
        val payload = collateralPayload() - "currency"

        // Act
        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertCollateral(payload)

        // Assert
        assertThat(result?.currency).isEqualTo(Currency.getInstance("USD"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["type", "transaction_id", "amount", "transaction_hash", "posted_at"])
    fun `GIVEN required field missing WHEN convertCollateral THEN returns null`(missingKey: String) {
        // Arrange
        val payload = collateralPayload() - missingKey

        // Act
        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertCollateral(payload)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN empty transaction_hash WHEN convertCollateral THEN returns null`() {
        // Arrange
        val payload = collateralPayload() + ("transaction_hash" to "")

        // Act
        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertCollateral(payload)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN spend push type WHEN convertCollateral THEN returns null`() {
        // Arrange
        val payload = collateralPayload(type = "transaction_spend")

        // Act
        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertCollateral(payload)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `convertSpend handles optional fields as null`() {
        val payload = mapOf(
            "transaction_id" to "txn-123",
            "amount" to "5.24",
            "currency" to "usd",
            "merchant_name" to "Test",
            "status" to "completed",
            "authorized_at" to "2025-10-24T10:32:24.496Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        assertThat(result).isNotNull()
        assertThat(result!!.localAmount).isNull()
        assertThat(result.localCurrency).isNull()
        assertThat(result.enrichedMerchantName).isNull()
        assertThat(result.enrichedMerchantIconUrl).isNull()
        assertThat(result.enrichedMerchantCategory).isNull()
        assertThat(result.merchantCategory).isNull()
        assertThat(result.merchantCategoryCode).isNull()
        assertThat(result.declinedReason).isNull()
    }

    @Test
    fun `convertSpend returns null for empty payload`() {
        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(emptyMap())
        assertThat(result).isNull()
    }

    @Test
    fun `convertCollateral returns null for empty payload`() {
        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertCollateral(emptyMap())
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN refund type WHEN convertSpend THEN amount and local amount are negative`() {
        // Arrange
        val payload = spendPayload(type = "transaction_spend_refund", amount = "12.50", localAmount = "11.80")

        // Act
        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        // Assert
        assertThat(result!!.amount).isEqualTo(BigDecimal("-12.50"))
        assertThat(result.localAmount).isEqualTo(BigDecimal("-11.80"))
    }

    @Test
    fun `GIVEN refund type with already negative amount WHEN convertSpend THEN sign is kept`() {
        // Arrange
        val payload = spendPayload(type = "transaction_spend_refund", amount = "-12.50", localAmount = "-11.80")

        // Act
        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        // Assert
        assertThat(result!!.amount).isEqualTo(BigDecimal("-12.50"))
        assertThat(result.localAmount).isEqualTo(BigDecimal("-11.80"))
    }

    @Test
    fun `GIVEN spend type WHEN convertSpend THEN amount is left untouched`() {
        // Arrange
        val payload = spendPayload(type = "transaction_spend", amount = "12.50", localAmount = "11.80")

        // Act
        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        // Assert
        assertThat(result!!.amount).isEqualTo(BigDecimal("12.50"))
        assertThat(result.localAmount).isEqualTo(BigDecimal("11.80"))
    }

    private fun spendPayload(type: String, amount: String, localAmount: String) = mapOf(
        "type" to type,
        "transaction_id" to "txn-123",
        "amount" to amount,
        "currency" to "usd",
        "local_amount" to localAmount,
        "local_currency" to "eur",
        "merchant_name" to "Tangem Coffee",
        "status" to "completed",
        "authorized_at" to "2025-10-24T10:32:24.496Z",
    )

    private fun collateralPayload(type: String = "collateral_deposit", amount: String = "50.00") = mapOf(
        "type" to type,
        "transaction_id" to "col-123",
        "amount" to amount,
        "currency" to "usdc",
        "transaction_hash" to "0xabc123",
        "posted_at" to "2025-10-25T19:22:22.597Z",
    )

    private fun expectedCollateral(
        payload: Map<String, String>,
        amount: String,
        type: TangemPayTxHistoryItem.Type,
    ) = TangemPayTxHistoryItem.Collateral(
        id = "col-123",
        jsonRepresentation = payload.toString(),
        date = DateTime.parse("2025-10-25T19:22:22.597Z").withZone(DateTimeZone.getDefault()),
        amount = BigDecimal(amount),
        currency = Currency.getInstance("USD"),
        transactionHash = "0xabc123",
        type = type,
    )

    private fun provideTestModels() = listOf(
        CollateralTypeModel(
            type = "collateral_deposit",
            amount = "50.00",
            expectedType = TangemPayTxHistoryItem.Type.Deposit,
            expectedAmount = "50.00",
        ),
        CollateralTypeModel(
            type = "collateral_withdraw",
            amount = "50.00",
            expectedType = TangemPayTxHistoryItem.Type.Withdrawal,
            expectedAmount = "-50.00",
        ),
        CollateralTypeModel(
            type = "collateral_withdraw",
            amount = "-50.00",
            expectedType = TangemPayTxHistoryItem.Type.Withdrawal,
            expectedAmount = "-50.00",
        ),
        CollateralTypeModel(
            type = "cashback_topup",
            amount = "0.30",
            expectedType = TangemPayTxHistoryItem.Type.Cashback,
            expectedAmount = "0.30",
        ),
    )

    data class CollateralTypeModel(
        val type: String,
        val amount: String,
        val expectedType: TangemPayTxHistoryItem.Type,
        val expectedAmount: String,
    )
}
