package com.tangem.domain.staking

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.utils.ProvideTestModels
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.staking.model.StakingApproval
import com.tangem.domain.staking.model.StakingIntegrationID
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
    fun `all blockchains are unique`() {
        // Act
        val actual = StakingIntegrationID.entries.distinctBy(StakingIntegrationID::blockchain)

        // Assert
        val expected = StakingIntegrationID.entries.size
        Truth.assertThat(actual).hasSize(expected)
    }

    @Test
    fun `all sub blockchains are unique`() {
        // Act
        val actual = StakingIntegrationID.EthereumToken.entries
            .distinctBy(StakingIntegrationID.EthereumToken::subBlockchain)

        // Assert
        val expected = StakingIntegrationID.EthereumToken.entries.size
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
        val actual = StakingIntegrationID.EthereumToken.entries
            .map(StakingIntegrationID.EthereumToken::subBlockchain)
            .all { it.isEvm() }

        // Assert
        Truth.assertThat(actual).isTrue()
    }

    @Test
    fun `all Coin IDs do not need staking approval`() {
        // Act
        val actual = StakingIntegrationID.Coin.entries
            .map(StakingIntegrationID.Coin::approval)
            .none { it is StakingApproval.Needed }

        // Assert
        Truth.assertThat(actual).isTrue()
    }

    @Test
    fun `all EthereumToken IDs need staking approval`() {
        // Act
        val actual = StakingIntegrationID.EthereumToken.entries
            .map(StakingIntegrationID.EthereumToken::approval)
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
                expected = StakingIntegrationID.Coin.Ton,
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Solana),
                expected = StakingIntegrationID.Coin.Solana,
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Cosmos),
                expected = StakingIntegrationID.Coin.Cosmos,
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Tron),
                expected = StakingIntegrationID.Coin.Tron,
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.BSC),
                expected = StakingIntegrationID.Coin.BSC,
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Cardano),
                expected = StakingIntegrationID.Coin.Cardano,
            ),
            CreateModel(
                currencyId = CryptoCurrency.ID.fromValue(value = "coin⟨ETH⟩polygon-ecosystem-token⚓"),
                expected = StakingIntegrationID.EthereumToken.Polygon,
            ),
        )
    }

    data class CreateModel(val currencyId: CryptoCurrency.ID, val expected: StakingIntegrationID?)

    private fun createCurrencyId(blockchain: Blockchain): CryptoCurrency.ID {
        return CryptoCurrency.ID.fromValue(value = "coin⟨${blockchain.id}⟩")
    }
}