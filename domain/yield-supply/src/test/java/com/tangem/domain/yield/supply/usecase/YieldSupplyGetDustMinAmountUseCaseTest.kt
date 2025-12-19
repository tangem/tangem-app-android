package com.tangem.domain.yield.supply.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class YieldSupplyGetDustMinAmountUseCaseTest {

    private val useCase = YieldSupplyGetDustMinAmountUseCase()

    @Test
    fun `GIVEN supported currency WHEN invoke THEN return dust min amount`() {
        val minAmount = BigDecimal("123.456")
        val appCurrency = AppCurrency(code = "EUR", name = "Euro", symbol = "€")
        val tokenStatus = createTokenStatus(fiatRate = BigDecimal("2.0"))

        val result = useCase(minAmount, appCurrency, tokenStatus)

        assertThat(result).isEqualTo(BigDecimal("0.1"))
    }

    @Test
    fun `GIVEN unsupported currency WHEN invoke THEN return min amount multiplied by fiat rate`() {
        val minAmount = BigDecimal("1.23")
        val fiatRate = BigDecimal("150.0")
        val appCurrency = AppCurrency(code = "JPY", name = "Japanese Yen", symbol = "¥")
        val tokenStatus = createTokenStatus(fiatRate = fiatRate)

        val result = useCase(minAmount, appCurrency, tokenStatus)

        assertThat(result).isEqualTo(minAmount.multiply(fiatRate))
    }

    private fun createTokenStatus(fiatRate: BigDecimal): CryptoCurrencyStatus {
        val network = createNetwork()
        val token = createToken(network)
        return CryptoCurrencyStatus(
            currency = token,
            value = CryptoCurrencyStatus.Loaded(
                amount = BigDecimal.ZERO,
                fiatAmount = BigDecimal.ZERO,
                fiatRate = fiatRate,
                priceChange = BigDecimal.ZERO,
                yieldBalance = null,
                yieldSupplyStatus = null,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = NetworkAddress.Single(
                    NetworkAddress.Address(value = "0xabc", type = NetworkAddress.Address.Type.Primary),
                ),
                sources = CryptoCurrencyStatus.Sources(),
            ),
        )
    }

    private fun createNetwork(): Network {
        val derivationPath = Network.DerivationPath.None
        return Network(
            id = Network.ID(Network.RawID("polygon"), derivationPath),
            backendId = "polygon",
            name = "Polygon",
            currencySymbol = "MATIC",
            derivationPath = derivationPath,
            isTestnet = false,
            standardType = Network.StandardType.ERC20,
            hasFiatFeeRate = false,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.ENS,
        )
    }

    private fun createToken(network: Network): CryptoCurrency.Token {
        val tokenId = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId(network.rawId),
            suffix = CryptoCurrency.ID.Suffix.RawID("test-token", "0xContract"),
        )
        return CryptoCurrency.Token(
            id = tokenId,
            network = network,
            name = "Test Token",
            symbol = "TT",
            decimals = 18,
            iconUrl = null,
            isCustom = false,
            contractAddress = "0xContract",
        )
    }
}