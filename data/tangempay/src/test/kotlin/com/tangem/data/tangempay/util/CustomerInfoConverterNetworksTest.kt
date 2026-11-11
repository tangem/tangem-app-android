package com.tangem.data.tangempay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.spend.datasource.tangempay.models.response.BalanceResponse
import com.tangem.spend.datasource.tangempay.models.response.CustomerMeResponse
import com.tangem.domain.tangempay.model.CustomerInfo
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CustomerInfoConverterNetworksTest {

    private fun result(balance: BalanceResponse?) = CustomerMeResponse.Result(
        id = "c1",
        state = "ACTIVE",
        createdAt = "2026-01-01T00:00:00Z",
        paymentAccount = null,
        kyc = null,
        balance = balance,
        productInstances = emptyList(),
        cards = emptyList(),
        customerTariffPlan = null,
    )

    private fun balance(networks: List<BalanceResponse.NetworkResponse>?) = BalanceResponse(
        fiat = null,
        networks = networks,
    )

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN null balance or null networks WHEN convert THEN empty networks`(model: MissingNetworksModel) {
        // Act
        val info = CustomerInfoConverter.convert(result(balance = model.balance))

        // Assert
        assertThat(info.networks).isEmpty()
    }

    @Test
    fun `GIVEN enabled and unknown status WHEN convert THEN maps fields and defaults unknown to DISABLED`() {
        // Arrange
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

        // Act
        val networks = CustomerInfoConverter.convert(result(balance = balance(networks = wire))).networks

        // Assert
        assertThat(networks).containsExactly(
            CustomerInfo.NetworkInfo(
                name = "base",
                chainId = 84532L,
                isTestnet = true,
                status = CustomerInfo.NetworkInfo.Status.ENABLED,
                depositAddress = "0xEED",
                tokens = listOf(
                    CustomerInfo.NetworkInfo.Token(
                        symbol = "USDC",
                        contractAddress = "0x036",
                        availableForWithdrawal = BigDecimal("6"),
                    ),
                ),
            ),
            CustomerInfo.NetworkInfo(
                name = "mystery",
                chainId = 999L,
                isTestnet = false,
                status = CustomerInfo.NetworkInfo.Status.DISABLED,
                depositAddress = null,
                tokens = listOf(
                    CustomerInfo.NetworkInfo.Token(
                        symbol = "USDT",
                        contractAddress = "0xAAA",
                        availableForWithdrawal = null,
                    ),
                ),
            ),
        ).inOrder()
    }

    @Test
    fun `GIVEN not issued network shaped like the backend sends it WHEN convert THEN it survives with no tokens`() {
        // Arrange
        // Real `customer/me` shape for a network whose contract is not issued: chain id present, no deposit
        // address, and an empty token list.
        val wire = listOf(
            BalanceResponse.NetworkResponse(
                name = "ethereum",
                status = "NOT_ISSUED",
                isTestnet = true,
                chainId = 11155111L,
                depositAddress = null,
                tokens = emptyList(),
            ),
        )

        // Act
        val networks = CustomerInfoConverter.convert(result(balance = balance(networks = wire))).networks

        // Assert
        assertThat(networks).containsExactly(
            CustomerInfo.NetworkInfo(
                name = "ethereum",
                chainId = 11155111L,
                isTestnet = true,
                status = CustomerInfo.NetworkInfo.Status.NOT_ISSUED,
                depositAddress = null,
                tokens = emptyList(),
            ),
        )
    }

    @Test
    fun `GIVEN token without contract address WHEN convert THEN contract address stays null`() {
        // Arrange
        val wire = listOf(
            BalanceResponse.NetworkResponse(
                name = "base",
                isTestnet = true,
                chainId = 84532L,
                status = "NOT_ISSUED",
                tokens = listOf(BalanceResponse.NetworkTokenResponse(token = "USDC")),
            ),
        )

        // Act
        val networks = CustomerInfoConverter.convert(result(balance = balance(networks = wire))).networks

        // Assert
        assertThat(networks[0].tokens).containsExactly(
            CustomerInfo.NetworkInfo.Token(symbol = "USDC", contractAddress = null, availableForWithdrawal = null),
        )
    }

    private fun provideTestModels() = listOf(
        MissingNetworksModel(balance = null),
        MissingNetworksModel(balance = balance(networks = null)),
    )

    internal data class MissingNetworksModel(val balance: BalanceResponse?)
}