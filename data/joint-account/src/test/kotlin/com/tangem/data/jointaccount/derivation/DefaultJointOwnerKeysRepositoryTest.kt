package com.tangem.data.jointaccount.derivation

import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.hexToBytes
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.jointaccount.derivation.jointAccountOwnerDerivationPath
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.OwnerKeyIndex
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultJointOwnerKeysRepositoryTest {

    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val derivationsRepository: DerivationsRepository = mockk()

    private val repository = DefaultJointOwnerKeysRepository(
        singleAccountListSupplier = singleAccountListSupplier,
        userWalletsListRepository = userWalletsListRepository,
        derivationsRepository = derivationsRepository,
    )

    private val userWalletId = UserWalletId("011")
    private val seedKey = "02AABB".hexToBytes()
    private val seedKeyWrapped = ByteArrayKey(seedKey)
    private val userWallet = coldWallet()

    @BeforeEach
    fun setUp() {
        clearMocks(singleAccountListSupplier, userWalletsListRepository, derivationsRepository)
        mockkStatic(UserWalletsListRepository::getSyncOrNull)
        every { userWalletsListRepository.getSyncOrNull(userWalletId) } returns userWallet
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(UserWalletsListRepository::getSyncOrNull)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MissingOwnerKeysCount {

        @Test
        fun `GIVEN no joint accounts WHEN missingOwnerKeysCount THEN zero`() = runTest {
            // Act
            val actual = repository.missingOwnerKeysCount(jointAccounts = emptyList(), userWallet = userWallet)

            // Assert
            assertThat(actual).isEqualTo(0)
        }

        @Test
        fun `GIVEN owner keys already derived WHEN missingOwnerKeysCount THEN zero`() = runTest {
            // Arrange
            coEvery { derivationsRepository.getExistingDerivedKeys(userWallet, seedKeyWrapped) } returns
                ExtendedPublicKeysMap(mapOf(jointAccountOwnerDerivationPath(index = 2) to mockk()))

            // Act
            val actual = repository.missingOwnerKeysCount(
                jointAccounts = listOf(joint(index = 2)),
                userWallet = userWallet,
            )

            // Assert
            assertThat(actual).isEqualTo(0)
        }

        @Test
        fun `GIVEN two joint accounts sharing a missing index WHEN missingOwnerKeysCount THEN counted once`() =
            runTest {
                // Arrange
                coEvery { derivationsRepository.getExistingDerivedKeys(userWallet, seedKeyWrapped) } returns
                    ExtendedPublicKeysMap(emptyMap())

                // Act
                val actual = repository.missingOwnerKeysCount(
                    jointAccounts = listOf(joint(index = 2), joint(index = 2)),
                    userWallet = userWallet,
                )

                // Assert
                assertThat(actual).isEqualTo(1)
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FindMissingOwnerKeyDerivations {

        @Test
        fun `GIVEN the wallet is not found WHEN findMissingOwnerKeyDerivations THEN empty map instead of throwing`() =
            runTest {
                // Arrange
                coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns accountList(joint(index = 2))
                every { userWalletsListRepository.getSyncOrNull(userWalletId) } returns null

                // Act
                val actual = repository.findMissingOwnerKeyDerivations(userWalletId)

                // Assert
                assertThat(actual).isEmpty()
            }

        @Test
        fun `GIVEN a missing owner key WHEN findMissingOwnerKeyDerivations THEN its path is returned`() = runTest {
            // Arrange
            coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns accountList(joint(index = 3))
            coEvery { derivationsRepository.getExistingDerivedKeys(userWallet, seedKeyWrapped) } returns
                ExtendedPublicKeysMap(emptyMap())

            // Act
            val actual = repository.findMissingOwnerKeyDerivations(userWalletId)

            // Assert
            assertThat(actual).isEqualTo(mapOf(seedKeyWrapped to listOf(jointAccountOwnerDerivationPath(index = 3))))
        }

        @Test
        fun `GIVEN no missing owner keys WHEN findMissingOwnerKeyDerivations THEN empty map`() = runTest {
            // Arrange
            coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns accountList(joint(index = 3))
            coEvery { derivationsRepository.getExistingDerivedKeys(userWallet, seedKeyWrapped) } returns
                ExtendedPublicKeysMap(mapOf(jointAccountOwnerDerivationPath(index = 3) to mockk()))

            // Act
            val actual = repository.findMissingOwnerKeyDerivations(userWalletId)

            // Assert
            assertThat(actual).isEmpty()
        }
    }

    private fun joint(index: Int): Account.Joint = mockk {
        every { ownerKeyIndex } returns OwnerKeyIndex(value = index).getOrNull()!!
    }

    private fun accountList(vararg jointAccounts: Account.Joint): AccountList = mockk {
        every { accounts } returns jointAccounts.toList()
    }

    private fun coldWallet(): UserWallet.Cold {
        val cardWallet = mockk<com.tangem.domain.models.scan.CardDTO.Wallet> {
            every { curve } returns EllipticCurve.Secp256k1
            every { publicKey } returns seedKey
        }
        return mockk {
            every { walletId } returns userWalletId
            every { scanResponse } returns mockk(relaxed = true) {
                every { card } returns mockk(relaxed = true) {
                    every { wallets } returns listOf(cardWallet)
                }
            }
        }
    }
}