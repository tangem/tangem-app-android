package com.tangem.lib.crypto.derivation

import com.google.common.truth.Truth
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EllipticCurveDerivationSupportTest {

    @ParameterizedTest
    @ProvideTestModels
    fun supportsDerivationPath(model: TestModel) {
        // Arrange
        val path = DerivationPath(rawPath = model.derivationPath)

        // Act
        val actual = model.curve.supportsDerivationPath(path)

        // Assert
        Truth.assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = provideEd25519Models() +
        provideSecpModels() +
        provideBlsModels()

    private fun provideEd25519Models() = listOf(
        // region ed25519-family require a fully-hardened path
        // Algorand default path — fully hardened
        TestModel(curve = EllipticCurve.Ed25519, derivationPath = "m/44'/283'/0'/0'/0'", expected = true),
        TestModel(curve = EllipticCurve.Ed25519Slip0010, derivationPath = "m/44'/283'/0'/0'/0'", expected = true),
        // The reported bug: Algorand (ed25519) + ApeChain/EVM path with non-hardened tail
        TestModel(curve = EllipticCurve.Ed25519, derivationPath = "m/44'/60'/0'/0/0", expected = false),
        TestModel(curve = EllipticCurve.Ed25519Slip0010, derivationPath = "m/44'/60'/0'/0/0", expected = false),
        // Solana default path — fully hardened
        TestModel(curve = EllipticCurve.Ed25519Slip0010, derivationPath = "m/44'/501'/0'/0'", expected = true),
        // A single non-hardened node is enough to make it unsupported
        TestModel(curve = EllipticCurve.Ed25519, derivationPath = "m/44'/283'/0'/0'/0", expected = false),
        // endregion
    )

    private fun provideSecpModels() = listOf(
        // region secp256k1 / secp256r1 / bip0340 accept any path
        TestModel(curve = EllipticCurve.Secp256k1, derivationPath = "m/44'/60'/0'/0/0", expected = true),
        TestModel(curve = EllipticCurve.Secp256k1, derivationPath = "m/44'/0'/0'/0/0", expected = true),
        TestModel(curve = EllipticCurve.Secp256k1, derivationPath = "m/44'/283'/0'/0'/0'", expected = true),
        TestModel(curve = EllipticCurve.Secp256r1, derivationPath = "m/44'/60'/0'/0/0", expected = true),
        TestModel(curve = EllipticCurve.Bip0340, derivationPath = "m/44'/60'/0'/0/0", expected = true),
        // endregion
    )

    private fun provideBlsModels() = listOf(
        // region BLS curves do not support derivation at all
        TestModel(curve = EllipticCurve.Bls12381G2, derivationPath = "m/44'/60'/0'/0/0", expected = false),
        TestModel(curve = EllipticCurve.Bls12381G2Aug, derivationPath = "m/44'/60'/0'/0'/0'", expected = false),
        TestModel(curve = EllipticCurve.Bls12381G2Pop, derivationPath = "m/44'/60'/0'/0'/0'", expected = false),
        // endregion
    )

    data class TestModel(
        val curve: EllipticCurve,
        val derivationPath: String,
        val expected: Boolean,
    )
}