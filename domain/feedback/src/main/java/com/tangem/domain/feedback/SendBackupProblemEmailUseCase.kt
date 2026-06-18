package com.tangem.domain.feedback

import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.wallet.UserWalletId

class SendBackupProblemEmailUseCase(
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
) {

    suspend operator fun invoke(userWalletId: UserWalletId) {
        val metaInfo = getWalletMetaInfoUseCase(userWalletId).getOrNull() ?: return
        sendFeedbackEmailUseCase(type = FeedbackEmailType.BackupProblem(walletMetaInfo = metaInfo))
    }
}