package com.tangem.domain.jointaccount.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.model.JointAccountInviteIssueError
import com.tangem.domain.jointaccount.model.JointAccountIssuedInvite
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IssueJointAccountInviteUseCaseTest {

    private val repository: JointAccountRepository = mockk()
    private val fetcher: SingleJointAccountListFetcher = mockk()

    private val useCase = IssueJointAccountInviteUseCase(
        repository = repository,
        fetcher = fetcher,
        currentTimeMillis = { NOW_MILLIS },
    )

    private val accountId = AccountId.forJointAccount(userWalletId = WALLET_ID, value = CRYPTO_ACCOUNT_ID).getOrNull()!!
    private val fetcherParams = SingleJointAccountListFetcher.Params(userWalletId = WALLET_ID)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository, fetcher)
        coEvery { fetcher.invoke(params = any()) } returns Unit.right()
    }

    @Test
    fun `GIVEN invite of requested slot expiring later WHEN invoke THEN it is returned and list refetched`() = runTest {
        // Arrange
        val invite = createInvite()
        coEvery { repository.issueInvite(WALLET_ID, CRYPTO_ACCOUNT_ID, REQUESTED_SLOT) } returns invite.right()

        // Act
        val actual = useCase(accountId = accountId, slot = REQUESTED_SLOT)

        // Assert
        assertThat(actual).isEqualTo(invite.right())
        coVerify(exactly = 1) { fetcher.invoke(params = fetcherParams) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN invite not matching the request WHEN invoke THEN fails and list refetched`(model: InvalidInviteModel) =
        runTest {
            // Arrange
            coEvery {
                repository.issueInvite(WALLET_ID, CRYPTO_ACCOUNT_ID, REQUESTED_SLOT)
            } returns model.invite.right()

            // Act
            val actual = useCase(accountId = accountId, slot = REQUESTED_SLOT)

            // Assert
            assertThat(actual.leftOrNull()).isInstanceOf(JointAccountInviteIssueError.Failed::class.java)
            coVerify(exactly = 1) { fetcher.invoke(params = fetcherParams) }
        }

    @Test
    fun `GIVEN slot unavailable WHEN invoke THEN error returned and list refetched`() = runTest {
        // Arrange
        val error = JointAccountInviteIssueError.SlotUnavailable(cause = IllegalStateException("Conflict"))
        coEvery { repository.issueInvite(WALLET_ID, CRYPTO_ACCOUNT_ID, REQUESTED_SLOT) } returns error.left()

        // Act
        val actual = useCase(accountId = accountId, slot = REQUESTED_SLOT)

        // Assert
        assertThat(actual).isEqualTo(error.left())
        coVerify(exactly = 1) { fetcher.invoke(params = fetcherParams) }
    }

    @Test
    fun `GIVEN request failed WHEN invoke THEN error returned and list not refetched`() = runTest {
        // Arrange
        val error = JointAccountInviteIssueError.Failed(cause = IllegalStateException("No network"))
        coEvery { repository.issueInvite(WALLET_ID, CRYPTO_ACCOUNT_ID, REQUESTED_SLOT) } returns error.left()

        // Act
        val actual = useCase(accountId = accountId, slot = REQUESTED_SLOT)

        // Assert
        assertThat(actual).isEqualTo(error.left())
        coVerify(exactly = 0) { fetcher.invoke(params = any()) }
    }

    private fun provideTestModels() = listOf(
        InvalidInviteModel(invite = createInvite(slot = REQUESTED_SLOT + 1)),
        InvalidInviteModel(invite = createInvite(expiresAtMillis = NOW_MILLIS)),
        InvalidInviteModel(invite = createInvite(expiresAtMillis = NOW_MILLIS - HOUR_MILLIS)),
    )

    data class InvalidInviteModel(val invite: JointAccountIssuedInvite)

    private fun createInvite(slot: Int = REQUESTED_SLOT, expiresAtMillis: Long = NOW_MILLIS + HOUR_MILLIS) =
        JointAccountIssuedInvite(id = INVITE_ID, slot = slot, expiresAtMillis = expiresAtMillis)

    private companion object {
        val WALLET_ID = UserWalletId("011121314151617181910A0B0C0D0E0F011121314151617181910A0B0C0D0E0F")
        const val CRYPTO_ACCOUNT_ID = "AA11BB22CC33DD44EE55FF66AA77BB88CC99DD00EE11FF22AA33BB44CC55DD66"
        const val INVITE_ID = "BB11BB22CC33DD44EE55FF66AA77BB88CC99DD00EE11FF22AA33BB44CC55DD66"
        const val REQUESTED_SLOT = 1
        const val NOW_MILLIS = 1_800_000_000_000L
        const val HOUR_MILLIS = 3_600_000L
    }
}