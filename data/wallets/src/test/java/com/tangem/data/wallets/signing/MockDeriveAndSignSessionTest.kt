package com.tangem.data.wallets.signing

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.UnmarshalHelper
import com.tangem.blockchain.common.Wallet
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.domain.wallets.signing.SignRequest
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class MockDeriveAndSignSessionTest {

    private val derivationsRepository: DerivationsRepository = mockk(relaxUnitFun = true)
    private val sut = MockDeriveAndSignSession(derivationsRepository)

    @Test
    fun `GIVEN a hash to sign WHEN run THEN the signature recovers to the derived key`() = runTest {
        // Arrange
        val wallet = MockUserWalletFactory.create()
        coEvery { derivationsRepository.getExistingDerivedKeys(wallet, ByteArrayKey(SEED_KEY)) } returns
            ExtendedPublicKeysMap(emptyMap())
        var derivedKey: ExtendedPublicKey? = null

        // Act
        val signed = sut.run(wallet, SEED_KEY, PATH) { key ->
            derivedKey = key
            SignRequest(value = Unit, hashes = listOf(HASH))
        }.getOrNull()

        // Assert
        val publicKey = Wallet.PublicKey(
            seedKey = SEED_KEY,
            derivationType = Wallet.PublicKey.DerivationType.Plain(
                Wallet.HDKey(extendedPublicKey = requireNotNull(derivedKey), path = PATH),
            ),
        )
        val actual = runCatching {
            UnmarshalHelper.unmarshalSignatureExtended(
                signature = requireNotNull(signed).signatures.single(),
                hash = HASH,
                publicKey = publicKey,
            )
        }
        assertThat(actual.exceptionOrNull()).isNull()
    }

    private companion object {
        val SEED_KEY = "02".repeat(33).hexToBytes()
        val PATH = DerivationPath("m/44'/60'/0'/0/0")
        val HASH = "ab".repeat(32).hexToBytes()
    }
}