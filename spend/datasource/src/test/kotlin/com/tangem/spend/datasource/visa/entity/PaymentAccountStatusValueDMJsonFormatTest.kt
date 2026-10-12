package com.tangem.spend.datasource.visa.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.KotlinxDataStoreSerializer
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.test.core.ProvideTestModels
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * Guards the stored JSON format of every [PaymentAccountStatusValueDM] subtype. The expected strings are what the
 * store writes to `payment_account_statuses_v2`, so statuses cached by one app version must read back unchanged in
 * the next one.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PaymentAccountStatusValueDMJsonFormatTest {

    private val json = KotlinxDataStoreSerializer.jsonBuilder { classDiscriminator = "__type" }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN json in the stored format WHEN decode THEN reads the same status`(model: FormatModel) {
        // Act
        val actual = json.decodeFromString(PaymentAccountStatusValueDM.serializer(), model.json)

        // Assert
        assertThat(actual).isEqualTo(model.status)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN status WHEN encode THEN writes the stored format`(model: FormatModel) {
        // Act
        val actual = json.encodeToString(PaymentAccountStatusValueDM.serializer(), model.status)

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    @Test
    fun `GIVEN statuses keyed by wallet WHEN encode and decode as map THEN every entry is restored`() {
        // Arrange
        val serializer = MapSerializer(String.serializer(), PaymentAccountStatusValueDM.serializer())
        val statuses = provideTestModels().mapIndexed { index, model -> "wallet_$index" to model.status }.toMap()

        // Act
        val restored = json.decodeFromString(serializer, json.encodeToString(serializer, statuses))

        // Assert
        assertThat(restored).isEqualTo(statuses)
    }

    @Test
    fun `GIVEN status in the previous Moshi format WHEN decode THEN it fails`() {
        // Act
        val error = runCatching {
            json.decodeFromString(PaymentAccountStatusValueDM.serializer(), """{"not_created":true}""")
        }.exceptionOrNull()

        // Assert
        assertThat(error).isNotNull()
    }

    private fun provideTestModels() = listOf(
        FormatModel(status = PaymentAccountStatusValueDM.Empty, json = """{"__type":"empty"}"""),
        FormatModel(status = PaymentAccountStatusValueDM.NotCreated, json = """{"__type":"not_created"}"""),
        FormatModel(
            status = PaymentAccountStatusValueDM.UnderReview(kycStatus = KycStatus.PENDING, customerId = "cust-1"),
            json = """{"__type":"kyc_status","kyc_status":"in_progress","customer_id":"cust-1"}""",
        ),
        FormatModel(status = PaymentAccountStatusValueDM.IssuingCard, json = """{"__type":"issuing_card"}"""),
        FormatModel(
            status = PaymentAccountStatusValueDM.ActiveAccount(
                customerId = "cust-1",
                currencyCode = "USD",
                paymentAccountAddress = "0xpay",
                fiatBalance = FIAT_BALANCE,
                fiatRate = BigDecimal("0.99"),
                cards = listOf(CARD),
            ),
            json = """{"__type":"active_account","customer_id":"cust-1","currency_code":"USD",""" +
                """"payment_account_address":"0xpay","fiat_balance":$FIAT_BALANCE_JSON,"fiat_rate":"0.99",""" +
                """"cards":[$CARD_JSON]}""",
        ),
        FormatModel(
            status = PaymentAccountStatusValueDM.ActiveAccount(
                customerId = "cust-2",
                currencyCode = null,
                paymentAccountAddress = null,
                fiatBalance = null,
                fiatRate = null,
                cards = emptyList(),
            ),
            json = """{"__type":"active_account","customer_id":"cust-2","currency_code":null,""" +
                """"payment_account_address":null,"fiat_balance":null,"fiat_rate":null,"cards":[]}""",
        ),
        FormatModel(
            status = PaymentAccountStatusValueDM.CardIssueFailed(customerId = "cust-1"),
            json = """{"__type":"card_issue_failed","customer_id":"cust-1","fiat_rate":null,"balance":null}""",
        ),
        FormatModel(
            status = PaymentAccountStatusValueDM.CardIssueFailed(
                customerId = "cust-1",
                fiatRate = BigDecimal("1.01"),
                balance = PaymentAccountStatusValueDM.BalanceDM(fiatBalance = FIAT_BALANCE),
            ),
            json = """{"__type":"card_issue_failed","customer_id":"cust-1","fiat_rate":"1.01",""" +
                """"balance":{"fiat_balance":$FIAT_BALANCE_JSON}}""",
        ),
        FormatModel(
            status = PaymentAccountStatusValueDM.DeactivatedAccount(
                customerId = "cust-1",
                fiatRate = null,
                fiatBalance = FIAT_BALANCE,
            ),
            json = """{"__type":"deactivated_account","customer_id":"cust-1","fiat_rate":null,""" +
                """"fiat_balance":$FIAT_BALANCE_JSON}""",
        ),
    )

    data class FormatModel(val status: PaymentAccountStatusValueDM, val json: String)

    private companion object {
        val FIAT_BALANCE = PaymentAccountStatusValueDM.FiatBalanceDM(
            availableBalance = BigDecimal("12.50"),
            currency = "USD",
        )
        const val FIAT_BALANCE_JSON = """{"available_balance":"12.50","currency":"USD"}"""
        val CARD = PaymentAccountStatusValueDM.TangemPayCard(
            id = "card-1",
            productInstanceId = "pi-1",
            cardStatus = "active",
            hasPinCode = true,
            displayName = null,
            actualDailyLimit = BigDecimal("1000"),
            adminDailyLimit = null,
            frozenState = "unfrozen",
            lastDigits = "4242",
            images = listOf(PaymentAccountStatusValueDM.ImageDM(type = "front", url = "https://img/front.png")),
            state = "ready",
            embossName = "JOHN DOE",
            cardType = "virtual",
        )
        const val CARD_JSON = """{"id":"card-1","product_instance_id":"pi-1","card_status":"active",""" +
            """"has_pin_code":true,"display_name":null,"actual_daily_limit":"1000","admin_daily_limit":null,""" +
            """"frozen_state":"unfrozen","last_digits":"4242",""" +
            """"images":[{"type":"front","url":"https://img/front.png"}],"state":"ready",""" +
            """"emboss_name":"JOHN DOE","card_type":"virtual"}"""
    }
}