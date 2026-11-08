package com.tangem.tap.domain.sdk.mocks

import android.util.Base64
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.lib.auth.session.WalletRegistrar
import com.tangem.lib.auth.session.WalletSignatureBundle
import com.tangem.lib.auth.session.WalletSigner
import com.tangem.tap.domain.walletregistration.isColdRegistrationSupported
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import dagger.Lazy

internal class MockColdWalletRegistration(
    private val walletRegistrar: Lazy<WalletRegistrar>,
) {

    suspend fun register(card: CardDTO) {
        if (!card.isColdRegistrationSupported) return
        val walletId = UserWalletIdBuilder.card(card).build() ?: return

        runSuspendCatching {
            walletRegistrar.get().register(
                walletId = Base64.encodeToString(walletId.value, Base64.NO_WRAP),
                signer = CannedSigner,
            ).onLeft { error -> TangemLogger.e("Mock cold wallet registration deferred: $error") }
        }.onFailure { error -> TangemLogger.e("Mock cold wallet registration failed", error) }
    }

    private object CannedSigner : WalletSigner {
        override suspend fun sign(nonceBytes: ByteArray): WalletSignatureBundle = WalletSignatureBundle(
            walletSignature = ByteArray(size = SIGNATURE_SIZE),
            walletSignatureSalt = ByteArray(size = SALT_SIZE),
            cardSignature = ByteArray(size = SIGNATURE_SIZE),
            cardSignatureSalt = ByteArray(size = SALT_SIZE),
            walletStatusByte = WALLET_GENERATED_ON_CARD,
        )
    }

    private companion object {
        const val SIGNATURE_SIZE = 65
        const val SALT_SIZE = 16
        const val WALLET_GENERATED_ON_CARD: Byte = 0x82.toByte()
    }
}