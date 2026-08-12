package com.tangem.domain.wallets.derivations

import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.models.scan.CardDTO

/**
 * Provides default derivation paths (Bitcoin/Ethereum or demo/test-card specific)
 * used when creating or backing up a wallet.
 */
interface DerivationsHelper {

    fun getDefaultDerivations(
        derivationStyleProvider: DerivationStyleProvider,
        cardId: String,
        isTestCard: Boolean,
        wallets: List<CardDTO.Wallet>,
    ): Map<ByteArrayKey, List<DerivationPath>>

    fun getDefaultDerivationsWithCurves(
        derivationStyleProvider: DerivationStyleProvider,
        cardId: String,
        isTestCard: Boolean,
        curves: List<EllipticCurve>,
    ): Map<EllipticCurve, List<DerivationPath>>
}