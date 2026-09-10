package com.tangem.tap.domain.walletregistration

import android.util.Base64
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.lib.auth.session.WalletSigner
import com.tangem.lib.auth.session.WalletSignerProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

/**
 * Cold wallets are left out on purpose: their signature needs an open card session, which only the
 * flow owning the NFC tap can supply.
 */
internal class DefaultWalletSignerProvider @Inject constructor(
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val mobileSigner: MobileWalletRegistrationSigner,
) : WalletSignerProvider {

    override suspend fun signerFor(walletId: String): WalletSigner? {
        val userWalletId = runCatching { UserWalletId(Base64.decode(walletId, Base64.NO_WRAP)) }.getOrNull()
            ?: return null
        // the use case throws rather than returning a Left while the wallet list is still loading, and
        // callers of this one read `null` as "no signer", not as a crash
        val userWallet = runSuspendCatching { getUserWalletUseCase(userWalletId).getOrNull() }
            .onFailure { TangemLogger.e("Cannot resolve the wallet to sign with", it) }
            .getOrNull() as? UserWallet.Hot
            ?: return null

        return mobileSigner.signerFor(userWallet)
    }
}