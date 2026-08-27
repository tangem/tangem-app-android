package com.tangem.domain.wallets.analytics

import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.cloudbackup.models.CloudBackupStatus

fun CloudBackupStatus.toAnalyticsState(): AnalyticsParam.CloudBackupState = when (this) {
    CloudBackupStatus.Incomplete -> AnalyticsParam.CloudBackupState.Incomplete
    CloudBackupStatus.Done -> AnalyticsParam.CloudBackupState.Done
    CloudBackupStatus.ActionRequired -> AnalyticsParam.CloudBackupState.ActionRequired
}