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
    fun `GIVEN the collateral WHEN it is created THEN its contract address is lower case`() {
        // Act
        val actual = factory.create().contractAddress

        // Assert — Express answers with a lower-cased address and the swap matches pairs by string equality
        assertThat(actual).isEqualTo(actual.lowercase())
    }

    @Test
    fun `GIVEN both tokens WHEN created THEN a deposit is not made in the token the exchange settles in`() {
        // Act
        val settlement = factory.create().contractAddress
        val deposit = factory.createDepositTarget().contractAddress

        // Assert
        assertThat(deposit).isNotEqualTo(settlement)
        assertThat(settlement).isEqualTo(PolymarketContracts.COLLATERAL.lowercase())
    }

    @Test
    fun `GIVEN both tokens WHEN created THEN the exchange settles in pUSD and a deposit is made in USDC`() {
        // Act
        val settlement = factory.create()
        val deposit = factory.createDepositTarget()

        // Assert
        assertThat(settlement.symbol).isEqualTo("pUSD")
        assertThat(settlement.name).isEqualTo("Polymarket USD")
        assertThat(deposit.symbol).isEqualTo("USDC")
        assertThat(deposit.name).isEqualTo("USDC")
    }

    @Test
    fun `GIVEN the deposit target WHEN created THEN it is the USDC the bridge accepts on Polygon`() {
        // Act
        val actual = factory.createDepositTarget()

        // Assert — the address the bridge lists for chain 137, lower-cased as Express answers it
        assertThat(actual.contractAddress).isEqualTo("0x3c499c542cef5e3811e1192ce70d8cc03d5c3359")
        assertThat(actual.decimals).isEqualTo(PolymarketContracts.COLLATERAL_DECIMALS)
        assertThat(actual.network).isEqualTo(expectedNetwork)
    }

    @Test
    fun `GIVEN the collateral WHEN it is created THEN it matches the token the shared factory would build`() {
        // Arrange
        val expected = CryptoCurrencyFactory(excludedBlockchains).createToken(
            network = requireNotNull(expectedNetwork),
            rawId = PredictionCollateral.RAW_ID,
            name = PolymarketCollateralCurrencyFactory.COLLATERAL_NAME,
            symbol = PolymarketCollateralCurrencyFactory.COLLATERAL_SYMBOL,
            decimals = PolymarketContracts.COLLATERAL_DECIMALS,
            contractAddress = PolymarketContracts.COLLATERAL.lowercase(),
        )

        // Act
        val actual = factory.create()

        // Assert
        assertThat(actual).isEqualTo(expected)
    }
}