package com.tangem.data.jointaccount.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.usecase.ActivateJointAccountUseCase
import com.tangem.data.jointaccount.safe.DefaultSafeAddressCalculator
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountActivationError
import com.tangem.domain.jointaccount.model.JointAccountActivationPayload
import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.domain.jointaccount.model.JointAccountFlowError
import com.tangem.domain.jointaccount.model.JointAccountSignInput
import com.tangem.domain.jointaccount.model.JointAccountSignResult
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.jointaccount.safe.SafeSingleton
import com.tangem.domain.jointaccount.signing.JointAccountSigner
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.MockAccounts
import com.tangem.utils.logging.TangemLogger
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ActivateJointAccountUseCaseTest {

    private val repository: JointAccountRepository = mockk()
    private val signer: JointAccountSigner = mockk()
    private val fetcher: SingleJointAccountListFetcher = mockk()
    private val supplier: SingleJointAccountListSupplier = mockk()

    private val useCase = ActivateJointAccountUseCase(
        repository = repository,
        signer = signer,
        fetcher = fetcher,
        supplier = supplier,
        safeAddressCalculator = DefaultSafeAddressCalculator(),
    )

    private val account = MockAccounts.createJointAccount(
        ownerKeyIndex = 3,
        name = "Family",
        icon = CryptoPortfolioIcon.ofCustomAccount(
            value = CryptoPortfolioIcon.Icon.Star,
            color = CryptoPortfolioIcon.Color.Azure,
        ),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository, signer, fetcher, supplier)
        coEvery { fetcher.invoke(params = any()) } returns Unit.right()
        coEvery { signer.sign(userWalletId = any(), input = any()) } returns signResult().right()
        coEvery { repository.activate(any(), any(), any()) } returns Unit.right()
    }

    @Test
    fun `GIVEN confirming account WHEN invoke THEN signed payload is sent and the list refreshed`() = runTest {
        // Arrange
        val jointAccount = jointAccount(safeAddress = computedSafeAddress(SafeSingleton.SAFE_L2))
        every { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns flowOf(listOf(jointAccount))
        val inputSlot = slot<JointAccountSignInput>()
        coEvery { signer.sign(userWalletId = MockAccounts.userWalletId, input = capture(inputSlot)) } returns
            signResult().right()
        val payloadSlot = slot<JointAccountActivationPayload>()
        coEvery {
            repository.activate(
                userWalletId = MockAccounts.userWalletId,
                payload = capture(payloadSlot),
                signature = SIGNATURE,
            )
        } returns Unit.right()

        // Act
        val actual = useCase(account = account)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        assertThat(inputSlot.captured.derivationIndex.value).isEqualTo(3)
        val expectedPayload = JointAccountActivationPayload(
            walletId = MockAccounts.userWalletId.stringValue,
            cryptoAccountId = jointAccount.cryptoAccountId,
            config = JointAccountActivationPayload.ConfirmedConfig(
                base = JointAccountConfig(
                    name = "Family",
                    icon = "Star",
                    iconColor = "Azure",
                    membersCount = 2,
                    threshold = 2,
                ),
                safeAddress = computedSafeAddress(SafeSingleton.SAFE_L2),
            ),
        )
        assertThat(payloadSlot.captured).isEqualTo(expectedPayload)
        // The signed payload and the sent payload are one and the same object of values
        assertThat(inputSlot.captured.makePayload(OWNER_ADDRESS)).isEqualTo(expectedPayload)
        coVerify(exactly = 1) {
            fetcher.invoke(params = SingleJointAccountListFetcher.Params(MockAccounts.userWalletId))
        }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `the backend address picks the singleton`(model: SingletonModel) = runTest {
        // Arrange — which Safe singleton the backend deploys is undecided (alignment №21), both must work
        val jointAccount = jointAccount(safeAddress = computedSafeAddress(model.singleton))
        every { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns flowOf(listOf(jointAccount))
        val payloadSlot = slot<JointAccountActivationPayload>()
        coEvery { repository.activate(any(), capture(payloadSlot), any()) } returns Unit.right()

        // Act
        val actual = useCase(account = account)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        assertThat(payloadSlot.captured.config.safeAddress).isEqualTo(computedSafeAddress(model.singleton))
    }

    @Test
    fun `GIVEN backend spells the address in lowercase WHEN invoke THEN that spelling is signed and sent`() = runTest {
        // Arrange — the backend stores the config it compares with, so the payload must carry its exact string
        val lowercaseAddress = computedSafeAddress(SafeSingleton.SAFE_L2).lowercase()
        val jointAccount = jointAccount(safeAddress = lowercaseAddress)
        every { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns flowOf(listOf(jointAccount))
        val inputSlot = slot<JointAccountSignInput>()
        coEvery { signer.sign(userWalletId = MockAccounts.userWalletId, input = capture(inputSlot)) } returns
            signResult().right()
        val payloadSlot = slot<JointAccountActivationPayload>()
        coEvery { repository.activate(any(), capture(payloadSlot), any()) } returns Unit.right()

        // Act
        val actual = useCase(account = account)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        assertThat(payloadSlot.captured.config.safeAddress).isEqualTo(lowercaseAddress)
        // The signed payload carries the same spelling as the one sent
        assertThat(inputSlot.captured.makePayload(OWNER_ADDRESS)).isEqualTo(payloadSlot.captured)
    }

    private fun provideTestModels() = SafeSingleton.entries.map(::SingletonModel)

    data class SingletonModel(val singleton: SafeSingleton)

    @Test
    fun `GIVEN backend address matches no singleton WHEN invoke THEN AddressMismatch and the card is never asked`() =
        runTest {
            // Arrange
            val jointAccount = jointAccount(safeAddress = DEAD_ADDRESS)
            every { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns flowOf(listOf(jointAccount))

            // Act
            val actual = useCase(account = account)

            // Assert
            assertThat(actual).isEqualTo(JointAccountActivationError.AddressMismatch.left())
            coVerify(exactly = 0) { signer.sign(any(), any()) }
        }

    @Test
    fun `GIVEN pending account WHEN invoke THEN NotReadyForActivation`() = runTest {
        // Arrange
        val jointAccount = jointAccount(status = JointAccount.Status.PENDING)
        every { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns flowOf(listOf(jointAccount))

        // Act
        val actual = useCase(account = account)

        // Assert
        assertThat(actual).isEqualTo(JointAccountActivationError.NotReadyForActivation.left())
        coVerify(exactly = 0) { signer.sign(any(), any()) }
    }

    @Test
    fun `GIVEN a free slot WHEN invoke THEN NotReadyForActivation`() = runTest {
        // Arrange — the button state may be stale by the time of the tap, so the gate is repeated here
        val jointAccount = jointAccount().let { it.copy(members = it.members.take(1)) }
        every { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns flowOf(listOf(jointAccount))

        // Act
        val actual = useCase(account = account)

        // Assert
        assertThat(actual).isEqualTo(JointAccountActivationError.NotReadyForActivation.left())
    }

    @Test
    fun `GIVEN no safe address yet WHEN invoke THEN NotReadyForActivation`() = runTest {
        // Arrange
        val jointAccount = jointAccount(safeAddress = null)
        every { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns flowOf(listOf(jointAccount))

        // Act
        val actual = useCase(account = account)

        // Assert
        assertThat(actual).isEqualTo(JointAccountActivationError.NotReadyForActivation.left())
    }

    @Test
    fun `GIVEN no matching joint account WHEN invoke THEN AccountNotFound`() = runTest {
        // Arrange
        val foreign = jointAccount().copy(cryptoAccountId = "f".repeat(n = 64))
        every { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns flowOf(listOf(foreign))

        // Act
        val actual = useCase(account = account)

        // Assert
        assertThat(actual).isEqualTo(JointAccountActivationError.AccountNotFound.left())
    }

    @Test
    fun `GIVEN the tapped wallet is not the creator WHEN invoke THEN NotCreator and nothing is sent`() = runTest {
        // Arrange — the backend would reject the signature with 403 anyway, the check only fails fast
        every { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns
            flowOf(listOf(jointAccount()))
        coEvery { signer.sign(any(), any()) } returns signResult(ownerAddress = MEMBER_ADDRESS).right()

        // Act
        val actual = useCase(account = account)

        // Assert
        assertThat(actual).isEqualTo(JointAccountActivationError.NotCreator.left())
        coVerify(exactly = 0) { repository.activate(any(), any(), any()) }
    }

    @Test
    fun `GIVEN a member address no Safe accepts WHEN invoke THEN Failed and the card is never asked`() = runTest {
        // Arrange — a composition no Safe can be deployed from is a distinct failure, not a mismatch
        val jointAccount = jointAccount().let { broken ->
            broken.copy(
                members = broken.members.map { it.copy(address = "not-an-address") },
            )
        }
        every { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns flowOf(listOf(jointAccount))

        // Act
        val actual = useCase(account = account)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(JointAccountFlowError.Failed::class.java)
        coVerify(exactly = 0) { signer.sign(any(), any()) }
    }

    @Test
    fun `GIVEN card session dismissed WHEN invoke THEN UserCancelled and nothing is sent`() = runTest {
        // Arrange
        every { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns
            flowOf(listOf(jointAccount()))
        coEvery { signer.sign(any(), any()) } returns TangemSdkError.UserCancelled().left()

        // Act
        val actual = useCase(account = account)

        // Assert
        assertThat(actual).isEqualTo(JointAccountFlowError.UserCancelled.left())
        coVerify(exactly = 0) { repository.activate(any(), any(), any()) }
    }

    @Test
    fun `GIVEN activation request fails WHEN invoke THEN Failed`() = runTest {
        // Arrange
        val cause = IllegalStateException("500")
        every { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns
            flowOf(listOf(jointAccount()))
        coEvery { repository.activate(any(), any(), any()) } returns cause.left()

        // Act
        val actual = useCase(account = account)

        // Assert
        assertThat(actual).isEqualTo(JointAccountFlowError.Failed(cause = cause).left())
        coVerify(exactly = 0) { fetcher.invoke(params = any()) }
    }

    @Test
    fun `GIVEN refresh fails after activation WHEN invoke THEN still a success`() = runTest {
        // Arrange — the account is active on the backend; failing here would push the user into a pointless retry
        every { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns
            flowOf(listOf(jointAccount()))
        coEvery { fetcher.invoke(params = any()) } returns IllegalStateException("no network").left()

        // Act
        val actual = useCase(account = account)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
    }

    /**
     * Every refusal reaches the user as the same generic dialog, so the log is the only place the reason
     * survives — and the only thing a support request carries. The address must reach it unsanitized: the
     * sanitizer replaces every hex run of 8 or more characters in the exported file.
     */
    @Test
    fun `GIVEN backend address matches no singleton WHEN invoke THEN both addresses are logged unsanitized`() =
        runTest {
            // Arrange
            mockkObject(TangemLogger)
            val jointAccount = jointAccount(safeAddress = DEAD_ADDRESS)
            every { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns flowOf(listOf(jointAccount))
            val message = slot<String>()
            val shouldSanitize = slot<Boolean>()
            every { TangemLogger.e(capture(message), any(), capture(shouldSanitize)) } just Runs

            // Act
            useCase(account = account)

            // Assert
            assertThat(message.captured).contains(DEAD_ADDRESS)
            assertThat(message.captured).contains(computedSafeAddress(SafeSingleton.SAFE_L2))
            assertThat(shouldSanitize.captured).isFalse()
            unmockkObject(TangemLogger)
        }

    @Test
    fun `GIVEN confirming account WHEN invoke THEN nothing is logged`() = runTest {
        // Arrange
        mockkObject(TangemLogger)
        every { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns flowOf(listOf(jointAccount()))

        // Act
        useCase(account = account)

        // Assert
        verify(exactly = 0) { TangemLogger.e(any(), any(), any()) }
        unmockkObject(TangemLogger)
    }

    private fun jointAccount(
        status: JointAccount.Status = JointAccount.Status.CONFIRMING,
        safeAddress: String? = computedSafeAddress(SafeSingleton.SAFE_L2),
    ): JointAccount {
        return JointAccount(
            cryptoAccountId = account.accountId.value,
            membersCount = 2,
            threshold = 2,
            safeAddress = safeAddress,
            status = status,
            members = listOf(
                JointAccount.Member(name = "Alice", address = OWNER_ADDRESS, role = JointAccount.Role.CREATOR),
                JointAccount.Member(name = "Bob", address = MEMBER_ADDRESS, role = JointAccount.Role.MEMBER),
            ),
            source = StatusSource.ACTUAL,
        )
    }

    private fun computedSafeAddress(singleton: SafeSingleton): String {
        return DefaultSafeAddressCalculator()
            .compute(owners = listOf(OWNER_ADDRESS, MEMBER_ADDRESS), threshold = 2, singleton = singleton)
            .getOrNull()!!
    }

    private fun signResult(ownerAddress: String = OWNER_ADDRESS) = JointAccountSignResult(
        ownerAddress = ownerAddress,
        canonicalPayload = byteArrayOf(1, 2, 3),
        signature = SIGNATURE,
        derivedKeys = emptyMap(),
    )

    private companion object {
        const val OWNER_ADDRESS = "0x7e5f4552091a69125d5DfCb7b8C2659029395Bdf"
        const val MEMBER_ADDRESS = "0x2B5AD5c4795c026514f8317c7a215E218DcCD6cF"
        const val DEAD_ADDRESS = "0x000000000000000000000000000000000000dEaD"
        const val SIGNATURE = "0xdeadbeef"
    }
}