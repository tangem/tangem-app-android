package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.PortfolioId
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.feature.wallet.presentation.wallet.state.transformers.TokenConverterParams
import org.junit.Test
import java.math.BigDecimal

class YieldSupplyPromoBannerConverterTest {

    @Test
    fun `GIVEN promo disabled WHEN convert THEN return null`() {
        val token = createToken(networkId = "ethereum", backendId = "ethereum", contract = "0xABCDEF")
        val status = createLoadedStatus(token = token, amount = BigDecimal.ONE, isYieldActive = false)
        val tokenList = ungroupedTokenList(status)
        val params = TokenConverterParams.Wallet(
            portfolioId = PortfolioId.Wallet(UserWalletId("00")),
            tokenList = tokenList,
        )
        val converter = YieldSupplyPromoBannerConverter(
            yieldModuleApyMap = mapOf("${token.network.rawId}_${token.contractAddress}" to BigDecimal("0.10")),
            shouldShowMainPromo = false,
        )

        val result = converter.convert(params)

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN empty apy map WHEN convert THEN return null`() {
        val token = createToken(networkId = "ethereum", backendId = "ethereum", contract = "0xA1")
        val status = createLoadedStatus(token = token, amount = BigDecimal("2.0"), isYieldActive = false)
        val params = TokenConverterParams.Wallet(
            portfolioId = PortfolioId.Wallet(UserWalletId("00")),
            tokenList = ungroupedTokenList(status),
        )
        val converter = YieldSupplyPromoBannerConverter(
            yieldModuleApyMap = emptyMap(),
            shouldShowMainPromo = true,
        )

        val result = converter.convert(params)

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN active yield token present WHEN convert THEN return null`() {
        val token = createToken(networkId = "ethereum", backendId = "ethereum", contract = "0xAA")
        val statusActive = createLoadedStatus(token = token, amount = BigDecimal("5"), isYieldActive = true)
        val params = TokenConverterParams.Wallet(
            portfolioId = PortfolioId.Wallet(UserWalletId("00")),
            tokenList = ungroupedTokenList(statusActive),
        )
        val converter = YieldSupplyPromoBannerConverter(
            yieldModuleApyMap = mapOf("${token.network.rawId}_${token.contractAddress}" to BigDecimal("0.12")),
            shouldShowMainPromo = true,
        )

        val result = converter.convert(params)

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN multiple candidates EVM case insensitive WHEN convert THEN return status of max amount`() {
        val evmNetworkId = "ETH"
        val tokenSmall = createToken(networkId = evmNetworkId, backendId = evmNetworkId, contract = "0xAbCd")
        val tokenBig = createToken(networkId = evmNetworkId, backendId = evmNetworkId, contract = "0xBEEF")

        val statusSmall = createLoadedStatus(token = tokenSmall, amount = BigDecimal("1.00"), isYieldActive = false)
        val statusBig = createLoadedStatus(token = tokenBig, amount = BigDecimal("10.00"), isYieldActive = false)

        val apyMap = mapOf(
            "${tokenSmall.network.rawId}_${tokenSmall.contractAddress.lowercase()}" to BigDecimal("0.05"),
            "${tokenBig.network.rawId}_${tokenBig.contractAddress.uppercase()}" to BigDecimal("0.15"),
        )

        val params = TokenConverterParams.Wallet(
            portfolioId = PortfolioId.Wallet(UserWalletId("00")),
            tokenList = ungroupedTokenList(statusSmall, statusBig),
        )
        val converter = YieldSupplyPromoBannerConverter(
            yieldModuleApyMap = apyMap,
            shouldShowMainPromo = true,
        )

        val result = converter.convert(params)

        assertThat(result).isEqualTo(statusBig)
    }

    @Test
    fun `GIVEN non evm case sensitive mismatch WHEN convert THEN return null`() {
        val nonEvmId = "xrp"
        val token = createToken(networkId = nonEvmId, backendId = nonEvmId, contract = "rAbC123")
        val status = createLoadedStatus(token = token, amount = BigDecimal("3"), isYieldActive = false)

        val mismatchedKey = "${token.network.rawId}_${token.contractAddress.lowercase()}"
        val apyMap = mapOf(mismatchedKey to BigDecimal("0.07"))

        val params = TokenConverterParams.Wallet(
            portfolioId = PortfolioId.Wallet(UserWalletId("00")),
            tokenList = ungroupedTokenList(status),
        )
        val converter = YieldSupplyPromoBannerConverter(
            yieldModuleApyMap = apyMap,
            shouldShowMainPromo = true,
        )

        val result = converter.convert(params)

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN custom status WHEN convert THEN return null`() {
        val token = createToken(networkId = "ethereum", backendId = "ethereum", contract = "0xCUSTOM")
        val status = createCustomStatus(token = token, amount = BigDecimal("5.0"), isYieldActive = false)
        val params = TokenConverterParams.Wallet(
            portfolioId = PortfolioId.Wallet(UserWalletId("00")),
            tokenList = ungroupedTokenList(status),
        )
        val converter = YieldSupplyPromoBannerConverter(
            yieldModuleApyMap = mapOf("${token.network.rawId}_${token.contractAddress}" to BigDecimal("0.10")),
            shouldShowMainPromo = true,
        )

        val result = converter.convert(params)

        assertThat(result).isNull()
    }

    private fun ungroupedTokenList(vararg statuses: CryptoCurrencyStatus): TokenList.Ungrouped {
        return TokenList.Ungrouped(
            totalFiatBalance = com.tangem.domain.models.TotalFiatBalance.Loaded(
                amount = BigDecimal.ZERO,
                source = StatusSource.ACTUAL,
            ),
            sortedBy = com.tangem.domain.models.TokensSortType.NONE,
            currencies = statuses.toList(),
        )
    }

    private fun createLoadedStatus(
        token: CryptoCurrency.Token,
        amount: BigDecimal,
        isYieldActive: Boolean,
    ): CryptoCurrencyStatus {
        val networkAddress = NetworkAddress.Single(
            defaultAddress = NetworkAddress.Address(
                value = "addr",
                type = NetworkAddress.Address.Type.Primary,
            ),
        )
        val value = CryptoCurrencyStatus.Loaded(
            amount = amount,
            fiatAmount = BigDecimal.ZERO,
            fiatRate = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            stakingBalance = null,
            yieldSupplyStatus = if (isYieldActive) {
                YieldSupplyStatus(
                    isActive = true,
                    isInitialized = true,
                    isAllowedToSpend = true,
                    effectiveProtocolBalance = null,
                )
            } else {
                null
            },
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = networkAddress,
            sources = CryptoCurrencyStatus.Sources(),
        )
        return CryptoCurrencyStatus(
            currency = token,
            value = value,
        )
    }

    private fun createCustomStatus(
        token: CryptoCurrency.Token,
        amount: BigDecimal,
        isYieldActive: Boolean,
    ): CryptoCurrencyStatus {
        val networkAddress = NetworkAddress.Single(
            defaultAddress = NetworkAddress.Address(
                value = "addr",
                type = NetworkAddress.Address.Type.Primary,
            ),
        )
        val value = CryptoCurrencyStatus.Custom(
            amount = amount,
            fiatAmount = null,
            fiatRate = null,
            priceChange = null,
            stakingBalance = null,
            yieldSupplyStatus = if (isYieldActive) {
                YieldSupplyStatus(
                    isActive = true,
                    isInitialized = true,
                    isAllowedToSpend = true,
                    effectiveProtocolBalance = null,
                )
            } else {
                null
            },
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = networkAddress,
            sources = CryptoCurrencyStatus.Sources(),
        )
        return CryptoCurrencyStatus(
            currency = token,
            value = value,
        )
    }

    private fun createToken(networkId: String, backendId: String, contract: String): CryptoCurrency.Token {
        val network = Network(
            id = Network.ID(value = networkId, derivationPath = Network.DerivationPath.None),
            backendId = backendId,
            name = backendId,
            currencySymbol = "SYM",
            derivationPath = Network.DerivationPath.None,
            isTestnet = false,
            standardType = when (backendId) {
                "ethereum" -> Network.StandardType.ERC20
                else -> Network.StandardType.Unspecified("UNSPEC")
            },
            hasFiatFeeRate = false,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
        return CryptoCurrency.Token(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(networkId),
                suffix = CryptoCurrency.ID.Suffix.ContractAddress(contract),
            ),
            network = network,
            name = "Token",
            symbol = "TKN",
            decimals = 18,
            iconUrl = null,
            isCustom = false,
            contractAddress = contract,
        )
    }
}