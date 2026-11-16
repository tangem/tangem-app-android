package com.tangem.data.collectibles.account.derivation

import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.collectibles.account.derivation.COLLECTIBLES_OWNER_DERIVATION_PATH
import com.tangem.domain.collectibles.account.repository.CollectiblesAccountRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.MobileWallet
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.features.collectibles.api.CollectiblesFeatureToggles
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.utils.extensions.hexToBytes
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class DefaultCollectiblesOwnerKeysRepositoryTest {

    private val accountRepository: CollectiblesAccountRepository = mockk()
    private val derivationsRepository: DerivationsRepository = mockk()
    private val walletAccountsFetcher: WalletAccountsFetcher = mockk()
    private val collectiblesFeatureToggles: CollectiblesFeatureToggles = mockk()

    private val userWalletId = UserWalletId("011")
    private val seedKey = "02AABB".hexToBytes()
    private val path = DerivationPath(rawPath = COLLECTIBLES_OWNER_DERIVATION_PATH)
    private val userWallet = hotWallet()

    private val repository = DefaultCollectiblesOwnerKeysRepository(
        collectiblesAccountRepository = accountRepository,
        userWalletsListRepository = mockk {
            every { userWallets } returns MutableStateFlow<List<UserWallet>?>(listOf(userWallet))
        },
        derivationsRepository = derivationsRepository,
        walletAccountsFetcher = walletAccountsFetcher,
        collectiblesFeatureToggles = collectiblesFeatureToggles,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(accountRepository, derivationsRepository, walletAccountsFetcher, collectiblesFeatureToggles)
        every { collectiblesFeatureToggles.isCollectiblesEnabled } returns true
        coEvery { accountRepository.getStatusSync(userWalletId) } returns
            CollectiblesAccountStatusValue.Onboarded(source = StatusSource.CACHE)
        coEvery { walletAccountsFetcher.getSaved(userWalletId) } returns null
        coEvery { derivationsRepository.getExistingDerivedKeys(userWallet, ByteArrayKey(seedKey)) } returns
            ExtendedPublicKeysMap(emptyMap())
    }

    @Test
    fun `GIVEN an account without the owner key WHEN findMissingOwnerKeyDerivation THEN the owner path is missing`() =
        runTest {
            // Act
            val actual = repository.findMissingOwnerKeyDerivation(userWalletId)

            // Assert
            assertThat(actual).isEqualTo(mapOf(ByteArrayKey(seedKey) to listOf(path)))
        }

    @Test
    fun `GIVEN collectibles disabled WHEN findMissingOwnerKeyDerivation THEN nothing is missing`() =
        runTest {
            // Arrange
            every { collectiblesFeatureToggles.isCollectiblesEnabled } returns false
            coEvery { accountRepository.getStatusSync(userWalletId) } returns null
            coEvery { walletAccountsFetcher.getSaved(userWalletId) } returns mockk<GetWalletAccountsResponse> {
                every { accounts } returns listOf(COLLECTIBLES_RECORD)
            }

            // Act
            val actual = repository.findMissingOwnerKeyDerivation(userWalletId)

            // Assert
            assertThat(actual).isEmpty()
            coVerify(exactly = 0) { derivationsRepository.getExistingDerivedKeys(any<UserWallet>(), any()) }
        }

    /** A re-added wallet: the record went with it, the backend still lists the account */
    @Test
    fun `GIVEN only the backend lists the account WHEN findMissingOwnerKeyDerivation THEN the owner path is missing`() =
        runTest {
            // Arrange
            coEvery { accountRepository.getStatusSync(userWalletId) } returns null
            coEvery { walletAccountsFetcher.getSaved(userWalletId) } returns mockk<GetWalletAccountsResponse> {
                every { accounts } returns listOf(COLLECTIBLES_RECORD)
            }

            // Act
            val actual = repository.findMissingOwnerKeyDerivation(userWalletId)

            // Assert
            assertThat(actual).isEqualTo(mapOf(ByteArrayKey(seedKey) to listOf(path)))
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN an account WHEN missingOwnerKeysCount THEN it counts the key unless derived`(isDerived: Boolean) =
        runTest {
            // Arrange
            val derived = if (isDerived) mapOf(path to mockk<ExtendedPublicKey>()) else emptyMap()
            coEvery { derivationsRepository.getExistingDerivedKeys(userWallet, ByteArrayKey(seedKey)) } returns
                ExtendedPublicKeysMap(derived)

            // Act
            val actual = repository.missingOwnerKeysCount(listOf(COLLECTIBLES_ACCOUNT), userWallet)

            // Assert
            assertThat(actual).isEqualTo(if (isDerived) 0 else 1)
        }

    @Test
    fun `GIVEN no collectibles account WHEN missingOwnerKeysCount THEN nothing is counted`() = runTest {
        // Act
        val actual = repository.missingOwnerKeysCount(emptyList(), userWallet)

        // Assert
        assertThat(actual).isEqualTo(0)
        coVerify(exactly = 0) { derivationsRepository.getExistingDerivedKeys(any<UserWallet>(), any()) }
    }

    @Test
    fun `GIVEN a wallet without a Solana seed key WHEN missingOwnerKeysCount THEN nothing is counted`() = runTest {
        // Arrange
        val walletWithoutSolana = mockk<UserWallet.Hot> {
            every { walletId } returns userWalletId
            every { wallets } returns emptyList()
        }

        // Act
        val actual = repository.missingOwnerKeysCount(listOf(COLLECTIBLES_ACCOUNT), walletWithoutSolana)

        // Assert
        assertThat(actual).isEqualTo(0)
    }

    @Test
    fun `GIVEN an unknown wallet WHEN findMissingOwnerKeyDerivation THEN nothing is missing`() = runTest {
        // Act
        val actual = repository.findMissingOwnerKeyDerivation(UserWalletId("ff"))

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN no collectibles account WHEN findMissingOwnerKeyDerivation THEN nothing is missing`() = runTest {
        // Arrange
        coEvery { accountRepository.getStatusSync(userWalletId) } returns null

        // Act
        val actual = repository.findMissingOwnerKeyDerivation(userWalletId)

        // Assert
        assertThat(actual).isEmpty()
        coVerify(exactly = 0) { derivationsRepository.getExistingDerivedKeys(any<UserWallet>(), any()) }
    }

    private fun hotWallet(): UserWallet.Hot {
        val mobileWallet = MobileWallet(
            publicKey = seedKey,
            chainCode = null,
            curve = EllipticCurve.Ed25519Slip0010,
            derivedKeys = emptyMap(),
        )
        return mockk {
            every { walletId } returns userWalletId
            every { wallets } returns listOf(mobileWallet)
        }
    }

    private companion object {
        val COLLECTIBLES_ACCOUNT = Account.Collectibles(UserWalletId("011"))

        val COLLECTIBLES_RECORD = WalletAccountDTO(
            id = "collectibles",
            name = "Collectibles",
            derivationIndex = 999799,
            icon = "Package",
            iconColor = "Azure",
            type = WalletAccountDTO.Type.COLLECTIBLES,
        )
    }
}