package com.tangem.domain.jointaccount.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.domain.jointaccount.model.JointAccountCreationError
import com.tangem.domain.jointaccount.model.JointAccountCreationPayload
import com.tangem.domain.jointaccount.model.JointAccountCreationResult
import com.tangem.domain.jointaccount.model.JointAccountSignInput
import com.tangem.domain.jointaccount.model.JointAccountSignResult
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.jointaccount.signing.JointAccountSigner
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.models.StatusSource
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

internal class CreateJointAccountUseCaseTest {

    private val repository: JointAccountRepository = mockk()
    private val signer: JointAccountSigner = mockk()
    private val fetcher: SingleJointAccountListFetcher = mockk()
    private val supplier: SingleJointAccountListSupplier = mockk()

    private val useCase = CreateJointAccountUseCase(
        repository = repository,
        signer = signer,
        fetcher = fetcher,
        supplier = supplier,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository, signer, fetcher, supplier)
        coEvery { fetcher.invoke(params = any()) } returns Unit.right()
    }

    @Test
    fun `GIVEN backend accepts WHEN invoke THEN account returned and the list refreshed`() = runTest {
        // Arrange
        coEvery { repository.getFreeOwnerDerivationIndex(WALLET_ID) } returns 3
        coEvery { signer.sign(WALLET_ID, any()) } returns signResult().right()
        val payloadSlot = slot<JointAccountCreationPayload>()
        coEvery {
            repository.create(userWalletId = WALLET_ID, payload = capture(payloadSlot), signature = SIGNATURE)
        } returns JointAccountCreationResult.Created(account = account())

        // Act
        val actual = useCase(userWalletId = WALLET_ID, config = config(), creatorName = CREATOR_NAME)

        // Assert
        assertThat(actual).isEqualTo(account().right())
        // The index read before the tap is the one the payload declares — the backend matches them
        assertThat(payloadSlot.captured.creator.derivation).isEqualTo(3)
        assertThat(payloadSlot.captured.creator.address).isEqualTo(OWNER_ADDRESS)
        assertThat(payloadSlot.captured.creator.name).isEqualTo(CREATOR_NAME)
        assertThat(payloadSlot.captured.config).isEqualTo(config())
        coVerify(exactly = 1) { fetcher.invoke(params = SingleJointAccountListFetcher.Params(WALLET_ID)) }
    }

    @Test
    fun `GIVEN index read WHEN sign THEN the same index goes into the sign input`() = runTest {
        // Arrange
        coEvery { repository.getFreeOwnerDerivationIndex(WALLET_ID) } returns 7
        val inputSlot = slot<JointAccountSignInput>()
        coEvery { signer.sign(WALLET_ID, capture(inputSlot)) } returns signResult().right()
        coEvery { repository.create(any(), any(), any()) } returns
            JointAccountCreationResult.Created(account = account())

        // Act
        useCase(userWalletId = WALLET_ID, config = config(), creatorName = CREATOR_NAME)

        // Assert
        assertThat(inputSlot.captured.derivationIndex).isEqualTo(7)
    }

    @Test
    fun `GIVEN creator already registered WHEN invoke THEN own account is resolved from the list`() = runTest {
        // Arrange
        coEvery { repository.getFreeOwnerDerivationIndex(WALLET_ID) } returns 0
        coEvery { signer.sign(WALLET_ID, any()) } returns signResult().right()
        coEvery { repository.create(any(), any(), any()) } returns
            JointAccountCreationResult.CreatorAlreadyRegistered
        // The backend returns checksummed addresses; the comparison must not depend on the case
        val own = account(memberAddress = OWNER_ADDRESS.lowercase())
        every { supplier.invoke(userWalletId = WALLET_ID) } returns flowOf(listOf(otherAccount(), own))

        // Act
        val actual = useCase(userWalletId = WALLET_ID, config = config(), creatorName = CREATOR_NAME)

        // Assert
        assertThat(actual).isEqualTo(own.right())
        // The list is refreshed first — the existing account may be missing from the cached copy
        coVerify(exactly = 1) { fetcher.invoke(params = SingleJointAccountListFetcher.Params(WALLET_ID)) }
    }

    @Test
    fun `GIVEN creator already registered but no such account WHEN invoke THEN ExistingAccountNotFound`() = runTest {
        // Arrange
        coEvery { repository.getFreeOwnerDerivationIndex(WALLET_ID) } returns 0
        coEvery { signer.sign(WALLET_ID, any()) } returns signResult().right()
        coEvery { repository.create(any(), any(), any()) } returns
            JointAccountCreationResult.CreatorAlreadyRegistered
        every { supplier.invoke(userWalletId = WALLET_ID) } returns flowOf(listOf(otherAccount()))

        // Act
        val actual = useCase(userWalletId = WALLET_ID, config = config(), creatorName = CREATOR_NAME)

        // Assert
        assertThat(actual).isEqualTo(JointAccountCreationError.ExistingAccountNotFound.left())
    }

    @Test
    fun `GIVEN card session dismissed WHEN invoke THEN UserCancelled and nothing is created`() = runTest {
        // Arrange
        coEvery { repository.getFreeOwnerDerivationIndex(WALLET_ID) } returns 0
        coEvery { signer.sign(WALLET_ID, any()) } returns TangemSdkError.UserCancelled().left()

        // Act
        val actual = useCase(userWalletId = WALLET_ID, config = config(), creatorName = CREATOR_NAME)

        // Assert
        assertThat(actual).isEqualTo(JointAccountCreationError.UserCancelled.left())
        coVerify(exactly = 0) { repository.create(any(), any(), any()) }
    }

    @Test
    fun `GIVEN index request fails WHEN invoke THEN Failed and the card is never asked`() = runTest {
        // Arrange
        val cause = IllegalStateException("no network")
        coEvery { repository.getFreeOwnerDerivationIndex(WALLET_ID) } throws cause

        // Act
        val actual = useCase(userWalletId = WALLET_ID, config = config(), creatorName = CREATOR_NAME)

        // Assert
        assertThat(actual).isEqualTo(JointAccountCreationError.Failed(cause = cause).left())
        coVerify(exactly = 0) { signer.sign(any(), any()) }
    }

    @Test
    fun `GIVEN creation request fails WHEN invoke THEN Failed`() = runTest {
        // Arrange
        val cause = IllegalStateException("500")
        coEvery { repository.getFreeOwnerDerivationIndex(WALLET_ID) } returns 0
        coEvery { signer.sign(WALLET_ID, any()) } returns signResult().right()
        coEvery { repository.create(any(), any(), any()) } throws cause

        // Act
        val actual = useCase(userWalletId = WALLET_ID, config = config(), creatorName = CREATOR_NAME)

        // Assert
        assertThat(actual).isEqualTo(JointAccountCreationError.Failed(cause = cause).left())
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
            JointAccount.Member(name = "Alice", address = memberAddress, role = JointAccount.Role.CREATOR),
        ),
        source = StatusSource.ACTUAL,
    )

    private fun otherAccount() = account(memberAddress = "0x1111111111111111111111111111111111111111")

    private companion object {
        val WALLET_ID = UserWalletId("011121314151617181910A0B0C0D0E0F011121314151617181910A0B0C0D0E0F")
        const val OWNER_ADDRESS = "0x7e5f4552091a69125d5DfCb7b8C2659029395Bdf"
        const val SIGNATURE = "0xdeadbeef"
        const val CREATOR_NAME = " Alice "
    }
}