package com.tangem.domain.feedback

import arrow.core.Either
import arrow.core.Either.Companion.catch
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.feedback.repository.FeedbackRepository
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWalletId

/**
 * UseCase for creating 'UserWalletMetaInfo' from [UserWalletId] or [ScanResponse]
 *
 * @property feedbackRepository feedback repository
 *
[REDACTED_AUTHOR]
 */
class GetWalletMetaInfoUseCase(
    private val feedbackRepository: FeedbackRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<Throwable, WalletMetaInfo> = catch {
        feedbackRepository.getUserWalletMetaInfo(userWalletId)
    }

    operator fun invoke(scanResponse: ScanResponse): Either<Throwable, WalletMetaInfo> = catch {
        feedbackRepository.getUserWalletMetaInfo(scanResponse)
    }
}