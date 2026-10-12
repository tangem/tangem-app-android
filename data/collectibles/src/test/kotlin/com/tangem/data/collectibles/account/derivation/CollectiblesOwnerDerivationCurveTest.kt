package com.tangem.data.collectibles.account.derivation

import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.collectibles.account.derivation.COLLECTIBLES_OWNER_DERIVATION_PATH
import com.tangem.lib.crypto.derivation.supportsDerivationPath
import org.junit.jupiter.api.Test

/**
 * The vault owner is a Solana key, and Solana keys live on `Ed25519Slip0010` (see `Wallet2CardConfig`). A path that
 * curve cannot derive yields no key and no address, so the constant has to satisfy it.
 */
internal class CollectiblesOwnerDerivationCurveTest {

    @Test
    fun `GIVEN owner derivation path WHEN checked against the Solana curve THEN it is derivable`() {
        // Arrange
        val path = DerivationPath(rawPath = COLLECTIBLES_OWNER_DERIVATION_PATH)

        // Act
        val actual = EllipticCurve.Ed25519Slip0010.supportsDerivationPath(path)

        // Assert
        assertThat(actual).isTrue()
    }
}