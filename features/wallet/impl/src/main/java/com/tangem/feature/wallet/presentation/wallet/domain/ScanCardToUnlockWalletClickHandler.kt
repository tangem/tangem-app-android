package com.tangem.feature.wallet.presentation.wallet.domain

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.common.core.TangemSdkError
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
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
            scanCardProcessor.scan()
                .doOnSuccess { scanResponse ->
                    scanFailsCounter = 0

                    // If card's public key is null then user wallet will be null
                    val scannedWallet = UserWalletBuilder(scanResponse = scanResponse).build()

                    if (walletId == scannedWallet?.walletId) {
                        saveWalletUseCase(userWallet = scannedWallet, canOverride = true)
                    } else {
                        raise(ScanCardToUnlockWalletError.WrongCardIsScanned)
                    }
                }
                .doOnFailure {
                    if (it is TangemSdkError.UserCancelled) {
                        scanFailsCounter++
                        if (scanFailsCounter >= 2) raise(ScanCardToUnlockWalletError.ManyScanFails)
                    }
                }
        }
    }
}

internal sealed class ScanCardToUnlockWalletError {

    object WrongCardIsScanned : ScanCardToUnlockWalletError()

    object ManyScanFails : ScanCardToUnlockWalletError()
}