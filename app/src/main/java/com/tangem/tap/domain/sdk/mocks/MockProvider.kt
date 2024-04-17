package com.tangem.tap.domain.sdk.mocks

import com.tangem.domain.models.scan.ProductType
import com.tangem.tap.domain.sdk.mocks.wallet.WalletMocks
import com.tangem.tap.domain.sdk.mocks.wallet2.Wallet2Mocks

object MockProvider {

    var productType: ProductType = ProductType.Wallet

    fun getScanResponse() = getMocks(productType).scanResponse

    private fun getMocks(productType: ProductType): Mocks {
        return when (productType) {
            ProductType.Wallet -> WalletMocks
            ProductType.Wallet2 -> Wallet2Mocks
            else -> TODO()
        }
    }
}