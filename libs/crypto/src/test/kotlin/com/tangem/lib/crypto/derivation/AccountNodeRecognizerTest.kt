package com.tangem.lib.crypto.derivation

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.models.network.Network
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AccountNodeRecognizerTest {

    private val utxoBlockchain = Blockchain.Bitcoin

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Recognize {

        @ParameterizedTest
        @ProvideTestModels
        fun recognize(model: TestModel) {
            // Arrange
            val recognizer = AccountNodeRecognizer(blockchain = model.blockchain)
            val derivationPath = DerivationPath(rawPath = model.derivationPath)

            // Act
            val actual = recognizer.recognize(derivationPath)

            // Assert
            Truth.assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<TestModel> {
            return provideEthLikeTestModels() +
                provideUTXOTestModels() +
                provideOtherBlockchainTests()
        }

        private fun provideEthLikeTestModels() = listOf(
            // region Tezos blockchain
            TestModel(
                blockchain = Blockchain.Tezos,
                derivationPath = "m/44'/1729'/1'/0/0",
                expected = 1,
            ),
            TestModel(
                blockchain = Blockchain.Tezos,
                derivationPath = "m/44'/1729'/1'/0",
                expected = 1,
            ),
            TestModel(
                blockchain = Blockchain.Tezos,
                derivationPath = "m/44'/1729'/1'",
                expected = 1,
            ),
            TestModel(
                blockchain = Blockchain.Tezos,
                derivationPath = "m/44'/1729'",
                expected = null,
            ),
            // endregion

            // region Quai blockchain
            TestModel(
                blockchain = Blockchain.Quai,
                derivationPath = "m/44'/994'/1'/0",
                expected = 1,
            ),
            TestModel(
                blockchain = Blockchain.Quai,
                derivationPath = "m/44'/994'/1'",
                expected = 1,
            ),
            TestModel(
                blockchain = Blockchain.Quai,
                derivationPath = "m/44'/994'",
                expected = null,
            ),
            // endregion

            // region Ethereum-like blockchain
            TestModel(
                blockchain = Blockchain.Ethereum,
                derivationPath = "m/44'/60'/0'/0/1",
                expected = 1,
            ),
            TestModel(
                blockchain = Blockchain.Ethereum,
                derivationPath = "m/44'/60'/0'/0",
                expected = null,
            ),
            TestModel(
                blockchain = Blockchain.Ethereum,
                derivationPath = "m/44'/60'/0'",
                expected = null,
            ),
            TestModel(
                blockchain = Blockchain.Ethereum,
                derivationPath = "m/44'/60'",
                expected = null,
            ),
            // endregion
        )

        private fun provideUTXOTestModels() = listOf(
            // region Bitcoin blockchain
            TestModel(
                blockchain = Blockchain.Bitcoin,
                derivationPath = "m/44'/0'/1'/0/0",
                expected = 1,
            ),
            TestModel(
                blockchain = Blockchain.Bitcoin,
                derivationPath = "m/44'/0'/1'/0",
                expected = 1,
            ),
            TestModel(
                blockchain = Blockchain.Bitcoin,
                derivationPath = "m/44'/0'/1'",
                expected = 1,
            ),
            TestModel(
                blockchain = Blockchain.Bitcoin,
                derivationPath = "m/44'/0'",
                expected = null,
            ),
            TestModel(
                blockchain = Blockchain.Bitcoin,
                derivationPath = "m/44'",
                expected = null,
            ),
            // endregion
        )

        private fun provideOtherBlockchainTests(): List<TestModel> = listOf(
            // region Solana blockchain
            TestModel(
                blockchain = Blockchain.Solana,
                derivationPath = "m/44'/501'/1'",
                expected = 1,
            ),
            TestModel(
                blockchain = Blockchain.Solana,
                derivationPath = "m/44'/501'",
                expected = null,
            ),
            // endregion

            // region Cardano blockchain
            TestModel(
                blockchain = Blockchain.Cardano,
                derivationPath = "m/1852'/1815'/1'/0/0",
                expected = 1,
            ),
            TestModel(
                blockchain = Blockchain.Cardano,
                derivationPath = "m/1852'/1815'/1'/0",
                expected = 1,
            ),
            TestModel(
                blockchain = Blockchain.Cardano,
                derivationPath = "m/1852'/1815'/1'",
                expected = 1,
            ),
            TestModel(
                blockchain = Blockchain.Cardano,
                derivationPath = "m/1852'/1815'",
                expected = null,
            ),
            // endregion

            // region Tron blockchain
            TestModel(
                blockchain = Blockchain.Tron,
                derivationPath = "m/44'/195'/1'/0/0",
                expected = 1,
            ),
            TestModel(
                blockchain = Blockchain.Tron,
                derivationPath = "m/44'/195'/1'/0",
                expected = 1,
            ),
            TestModel(
                blockchain = Blockchain.Tron,
                derivationPath = "m/44'/195'/1'",
                expected = 1,
            ),
            TestModel(
                blockchain = Blockchain.Tron,
                derivationPath = "m/44'/195'",
                expected = null,
            ),
            // endregion
        )
    }

    data class TestModel(
        val blockchain: Blockchain,
        val derivationPath: String,
        val expected: Long?,
    )

    @Nested
    inner class RecognizeAsString {

        @Test
        fun `GIVEN empty derivation path THEN returns null`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(utxoBlockchain)
            val derivationPathValue = "invalid/path"

            // Act
            val actual = recognizer.recognize(derivationPathValue)

            // Assert
            Truth.assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN invalid derivation path THEN returns null`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(utxoBlockchain)
            val derivationPathValue = "invalid/path"

            // Act
            val actual = recognizer.recognize(derivationPathValue)

            // Assert
            Truth.assertThat(actual).isNull()
        }
    }

    @Nested
    inner class RecognizeAsNetworkDerivationPath {

        @Test
        fun `GIVEN empty derivation path THEN returns null`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(utxoBlockchain)
            val derivationPath = Network.DerivationPath.None

            // Act
            val actual = recognizer.recognize(derivationPath)

            // Assert
            Truth.assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN invalid derivation path THEN returns null`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(utxoBlockchain)
            val derivationPathValue = Network.DerivationPath.Card("invalid/path")

            // Act
            val actual = recognizer.recognize(derivationPathValue)

            // Assert
            Truth.assertThat(actual).isNull()
        }
    }
}