package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.spend.datasource.pay.models.response.BalanceResponse
import com.tangem.spend.datasource.pay.models.response.CustomerMeResponse
import com.tangem.domain.pay.model.CustomerInfo
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class CustomerInfoConverterNetworksTest {

    private fun result(balance: BalanceResponse?) = CustomerMeResponse.Result(
        id = "c1",
        state = "ACTIVE",
        createdAt = "2026-01-01T00:00:00Z",
        paymentAccount = null,
        kyc = null,
        depositAddress = null,
        balance = balance,
        productInstances = emptyList(),
        cards = emptyList(),
        customerTariffPlan = null,
    )

    private fun balance(networks: List<BalanceResponse.NetworkResponse>?) = BalanceResponse(
        fiat = null,
        crypto = null,
        availableForWithdrawal = null,
        networks = networks,
    )

    @Test
    fun `GIVEN null balance WHEN convert THEN empty networks`() {
        val info = CustomerInfoConverter.convert(result(balance = null))
        assertThat(info.networks).isEmpty()
    }

    @Test
    fun `GIVEN null networks WHEN convert THEN empty list`() {
        val info = CustomerInfoConverter.convert(result(balance = balance(networks = null)))
        assertThat(info.networks).isEmpty()
    }

    @Test
    fun `GIVEN enabled and unknown status WHEN convert THEN maps fields and defaults unknown to DISABLED`() {
        val wire = listOf(
            BalanceResponse.NetworkResponse(
                name = "base", isTestnet = true, chainId = 84532L, status = "ENABLED",
                depositAddress = "0xEED",
                tokens = listOf(
                    BalanceResponse.NetworkTokenResponse("USDC", "0x036", BigDecimal("6")),
                ),
            ),
            BalanceResponse.NetworkResponse(
                name = "mystery", isTestnet = false, chainId = 999L, status = "WAT",
                depositAddress = null,
                tokens = listOf(
                    BalanceResponse.NetworkTokenResponse("USDT", "0xAAA", null),
                ),
            ),
        )

        val networks = CustomerInfoConverter.convert(result(balance = balance(networks = wire))).networks

        assertThat(networks).hasSize(2)
        assertThat(networks[0].status).isEqualTo(CustomerInfo.NetworkInfo.Status.ENABLED)
        assertThat(networks[0].chainId).isEqualTo(84532L)
        assertThat(networks[0].tokens[0].symbol).isEqualTo("USDC")
        assertThat(networks[0].tokens[0].availableForWithdrawal).isEqualTo(BigDecimal("6"))
        assertThat(networks[1].status).isEqualTo(CustomerInfo.NetworkInfo.Status.DISABLED)
        assertThat(networks[1].depositAddress).isNull()
        assertThat(networks[1].tokens[0].availableForWithdrawal).isNull()
    }
}