package com.tangem.lib.crypto.derivation

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.crypto.hdWallet.DerivationPath
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class AccountNodeRecognizerTest {

    private val utxoBlockchain = Blockchain.Bitcoin
    private val ethLikeBlockchain = Blockchain.Ethereum

    @Nested
    inner class RecognizeAsDerivationPath {

        @Test
        fun `returns account node value for a derivation path with 5 nodes and UTXO blockchain`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(utxoBlockchain)
            val derivationPath = DerivationPath(rawPath = "m/44'/0'/1'/0/0")

            // Act
            val actual = recognizer.recognize(derivationPath)

            // Assert
            val expected = 1
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `returns account node value for a derivation path with 4 nodes and UTXO blockchain`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(utxoBlockchain)
            val derivationPath = DerivationPath(rawPath = "m/44'/0'/1'/0")

            // Act
            val actual = recognizer.recognize(derivationPath)

            // Assert
            Truth.assertThat(actual).isNull()
        }

        @Test
        fun `returns account node value for a derivation path with 5 nodes and non-UTXO blockchain`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(ethLikeBlockchain)
            val derivationPath = DerivationPath(rawPath = "m/44'/0'/1'/2/3")

            // Act
            val actual = recognizer.recognize(derivationPath)

            // Assert
            val expected = 3
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `returns account node value for a derivation path with 4 nodes and non-UTXO blockchain`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(ethLikeBlockchain)
            val derivationPath = DerivationPath(rawPath = "m/44'/0'/1'/2")

            // Act
            val actual = recognizer.recognize(derivationPath)

            // Assert
            Truth.assertThat(actual).isNull()
        }

        @Test
        fun `returns account node value for a derivation path with 3 nodes and non-UTXO blockchain`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(ethLikeBlockchain)
            val derivationPath = DerivationPath(rawPath = "m/44'/0'/1'")

            // Act
            val actual = recognizer.recognize(derivationPath)

            // Assert
            val expected = 1
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `returns account node value for a derivation path with 4 nodes and Tezos blockchain`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(Blockchain.Tezos)
            val derivationPath = DerivationPath(rawPath = "m/44'/0'/0/5'")

            // Act
            val actual = recognizer.recognize(derivationPath)

            // Assert
            val expected = 0
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `returns account node value for a derivation path with 5 nodes and Tezos blockchain`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(Blockchain.Tezos)
            val derivationPath = DerivationPath(rawPath = "m/44'/0'/0'/5/1")

            // Act
            val actual = recognizer.recognize(derivationPath)

            // Assert
            Truth.assertThat(actual).isNull()
        }

        @Test
        fun `returns account node value for a derivation path with 4 nodes and Quai blockchain`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(Blockchain.Quai)
            val derivationPath = DerivationPath(rawPath = "m/44'/0'/0/5'")

            // Act
            val actual = recognizer.recognize(derivationPath)

            // Assert
            Truth.assertThat(actual).isNull()
        }

        @Test
        fun `returns account node value for a derivation path with 5 nodes and Quai blockchain`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(Blockchain.Quai)
            val derivationPath = DerivationPath(rawPath = "m/44'/0'/0'/5/1")

            // Act
            val actual = recognizer.recognize(derivationPath)

            // Assert
            val expected = 0
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `returns null if derivation path is shorter than expected`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(ethLikeBlockchain)
            val derivationPath = DerivationPath(rawPath = "m/44'/0'")

            // Act
            val actual = recognizer.recognize(derivationPath)

            // Assert
            Truth.assertThat(actual).isNull()
        }
    }

    @Nested
    inner class RecognizeAsString {

        @Test
        fun `returns account node value for a derivation path with 5 nodes and UTXO blockchain`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(utxoBlockchain)
            val derivationPathValue = "m/44'/0'/1'/0/0"

            // Act
            val actual = recognizer.recognize(derivationPathValue)

            // Assert
            val expected = 1
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `returns account node value for a derivation path with 4 nodes and UTXO blockchain`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(utxoBlockchain)
            val derivationPathValue = "m/44'/0'/1'/0"

            // Act
            val actual = recognizer.recognize(derivationPathValue)

            // Assert
            Truth.assertThat(actual).isNull()
        }

        @Test
        fun `returns account node value for a derivation path with 5 nodes and non-UTXO blockchain`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(ethLikeBlockchain)
            val derivationPathValue = "m/44'/0'/1'/2/3"

            // Act
            val actual = recognizer.recognize(derivationPathValue)

            // Assert
            val expected = 3
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `returns account node value for a derivation path with 4 nodes and non-UTXO blockchain`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(ethLikeBlockchain)
            val derivationPathValue = "m/44'/0'/1'/2"

            // Act
            val actual = recognizer.recognize(derivationPathValue)

            // Assert
            Truth.assertThat(actual).isNull()
        }

        @Test
        fun `returns account node value for a derivation path with 3 nodes and non-UTXO blockchain`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(ethLikeBlockchain)
            val derivationPathValue = "m/44'/0'/1'"

            // Act
            val actual = recognizer.recognize(derivationPathValue)

            // Assert
            val expected = 1
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `returns account node value for a derivation path with 4 nodes and Tezos blockchain`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(Blockchain.Tezos)
            val derivationPathValue = "m/44'/0'/0/5'"

            // Act
            val actual = recognizer.recognize(derivationPathValue)

            // Assert
            val expected = 0
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `returns account node value for a derivation path with 5 nodes and Tezos blockchain`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(Blockchain.Tezos)
            val derivationPathValue = "m/44'/0'/0'/5/1"

            // Act
            val actual = recognizer.recognize(derivationPathValue)

            // Assert
            Truth.assertThat(actual).isNull()
        }

        @Test
        fun `returns account node value for a derivation path with 4 nodes and Quai blockchain`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(Blockchain.Quai)
            val derivationPathValue = "m/44'/0'/0/5'"

            // Act
            val actual = recognizer.recognize(derivationPathValue)

            // Assert
            Truth.assertThat(actual).isNull()
        }

        @Test
        fun `returns account node value for a derivation path with 5 nodes and Quai blockchain`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(Blockchain.Quai)
            val derivationPathValue = "m/44'/0'/0'/5/1"

            // Act
            val actual = recognizer.recognize(derivationPathValue)

            // Assert
            val expected = 0
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `returns null if derivation path is shorter than expected`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(ethLikeBlockchain)
            val derivationPathValue = "m/44'/0'"

            // Act
            val actual = recognizer.recognize(derivationPathValue)

            // Assert
            Truth.assertThat(actual).isNull()
        }

        @Test
        fun `returns null if derivation path string is invalid`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(utxoBlockchain)
            val derivationPathValue = "invalid/path"

            // Act
            val actual = recognizer.recognize(derivationPathValue)

            // Assert
            Truth.assertThat(actual).isNull()
        }
    }
}