package com.tangem.tap.domain.sdk.mocks.wallet2

import com.tangem.common.SuccessResponse
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.operations.derivation.DerivationTaskResponse
import com.tangem.operations.wallet.CreateWalletResponse
import com.tangem.tap.domain.sdk.mocks.Mocks
import com.tangem.tap.domain.tasks.product.CreateProductWalletTaskResponse

object Wallet2Mocks : Mocks {

    override val scanResponse: ScanResponse
        get() = TODO("Not yet implemented")

    override val cardDto: CardDTO
        get() = TODO("Not yet implemented")

    override val derivationTaskResponse: DerivationTaskResponse
        get() = TODO("Not yet implemented")

    override val extendedPublicKey: ExtendedPublicKey
        get() = TODO("Not yet implemented")

    override val successResponse: SuccessResponse
        get() = TODO("Not yet implemented")

    override val createProductWalletTaskResponse: CreateProductWalletTaskResponse
        get() = TODO("Not yet implemented")

    override val importWalletResponse: CreateProductWalletTaskResponse
        get() = TODO("Not yet implemented")

    override val createFirstTwinResponse: CreateWalletResponse
        get() = error("Available only for Twin")

    override val createSecondTwinResponse: CreateWalletResponse
        get() = error("Available only for Twin")

    override val finalizeTwinResponse: ScanResponse
        get() = error("Available only for Twin")
}