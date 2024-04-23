package com.tangem.tap.domain.sdk.mocks

import com.tangem.common.CompletionResult
import com.tangem.domain.models.scan.ProductType
import com.tangem.tap.domain.sdk.mocks.wallet.WalletMocks
import com.tangem.tap.domain.sdk.mocks.wallet2.Wallet2Mocks
import com.tangem.tap.domain.tasks.product.CreateProductWalletTaskResponse

object MockProvider {

    private var mocks: Mocks = getMocks(ProductType.Wallet)

    fun setMockConfig(mockConfig: MockConfig) {
        mockConfig.content.fold(
            ifLeft = {
                mocks = getMocks(it)
            },
            ifRight = {
                mocks = it
            },
        )
    }

    fun getSuccessResponse() = CompletionResult.Success(mocks.successResponse)

    fun getScanResponse() = CompletionResult.Success(mocks.scanResponse)

    fun getDerivationTaskResponse() = CompletionResult.Success(mocks.derivationTaskResponse)

    fun getCardDto() = CompletionResult.Success(mocks.cardDto)

    fun getExtendedPublicKey() = CompletionResult.Success(mocks.extendedPublicKey)

    fun getCreateProductWalletResponse(): CompletionResult<CreateProductWalletTaskResponse> {
        return CompletionResult.Success(mocks.createProductWalletTaskResponse)
    }

    fun getImportWalletResponse(): CompletionResult<CreateProductWalletTaskResponse> {
        return CompletionResult.Success(mocks.importWalletResponse)
    }

    private fun getMocks(productType: ProductType): Mocks {
        return when (productType) {
            ProductType.Wallet -> WalletMocks
            ProductType.Wallet2 -> Wallet2Mocks
            else -> TODO()
        }
    }

    // region Twin-specific

    fun finalizeTwin() = CompletionResult.Success(mocks.finalizeTwinResponse)

    fun createFirstTwinWallet() = CompletionResult.Success(mocks.createFirstTwinResponse)

    fun createSecondTwinWallet() = CompletionResult.Success(mocks.createSecondTwinResponse)

    // endregion
}
