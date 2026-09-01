package com.tangem.data.polymarket.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory
import com.tangem.domain.polymarket.PolymarketDepositBlockchain
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PredictionCollateral
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

/** Describing the network by hand is only safe while it agrees with what the shared factory builds. */
internal class DefaultPolymarketCollateralCurrencyFactoryTest {

    private val excludedBlockchains: ExcludedBlockchains = mockk {
        every { contains(any<Blockchain>()) } returns false
    }

    private val factory = DefaultPolymarketCollateralCurrencyFactory()

    private val expectedNetwork by lazy {
        NetworkFactory(excludedBlockchains).create(
            blockchain = PolymarketDepositBlockchain,
            extraDerivationPath = null,
            derivationStyleProvider = null,
            canHandleTokens = true,
        )
    }

    @Test
    fun `GIVEN the deposit chain WHEN the collateral is created THEN its network is the one the app builds`() {
        // Act
        val actual = factory.create().network

        // Assert
        assertThat(actual).isEqualTo(expectedNetwork)
    }

    @Test
    fun `GIVEN the collateral WHEN it is created THEN it matches the token the shared factory would build`() {
        // Arrange
        val expected = CryptoCurrencyFactory(excludedBlockchains).createToken(
            network = requireNotNull(expectedNetwork),
            rawId = PredictionCollateral.RAW_ID,
            name = PolymarketCollateralCurrencyFactory.TOKEN_NAME,
            symbol = PolymarketCollateralCurrencyFactory.TOKEN_SYMBOL,
            decimals = PolymarketContracts.COLLATERAL_DECIMALS,
            contractAddress = PolymarketContracts.COLLATERAL,
        )

        // Act
        val actual = factory.create()

        // Assert
        assertThat(actual).isEqualTo(expected)
    }
}