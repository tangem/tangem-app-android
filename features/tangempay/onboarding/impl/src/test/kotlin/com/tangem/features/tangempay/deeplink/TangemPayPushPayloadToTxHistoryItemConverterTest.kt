package com.tangem.features.tangempay.deeplink

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.tangempay.model.TangemPayTxHistoryItem
import com.tangem.test.core.ProvideTestModels
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.util.Currency

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TangemPayPushPayloadToTxHistoryItemConverterTest {

    @Test
    fun `GIVEN a complete payload WHEN convertSpend THEN Spend with every field is returned`() {
        // Arrange
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
        val expected = TangemPayTxHistoryItem.Spend(
            id = "txn-123",
            jsonRepresentation = payload.toString(),
            date = DateTime.parse("2025-10-24T10:32:24.496Z").withZone(DateTimeZone.getDefault()),
            amount = BigDecimal("5.24"),
            currency = Currency.getInstance("USD"),
            authorizedAmount = BigDecimal("5.24"),
            localAmount = BigDecimal("5.24"),
            localCurrency = Currency.getInstance("USD"),
            enrichedMerchantName = "Playstation",
            merchantName = "PLAYSTATION NETWORK",
            enrichedMerchantCategory = "Gaming",
            merchantCategoryCode = "5818",
            merchantCategory = "Digital Goods",
            status = TangemPayTxHistoryItem.Status.COMPLETED,
            enrichedMerchantIconUrl = "https://example.com/icon.png",
            declinedReason = "",
        )

        // Act
        val actual = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN only the required fields WHEN convertSpend THEN the optional fields are empty`() {
        // Arrange
        val payload = requiredSpendPayload()
        val expected = TangemPayTxHistoryItem.Spend(
            id = "txn-123",
            jsonRepresentation = payload.toString(),
            date = DateTime.parse("2025-10-24T10:32:24.496Z").withZone(DateTimeZone.getDefault()),
            amount = BigDecimal("5.24"),
            currency = Currency.getInstance("USD"),
            authorizedAmount = BigDecimal.ZERO,
            localAmount = null,
            localCurrency = null,
            enrichedMerchantName = null,
            merchantName = "Test",
            enrichedMerchantCategory = null,
            merchantCategoryCode = null,
            merchantCategory = null,
            status = TangemPayTxHistoryItem.Status.COMPLETED,
            enrichedMerchantIconUrl = null,
            declinedReason = null,
        )

        // Act
        val actual = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN merchant_name is absent WHEN convertSpend THEN enriched_merchant_name is used instead`() {
        // Arrange
        val payload = requiredSpendPayload() - "merchant_name" + ("enriched_merchant_name" to "Playstation")

        // Act
        val actual = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        // Assert
        assertThat(actual?.merchantName).isEqualTo("Playstation")
    }

    @ParameterizedTest
    @MethodSource("unusableSpendPayloads")
    fun `GIVEN a required field missing or unusable WHEN convertSpend THEN returns null`(model: UnusableSpendModel) {
        // Act
        val actual = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(model.payload)

        // Assert
        assertThat(actual).isNull()
    }

    @ParameterizedTest
    @MethodSource("spendStatusModels")
    fun `GIVEN a push status WHEN convertSpend THEN it is mapped to the history status`(model: SpendStatusModel) {
        // Arrange
        val payload = requiredSpendPayload() + ("status" to model.status)

        // Act
        val actual = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        // Assert
        assertThat(actual?.status).isEqualTo(model.expected)
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

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = ["usd", "usdc", "rusd"])
    fun `GIVEN collateral push in any or no currency WHEN convertCollateral THEN Collateral in USD`(currency: String?) {
        // Arrange
        val payload = if (currency == null) collateralPayload() - "currency" else collateralPayload(currency = currency)
        val expected = expectedCollateral(
            payload = payload,
            amount = "50.00",
            type = TangemPayTxHistoryItem.Type.Deposit,
        )

        // Act
        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertCollateral(payload)

        // Assert
        assertThat(result).isEqualTo(expected)
    }

    @ParameterizedTest
    @ValueSource(strings = ["type", "transaction_id", "amount", "posted_at"])
    fun `GIVEN required field missing WHEN convertCollateral THEN returns null`(missingKey: String) {
        // Arrange
        val payload = collateralPayload() - missingKey

        // Act
        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertCollateral(payload)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN an empty payload WHEN convertCollateral THEN returns null`() {
        // Act
        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertCollateral(emptyMap())

        // Assert
        assertThat(result).isNull()
    }

    @ParameterizedTest
    @NullAndEmptySource
    fun `GIVEN no transaction_hash WHEN convertCollateral THEN Collateral without hash`(transactionHash: String?) {
        // Arrange
        val payload = if (transactionHash == null) {
            collateralPayload() - "transaction_hash"
        } else {
            collateralPayload() + ("transaction_hash" to transactionHash)
        }
        val expected = expectedCollateral(
            payload = payload,
            amount = "50.00",
            type = TangemPayTxHistoryItem.Type.Deposit,
            transactionHash = null,
        )

        // Act
        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertCollateral(payload)

        // Assert
        assertThat(result).isEqualTo(expected)
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

    private fun requiredSpendPayload() = mapOf(
        "transaction_id" to "txn-123",
        "amount" to "5.24",
        "currency" to "usd",
        "merchant_name" to "Test",
        "status" to "completed",
        "authorized_at" to "2025-10-24T10:32:24.496Z",
    )

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

    private fun collateralPayload(
        type: String = "collateral_deposit",
        amount: String = "50.00",
        currency: String = "usdc",
    ) = mapOf(
        "type" to type,
        "transaction_id" to "col-123",
        "amount" to amount,
        "currency" to currency,
        "transaction_hash" to "0xabc123",
        "posted_at" to "2025-10-25T19:22:22.597Z",
    )

    private fun expectedCollateral(
        payload: Map<String, String>,
        amount: String,
        type: TangemPayTxHistoryItem.Type,
        transactionHash: String? = "0xabc123",
    ) = TangemPayTxHistoryItem.Collateral(
        id = "col-123",
        jsonRepresentation = payload.toString(),
        date = DateTime.parse("2025-10-25T19:22:22.597Z").withZone(DateTimeZone.getDefault()),
        amount = BigDecimal(amount),
        currency = Currency.getInstance("USD"),
        transactionHash = transactionHash,
        type = type,
    )

    private fun unusableSpendPayloads() = listOf(
        UnusableSpendModel(field = "transaction_id missing", payload = requiredSpendPayload() - "transaction_id"),
        UnusableSpendModel(field = "transaction_id empty", payload = requiredSpendPayload() + ("transaction_id" to "")),
        UnusableSpendModel(field = "amount missing", payload = requiredSpendPayload() - "amount"),
        UnusableSpendModel(field = "amount not a number", payload = requiredSpendPayload() + ("amount" to "abc")),
        UnusableSpendModel(field = "currency missing", payload = requiredSpendPayload() - "currency"),
        UnusableSpendModel(field = "currency invalid", payload = requiredSpendPayload() + ("currency" to "INVALID")),
        UnusableSpendModel(field = "merchant_name missing", payload = requiredSpendPayload() - "merchant_name"),
        UnusableSpendModel(field = "status missing", payload = requiredSpendPayload() - "status"),
        UnusableSpendModel(field = "authorized_at missing", payload = requiredSpendPayload() - "authorized_at"),
        UnusableSpendModel(field = "empty payload", payload = emptyMap()),
    )

    private fun spendStatusModels() = listOf(
        SpendStatusModel(status = "pending", expected = TangemPayTxHistoryItem.Status.PENDING),
        SpendStatusModel(status = "reserved", expected = TangemPayTxHistoryItem.Status.RESERVED),
        SpendStatusModel(status = "completed", expected = TangemPayTxHistoryItem.Status.COMPLETED),
        SpendStatusModel(status = "declined", expected = TangemPayTxHistoryItem.Status.DECLINED),
        SpendStatusModel(status = "reversed", expected = TangemPayTxHistoryItem.Status.REVERSED),
        SpendStatusModel(status = "unknown_value", expected = TangemPayTxHistoryItem.Status.UNKNOWN),
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

    data class UnusableSpendModel(val field: String, val payload: Map<String, String>)

    data class SpendStatusModel(val status: String, val expected: TangemPayTxHistoryItem.Status)

    data class CollateralTypeModel(
        val type: String,
        val amount: String,
        val expectedType: TangemPayTxHistoryItem.Type,
        val expectedAmount: String,
    )
}