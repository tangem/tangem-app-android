package com.tangem.domain.feedback

import arrow.core.left
import arrow.core.right
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SendBackupProblemEmailUseCaseTest {

    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase = mockk()
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase = mockk(relaxed = true)

    private val useCase = SendBackupProblemEmailUseCase(
        getWalletMetaInfoUseCase = getWalletMetaInfoUseCase,
        sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
    )

    @AfterEach
    fun tearDown() {
        clearMocks(getWalletMetaInfoUseCase, sendFeedbackEmailUseCase)
    }

    @Test
    fun `does not send email when wallet meta info is unavailable`() = runTest {
        coEvery { getWalletMetaInfoUseCase(walletId) } returns Throwable().left()

        useCase(walletId)

        coVerify(exactly = 0) { sendFeedbackEmailUseCase(any()) }
    }

    @Test
    fun `sends backup problem email when wallet meta info is available`() = runTest {
        val metaInfo = mockk<WalletMetaInfo>()
        coEvery { getWalletMetaInfoUseCase(walletId) } returns metaInfo.right()

        useCase(walletId)

        coVerify(exactly = 1) {
            sendFeedbackEmailUseCase(FeedbackEmailType.BackupProblem(walletMetaInfo = metaInfo))
        }
    }

    private companion object {
        val walletId = UserWalletId("011")
    }
}