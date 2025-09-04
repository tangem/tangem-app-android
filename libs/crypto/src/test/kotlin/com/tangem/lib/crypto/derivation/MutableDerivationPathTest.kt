package com.tangem.lib.crypto.derivation

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.crypto.hdWallet.DerivationPath
import org.junit.jupiter.api.Test

internal class MutableDerivationPathTest {

    private val utxoBlockchain = Blockchain.Bitcoin
    private val nonUtxoBlockchain = Blockchain.Ethereum

    @Test
    fun `replaces account node with hardened value`() {
        // Arrange
        val derivationPath = DerivationPath(rawPath = "m/44'/0'/0'/0/0")
        val mutablePath = derivationPath.toMutable()

        // Act
        val actual = mutablePath
            .replaceAccountNode(value = 1, blockchain = utxoBlockchain)
            .apply()

        // Assert
        val expected = DerivationPath(rawPath = "m/44'/0'/1'/0/0")
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `replaces account node with non hardened value`() {
        // Arrange
        val derivationPath = DerivationPath(rawPath = "m/44'/0'/0/0/0")
        val mutablePath = derivationPath.toMutable()

        // Act
        val actual = mutablePath
            .replaceAccountNode(value = 1, blockchain = utxoBlockchain)
            .apply()

        // Assert
        val expected = DerivationPath(rawPath = "m/44'/0'/1/0/0")
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `does nothing if account node not found`() {
        // Arrange
        val derivationPath = DerivationPath(rawPath = "m/44'/0'")
        val mutablePath = derivationPath.toMutable()

        // Act
        val actual = mutablePath
            .replaceAccountNode(value = 1, blockchain = utxoBlockchain)
            .apply()

        // Assert
        val expected = DerivationPath(rawPath = "m/44'/0'")
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `replaces account node with hardened value for non-utxo blockchain`() {
        // Arrange
        val derivationPath = DerivationPath(rawPath = "m/44'/60'/0'/0/0")
        val mutablePath = derivationPath.toMutable()

        // Act
        val actual = mutablePath
            .replaceAccountNode(value = 2, blockchain = nonUtxoBlockchain)
            .apply()

        // Assert
        val expected = DerivationPath(rawPath = "m/44'/60'/0'/0/2")
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `does nothing if account node not found for non-utxo blockchain`() {
        // Arrange
        val derivationPath = DerivationPath(rawPath = "m/44'/60'")
        val mutablePath = derivationPath.toMutable()

        // Act
        val actual = mutablePath
            .replaceAccountNode(value = 2, blockchain = nonUtxoBlockchain)
            .apply()

        // Assert
        val expected = DerivationPath(rawPath = "m/44'/60'")
        Truth.assertThat(actual).isEqualTo(expected)
    }
}