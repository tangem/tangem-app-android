package com.tangem.domain.wallets.derivations

import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet

val UserWallet.derivationStyleProvider: DerivationStyleProvider
    get() = when (this) {
        is UserWallet.Cold -> scanResponse.derivationStyleProvider
        is UserWallet.Hot -> TangemHotDerivationStyleProvider()
    }

val ScanResponse.derivationStyleProvider: DerivationStyleProvider
    get() = card.derivationStyleProvider

val CardDTO.derivationStyleProvider: DerivationStyleProvider
    get() = TangemDerivationStyleProvider(this)