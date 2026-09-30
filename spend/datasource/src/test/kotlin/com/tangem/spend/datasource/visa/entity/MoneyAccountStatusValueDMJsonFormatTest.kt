package com.tangem.spend.datasource.visa.entity

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.spend.datasource.di.SpendDatasourceModule
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * Guards the stored JSON format of every [MoneyAccountStatusValueDM] subtype now that the subtypes have
 * generated adapters. The expected strings are what the reflective `KotlinJsonAdapterFactory` wrote before,
 * so statuses already cached on devices must still read back unchanged. The Moshi instance has no reflective
 * factory on purpose: a subtype without a generated adapter would not serialize at all.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MoneyAccountStatusValueDMJsonFormatTest {

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
        .adapter(MoneyAccountStatusValueDM::class.java)

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
        FormatModel(status = MoneyAccountStatusValueDM.Empty(), json = """{"empty":true}"""),
        FormatModel(status = MoneyAccountStatusValueDM.NotCreated(), json = """{"not_created":true}"""),
        FormatModel(
            status = MoneyAccountStatusValueDM.CountryNotSupported(),
            json = """{"country_not_supported":true}""",
        ),
        FormatModel(
            status = MoneyAccountStatusValueDM.Issuing(
                step = MoneyAccountStatusValue.IssuingStep.RAIN_APPROVAL,
                customerId = null,
                balance = null,
            ),
            json = """{"issuing":true,"step":"RAIN_APPROVAL"}""",
        ),
        FormatModel(
            status = MoneyAccountStatusValueDM.UnderReview(
                kycStatus = KycStatus.REJECTED,
                customerId = "cust-2",
                balance = BALANCE,
            ),
            json = """{"kyc_status":"declined","customer_id":"cust-2","balance":$BALANCE_JSON}""",
        ),
        FormatModel(
            status = MoneyAccountStatusValueDM.VerificationDeclined(customerId = "cust-2", balance = null),
            json = """{"verification_declined":true,"customer_id":"cust-2"}""",
        ),
        FormatModel(
            status = MoneyAccountStatusValueDM.Activating(customerId = "cust-2", balance = BALANCE),
            json = """{"activating":true,"customer_id":"cust-2","balance":$BALANCE_JSON}""",
        ),
        FormatModel(
            status = MoneyAccountStatusValueDM.ActiveAccount(
                customerId = "cust-2",
                productInstanceId = "pi-2",
                balance = BALANCE,
            ),
            json = """{"active_account":true,"customer_id":"cust-2","product_instance_id":"pi-2",""" +
                """"balance":$BALANCE_JSON}""",
        ),
        FormatModel(
            status = MoneyAccountStatusValueDM.Deactivated(
                customerId = "cust-2",
                productInstanceId = "pi-2",
                balance = null,
            ),
            json = """{"deactivated":true,"customer_id":"cust-2","product_instance_id":"pi-2"}""",
        ),
    )

    data class FormatModel(val status: MoneyAccountStatusValueDM, val json: String)

    private companion object {
        val BALANCE = MoneyAccountStatusValueDM.BalanceDM(
            cryptoAmount = BigDecimal("101.56"),
            depositAddress = "0xabc",
            currencyCode = "USD",
            fiatRate = BigDecimal("0.95"),
        )
        const val BALANCE_JSON =
            """{"crypto_amount":"101.56","deposit_address":"0xabc","currency_code":"USD","fiat_rate":"0.95"}"""
    }
}