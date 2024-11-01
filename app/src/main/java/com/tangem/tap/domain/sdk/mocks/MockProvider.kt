package com.tangem.tap.domain.sdk.mocks

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.models.scan.ProductType
import com.tangem.sdk.api.CreateProductWalletTaskResponse
import com.tangem.tap.domain.sdk.mocks.content.NoteMockContent
import com.tangem.tap.domain.sdk.mocks.content.WalletMockContent
import com.tangem.tap.domain.sdk.mocks.content.Wallet2MockContent

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

    fun getSuccessResponse() = CompletionResult.Success(content.successResponse).orFailure()

    fun getScanResponse() = CompletionResult.Success(content.scanResponse).orFailure()

    fun getDerivationTaskResponse() = CompletionResult.Success(content.derivationTaskResponse).orFailure()

    fun getCardDto() = CompletionResult.Success(content.cardDto).orFailure()

    fun getExtendedPublicKey() = CompletionResult.Success(content.extendedPublicKey).orFailure()

    fun getCreateProductWalletResponse(): CompletionResult<CreateProductWalletTaskResponse> {
        return CompletionResult.Success(content.createProductWalletTaskResponse).orFailure()
    }

    fun getImportWalletResponse(): CompletionResult<CreateProductWalletTaskResponse> {
        return CompletionResult.Success(content.importWalletResponse).orFailure()
    }

    // region Twin-specific

    fun finalizeTwin() = CompletionResult.Success(content.finalizeTwinResponse).orFailure()

    fun createFirstTwinWallet() = CompletionResult.Success(content.createFirstTwinResponse).orFailure()

    fun createSecondTwinWallet() = CompletionResult.Success(content.createSecondTwinResponse).orFailure()

    // endregion

    private fun getMockContent(productType: ProductType): MockContent {
        return when (productType) {
            ProductType.Wallet -> WalletMockContent
            ProductType.Wallet2 -> Wallet2MockContent
            ProductType.Note -> NoteMockContent
            else -> TODO()
        }
    }

    private fun <T> CompletionResult.Success<T>.orFailure(): CompletionResult<T> {
        return if (emulateError) {
            CompletionResult.Failure(emulatedError)
        } else {
            this
        }
    }
}