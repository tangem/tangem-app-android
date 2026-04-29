package com.tangem.tap.domain.sdk.mocks

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.models.scan.ProductType
import com.tangem.sdk.api.CreateProductWalletTaskResponse
import com.tangem.tap.domain.sdk.mocks.content.*

object MockProvider {

    private var content: MockContent = getMockContent(ProductType.Wallet)

    var isPreset: Boolean = false
        private set

    private var isEmulatingError: Boolean = false

    private var emulatedError: TangemError = TangemSdkError.TagLost()

    val availableMocks: List<Pair<String, MockContent>> = listOf(
        "Wallet" to WalletMockContent,
        "Note" to NoteMockContent,
        "Twins" to TwinsMockContent,
        "Ring" to RingMockContent,
        "Wallet 2" to Wallet2MockContent,
        "Wallet 2 (No Backup)" to Wallet2NoBackupMockContent,
        "Wallet 2 (No Backup, No Wallets)" to Wallet2NoBackupNoWalletsMockContent,
        "Wallet 2 (Seed Phrase)" to Wallet2WithSeedPhraseMockContent,
        "Wallet 2 (With derivations)" to Wallet2WithDerivationsMockContent,
        "Shiba" to ShibaMockContent,
        "Shiba (No Backup)" to ShibaNoBackupMockContent,
        "Shiba (No Backup, No Wallets)" to ShibaNoBackupNoWalletsMockContent,
        "Ed25519 Curve" to EdCurveMockContent,
        "Secp256k1 Curve" to Secpk1CurveMockContent,
        "Backup Wallet" to BackupWalletMockContent,
        "Dev Wallet" to DevWalletMockContent,
        "Firmware 4.12" to Firmware412MockContent,
        "French Blue (Triple)" to FrenchBlueMockContent,
        "French White (Double)" to FrenchWhiteMockContent,
        "Football Black (Double)" to FootballBlackMockContent,
        "Football Dark Green (Triple)" to FootballDarkGreenMockContent,
        "Metaplanet (Triple)" to MetaplanetMockContent,
        "Metaplanet (Double)" to MetaplanetDoubleMockContent,
        "Red Panda (Triple)" to RedPandaMockContent,
        "Red Panda (Double)" to RedPandaDoubleMockContent,
    )

    fun setEmulateError(error: TangemError? = null) {
        isEmulatingError = true
        error?.let {
            emulatedError = it
        }
    }

    fun resetEmulateError() {
        isEmulatingError = false
    }

    fun setMocks(productType: ProductType) {
        content = getMockContent(productType)
        isPreset = true
    }

    fun setMocks(mockContent: MockContent) {
        content = mockContent
        isPreset = true
    }

    fun setMocksWithoutPresetFlag(mockContent: MockContent) {
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
            ProductType.Wallet2 -> Wallet2WithSeedPhraseMockContent
            ProductType.Note -> NoteMockContent
            ProductType.Ring -> RingMockContent
            ProductType.Twins -> TwinsMockContent
            else -> TODO()
        }
    }

    private fun <T> CompletionResult.Success<T>.orFailure(): CompletionResult<T> {
        return if (isEmulatingError) {
            CompletionResult.Failure(emulatedError)
        } else {
            this
        }
    }
}