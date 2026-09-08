package com.tangem.domain.collectibles.account.derivation

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class CollectiblesOwnerDerivationPathTest {

    /**
     * The vault address is `PDA(["vault", owner])`, so this path decides which vault a wallet owns. Editing it

     */
    @Test
    fun `GIVEN the owner derivation path WHEN read THEN it is the one vaults were created with`() {
        // Assert
        assertThat(COLLECTIBLES_OWNER_DERIVATION_PATH).isEqualTo("m/44'/501'/999700'/0'/0'")
    }
}