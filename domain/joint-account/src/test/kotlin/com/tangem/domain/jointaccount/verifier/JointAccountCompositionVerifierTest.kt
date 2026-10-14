package com.tangem.domain.jointaccount.verifier

import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.address.JointOwnerAddressResolver
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.safe.SafeAddressCalculator
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.OwnerKeyIndex
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class JointAccountCompositionVerifierTest {

    private val ownerAddressResolver: JointOwnerAddressResolver = mockk()
    private val safeAddressCalculator: SafeAddressCalculator = mockk()

    private lateinit var verifier: JointAccountCompositionVerifier

    @BeforeEach
    fun setUp() {
        clearMocks(ownerAddressResolver, safeAddressCalculator)
        verifier = JointAccountCompositionVerifier(
            ownerAddressResolver = ownerAddressResolver,
            safeAddressCalculator = safeAddressCalculator,
        )
        coEvery { ownerAddressResolver.resolve(userWalletId, ownerKeyIndex) } returns ALICE.right()
        every { safeAddressCalculator.compute(owners = any(), threshold = any(), singleton = any()) } returns
            SAFE_ADDRESS.right()
    }

    @Test
    fun `GIVEN own address among members and matching safe WHEN verify THEN verified`() = runTest {
        // Act
        val actual = verifier.isVerified(userWalletId, ownerKeyIndex, jointAccount())

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN own address missing from members WHEN verify THEN not verified`() = runTest {
        // Act
        val actual = verifier.isVerified(userWalletId, ownerKeyIndex, jointAccount(members = listOf(BOB)))

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN safe address not computed from members WHEN verify THEN not verified`() = runTest {
        // Arrange
        every { safeAddressCalculator.compute(owners = any(), threshold = any(), singleton = any()) } returns
            OTHER_SAFE_ADDRESS.right()

        // Act
        val actual = verifier.isVerified(userWalletId, ownerKeyIndex, jointAccount())

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN pending account without safe and own membership WHEN verify THEN verified without computing`() =
        runTest {
            // Act
            val actual = verifier.isVerified(userWalletId, ownerKeyIndex, jointAccount(safeAddress = null))

            // Assert
            assertThat(actual).isTrue()
            coVerify(exactly = 0) { safeAddressCalculator.compute(owners = any(), threshold = any(), singleton = any()) }
        }

    @Test
    fun `GIVEN an active account without a safe address WHEN verify THEN not verified`() = runTest {
        // Act
        val actual = verifier.isVerified(
            userWalletId,
            ownerKeyIndex,
            jointAccount(safeAddress = null).copy(status = JointAccount.Status.ACTIVE),
        )

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN own address cannot be derived WHEN verify THEN not verified`() = runTest {
        // Arrange
        coEvery { ownerAddressResolver.resolve(userWalletId, ownerKeyIndex) } returns IllegalStateException().left()

        // Act
        val actual = verifier.isVerified(userWalletId, ownerKeyIndex, jointAccount())

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN no owner key stored WHEN verify THEN not verified`() = runTest {
        // Arrange
        coEvery { ownerAddressResolver.resolve(userWalletId, ownerKeyIndex) } returns null.right()

        // Act
        val actual = verifier.isVerified(userWalletId, ownerKeyIndex, jointAccount())

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
        val first = verifier.isVerified(userWalletId, ownerKeyIndex, jointAccount())
        val second = verifier.isVerified(userWalletId, ownerKeyIndex, jointAccount())

        // Assert
        assertThat(first).isFalse()
        assertThat(second).isTrue()
    }

    @Test
    fun `GIVEN repeated checks WHEN verify THEN own address derived once`() = runTest {
        // Act
        repeat(times = 3) { verifier.isVerified(userWalletId, ownerKeyIndex, jointAccount()) }

        // Assert
        coVerify(exactly = 1) { ownerAddressResolver.resolve(userWalletId, ownerKeyIndex) }
    }

    private fun jointAccount(
        members: List<String> = listOf(ALICE, BOB),
        safeAddress: String? = SAFE_ADDRESS,
    ) = JointAccount(
        cryptoAccountId = "joint",
        membersCount = members.size,
        threshold = 2,
        safeAddress = safeAddress,
        status = if (safeAddress == null) JointAccount.Status.PENDING else JointAccount.Status.ACTIVE,
        members = members.map { address ->
            JointAccount.Member(name = "Member", address = address, role = JointAccount.Role.MEMBER)
        },
        source = StatusSource.ACTUAL,
        inFlightOperations = emptyMap(),
    )

    private companion object {
        val userWalletId = UserWalletId("011")
        val ownerKeyIndex: OwnerKeyIndex = OwnerKeyIndex(value = 3).getOrElse { error(it) }
        const val ALICE = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf"
        const val BOB = "0x2b5ad5c4795c026514f8317c7a215e218dccd6cf"
        const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"
        const val OTHER_SAFE_ADDRESS = "0x5afe000000000000000000000000000000000002"
    }
}