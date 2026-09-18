package com.tangem.spend.datasource.visa.entity

import com.google.common.truth.Truth
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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

    @Test
    fun `round-trip NotCreated`() {
        val model: MoneyAccountStatusValueDM = MoneyAccountStatusValueDM.NotCreated()

        val restored = adapter.fromJson(adapter.toJson(model))

        Truth.assertThat(restored).isInstanceOf(MoneyAccountStatusValueDM.NotCreated::class.java)
    }

    @Test
    fun `round-trip Provisioning`() {
        val model: MoneyAccountStatusValueDM = MoneyAccountStatusValueDM.Provisioning()

        val restored = adapter.fromJson(adapter.toJson(model))

        Truth.assertThat(restored).isInstanceOf(MoneyAccountStatusValueDM.Provisioning::class.java)
    }

    @Test
    fun `round-trip CountryNotSupported`() {
        val model: MoneyAccountStatusValueDM = MoneyAccountStatusValueDM.CountryNotSupported()

        val restored = adapter.fromJson(adapter.toJson(model))

        Truth.assertThat(restored).isInstanceOf(MoneyAccountStatusValueDM.CountryNotSupported::class.java)
    }

    @Test
    fun `round-trip ActiveAccount`() {
        val model: MoneyAccountStatusValueDM = MoneyAccountStatusValueDM.ActiveAccount(
            customerId = "cust-1",
            currencyCode = "USD",
            depositAddress = "0xabc",
            fiatBalance = MoneyAccountStatusValueDM.FiatBalanceDM(
                availableBalance = BigDecimal("101.56"),
                currency = "USD",
            ),
            cryptoBalance = MoneyAccountStatusValueDM.CryptoBalanceDM(
                id = "usd-coin",
                chainId = 137L,
                depositAddress = "0xabc",
                tokenContractAddress = "0xdef",
                balance = BigDecimal("101.56"),
            ),
            fiatRate = BigDecimal("0.95"),
            availableForWithdrawal = BigDecimal("100.00"),
        )

        val restored = adapter.fromJson(adapter.toJson(model))

        Truth.assertThat(restored).isEqualTo(model)
    }
}