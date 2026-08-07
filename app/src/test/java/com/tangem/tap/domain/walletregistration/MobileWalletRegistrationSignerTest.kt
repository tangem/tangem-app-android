package com.tangem.tap.domain.walletregistration

import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.CryptoUtils
import com.tangem.crypto.CryptoUtils.generatePublicKey
import com.tangem.crypto.Secp256k1
import com.tangem.domain.models.MobileWallet
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.hot.HotWalletAccessor
import com.tangem.hot.sdk.model.DataToSign
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.hot.sdk.model.SignedData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MobileWalletRegistrationSignerTest {

    init {
        CryptoUtils.initCrypto()
    }

    private val curve = EllipticCurve.Secp256k1
    private val hotWalletAccessor: HotWalletAccessor = mockk()
    private val signer = MobileWalletRegistrationSigner(hotWalletAccessor)

    private val walletPrivateKey = ByteArray(KEY) { 1 }
    private val walletPublicKey = generatePublicKey(walletPrivateKey, curve)
    private val nonceBytes = "nonce-value".toByteArray()

    @Test
    fun `signer produces 65-byte RSV over the wallet nonce with null card fields`() = runTest {
        val userWallet = hotWallet()
        // The hot SDK signs the provided hash directly (no re-hash), like the real signHashes.
        coEvery { hotWalletAccessor.signHashes(any(), any()) } answers {
            val hash = secondArg<List<DataToSign>>().first().hashes.first()
            listOf(SignedData(curve = curve, signatures = listOf(Secp256k1.ecdsaSignDigest(hash, walletPrivateKey))))
        }

        val bundle = signer.signerFor(userWallet).sign(nonceBytes)

        assertThat(bundle.walletSignature.size).isEqualTo(RSV)
        assertThat(bundle.walletSignature.last().toInt()).isAtLeast(EVM_V_OFFSET)
        assertThat(bundle.walletSignature.last().toInt()).isAtMost(EVM_V_OFFSET + 3)
        assertThat(bundle.walletSignatureSalt.size).isEqualTo(SALT)
        assertThat(bundle.cardSignature).isNull()
        assertThat(bundle.cardSignatureSalt).isNull()
        assertThat(bundle.walletStatusByte).isNull()
    }

    private fun hotWallet(): UserWallet.Hot {
        val mobileWallet = mockk<MobileWallet>()
        every { mobileWallet.curve } returns EllipticCurve.Secp256k1
        every { mobileWallet.publicKey } returns walletPublicKey

        val userWallet = mockk<UserWallet.Hot>()
        every { userWallet.wallets } returns listOf(mobileWallet)
        every { userWallet.hotWalletId } returns mockk<HotWalletId>()
        every { userWallet.walletId } returns UserWalletId(value = ByteArray(KEY) { 5 })
        return userWallet
    }

    private companion object {
        const val KEY = 32
        const val SALT = 16
        const val RSV = 65
        const val EVM_V_OFFSET = 27
    }
}