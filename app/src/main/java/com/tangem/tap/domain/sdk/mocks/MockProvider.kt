package com.tangem.tap.domain.sdk.mocks

import com.tangem.common.CompletionResult
import com.tangem.domain.models.scan.ProductType
import com.tangem.tap.domain.sdk.mocks.wallet.WalletMocks
import com.tangem.tap.domain.sdk.mocks.wallet2.Wallet2Mocks

object MockProvider {

    private var mocks: Mocks = getMocks(ProductType.Wallet)

    fun setMocks(
        productType: ProductType
    ) {
        mocks = getMocks(productType)
    }

    fun getSuccessResponse() = CompletionResult.Success(mocks.successResponse)

    fun getScanResponse() = CompletionResult.Success(mocks.scanResponse)

    fun getDerivationTaskResponse() = CompletionResult.Success(mocks.derivationTaskResponse)

    fun getCardDto() = CompletionResult.Success(mocks.cardDto)

    fun getExtendedPublicKey() = CompletionResult.Success(mocks.extendedPublicKey)

    private fun getMocks(productType: ProductType): Mocks {
        return when (productType) {
            ProductType.Wallet -> WalletMocks
            ProductType.Wallet2 -> Wallet2Mocks
            else -> TODO()
        }
    }
}
