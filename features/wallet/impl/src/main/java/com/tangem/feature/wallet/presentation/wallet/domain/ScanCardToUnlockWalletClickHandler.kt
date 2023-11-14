package com.tangem.feature.wallet.presentation.wallet.domain

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.userwallets.UserWalletBuilder
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import javax.inject.Inject

internal class ScanCardToUnlockWalletClickHandler @Inject constructor(
    private val scanCardProcessor: ScanCardProcessor,
    private val saveWalletUseCase: SaveWalletUseCase,
) {

    private var scanFailsCounter = 0

    suspend operator fun invoke(walletId: UserWalletId): Either<ScanCardToUnlockWalletError, Unit> {
        return either {
            when (val result = scanCardProcessor.scan()) {
                is CompletionResult.Failure -> {
                    if (result.error is TangemSdkError.UserCancelled) {
                        scanFailsCounter++
                        ensure(scanFailsCounter < 2) { ScanCardToUnlockWalletError.ManyScanFails }
                    }
                }
                is CompletionResult.Success -> {
                    scanFailsCounter = 0

                    // If card's public key is null then user wallet will be null
                    val scannedWallet = UserWalletBuilder(scanResponse = result.data).build()

                    ensure(walletId == scannedWallet?.walletId) {
                        ScanCardToUnlockWalletError.WrongCardIsScanned
                    }

                    if (scannedWallet != null) {
                        saveWalletUseCase(userWallet = scannedWallet, canOverride = true)
                    }
                }
            }
        }
    }
}

internal sealed class ScanCardToUnlockWalletError {

    object WrongCardIsScanned : ScanCardToUnlockWalletError()

    object ManyScanFails : ScanCardToUnlockWalletError()
}
