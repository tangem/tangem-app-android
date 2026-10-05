package com.tangem.spend.datasource.visa.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.KotlinxDataStoreSerializer
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.test.core.ProvideTestModels
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * Guards the stored JSON format of every [MoneyAccountStatusValueDM] subtype. The expected strings are what the
 * store writes to `money_account_statuses_v2`, so statuses cached by one app version must read back unchanged in
 * the next one.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MoneyAccountStatusValueDMJsonFormatTest {

    private val json = KotlinxDataStoreSerializer.jsonBuilder { classDiscriminator = "__type" }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN json in the stored format WHEN decode THEN reads the same status`(model: FormatModel) {
        // Act
        val actual = json.decodeFromString(MoneyAccountStatusValueDM.serializer(), model.json)

        // Assert
        assertThat(actual).isEqualTo(model.status)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN status WHEN encode THEN writes the stored format`(model: FormatModel) {
        // Act
        val actual = json.encodeToString(MoneyAccountStatusValueDM.serializer(), model.status)

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    @Test
    fun `GIVEN statuses keyed by wallet WHEN encode and decode as map THEN every entry is restored`() {
        // Arrange
        val serializer = MapSerializer(String.serializer(), MoneyAccountStatusValueDM.serializer())
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
            json.decodeFromString(MoneyAccountStatusValueDM.serializer(), """{"not_created":true}""")
        }.exceptionOrNull()

        // Assert
        assertThat(error).isNotNull()
    }

    private fun provideTestModels() = listOf(
        FormatModel(status = MoneyAccountStatusValueDM.Empty, json = """{"__type":"empty"}"""),
        FormatModel(status = MoneyAccountStatusValueDM.NotCreated, json = """{"__type":"not_created"}"""),
        FormatModel(
            status = MoneyAccountStatusValueDM.CountryNotSupported,
            json = """{"__type":"country_not_supported"}""",
        ),
        FormatModel(
            status = MoneyAccountStatusValueDM.Issuing(
                step = MoneyAccountStatusValue.IssuingStep.RAIN_APPROVAL,
                customerId = null,
                balance = null,
            ),
            json = """{"__type":"issuing","step":"RAIN_APPROVAL","customer_id":null,"balance":null}""",
        ),
        FormatModel(
            status = MoneyAccountStatusValueDM.Issuing(
                step = MoneyAccountStatusValue.IssuingStep.ELIGIBILITY_CHECK,
                customerId = "cust-1",
                balance = BALANCE,
            ),
            json = """{"__type":"issuing","step":"ELIGIBILITY_CHECK","customer_id":"cust-1",""" +
                """"balance":$BALANCE_JSON}""",
        ),
        FormatModel(
            status = MoneyAccountStatusValueDM.UnderReview(
                kycStatus = KycStatus.REJECTED,
                customerId = "cust-2",
                balance = BALANCE,
            ),
            json = """{"__type":"kyc_status","kyc_status":"declined","customer_id":"cust-2",""" +
                """"balance":$BALANCE_JSON}""",
        ),
        FormatModel(
            status = MoneyAccountStatusValueDM.VerificationDeclined(customerId = "cust-2", balance = null),
            json = """{"__type":"verification_declined","customer_id":"cust-2","balance":null}""",
        ),
        FormatModel(
            status = MoneyAccountStatusValueDM.Activating(customerId = "cust-2", balance = BALANCE),
            json = """{"__type":"activating","customer_id":"cust-2","balance":$BALANCE_JSON}""",
        ),
        FormatModel(
            status = MoneyAccountStatusValueDM.ActiveAccount(
                customerId = "cust-2",
                productInstanceId = "pi-2",
                balance = BALANCE,
            ),
            json = """{"__type":"active_account","customer_id":"cust-2","product_instance_id":"pi-2",""" +
                """"balance":$BALANCE_JSON}""",
        ),
        FormatModel(
            status = MoneyAccountStatusValueDM.Deactivated(
                customerId = "cust-2",
                productInstanceId = "pi-2",
                balance = null,
            ),
            json = """{"__type":"deactivated","customer_id":"cust-2","product_instance_id":"pi-2",""" +
                """"balance":null}""",
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