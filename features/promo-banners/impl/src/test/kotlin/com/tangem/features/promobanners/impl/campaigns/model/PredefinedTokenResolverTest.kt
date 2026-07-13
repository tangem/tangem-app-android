package com.tangem.features.promobanners.impl.campaigns.model

import com.google.common.truth.Truth.assertThat
import com.tangem.data.common.currency.getTokenIconUrlFromDefaultHost
import com.tangem.domain.markets.RawMarketToken
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.promo.models.PromoPayoutToken
import com.tangem.features.commonfeatures.api.choosetoken.PredefinedTokenToAdd
import org.junit.jupiter.api.Test

internal class PredefinedTokenResolverTest {

    private val resolver = PredefinedTokenResolver()

    @Test
    fun `GIVEN payout token WHEN resolve THEN mapped from payload`() {
        // Arrange
        val payoutToken = createPayoutToken(
            tokenId = "cat-token",
            tokenAddress = "0xContract",
            tokenSymbol = "CAT",
            tokenName = "Cat Token",
            networkId = "ethereum",
            decimals = 6,
        )

        // Act
        val actual = resolver.resolve(listOf(payoutToken))

        // Assert
        val rawId = CryptoCurrency.RawID("cat-token")
        val expected = PredefinedTokenToAdd(
            token = RawMarketToken(id = rawId, name = "Cat Token", symbol = "CAT"),
            network = TokenMarketInfo.Network(
                networkId = "ethereum",
                isExchangeable = false,
                contractAddress = "0xContract",
                decimalCount = 6,
            ),
            iconUrl = getTokenIconUrlFromDefaultHost(rawId),
        )
        assertThat(actual).containsExactly(expected)
    }

    @Test
    fun `GIVEN multiple payout tokens WHEN resolve THEN input order preserved`() {
        // Arrange
        val first = createPayoutToken(tokenId = "first-token", tokenSymbol = "AAA", networkId = "ethereum")
        val second = createPayoutToken(tokenId = "second-token", tokenSymbol = "BBB", networkId = "polygon")

        // Act
        val actual = resolver.resolve(listOf(first, second)).map { it.token.id.value }

        // Assert
        assertThat(actual).containsExactly("first-token", "second-token").inOrder()
    }

    @Test
    fun `GIVEN two payouts with same id on same network WHEN resolve THEN duplicate removed`() {
        // Arrange
        val first = createPayoutToken(tokenId = "usd-coin", tokenAddress = "0xFirst", networkId = "ethereum")
        val second = createPayoutToken(tokenId = "usd-coin", tokenAddress = "0xSecond", networkId = "ethereum")

        // Act
        val actual = resolver.resolve(listOf(first, second))

        // Assert
        assertThat(actual).hasSize(1)
        assertThat(actual.single().network.contractAddress).isEqualTo("0xFirst")
    }

    private fun createPayoutToken(
        tokenId: String = "cat-token",
        tokenAddress: String = "0xContract",
        tokenSymbol: String = "CAT",
        tokenName: String = "Cat Token",
        networkId: String = "ethereum",
        decimals: Int = 6,
    ) = PromoPayoutToken(
        tokenId = tokenId,
        tokenAddress = tokenAddress,
        tokenSymbol = tokenSymbol,
        tokenName = tokenName,
        networkId = networkId,
        decimals = decimals,
    )
}