package com.tangem.domain.jointaccount.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.domain.jointaccount.model.JointAccountInvitePreview
import com.tangem.domain.jointaccount.model.JointAccountInvitePreviewError
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GetJointAccountInvitePreviewUseCaseTest {

    private val repository: JointAccountRepository = mockk()

    private val useCase = GetJointAccountInvitePreviewUseCase(repository = repository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @Test
    fun `GIVEN repository returns preview WHEN invoke THEN it is returned as is`() = runTest {
        // Arrange
        coEvery { repository.getInvitePreview(walletId = WALLET_ID, inviteId = INVITE_ID) } returns preview().right()

        // Act
        val actual = useCase(walletId = WALLET_ID, inviteId = INVITE_ID)

        // Assert
        assertThat(actual).isEqualTo(preview().right())
        coVerify(exactly = 1) { repository.getInvitePreview(walletId = WALLET_ID, inviteId = INVITE_ID) }
    }

    @Test
    fun `GIVEN repository returns error WHEN invoke THEN it is returned as is`() = runTest {
        // Arrange
        coEvery {
            repository.getInvitePreview(walletId = WALLET_ID, inviteId = INVITE_ID)
        } returns JointAccountInvitePreviewError.InvalidLink.left()

        // Act
        val actual = useCase(walletId = WALLET_ID, inviteId = INVITE_ID)

        // Assert
        assertThat(actual).isEqualTo(JointAccountInvitePreviewError.InvalidLink.left())
    }

    private fun preview() = JointAccountInvitePreview(
        config = JointAccountConfig(
            name = "Family savings",
            icon = "Family",
            iconColor = "Azure",
            membersCount = 3,
            threshold = 2,
        ),
        creator = JointAccountInvitePreview.Creator(
            name = "Alice",
            address = OWNER_ADDRESS,
        ),
    )

    private companion object {
        val WALLET_ID = UserWalletId("011121314151617181910A0B0C0D0E0F011121314151617181910A0B0C0D0E0F")
        const val OWNER_ADDRESS = "0x7e5f4552091a69125d5DfCb7b8C2659029395Bdf"
        const val INVITE_ID = "AA11BB22CC33DD44EE55FF66AA77BB88CC99DD00EE11FF22AA33BB44CC55DD66"
    }
}