package com.tangem.domain.payment.models.auth

import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.hdWallet.DerivationPath

data class PaymentAuthConfig(
    val customDerivationPath: DerivationPath,
    val curve: EllipticCurve = EllipticCurve.Secp256k1,
    val blockchainId: String,
    val singMessage: (nonce: String) -> String,
)