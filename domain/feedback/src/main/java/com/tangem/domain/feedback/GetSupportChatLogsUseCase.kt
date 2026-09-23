package com.tangem.domain.feedback

import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.feedback.repository.FeedbackRepository
import com.tangem.domain.feedback.utils.EmailMessageBodyResolver
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import java.io.File

/**
 * Builds the logs archive for the support chat: the app log file plus the wallet, blockchain and phone
 * info that the support email carries in its body, since the chat has no message body to put it in.
 */
class GetSupportChatLogsUseCase(
    private val feedbackRepository: FeedbackRepository,
) {

    private val emailMessageBodyResolver = EmailMessageBodyResolver(feedbackRepository)

    suspend operator fun invoke(walletMetaInfo: WalletMetaInfo): File? {
        val infoLogs = runSuspendCatching {
            emailMessageBodyResolver.resolve(FeedbackEmailType.DirectUserRequest(walletMetaInfo))
        }
            .onFailure { TangemLogger.e("Failed to collect info logs for the support chat", it) }
            .getOrNull()

        return feedbackRepository.getZipLogFile(infoLogs = infoLogs)
    }
}