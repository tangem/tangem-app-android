package com.tangem.data.jointaccount.address

import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.hexToBytes
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.data.jointaccount.signing.JointOwnerKeyReader
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.jointaccount.derivation.jointAccountOwnerDerivationPath
import com.tangem.domain.models.account.OwnerKeyIndex
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultJointOwnerAddressResolverTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val derivationsRepository: DerivationsRepository = mockk()

    private val resolver = DefaultJointOwnerAddressResolver(
        ownerKeyReader = JointOwnerKeyReader(
            userWalletsListRepository = userWalletsListRepository,
            derivationsRepository = derivationsRepository,
        ),
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val userWalletId = UserWalletId("011")
    private val ownerKeyIndex = OwnerKeyIndex(value = 2).getOrNull()!!
    private val ownerPath = jointAccountOwnerDerivationPath(index = 2)
    private val seedKey = "02AABB".hexToBytes()

    // The generator point G — the public key of private key 1, whose EVM address is a known constant
    private val ownerKey = ExtendedPublicKey(
        publicKey = "0279BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798".hexToBytes(),
        chainCode = ByteArray(size = 32),
    )

    @BeforeEach
    fun setUp() {
        clearMocks(userWalletsListRepository, derivationsRepository)
        mockkStatic(UserWalletsListRepository::getSyncStrict)
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet()
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(UserWalletsListRepository::getSyncStrict)
    }

    @Test
    fun `GIVEN a derived owner key WHEN resolve THEN the same address a signature recovers to`() = runTest {
        // Arrange
        coEvery { derivationsRepository.getExistingDerivedKeys(userWalletId, ByteArrayKey(seedKey)) } returns
            ExtendedPublicKeysMap(mapOf(ownerPath to ownerKey))

        // Act
        val actual = resolver.resolve(userWalletId = userWalletId, ownerKeyIndex = ownerKeyIndex)

        // Assert: the EVM address of private key 1 — pinned so derivation and recovery cannot diverge
        assertThat(actual.getOrNull()).isEqualTo("0x7E5F4552091A69125d5DfCb7b8C2659029395Bdf")
    }

    @Test
    fun `GIVEN the owner key is not derived WHEN resolve THEN null instead of an error`() = runTest {
        // Arrange
        coEvery { derivationsRepository.getExistingDerivedKeys(userWalletId, ByteArrayKey(seedKey)) } returns
            ExtendedPublicKeysMap(emptyMap())

        // Act
        val actual = resolver.resolve(userWalletId = userWalletId, ownerKeyIndex = ownerKeyIndex)

        // Assert
        assertThat(actual.getOrNull()).isNull()
        assertThat(actual.isRight()).isTrue()
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