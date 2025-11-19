package com.tangem.domain.staking

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.staking.model.StakingApproval
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.test.core.ProvideTestModels
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
    fun `all IDs are unique`() {
        // Act
        val actual = StakingIntegrationID.entries.distinctBy { it.value }

        // Assert
        val expected = StakingIntegrationID.entries.size
        Truth.assertThat(actual).hasSize(expected)
    }

    @Test
    fun `all Coin blockchains are unique`() {
        // Act
        val actual = StakingIntegrationID.StakeKit.Coin.entries
            .distinctBy(StakingIntegrationID.StakeKit.Coin::blockchain)

        // Assert
        val expected = StakingIntegrationID.StakeKit.Coin.entries.size
        Truth.assertThat(actual).hasSize(expected)
    }

    @Test
    fun `all P2P blockchains are unique`() {
        // Act
        val actual = StakingIntegrationID.P2P.entries
            .distinctBy(StakingIntegrationID.P2P::blockchain)

        // Assert
        val expected = StakingIntegrationID.P2P.entries.size
        Truth.assertThat(actual).hasSize(expected)
    }

    @Test
    fun `all sub blockchains are unique`() {
        // Act
        val actual = StakingIntegrationID.StakeKit.EthereumToken.entries
            .distinctBy(StakingIntegrationID.StakeKit.EthereumToken::subBlockchain)

        // Assert
        val expected = StakingIntegrationID.StakeKit.EthereumToken.entries.size
        Truth.assertThat(actual).hasSize(expected)
    }

    @Test
    fun `all spenderAddress of StakingApproval are unique`() {
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
    fun `all EthereumToken IDs are ethereum based blockchains`() {
        // Act
        val actual = StakingIntegrationID.StakeKit.EthereumToken.entries
            .map(StakingIntegrationID.StakeKit.EthereumToken::subBlockchain)
            .all { it.isEvm() }

        // Assert
        Truth.assertThat(actual).isTrue()
    }

    @Test
    fun `all Coin IDs do not need staking approval`() {
        // Act
        val actual = StakingIntegrationID.StakeKit.Coin.entries
            .map(StakingIntegrationID.StakeKit.Coin::approval)
            .none { it is StakingApproval.Needed }

        // Assert
        Truth.assertThat(actual).isTrue()
    }

    @Test
    fun `all EthereumToken IDs need staking approval`() {
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

        @ParameterizedTest
        @ProvideTestModels
        fun create(model: CreateModel) {
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
                currencyId = createCurrencyId(blockchain = Blockchain.Ethereum),
                expected = StakingIntegrationID.P2P.EthereumPooled,
            ),
            CreateModel(
                currencyId = CryptoCurrency.ID.fromValue(value = "token⟨ETH⟩polygon-ecosystem-token⚓1234567890"),
                expected = StakingIntegrationID.StakeKit.EthereumToken.Polygon,
            ),
            CreateModel(
                currencyId = CryptoCurrency.ID.fromValue(value = "token⟨SOLANA⟩solana⚓1234567890"),
                expected = null,
            ),
        )
    }

    data class CreateModel(val currencyId: CryptoCurrency.ID, val expected: StakingIntegrationID?)

    private fun createCurrencyId(blockchain: Blockchain): CryptoCurrency.ID {
        return CryptoCurrency.ID.fromValue(value = "coin⟨${blockchain.id}⟩")
    }
}