package com.tangem.data.virtualaccount.repository

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.visa.model.VirtualAccountActivationData
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultVirtualAccountActivationRepositoryTest {

    private val authDataSource: TangemPayAuthDataSource = mockk()
    private val derivationsRepository: DerivationsRepository = mockk(relaxUnitFun = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val repository = DefaultVirtualAccountActivationRepository(
        authDataSource = authDataSource,
        derivationsRepository = derivationsRepository,
        userWalletsListRepository = userWalletsListRepository,
        dispatchers = dispatchers,
    )

    private val userWalletId = UserWalletId("011")
    private val userWallet: UserWallet = mockk { every { walletId } returns userWalletId }

    private val derivedKeys: Map<ByteArrayKey, ExtendedPublicKeysMap> = mapOf(
        ByteArrayKey(byteArrayOf(1, 2, 3)) to ExtendedPublicKeysMap(emptyMap()),
    )
    private val activationData = VirtualAccountActivationData(address = "0xVA", derivedKeys = derivedKeys)

    @BeforeEach
    fun setUp() {
        clearMocks(authDataSource, derivationsRepository, userWalletsListRepository)
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
    }

    @Test
    fun `GIVEN datasource returns data WHEN activate THEN derived keys persisted`() = runTest {
        // Arrange
        coEvery { authDataSource.produceVirtualAccountData(userWallet) } returns activationData.right()

        // Act
        repository.activateVirtualAccount(userWalletId)

        // Assert
        coVerify(exactly = 1) { derivationsRepository.storeDerivedKeys(userWalletId, derivedKeys) }
    }

    @Test
    fun `GIVEN datasource returns error WHEN activate THEN throws AND nothing persisted`() = runTest {
        // Arrange
        coEvery { authDataSource.produceVirtualAccountData(userWallet) } returns IllegalStateException("nope").left()

        // Act
        val error = runCatching { repository.activateVirtualAccount(userWalletId) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
        coVerify(exactly = 0) { derivationsRepository.storeDerivedKeys(any(), any()) }
    }
}