package com.tangem.domain.card

import com.tangem.TangemSdk
import com.tangem.domain.card.chains.Chains
import com.tangem.domain.card.chains.CheckUnfinishedSaltPayBackupChain
import com.tangem.domain.card.chains.ScanCardChain
import com.tangem.domain.card.model.ScanCardParams
import com.tangem.domain.card.model.ScanCardResult
import com.tangem.domain.card.repository.ScanCardRepository
import com.tangem.domain.core.chain.ChainProcessor
import com.tangem.domain.core.chain.EmptyChains
import com.tangem.domain.core.result.Either
import com.tangem.operations.backup.BackupService
import javax.inject.Inject

class CardInteractor @Inject constructor(
    private val scanCardRepository: ScanCardRepository,
    private val tangemSdk: TangemSdk,
    private val backupService: BackupService,
) : ChainProcessor<ScanCardResult>() {
    suspend fun scanCard(params: ScanCardParams = ScanCardParams.Simple()): Either<ScanCardException, ScanCardResult> {
        params.chains.sortedBy { it.ordinal }.forEach { chain ->
            when (chain) {
                Chains.ScanCard -> ScanCardChain(params, tangemSdk, scanCardRepository)
                Chains.CheckUnfinishedSaltPayBackup -> CheckUnfinishedSaltPayBackupChain(backupService)
                Chains.Disclaimer -> TODO("Add Disclaimer chain (or not)")
            }
        }

        return launchChains().toEither { e ->
            when (e) {
                EmptyChains -> ScanCardException.EmptyChains
                // TODO: Add errors mapping
                else -> ScanCardException.Generic(e)
            }
        }
    }
}
