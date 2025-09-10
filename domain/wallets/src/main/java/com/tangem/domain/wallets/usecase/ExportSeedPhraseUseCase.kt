package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.domain.wallets.hot.HotWalletAccessor
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.hot.sdk.model.SeedPhrasePrivateInfo

class ExportSeedPhraseUseCase(
    private val hotWalletAccessor: HotWalletAccessor,
) {

    suspend operator fun invoke(hotWalletId: HotWalletId): Either<Throwable, SeedPhrasePrivateInfo> {
        return Either.catch {
            hotWalletAccessor.exportSeedPhrase(hotWalletId)
        }
    }
}