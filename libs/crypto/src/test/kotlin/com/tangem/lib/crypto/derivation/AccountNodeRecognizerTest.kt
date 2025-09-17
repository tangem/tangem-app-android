package com.tangem.lib.crypto.derivation

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.crypto.hdWallet.DerivationPath
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class AccountNodeRecognizerTest {

    private val utxoBlockchain = Blockchain.Bitcoin
    private val nonUtxoBlockchain = Blockchain.Ethereum

    @Nested
    inner class RecognizeAsDerivationPath {

        @Test
        fun `returns account node value for UTXO blockchain`() {
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
        fun `returns account node value for non-UTXO blockchain`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(nonUtxoBlockchain)
            val derivationPath = DerivationPath(rawPath = "m/44'/0'/0'/0/0")

            // Act
            val actual = recognizer.recognize(derivationPath)

            // Assert
            val expected = 0
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `returns null if derivation path is shorter than expected`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(nonUtxoBlockchain)
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
        fun `returns account node value for UTXO blockchain`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(utxoBlockchain)
            val derivationPath = "m/44'/0'/1'/0/0"

            // Act
            val actual = recognizer.recognize(derivationPath)

            // Assert
            val expected = 1
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `returns account node value for non-UTXO blockchain`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(nonUtxoBlockchain)
            val derivationPath = "m/44'/0'/0'/0/0"

            // Act
            val actual = recognizer.recognize(derivationPath)

            // Assert
            val expected = 0
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `returns null if derivation path is shorter than expected`() {
            // Arrange
            val recognizer = AccountNodeRecognizer(nonUtxoBlockchain)
            val derivationPath = "m/44'/0'"

            // Act
            val actual = recognizer.recognize(derivationPath)

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