package com.tangem.tap.domain.sdk.mocks

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.models.scan.ProductType
import com.tangem.tap.domain.sdk.mocks.content.WalletMockContent
import com.tangem.tap.domain.sdk.mocks.content.Wallet2MockContent
import com.tangem.tap.domain.tasks.product.CreateProductWalletTaskResponse

object MockProvider {

    private var content: MockContent = getMockContent(ProductType.Wallet)

    private var emulateError: Boolean = false

    private var emulatedError: TangemError = TangemSdkError.TagLost()

    fun setEmulateError(error: TangemError? = null) {
        emulateError = true
        error?.let {
            emulatedError = it
        }
    }

    fun resetEmulateError() {
        emulateError = false
    }

    fun setMocks(productType: ProductType) {
        content = getMockContent(productType)
    }

    fun setMocks(mockContent: MockContent) {
        content = mockContent
    }

    fun getSuccessResponse() = CompletionResult.Success(content.successResponse).orFail()

    fun getScanResponse() = CompletionResult.Success(content.scanResponse).orFail()

    fun getDerivationTaskResponse() = CompletionResult.Success(content.derivationTaskResponse).orFail()

    fun getCardDto() = CompletionResult.Success(content.cardDto).orFail()

    fun getExtendedPublicKey() = CompletionResult.Success(content.extendedPublicKey).orFail()

    fun getCreateProductWalletResponse(): CompletionResult<CreateProductWalletTaskResponse> {
        return CompletionResult.Success(content.createProductWalletTaskResponse).orFail()
    }

    fun getImportWalletResponse(): CompletionResult<CreateProductWalletTaskResponse> {
        return CompletionResult.Success(content.importWalletResponse).orFail()
    }

    // region Twin-specific

    fun finalizeTwin() = CompletionResult.Success(content.finalizeTwinResponse).orFail()

    fun createFirstTwinWallet() = CompletionResult.Success(content.createFirstTwinResponse).orFail()

    fun createSecondTwinWallet() = CompletionResult.Success(content.createSecondTwinResponse).orFail()

    // endregion

    private fun getMockContent(productType: ProductType): MockContent {
        return when (productType) {
            ProductType.Wallet -> WalletMockContent
            ProductType.Wallet2 -> Wallet2MockContent
            else -> TODO()
        }
    }

    private fun <T> CompletionResult.Success<T>.orFail(): CompletionResult<T> {
        return if (emulateError) {
            CompletionResult.Failure(emulatedError)
        } else {
            this
        }
    }
}
