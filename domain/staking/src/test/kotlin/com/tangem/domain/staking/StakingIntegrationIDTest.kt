package com.tangem.domain.staking

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.staking.model.StakingApproval
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.isStakingSupported
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StakingIntegrationIDTest {

    @Test
    fun `GIVEN all integration ids WHEN distinct by value THEN no duplicates`() {
        // Act
        val actual = StakingIntegrationID.entries.distinctBy { it.value }

        // Assert
        val expected = StakingIntegrationID.entries.size
        Truth.assertThat(actual).hasSize(expected)
    }

    @Test
    fun `GIVEN coin ids WHEN distinct by blockchain THEN no duplicates`() {
        // Act
        val actual = StakingIntegrationID.StakeKit.Coin.entries
            .distinctBy(StakingIntegrationID.StakeKit.Coin::blockchain)

        // Assert
        val expected = StakingIntegrationID.StakeKit.Coin.entries.size
        Truth.assertThat(actual).hasSize(expected)
    }

    @Test
    fun `GIVEN ethereum token ids WHEN distinct by sub blockchain THEN no duplicates`() {
        // Act
        val actual = StakingIntegrationID.StakeKit.EthereumToken.entries
            .distinctBy(StakingIntegrationID.StakeKit.EthereumToken::subBlockchain)

        // Assert
        val expected = StakingIntegrationID.StakeKit.EthereumToken.entries.size
        Truth.assertThat(actual).hasSize(expected)
    }

    @Test
    fun `GIVEN needed approvals WHEN distinct by spender address THEN no duplicates`() {
        // Arrange
        val allNeededApproval = StakingIntegrationID.entries.map(StakingIntegrationID::approval)
            .filterIsInstance<StakingApproval.Needed>()

        // Act
        val actual = allNeededApproval.distinctBy(StakingApproval.Needed::spenderAddress)

        // Assert
        val expected = allNeededApproval.size
        Truth.assertThat(actual).hasSize(expected)
    }

    @Test
    fun `GIVEN ethereum token ids WHEN sub blockchain checked THEN all are evm`() {
        // Act
        val actual = StakingIntegrationID.StakeKit.EthereumToken.entries
            .map(StakingIntegrationID.StakeKit.EthereumToken::subBlockchain)
            .all { it.isEvm() }

        // Assert
        Truth.assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN coin ids WHEN approval read THEN none needed`() {
        // Act
        val actual = StakingIntegrationID.StakeKit.Coin.entries
            .map(StakingIntegrationID.StakeKit.Coin::approval)
            .none { it is StakingApproval.Needed }

        // Assert
        Truth.assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN ethereum token ids WHEN approval read THEN all needed`() {
        // Act
        val actual = StakingIntegrationID.StakeKit.EthereumToken.entries
            .map(StakingIntegrationID.StakeKit.EthereumToken::approval)
            .all { it is StakingApproval.Needed }

        // Assert
        Truth.assertThat(actual).isTrue()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Create {

        // Pins current behaviour: a token id with blank contract is treated as a coin
        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN currency id WHEN create THEN expected integration returned`(model: CreateModel) {
            // Act
            val actual = StakingIntegrationID.create(currencyId = model.currencyId)

            // Assert
            Truth.assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Bitcoin),
                expected = null,
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.TON),
                expected = StakingIntegrationID.StakeKit.Coin.Ton,
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Solana),
                expected = StakingIntegrationID.StakeKit.Coin.Solana,
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Cosmos),
                expected = StakingIntegrationID.StakeKit.Coin.Cosmos,
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Tron),
                expected = StakingIntegrationID.StakeKit.Coin.Tron,
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.BSC),
                expected = StakingIntegrationID.StakeKit.Coin.BSC,
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Cardano),
                expected = StakingIntegrationID.StakeKit.Coin.Cardano,
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = StakingIntegrationID.P2PEthPool.blockchain),
                expected = StakingIntegrationID.P2PEthPool,
            ),
            CreateModel(
                currencyId = CryptoCurrency.ID.fromValue(value = "token⟨ethereum⟩polygon-ecosystem-token⚓1234567890"),
                expected = StakingIntegrationID.StakeKit.EthereumToken.Polygon,
            ),
            CreateModel(
                currencyId = CryptoCurrency.ID.fromValue(value = "token⟨solana⟩solana⚓1234567890"),
                expected = null,
            ),
            CreateModel(
                currencyId = MockCryptoCurrencyFactory().createCoin(Blockchain.Solana).id,
                expected = StakingIntegrationID.StakeKit.Coin.Solana,
            ),
            CreateModel(
                currencyId = MockCryptoCurrencyFactory().createToken(
                    blockchain = Blockchain.Ethereum,
                    id = "tether",
                    contractAddress = "0xdac17f958d2ee523a2206206994597c13d831ec7",
                ).id,
                expected = null,
            ),
            CreateModel(
                currencyId = MockCryptoCurrencyFactory().createToken(
                    blockchain = Blockchain.Ethereum,
                    id = "polygon-ecosystem-token",
                    contractAddress = MockStaking.POLYGON_CONTRACT,
                ).id,
                expected = StakingIntegrationID.StakeKit.EthereumToken.Polygon,
            ),
            CreateModel(
                currencyId = CryptoCurrency.ID.fromValue(value = "token⟨ethereum⟩polygon-ecosystem-token⚓"),
                expected = StakingIntegrationID.P2PEthPool,
            ),
            CreateModel(
                currencyId = CryptoCurrency.ID.fromValue(value = "token⟨bsc⟩polygon-ecosystem-token⚓0x1234"),
                expected = null,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsStakingSupported {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN blockchain WHEN isStakingSupported read THEN matches integration list`(model: SupportedModel) {
            // Act
            val actual = model.blockchain.isStakingSupported

            // Assert
            Truth.assertThat(actual).isEqualTo(model.expected)
        }

        @Test
        fun `GIVEN solana network WHEN isStakingSupported read THEN true`() {
            // Arrange
            val network = MockCryptoCurrencyFactory().createCoin(Blockchain.Solana).network

            // Act
            val actual = network.isStakingSupported

            // Assert
            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `GIVEN bitcoin network WHEN isStakingSupported read THEN false`() {
            // Arrange
            val network = MockCryptoCurrencyFactory().createCoin(Blockchain.Bitcoin).network

            // Act
            val actual = network.isStakingSupported

            // Assert
            Truth.assertThat(actual).isFalse()
        }

        private fun provideTestModels() = listOf(
            SupportedModel(Blockchain.TON, true),
            SupportedModel(Blockchain.Solana, true),
            SupportedModel(Blockchain.Cosmos, true),
            SupportedModel(Blockchain.Tron, true),
            SupportedModel(Blockchain.BSC, true),
            SupportedModel(Blockchain.Cardano, true),
            SupportedModel(Blockchain.Ethereum, true),
            SupportedModel(Blockchain.Bitcoin, false),
            SupportedModel(Blockchain.Polygon, false),
            SupportedModel(Blockchain.Binance, false),
            SupportedModel(Blockchain.EthereumTestnet, false),
            SupportedModel(Blockchain.Polkadot, false),
            SupportedModel(Blockchain.Avalanche, false),
        )
    }

    data class CreateModel(val currencyId: CryptoCurrency.ID, val expected: StakingIntegrationID?)

    data class SupportedModel(val blockchain: Blockchain, val expected: Boolean)

    private fun createCurrencyId(blockchain: Blockchain): CryptoCurrency.ID {
        return CryptoCurrency.ID.fromValue(value = "coin⟨${blockchain.toNetworkId()}⟩")
    }
}