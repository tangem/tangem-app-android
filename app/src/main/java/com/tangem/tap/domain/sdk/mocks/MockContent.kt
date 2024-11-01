package com.tangem.tap.domain.sdk.mocks

import com.tangem.common.SuccessResponse
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.operations.derivation.DerivationTaskResponse
import com.tangem.operations.wallet.CreateWalletResponse
import com.tangem.sdk.api.CreateProductWalletTaskResponse

interface MockContent {

    val successResponse: SuccessResponse

    val scanResponse: ScanResponse

    val derivationTaskResponse: DerivationTaskResponse

    val cardDto: CardDTO

    val extendedPublicKey: ExtendedPublicKey

    val createProductWalletTaskResponse: CreateProductWalletTaskResponse

    // Wallet2-specific
    val importWalletResponse: CreateProductWalletTaskResponse

    // Twin-specific
    val finalizeTwinResponse: ScanResponse

    // Twin-specific
    val createFirstTwinResponse: CreateWalletResponse

    // Twin-specific
    val createSecondTwinResponse: CreateWalletResponse
}