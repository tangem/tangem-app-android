package com.tangem.domain.card.chains

import com.tangem.domain.card.ScanCardChain
import com.tangem.domain.card.ScanCardChainResult
import com.tangem.domain.card.ScanCardException
import com.tangem.domain.core.chain.ChainResult
import com.tangem.domain.core.chain.successOr
import com.tangem.operations.backup.BackupService

internal class CheckUnfinishedSaltPayBackupChain(
    private val backupService: BackupService,
) : ScanCardChain {

    private val attachTestVisaBatches = listOf("FF03")
    private val visaBatches = listOf("AE02", "AE03") + attachTestVisaBatches

    override suspend fun invoke(previousChainResult: ScanCardChainResult): ScanCardChainResult {
        val scanResponse = previousChainResult.successOr { return previousChainResult }

        if (!backupService.hasIncompletedBackup || !primaryCardIsSaltPayVisa(backupService)) {
            return previousChainResult
        }

        val isTheSamePrimaryCard = backupService.primaryCardId
            ?.let { it == scanResponse.card.cardId }
            ?: false

        return if (scanResponse.cardTypesResolver.isSaltPayWallet() || !isTheSamePrimaryCard) {
            ChainResult.Failure(ScanCardException.SaltPayActivationError.PutVisaCard)
        } else {
            previousChainResult
        }
    }

    private fun primaryCardIsSaltPayVisa(backupService: BackupService): Boolean {
        return backupService.primaryCardId?.slice(0..3)?.let(::isVisaBatchId) ?: false
    }

    private fun isVisaBatchId(batchId: String): Boolean = visaBatches.contains(batchId)
}
