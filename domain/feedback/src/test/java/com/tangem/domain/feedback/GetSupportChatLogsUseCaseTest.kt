package com.tangem.domain.feedback

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.PhoneInfo
import com.tangem.domain.feedback.models.SupportChatInfoProvider
import com.tangem.domain.feedback.models.SupportChatTopic
import com.tangem.domain.feedback.models.UserWalletsInfo
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.feedback.repository.FeedbackRepository
import com.tangem.domain.feedback.utils.EmailMessageBodyResolver
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

internal class GetSupportChatLogsUseCaseTest {

    private val feedbackRepository: FeedbackRepository = mockk()
    private val predictionsInfoProvider: SupportChatInfoProvider = mockk()

    private val useCase = GetSupportChatLogsUseCase(
        feedbackRepository = feedbackRepository,
        infoProviders = setOf(predictionsInfoProvider),
    )

    @BeforeEach
    fun setUp() {
        clearMocks(feedbackRepository, predictionsInfoProvider)
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
        every { predictionsInfoProvider.topic } returns SupportChatTopic.PREDICTIONS
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
        coVerify(exactly = 0) { predictionsInfoProvider.getInfo(any()) }
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

    @Test
    fun `GIVEN predictions topic WHEN invoke THEN the provider block follows the info block`() = runTest {
        // Arrange
        val metaInfo = WalletMetaInfo(userWalletId = walletId)
        val expectedInfoLogs = EmailMessageBodyResolver(feedbackRepository)
            .resolve(FeedbackEmailType.DirectUserRequest(metaInfo))
        coEvery { predictionsInfoProvider.getInfo(walletId) } returns PREDICTIONS_INFO
        val infoLogs = slot<String>()
        coEvery { feedbackRepository.getZipLogFile(infoLogs = capture(infoLogs)) } returns File("log.zip")

        // Act
        useCase(walletMetaInfo = metaInfo, topic = SupportChatTopic.PREDICTIONS)

        // Assert
        assertThat(infoLogs.captured).isEqualTo("$expectedInfoLogs\n----------\n$PREDICTIONS_INFO")
    }

    @Test
    fun `GIVEN the provider fails WHEN invoke THEN archives logs with the info block only`() = runTest {
        // Arrange
        val metaInfo = WalletMetaInfo(userWalletId = walletId)
        val expectedInfoLogs = EmailMessageBodyResolver(feedbackRepository)
            .resolve(FeedbackEmailType.DirectUserRequest(metaInfo))
        coEvery { predictionsInfoProvider.getInfo(walletId) } throws IllegalStateException("No status")
        val infoLogs = slot<String>()
        coEvery { feedbackRepository.getZipLogFile(infoLogs = capture(infoLogs)) } returns File("log.zip")

        // Act
        useCase(walletMetaInfo = metaInfo, topic = SupportChatTopic.PREDICTIONS)

        // Assert
        assertThat(infoLogs.captured).isEqualTo(expectedInfoLogs)
    }

    @Test
    fun `GIVEN both blocks fail WHEN invoke THEN archives logs without info`() = runTest {
        // Arrange
        val archive = File("log.zip")
        coEvery { feedbackRepository.getBlockchainInfoList(any()) } throws IllegalStateException("No wallet managers")
        coEvery { predictionsInfoProvider.getInfo(walletId) } throws IllegalStateException("No status")
        coEvery { feedbackRepository.getZipLogFile(infoLogs = null) } returns archive

        // Act
        val actual = useCase(
            walletMetaInfo = WalletMetaInfo(userWalletId = walletId),
            topic = SupportChatTopic.PREDICTIONS,
        )

        // Assert
        assertThat(actual).isEqualTo(archive)
    }

    private companion object {
        val walletId = UserWalletId("011")
        const val PREDICTIONS_INFO = "Polymarket account state: active"
    }
}