package com.tangem.datasource.local.visa.entity

import com.google.common.truth.Truth
import com.squareup.moshi.adapter
import com.tangem.datasource.di.MoshiModule
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Verifies that the network Moshi (see [MoshiModule.provideNetworkMoshi]) resolves and round-trips the
 * polymorphic [VirtualAccountStatusValueDM] adapter.
 *
 * Guards against the missing `NamePolymorphicAdapterFactory` registration that caused a runtime
 * `ClassNotFoundException: ...VirtualAccountStatusValueDMJsonAdapter` (the adapter is registered manually,
 * not generated).
 */
class VirtualAccountStatusValueDMSerializationTest {

    @OptIn(ExperimentalStdlibApi::class)
    private val adapter = MoshiModule().provideNetworkMoshi().adapter<VirtualAccountStatusValueDM>()

    @Test
    fun `round-trip NotCreated`() {
        val model: VirtualAccountStatusValueDM = VirtualAccountStatusValueDM.NotCreated()

        val restored = adapter.fromJson(adapter.toJson(model))

        Truth.assertThat(restored).isInstanceOf(VirtualAccountStatusValueDM.NotCreated::class.java)
    }

    @Test
    fun `round-trip Provisioning`() {
        val model: VirtualAccountStatusValueDM = VirtualAccountStatusValueDM.Provisioning()

        val restored = adapter.fromJson(adapter.toJson(model))

        Truth.assertThat(restored).isInstanceOf(VirtualAccountStatusValueDM.Provisioning::class.java)
    }

    @Test
    fun `round-trip CountryNotSupported`() {
        val model: VirtualAccountStatusValueDM = VirtualAccountStatusValueDM.CountryNotSupported()

        val restored = adapter.fromJson(adapter.toJson(model))

        Truth.assertThat(restored).isInstanceOf(VirtualAccountStatusValueDM.CountryNotSupported::class.java)
    }

    @Test
    fun `round-trip ActiveAccount`() {
        val model: VirtualAccountStatusValueDM = VirtualAccountStatusValueDM.ActiveAccount(
            customerId = "cust-1",
            currencyCode = "USD",
            depositAddress = "0xabc",
            fiatBalance = VirtualAccountStatusValueDM.FiatBalanceDM(
                availableBalance = BigDecimal("101.56"),
                currency = "USD",
            ),
            cryptoBalance = VirtualAccountStatusValueDM.CryptoBalanceDM(
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