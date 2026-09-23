package com.tangem.spend.datasource.visa.entity

import com.google.common.truth.Truth
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.spend.datasource.di.SpendDatasourceModule
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Verifies that the [SpendDatasourceModule.provideVisaMoshiConfigurer] contribution resolves and
 * round-trips the polymorphic [MoneyAccountStatusValueDM] adapter.
 *
 * Guards against a missing `NamePolymorphicAdapterFactory` registration that caused a runtime
 * `ClassNotFoundException: ...MoneyAccountStatusValueDMJsonAdapter` (the adapter is registered
 * manually, not generated).
 */
class MoneyAccountStatusValueDMSerializationTest {

    private object BigDecimalAdapter {
        @FromJson
        fun fromJson(value: String): BigDecimal = BigDecimal(value)

        @ToJson
        fun toJson(value: BigDecimal): String = value.toPlainString()
    }

    @OptIn(ExperimentalStdlibApi::class)
    private val adapter = SpendDatasourceModule
        .provideVisaMoshiConfigurer()
        .configure(Moshi.Builder().add(BigDecimalAdapter))
        .addLast(KotlinJsonAdapterFactory())
        .build()
        .adapter<MoneyAccountStatusValueDM>()

    private val balance = MoneyAccountStatusValueDM.BalanceDM(
        cryptoAmount = BigDecimal("101.56"),
        depositAddress = "0xabc",
        currencyCode = "USD",
        fiatRate = BigDecimal("0.95"),
    )

    @Test
    fun `GIVEN NotCreated WHEN round-trip THEN restored`() {
        // Arrange
        val model: MoneyAccountStatusValueDM = MoneyAccountStatusValueDM.NotCreated()

        // Act
        val restored = adapter.fromJson(adapter.toJson(model))

        // Assert
        Truth.assertThat(restored).isInstanceOf(MoneyAccountStatusValueDM.NotCreated::class.java)
    }

    @Test
    fun `GIVEN CountryNotSupported WHEN round-trip THEN restored`() {
        // Arrange
        val model: MoneyAccountStatusValueDM = MoneyAccountStatusValueDM.CountryNotSupported()

        // Act
        val restored = adapter.fromJson(adapter.toJson(model))

        // Assert
        Truth.assertThat(restored).isInstanceOf(MoneyAccountStatusValueDM.CountryNotSupported::class.java)
    }

    @Test
    fun `GIVEN Issuing WHEN round-trip THEN restored`() {
        // Arrange
        val model: MoneyAccountStatusValueDM = MoneyAccountStatusValueDM.Issuing(
            step = MoneyAccountStatusValue.IssuingStep.ELIGIBILITY_CHECK,
            customerId = "cust-1",
            balance = balance,
        )

        // Act
        val restored = adapter.fromJson(adapter.toJson(model))

        // Assert
        Truth.assertThat(restored).isEqualTo(model)
    }

    @Test
    fun `GIVEN VerificationDeclined WHEN round-trip THEN restored`() {
        // Arrange
        val model: MoneyAccountStatusValueDM = MoneyAccountStatusValueDM.VerificationDeclined(
            customerId = "cust-1",
            balance = null,
        )

        // Act
        val restored = adapter.fromJson(adapter.toJson(model))

        // Assert
        Truth.assertThat(restored).isEqualTo(model)
    }

    @Test
    fun `GIVEN Activating WHEN round-trip THEN restored`() {
        // Arrange
        val model: MoneyAccountStatusValueDM = MoneyAccountStatusValueDM.Activating(
            customerId = "cust-1",
            balance = balance,
        )

        // Act
        val restored = adapter.fromJson(adapter.toJson(model))

        // Assert
        Truth.assertThat(restored).isEqualTo(model)
    }

    @Test
    fun `GIVEN ActiveAccount WHEN round-trip THEN restored`() {
        // Arrange
        val model: MoneyAccountStatusValueDM = MoneyAccountStatusValueDM.ActiveAccount(
            customerId = "cust-1",
            productInstanceId = "pi-1",
            balance = balance,
        )

        // Act
        val restored = adapter.fromJson(adapter.toJson(model))

        // Assert
        Truth.assertThat(restored).isEqualTo(model)
    }

    @Test
    fun `GIVEN Deactivated WHEN round-trip THEN restored`() {
        // Arrange
        val model: MoneyAccountStatusValueDM = MoneyAccountStatusValueDM.Deactivated(
            customerId = "cust-1",
            productInstanceId = "pi-1",
            balance = balance,
        )

        // Act
        val restored = adapter.fromJson(adapter.toJson(model))

        // Assert
        Truth.assertThat(restored).isEqualTo(model)
    }
}