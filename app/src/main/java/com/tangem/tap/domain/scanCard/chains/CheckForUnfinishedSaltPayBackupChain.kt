package com.tangem.tap.domain.scanCard.chains

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.card.ScanCardException
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.core.chain.Chain
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.operations.backup.BackupService
import com.tangem.tap.common.extensions.primaryCardIsSaltPayVisa

/**
 * Handles the verification for unfinished SaltPay backup processes after the card scanning operation.
 *
 * Returns [ScanChainException.PutSaltPayVisaCard] if backup is not finished.
 *
 * @param backupService a service responsible for managing backup operations, used here to check for unfinished backups
 * and verify the primary card type.
 *
 * @see Chain for more information about the Chain interface.
 */
internal class CheckForUnfinishedSaltPayBackupChain(
    private val backupService: BackupService,
) : Chain<ScanCardException.ChainException, ScanResponse> {

    override suspend fun invoke(
        previousChainResult: ScanResponse,
    ): Either<ScanCardException.ChainException, ScanResponse> {
        if (!backupService.hasIncompletedBackup || !backupService.primaryCardIsSaltPayVisa()) {
            return previousChainResult.right()
        }

        val isTheSamePrimaryCard = backupService.primaryCardId
            ?.let { it == previousChainResult.card.cardId }
            ?: false

        return if (previousChainResult.cardTypesResolver.isSaltPayWallet() || !isTheSamePrimaryCard) {
            ScanChainException.PutSaltPayVisaCard.left()
        } else {
            previousChainResult.right()
        }
    }
}