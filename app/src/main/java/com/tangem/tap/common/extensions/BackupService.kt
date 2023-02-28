package com.tangem.tap.common.extensions

import com.tangem.domain.common.SaltPayWorkaround
import com.tangem.operations.backup.BackupService

/**
[REDACTED_AUTHOR]
 */
@Suppress("MagicNumber")
fun BackupService.primaryCardIsSaltPayVisa(): Boolean {
    return primaryCardId?.slice(0..3)?.let(SaltPayWorkaround::isVisaBatchId) ?: false
}