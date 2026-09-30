package com.tangem.spend.datasource.visa.entity

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.spend.datasource.di.SpendDatasourceModule
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * Guards the stored JSON format of every [PaymentAccountStatusValueDM] subtype now that the subtypes have
 * generated adapters. The expected strings are what the reflective `KotlinJsonAdapterFactory` wrote before,
 * so statuses already cached on devices must still read back unchanged. The Moshi instance has no reflective
 * factory on purpose: a subtype without a generated adapter would not serialize at all.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PaymentAccountStatusValueDMJsonFormatTest {

    private object BigDecimalAdapter {
        @FromJson
        fun fromJson(value: String): BigDecimal = BigDecimal(value)

        @ToJson
        fun toJson(value: BigDecimal): String = value.toString()
    }

    private val adapter = SpendDatasourceModule
        .provideVisaMoshiConfigurer()
        .configure(Moshi.Builder().add(BigDecimalAdapter))
        .build()
        .adapter(PaymentAccountStatusValueDM::class.java)

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN json in the stored format WHEN fromJson THEN reads the same status`(model: FormatModel) {
        // Act
        val actual = adapter.fromJson(model.json)

        // Assert
        assertThat(actual).isEqualTo(model.status)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN status WHEN toJson THEN writes the stored format`(model: FormatModel) {
        // Act
        val actual = adapter.toJson(model.status)

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    private fun provideTestModels() = listOf(
        FormatModel(status = PaymentAccountStatusValueDM.Empty(), json = """{"empty":true}"""),
        FormatModel(status = PaymentAccountStatusValueDM.NotCreated(), json = """{"not_created":true}"""),
        FormatModel(
            status = PaymentAccountStatusValueDM.UnderReview(kycStatus = KycStatus.PENDING, customerId = "cust-1"),
            json = """{"kyc_status":"in_progress","customer_id":"cust-1"}""",
        ),
        FormatModel(status = PaymentAccountStatusValueDM.IssuingCard(), json = """{"issuing_card":true}"""),
        FormatModel(
            status = PaymentAccountStatusValueDM.ActiveAccount(
                customerId = "cust-1",
                currencyCode = "USD",
                paymentAccountAddress = "0xpay",
                fiatBalance = FIAT_BALANCE,
                fiatRate = BigDecimal("0.99"),
                cards = listOf(CARD),
            ),
            json = """{"active_account":true,"customer_id":"cust-1","currency_code":"USD",""" +
                """"payment_account_address":"0xpay","fiat_balance":{"available_balance":"12.50","currency":"USD"},""" +
                """"fiat_rate":"0.99","cards":[{"id":"card-1","product_instance_id":"pi-1","card_status":"active",""" +
                """"has_pin_code":true,"actual_daily_limit":"1000","frozen_state":"unfrozen","last_digits":"4242",""" +
                """"images":[{"type":"front","url":"https://img/front.png"}],"state":"ready",""" +
                """"emboss_name":"JOHN DOE","card_type":"virtual"}]}""",
        ),
        FormatModel(
            status = PaymentAccountStatusValueDM.CardIssueFailed(customerId = "cust-1"),
            json = """{"card_issue_failed":true,"customer_id":"cust-1"}""",
        ),
        FormatModel(
            status = PaymentAccountStatusValueDM.CardIssueFailed(
                customerId = "cust-1",
                fiatRate = BigDecimal("1.01"),
                balance = PaymentAccountStatusValueDM.BalanceDM(fiatBalance = FIAT_BALANCE),
            ),
            json = """{"card_issue_failed":true,"customer_id":"cust-1","fiat_rate":"1.01",""" +
                """"balance":{"fiat_balance":{"available_balance":"12.50","currency":"USD"}}}""",
        ),
        FormatModel(
            status = PaymentAccountStatusValueDM.DeactivatedAccount(
                customerId = "cust-1",
                fiatRate = null,
                fiatBalance = FIAT_BALANCE,
            ),
            json = """{"deactivated_account":true,"customer_id":"cust-1",""" +
                """"fiat_balance":{"available_balance":"12.50","currency":"USD"}}""",
        ),
    )

    data class FormatModel(val status: PaymentAccountStatusValueDM, val json: String)

    private companion object {
        val FIAT_BALANCE = PaymentAccountStatusValueDM.FiatBalanceDM(
            availableBalance = BigDecimal("12.50"),
            currency = "USD",
        )
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
    }
}