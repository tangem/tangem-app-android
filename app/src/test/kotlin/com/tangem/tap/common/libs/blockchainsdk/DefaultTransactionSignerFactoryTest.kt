package com.tangem.tap.common.libs.blockchainsdk

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.analytics.models.Basic.TransactionSent.WalletForm
import com.tangem.core.analytics.store.LastSignedWalletFormStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.tap.domain.TangemSignerResponse
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultTransactionSignerFactoryTest {

    private val lastSignedWalletFormStore = mockk<LastSignedWalletFormStore>(relaxed = true)
    private val userWalletsListRepository = mockk<UserWalletsListRepository>()

    private val factory = DefaultTransactionSignerFactory(
        lastSignedWalletFormStore = lastSignedWalletFormStore,
        userWalletsListRepository = userWalletsListRepository,
        coroutineScope = TestAppCoroutineScope(),
    )

    private val baseWallet = MockUserWalletFactory.create()

    /** Wallet that will be the target of the signing operation. */
    private val walletA = baseWallet.scanResponse.card.wallets.first().copy(
        publicKey = PUBLIC_KEY_A,
        totalSignedHashes = 0,
        remainingSignatures = 100,
    )

    /** Another wallet that must stay untouched after signing with [walletA]'s key. */
    private val walletB = baseWallet.scanResponse.card.wallets.first().copy(
        publicKey = PUBLIC_KEY_B,
        totalSignedHashes = 7,
        remainingSignatures = 50,
    )

    private val userWallet = baseWallet.copy(
        scanResponse = baseWallet.scanResponse.copy(
            card = baseWallet.scanResponse.card.copy(wallets = listOf(walletA, walletB)),
        ),
    )

    private val savedWalletSlot = slot<UserWallet>()

    @BeforeEach
    fun setup() {
        clearMocks(lastSignedWalletFormStore, userWalletsListRepository)

        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
        coEvery { userWalletsListRepository.saveWithoutLock(capture(savedWalletSlot), any()) } answers {
            savedWalletSlot.captured.right()
        }
    }

    @Test
    fun `updates signed hashes only for the wallet matching the signed public key`() {
        factory.onSignerResponse(
            userWalletId = userWallet.walletId,
            signResponse = signerResponse(
                signedWalletPublicKey = PUBLIC_KEY_A,
                totalSignedHashes = 5,
                remainingSignatures = 95,
            ),
        )

        val savedWallets = (savedWalletSlot.captured as UserWallet.Cold).scanResponse.card.wallets
        val savedA = savedWallets.first { it.publicKey.contentEquals(PUBLIC_KEY_A) }
        val savedB = savedWallets.first { it.publicKey.contentEquals(PUBLIC_KEY_B) }

        assertThat(savedA.totalSignedHashes).isEqualTo(5)
        assertThat(savedA.remainingSignatures).isEqualTo(95)
        // The non-signed wallet must keep its original values.
        assertThat(savedB.totalSignedHashes).isEqualTo(7)
        assertThat(savedB.remainingSignatures).isEqualTo(50)
    }

    @Test
    fun `keeps previously known counters when the signer response has null values`() {
        factory.onSignerResponse(
            userWalletId = userWallet.walletId,
            signResponse = signerResponse(
                signedWalletPublicKey = PUBLIC_KEY_A,
                totalSignedHashes = null,
                remainingSignatures = null,
            ),
        )

        val savedA = (savedWalletSlot.captured as UserWallet.Cold).scanResponse.card.wallets
            .first { it.publicKey.contentEquals(PUBLIC_KEY_A) }

        // Null response values must not overwrite the known counters.
        assertThat(savedA.totalSignedHashes).isEqualTo(0)
        assertThat(savedA.remainingSignatures).isEqualTo(100)
    }

    @Test
    fun `leaves all wallets untouched when no public key matches`() {
        factory.onSignerResponse(
            userWalletId = userWallet.walletId,
            signResponse = signerResponse(
                signedWalletPublicKey = UNKNOWN_PUBLIC_KEY,
                totalSignedHashes = 5,
                remainingSignatures = 95,
            ),
        )

        val savedWallets = (savedWalletSlot.captured as UserWallet.Cold).scanResponse.card.wallets
        assertThat(savedWallets.first { it.publicKey.contentEquals(PUBLIC_KEY_A) }.totalSignedHashes).isEqualTo(0)
        assertThat(savedWallets.first { it.publicKey.contentEquals(PUBLIC_KEY_B) }.totalSignedHashes).isEqualTo(7)
    }

    @Test
    fun `updates last signed wallet form with Card for a non-ring response`() {
        factory.onSignerResponse(
            userWalletId = userWallet.walletId,
            signResponse = signerResponse(signedWalletPublicKey = PUBLIC_KEY_A, isRing = false),
        )

        verify(exactly = 1) { lastSignedWalletFormStore.update(WalletForm.Card) }
    }

    @Test
    fun `updates last signed wallet form with Ring for a ring response`() {
        factory.onSignerResponse(
            userWalletId = userWallet.walletId,
            signResponse = signerResponse(signedWalletPublicKey = PUBLIC_KEY_A, isRing = true),
        )

        verify(exactly = 1) { lastSignedWalletFormStore.update(WalletForm.Ring) }
    }

    private fun signerResponse(
        signedWalletPublicKey: ByteArray,
        totalSignedHashes: Int? = 1,
        remainingSignatures: Int? = 1,
        isRing: Boolean = false,
    ) = TangemSignerResponse(
        totalSignedHashes = totalSignedHashes,
        remainingSignatures = remainingSignatures,
        isRing = isRing,
        signedWalletPublicKey = signedWalletPublicKey,
    )

    private companion object {
        val PUBLIC_KEY_A = byteArrayOf(1, 2, 3)
        val PUBLIC_KEY_B = byteArrayOf(4, 5, 6)
        val UNKNOWN_PUBLIC_KEY = byteArrayOf(9, 9, 9)
    }
}