package com.tangem.domain.jointaccount.verifier

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.address.JointOwnerAddressResolver
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.domain.jointaccount.model.JointAccountConfirmationPayload
import com.tangem.domain.jointaccount.safe.SafeAddressCalculator
import com.tangem.domain.jointaccount.signing.JointSignatureRecoverer
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class JointAccountCompositionVerifierTest {

    private val ownerAddressResolver: JointOwnerAddressResolver = mockk()
    private val safeAddressCalculator: SafeAddressCalculator = mockk()
    private val signatureRecoverer: JointSignatureRecoverer = mockk()

    private val account = MockAccounts.createJointAccount(
        ownerKeyIndex = 3,
        name = "Family",
        icon = CryptoPortfolioIcon.ofCustomAccount(
            value = CryptoPortfolioIcon.Icon.Star,
            color = CryptoPortfolioIcon.Color.Azure,
        ),
    )
    private val userWalletId = account.accountId.userWalletId
    private val ownerKeyIndex = account.ownerKeyIndex

    private lateinit var verifier: JointAccountCompositionVerifier

    @BeforeEach
    fun setUp() {
        clearMocks(ownerAddressResolver, safeAddressCalculator, signatureRecoverer)
        verifier = JointAccountCompositionVerifier(
            ownerAddressResolver = ownerAddressResolver,
            safeAddressCalculator = safeAddressCalculator,
            signatureRecoverer = signatureRecoverer,
        )
        coEvery { ownerAddressResolver.resolve(userWalletId, ownerKeyIndex) } returns ALICE.right()
        every { safeAddressCalculator.compute(owners = any(), threshold = any(), singleton = any()) } returns
            SAFE_ADDRESS.right()
        every { signatureRecoverer.recoverPayloadSigner(payload = any(), signature = any()) } answers {
            SIGNERS[secondArg<String>()]?.right() ?: IllegalArgumentException("not a signature").left()
        }
    }

    @Test
    fun `GIVEN own address among members and matching safe WHEN verify THEN verified`() = runTest {
        // Act
        val actual = verifier.isVerified(account, jointAccount())

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN own address missing from members WHEN verify THEN not verified`() = runTest {
        // Act
        val actual = verifier.isVerified(account, jointAccount(members = listOf(BOB to BOB_SIGNATURE)))

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN safe address not computed from members WHEN verify THEN not verified`() = runTest {
        // Arrange
        every { safeAddressCalculator.compute(owners = any(), threshold = any(), singleton = any()) } returns
            OTHER_SAFE_ADDRESS.right()

        // Act
        val actual = verifier.isVerified(account, jointAccount())

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN pending account without safe and own membership WHEN verify THEN verified without computing`() =
        runTest {
            // Act
            val actual = verifier.isVerified(account, jointAccount(safeAddress = null))

            // Assert
            assertThat(actual).isTrue()
            coVerify(exactly = 0) { safeAddressCalculator.compute(owners = any(), threshold = any(), singleton = any()) }
            verify(exactly = 0) { signatureRecoverer.recoverPayloadSigner(payload = any(), signature = any()) }
        }

    @Test
    fun `GIVEN an active account without a safe address WHEN verify THEN not verified`() = runTest {
        // Act
        val actual = verifier.isVerified(account, jointAccount(safeAddress = null, status = JointAccount.Status.ACTIVE))

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN own address cannot be derived WHEN verify THEN not verified`() = runTest {
        // Arrange
        coEvery { ownerAddressResolver.resolve(userWalletId, ownerKeyIndex) } returns IllegalStateException().left()

        // Act
        val actual = verifier.isVerified(account, jointAccount())

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN no owner key stored WHEN verify THEN not verified`() = runTest {
        // Arrange
        coEvery { ownerAddressResolver.resolve(userWalletId, ownerKeyIndex) } returns null.right()

        // Act
        val actual = verifier.isVerified(account, jointAccount())

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN derivation failed once WHEN verify again THEN the address is derived again`() = runTest {
        // Arrange
        coEvery { ownerAddressResolver.resolve(userWalletId, ownerKeyIndex) } returnsMany listOf(
            IllegalStateException().left(),
            ALICE.right(),
        )

        // Act
        val first = verifier.isVerified(account, jointAccount())
        val second = verifier.isVerified(account, jointAccount())

        // Assert
        assertThat(first).isFalse()
        assertThat(second).isTrue()
    }

    @Test
    fun `GIVEN repeated checks WHEN verify THEN own address derived once`() = runTest {
        // Act
        repeat(times = 3) { verifier.isVerified(account, jointAccount()) }

        // Assert
        coVerify(exactly = 1) { ownerAddressResolver.resolve(userWalletId, ownerKeyIndex) }
    }

    @Test
    fun `GIVEN signatures WHEN verify THEN they are recovered over the payload every member signs`() = runTest {
        // Arrange
        val payload = slot<JointAccountConfirmationPayload>()
        every {
            signatureRecoverer.recoverPayloadSigner(payload = capture(payload), signature = ALICE_SIGNATURE)
        } returns ALICE.right()

        // Act
        verifier.isVerified(
            account,
            jointAccount(members = listOf(ALICE to ALICE_SIGNATURE), safeAddress = CHECKSUMMED_SAFE),
        )

        // Assert
        assertThat(payload.captured).isEqualTo(
            JointAccountConfirmationPayload(
                config = JointAccountConfig(
                    name = "Family",
                    icon = "Star",
                    iconColor = "Azure",
                    membersCount = 1,
                    threshold = 1,
                ),
                safeAddress = CHECKSUMMED_SAFE.lowercase(),
            ),
        )
    }

    @Test
    fun `GIVEN an active account with a member not confirmed WHEN verify THEN not verified`() = runTest {
        // Act
        val actual = verifier.isVerified(account, jointAccount(members = listOf(ALICE to ALICE_SIGNATURE, BOB to null)))

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN an active account with a signature of someone else WHEN verify THEN not verified`() = runTest {
        // Act
        val actual = verifier.isVerified(
            account,
            jointAccount(members = listOf(ALICE to ALICE_SIGNATURE, BOB to ALICE_SIGNATURE)),
        )

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN an active account with a signature that does not recover WHEN verify THEN not verified`() = runTest {
        // Act
        val actual = verifier.isVerified(
            account,
            jointAccount(members = listOf(ALICE to ALICE_SIGNATURE, BOB to "0xgarbage")),
        )

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN a confirming account with a member not confirmed yet WHEN verify THEN verified`() = runTest {
        // Act
        val actual = verifier.isVerified(
            account,
            jointAccount(
                members = listOf(ALICE to ALICE_SIGNATURE, BOB to null),
                status = JointAccount.Status.CONFIRMING,
            ),
        )

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN a confirming account with no one confirmed WHEN verify THEN verified without recovering`() = runTest {
        // Act
        val actual = verifier.isVerified(
            account,
            jointAccount(members = listOf(ALICE to null, BOB to null), status = JointAccount.Status.CONFIRMING),
        )

        // Assert
        assertThat(actual).isTrue()
        verify(exactly = 0) { signatureRecoverer.recoverPayloadSigner(payload = any(), signature = any()) }
    }

    @Test
    fun `GIVEN a confirming account with a signature of someone else WHEN verify THEN not verified`() = runTest {
        // Act
        val actual = verifier.isVerified(
            account,
            jointAccount(
                members = listOf(ALICE to null, BOB to ALICE_SIGNATURE),
                status = JointAccount.Status.CONFIRMING,
            ),
        )

        // Assert
        assertThat(actual).isFalse()
    }

    private fun jointAccount(
        members: List<Pair<String, String?>> = listOf(ALICE to ALICE_SIGNATURE, BOB to BOB_SIGNATURE),
        safeAddress: String? = SAFE_ADDRESS,
        status: JointAccount.Status = when (safeAddress) {
            null -> JointAccount.Status.PENDING
            else -> JointAccount.Status.ACTIVE
        },
    ) = JointAccount(
        cryptoAccountId = "joint",
        membersCount = members.size,
        threshold = members.size,
        safeAddress = safeAddress,
        status = status,
        members = members.map { (address, signature) ->
            JointAccount.Member(
                name = "Member",
                address = address,
                role = JointAccount.Role.MEMBER,
                signature = signature,
            )
        },
        source = StatusSource.ACTUAL,
        inFlightOperations = emptyMap(),
    )

    private companion object {
        const val ALICE = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf"
        const val BOB = "0x2b5ad5c4795c026514f8317c7a215e218dccd6cf"
        val ALICE_SIGNATURE = "0x" + "aa".repeat(n = 64) + "1b"
        val BOB_SIGNATURE = "0x" + "bb".repeat(n = 64) + "1c"
        val SIGNERS = mapOf(ALICE_SIGNATURE to ALICE, BOB_SIGNATURE to BOB)
        const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"
        const val CHECKSUMMED_SAFE = "0x5AFE000000000000000000000000000000000001"
        const val OTHER_SAFE_ADDRESS = "0x5afe000000000000000000000000000000000002"
    }
}