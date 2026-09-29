package com.tangem.domain.feedback

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.PhoneInfo
import com.tangem.domain.feedback.models.UserWalletsInfo
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.feedback.repository.FeedbackRepository
import com.tangem.domain.feedback.utils.EmailMessageBodyResolver
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

internal class GetSupportChatLogsUseCaseTest {

    private val feedbackRepository: FeedbackRepository = mockk()

    private val useCase = GetSupportChatLogsUseCase(feedbackRepository = feedbackRepository)

    @BeforeEach
    fun setUp() {
        clearMocks(feedbackRepository)
        every { feedbackRepository.getUserWalletsInfo(any()) } returns UserWalletsInfo(
            selectedUserWalletId = walletId.stringValue,
            totalUserWallets = 1,
        )
        coEvery { feedbackRepository.getBlockchainInfoList(any()) } returns emptyList()
        every { feedbackRepository.getPhoneInfo() } returns PhoneInfo(
            phoneModel = "Pixel 9",
            osVersion = "36",
            appVersion = "1.0",
        )
    }

    @Test
    fun `GIVEN wallet meta info WHEN invoke THEN archives logs with the support email info block`() = runTest {
        // Arrange
        val metaInfo = WalletMetaInfo(userWalletId = walletId, cardId = "CB00000000000001")
        val expectedInfoLogs = EmailMessageBodyResolver(feedbackRepository)
            .resolve(FeedbackEmailType.DirectUserRequest(metaInfo))
        val archive = File("log.zip")
        val infoLogs = slot<String>()
        coEvery { feedbackRepository.getZipLogFile(infoLogs = capture(infoLogs)) } returns archive

        // Act
        val actual = useCase(metaInfo)

        // Assert
        assertThat(actual).isEqualTo(archive)
        assertThat(infoLogs.captured).isEqualTo(expectedInfoLogs)
        assertThat(infoLogs.captured).contains("CB00000000000001")
        assertThat(infoLogs.captured).contains("Pixel 9")
    }

    @Test
    fun `GIVEN info collection fails WHEN invoke THEN archives logs without info`() = runTest {
        // Arrange
        val archive = File("log.zip")
        coEvery { feedbackRepository.getBlockchainInfoList(any()) } throws IllegalStateException("No wallet managers")
        coEvery { feedbackRepository.getZipLogFile(infoLogs = null) } returns archive

        // Act
        val actual = useCase(WalletMetaInfo(userWalletId = walletId))

        // Assert
        assertThat(actual).isEqualTo(archive)
    }

    private companion object {
        val walletId = UserWalletId("011")
    }
}