package com.tangem.domain.jointaccount.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.domain.jointaccount.model.JointAccountFlowError
import com.tangem.domain.jointaccount.model.JointAccountJoinError
import com.tangem.domain.jointaccount.model.JointAccountJoinPayload
import com.tangem.domain.jointaccount.model.JointAccountJoinResult
import com.tangem.domain.jointaccount.model.JointAccountParticipant
import com.tangem.domain.jointaccount.model.JointAccountSignInput
import com.tangem.domain.jointaccount.model.JointAccountSignResult
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.jointaccount.signing.JointAccountSigner
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class JoinJointAccountUseCaseTest {

    private val repository: JointAccountRepository = mockk()
    private val signer: JointAccountSigner = mockk()
    private val fetcher: SingleJointAccountListFetcher = mockk()
    private val supplier: SingleJointAccountListSupplier = mockk()
    private val accountListFetcher: SingleAccountListFetcher = mockk()

    private val useCase = JoinJointAccountUseCase(
        repository = repository,
        signer = signer,
        fetcher = fetcher,
        supplier = supplier,
        accountListFetcher = accountListFetcher,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository, signer, fetcher, supplier, accountListFetcher)
        coEvery { fetcher.invoke(params = any()) } returns Unit.right()
        coEvery { accountListFetcher.invoke(params = any()) } returns Unit.right()
        coEvery { signer.sign(userWalletId = any(), input = any()) } returns signResult().right()
    }

    @Test
    fun `GIVEN backend accepts WHEN invoke THEN account returned and the list refreshed`() = runTest {
        // Arrange
        coEvery { repository.getFreeOwnerDerivationIndex(WALLET_ID) } returns 3
        val inputSlot = slot<JointAccountSignInput>()
        coEvery { signer.sign(WALLET_ID, capture(inputSlot)) } returns signResult().right()
        val payloadSlot = slot<JointAccountJoinPayload>()
        coEvery {
            repository.join(userWalletId = WALLET_ID, payload = capture(payloadSlot), signature = SIGNATURE)
        } returns JointAccountJoinResult.Joined(account = account())

        // Act
        val actual = useCase(userWalletId = WALLET_ID, inviteId = INVITE_ID, config = config(), memberName = NAME)

        // Assert
        assertThat(actual).isEqualTo(account().right())
        // The index read before the tap is the one the payload declares — the backend matches them
        assertThat(inputSlot.captured.derivationIndex.value).isEqualTo(3)
        val expectedPayload = JointAccountJoinPayload(
            inviteId = INVITE_ID,
            config = config(),
            member = JointAccountParticipant(
                walletId = WALLET_ID.stringValue,
                name = NAME,
                address = OWNER_ADDRESS,
                derivation = 3,
            ),
        )
        assertThat(payloadSlot.captured).isEqualTo(expectedPayload)
        // The signed payload and the sent payload are one and the same object of values
        assertThat(inputSlot.captured.makePayload(OWNER_ADDRESS)).isEqualTo(expectedPayload)
        coVerify(exactly = 1) { fetcher.invoke(params = SingleJointAccountListFetcher.Params(WALLET_ID)) }
        // The wallet screen reads the row from the accounts document, so it is refetched alongside
        coVerify(exactly = 1) { accountListFetcher.invoke(params = SingleAccountListFetcher.Params(WALLET_ID)) }
    }

    @Test
    fun `GIVEN conflict WHEN invoke THEN own account is resolved from the list`() = runTest {
        // Arrange — "address already registered" and a spent invite are the same 409; being a member decides
        coEvery { repository.getFreeOwnerDerivationIndex(WALLET_ID) } returns 0
        coEvery { repository.join(any(), any(), any()) } returns JointAccountJoinResult.Conflict
        // The backend returns checksummed addresses; the comparison must not depend on the case
        val own = account(memberAddress = OWNER_ADDRESS.lowercase())
        every { supplier.invoke(userWalletId = WALLET_ID) } returns flowOf(listOf(otherAccount(), own))

        // Act
        val actual = useCase(userWalletId = WALLET_ID, inviteId = INVITE_ID, config = config(), memberName = NAME)

        // Assert
        assertThat(actual).isEqualTo(own.right())
        coVerify(exactly = 1) { fetcher.invoke(params = SingleJointAccountListFetcher.Params(WALLET_ID)) }
        coVerify(exactly = 1) { accountListFetcher.invoke(params = SingleAccountListFetcher.Params(WALLET_ID)) }
    }

    @Test
    fun `GIVEN account list refetch fails on conflict WHEN invoke THEN own account is still resolved`() = runTest {
        // Arrange — the joint list is the source of the account; the document only feeds the wallet screen row
        coEvery { repository.getFreeOwnerDerivationIndex(WALLET_ID) } returns 0
        coEvery { repository.join(any(), any(), any()) } returns JointAccountJoinResult.Conflict
        coEvery { accountListFetcher.invoke(params = any()) } returns IllegalStateException("no network").left()
        val own = account()
        every { supplier.invoke(userWalletId = WALLET_ID) } returns flowOf(listOf(own))

        // Act
        val actual = useCase(userWalletId = WALLET_ID, inviteId = INVITE_ID, config = config(), memberName = NAME)

        // Assert
        assertThat(actual).isEqualTo(own.right())
    }

    @Test
    fun `GIVEN conflict and no own account WHEN invoke THEN InvalidInvite`() = runTest {
        // Arrange
        coEvery { repository.getFreeOwnerDerivationIndex(WALLET_ID) } returns 0
        coEvery { repository.join(any(), any(), any()) } returns JointAccountJoinResult.Conflict
        every { supplier.invoke(userWalletId = WALLET_ID) } returns flowOf(listOf(otherAccount()))

        // Act
        val actual = useCase(userWalletId = WALLET_ID, inviteId = INVITE_ID, config = config(), memberName = NAME)

        // Assert
        assertThat(actual).isEqualTo(JointAccountJoinError.InvalidInvite.left())
    }

    @Test
    fun `GIVEN refresh fails on conflict WHEN invoke THEN Failed and not InvalidInvite`() = runTest {
        // Arrange — the refresh is the only way to tell "already joined" from a dead invite, so its failure
        // is the real cause: blaming the link for a network problem would stop the user from retrying
        coEvery { repository.getFreeOwnerDerivationIndex(WALLET_ID) } returns 0
        coEvery { repository.join(any(), any(), any()) } returns JointAccountJoinResult.Conflict
        val cause = IllegalStateException("no network")
        coEvery { fetcher.invoke(params = any()) } returns cause.left()

        // Act
        val actual = useCase(userWalletId = WALLET_ID, inviteId = INVITE_ID, config = config(), memberName = NAME)

        // Assert
        assertThat(actual).isEqualTo(JointAccountFlowError.Failed(cause = cause).left())
    }

    @Test
    fun `GIVEN refresh fails after joining WHEN invoke THEN the account is still returned`() = runTest {
        // Arrange — the slot is taken; failing here would push the user into a retry that can only conflict
        coEvery { repository.getFreeOwnerDerivationIndex(WALLET_ID) } returns 0
        val joined = account()
        coEvery { repository.join(any(), any(), any()) } returns JointAccountJoinResult.Joined(joined)
        coEvery { fetcher.invoke(params = any()) } returns IllegalStateException("no network").left()

        // Act
        val actual = useCase(userWalletId = WALLET_ID, inviteId = INVITE_ID, config = config(), memberName = NAME)

        // Assert
        assertThat(actual).isEqualTo(joined.right())
    }

    @Test
    fun `GIVEN account list refetch fails after joining WHEN invoke THEN the account is still returned`() = runTest {
        // Arrange — the row on the wallet screen is best-effort; the document catches up on the next fetch
        coEvery { repository.getFreeOwnerDerivationIndex(WALLET_ID) } returns 0
        val joined = account()
        coEvery { repository.join(any(), any(), any()) } returns JointAccountJoinResult.Joined(joined)
        coEvery { accountListFetcher.invoke(params = any()) } returns IllegalStateException("no network").left()

        // Act
        val actual = useCase(userWalletId = WALLET_ID, inviteId = INVITE_ID, config = config(), memberName = NAME)

        // Assert
        assertThat(actual).isEqualTo(joined.right())
    }

    @Test
    fun `GIVEN card session dismissed WHEN invoke THEN UserCancelled and nothing is sent`() = runTest {
        // Arrange
        coEvery { repository.getFreeOwnerDerivationIndex(WALLET_ID) } returns 0
        coEvery { signer.sign(WALLET_ID, any()) } returns TangemSdkError.UserCancelled().left()

        // Act
        val actual = useCase(userWalletId = WALLET_ID, inviteId = INVITE_ID, config = config(), memberName = NAME)

        // Assert
        assertThat(actual).isEqualTo(JointAccountFlowError.UserCancelled.left())
        coVerify(exactly = 0) { repository.join(any(), any(), any()) }
        coVerify(exactly = 0) { accountListFetcher.invoke(params = any()) }
    }

    @Test
    fun `GIVEN negative index from the backend WHEN invoke THEN typed error and the card is never asked`() =
        runTest {
            // Arrange
            coEvery { repository.getFreeOwnerDerivationIndex(WALLET_ID) } returns -1

            // Act
            val actual = useCase(userWalletId = WALLET_ID, inviteId = INVITE_ID, config = config(), memberName = NAME)

            // Assert
            val expected = JointAccountFlowError.InvalidDerivationIndex(
                cause = DerivationIndex.Error.NegativeDerivationIndex(derivationIndex = -1),
            )
            assertThat(actual).isEqualTo(expected.left())
            coVerify(exactly = 0) { signer.sign(any(), any()) }
        }

    @Test
    fun `GIVEN index request fails WHEN invoke THEN Failed and the card is never asked`() = runTest {
        // Arrange
        val cause = IllegalStateException("no network")
        coEvery { repository.getFreeOwnerDerivationIndex(WALLET_ID) } throws cause

        // Act
        val actual = useCase(userWalletId = WALLET_ID, inviteId = INVITE_ID, config = config(), memberName = NAME)

        // Assert
        assertThat(actual).isEqualTo(JointAccountFlowError.Failed(cause = cause).left())
        coVerify(exactly = 0) { signer.sign(any(), any()) }
    }

    @Test
    fun `GIVEN join request fails WHEN invoke THEN Failed`() = runTest {
        // Arrange
        val cause = IllegalStateException("500")
        coEvery { repository.getFreeOwnerDerivationIndex(WALLET_ID) } returns 0
        coEvery { repository.join(any(), any(), any()) } throws cause

        // Act
        val actual = useCase(userWalletId = WALLET_ID, inviteId = INVITE_ID, config = config(), memberName = NAME)

        // Assert
        assertThat(actual).isEqualTo(JointAccountFlowError.Failed(cause = cause).left())
    }

    private fun config() = JointAccountConfig(
        name = "Family",
        icon = "Star",
        iconColor = "Azure",
        membersCount = 3,
        threshold = 2,
    )

    private fun signResult() = JointAccountSignResult(
        ownerAddress = OWNER_ADDRESS,
        canonicalPayload = byteArrayOf(1, 2, 3),
        signature = SIGNATURE,
        derivedKeys = emptyMap(),
    )

    private fun account(memberAddress: String = OWNER_ADDRESS) = JointAccount(
        cryptoAccountId = "AA11",
        membersCount = 3,
        threshold = 2,
        safeAddress = null,
        status = JointAccount.Status.PENDING,
        members = listOf(
            JointAccount.Member(name = NAME, address = memberAddress, role = JointAccount.Role.MEMBER),
        ),
        source = StatusSource.ACTUAL,
    )

    private fun otherAccount() = account(memberAddress = "0x1111111111111111111111111111111111111111")

    private companion object {
        val WALLET_ID = UserWalletId("011121314151617181910A0B0C0D0E0F011121314151617181910A0B0C0D0E0F")
        const val OWNER_ADDRESS = "0x7e5f4552091a69125d5DfCb7b8C2659029395Bdf"
        const val SIGNATURE = "0xdeadbeef"
        const val NAME = " Bob "
        const val INVITE_ID = "AA11BB22CC33DD44EE55FF66AA77BB88CC99DD00EE11FF22AA33BB44CC55DD66"
    }
}