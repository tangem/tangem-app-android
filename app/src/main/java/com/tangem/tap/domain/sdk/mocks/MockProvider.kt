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

    val availableMocks: List<MockOption> = listOf(
        MockOption("Wallet") { WalletMockContent },
        MockOption("Note") { NoteMockContent },
        MockOption("Twins") { TwinsMockContent },
        MockOption("Ring") { RingMockContent },
        MockOption("Wallet 2") { Wallet2MockContent },
        MockOption("Wallet 2 (No Backup)") { Wallet2NoBackupMockContent },
        MockOption("Wallet 2 (No Backup, No Wallets)") { Wallet2NoBackupNoWalletsMockContent },
        MockOption("Wallet 2 (Seed Phrase)") { Wallet2WithSeedPhraseMockContent },
        MockOption("Wallet 2 (With derivations)") { Wallet2WithDerivationsMockContent },
        MockOption("Shiba") { ShibaMockContent },
        MockOption("Shiba (No Backup)") { ShibaNoBackupMockContent },
        MockOption("Shiba (No Backup, No Wallets)") { ShibaNoBackupNoWalletsMockContent },
        MockOption("Ed25519 Curve") { EdCurveMockContent },
        MockOption("Secp256k1 Curve") { Secpk1CurveMockContent },
        MockOption("Backup Wallet") { BackupWalletMockContent },
        MockOption("Dev Wallet") { DevWalletMockContent },
        MockOption("Firmware 4.12") { Firmware412MockContent },
        MockOption("V3 Multicurrency") { V3MockContent },
        MockOption("Single Currency") { SingleCurrencyMockContent },
        MockOption("Start2Coin") { S2CMockContent },
        MockOption("Cobrand") { showCobrandConfigDialog(it) },
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
            ProductType.Start2Coin -> S2CMockContent
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