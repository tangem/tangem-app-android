package com.tangem.data.wallets.derivations

import arrow.core.right
import com.google.common.truth.Truth
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.card.configs.GenericCardConfig
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.derivations.ColdMapDerivationsRepository
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultDerivationsRepositoryTest {

    private val userWalletsListRepository = mockk<UserWalletsListRepository>()
    private val coldDerivationsRepository: ColdMapDerivationsRepository = mockk()

    private val repository = DefaultDerivationsRepository(
        userWalletsListRepository = userWalletsListRepository,
        hotDerivationsRepository = mockk(),
        coldDerivationsRepository = coldDerivationsRepository,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val defaultUserWalletId = UserWalletId("011")
    private val defaultUserWallet = UserWallet.Cold(
        name = "",
        walletId = defaultUserWalletId,
        cardsInWallet = setOf(),
        isMultiCurrency = false,
        scanResponse = MockScanResponseFactory.create(cardConfig = GenericCardConfig(2), derivedKeys = emptyMap()),
        hasBackupError = false,
    )

    @AfterEach
    fun tearDown() {
        clearMocks(userWalletsListRepository, coldDerivationsRepository)
    }

    @Test
    fun `error if userWalletId not found`() = runTest {
        val currencies = MockCryptoCurrencyFactory(defaultUserWallet).ethereum.let(::listOf)
        coEvery { userWalletsListRepository.getSyncStrict(defaultUserWalletId) } throws IllegalStateException()

        runCatching {
            repository.derivePublicKeys(userWalletId = defaultUserWalletId, currencies = currencies)
        }
            .onSuccess { error("Should throws exception") }
            .onFailure { Truth.assertThat(it).isInstanceOf(IllegalStateException::class.java) }

        coVerify(exactly = 1) { userWalletsListRepository.getSyncStrict(defaultUserWalletId) }
        coVerify(inverse = true) {
            coldDerivationsRepository.derivePublicKeysByNetworks(any(), any())
            userWalletsListRepository.saveWithoutLock(any(), any())
        }
    }

    @Test
    fun `success if currencies is empty`() = runTest {
        repository.derivePublicKeys(userWalletId = defaultUserWalletId, currencies = emptyList())

        coVerify(inverse = true) {
            userWalletsListRepository.getSyncStrict(any())
            coldDerivationsRepository.derivePublicKeysByNetworks(any(), any())
            userWalletsListRepository.saveWithoutLock(any(), any())
        }
    }

    @Test
    fun `error if coldDerivationsRepository throws exception`() = runTest {
        val currencies = MockCryptoCurrencyFactory(defaultUserWallet).ethereum.let(::listOf)

        coEvery { userWalletsListRepository.getSyncStrict(defaultUserWalletId) } returns defaultUserWallet
        coEvery {
            coldDerivationsRepository.derivePublicKeysByNetworks(
                userWallet = defaultUserWallet,
                networks = any(),
            )
        } throws IllegalStateException()

        runCatching {
            repository.derivePublicKeys(userWalletId = defaultUserWalletId, currencies = currencies)
        }
            .onSuccess { error("Should throws exception") }
            .onFailure { Truth.assertThat(it).isInstanceOf(IllegalStateException::class.java) }

        coVerifyOrder {
            userWalletsListRepository.getSyncStrict(defaultUserWalletId)
            coldDerivationsRepository.derivePublicKeysByNetworks(
                userWallet = defaultUserWallet,
                networks = currencies.map(CryptoCurrency.Coin::network),
            )
        }

        coVerify(inverse = true) { userWalletsListRepository.saveWithoutLock(any(), any()) }
    }

    @Test
    fun `success case`() = runTest {
        val currencies = MockCryptoCurrencyFactory(defaultUserWallet).ethereum.let(::listOf)
        val updatedWallet = defaultUserWallet.copy(cardsInWallet = setOf("AC01"))

        coEvery { userWalletsListRepository.getSyncStrict(defaultUserWalletId) } returns defaultUserWallet
        coEvery {
            coldDerivationsRepository.derivePublicKeysByNetworks(
                userWallet = defaultUserWallet,
                networks = currencies.map(CryptoCurrency.Coin::network),
            )
        } returns updatedWallet
        coEvery {
            userWalletsListRepository.saveWithoutLock(updatedWallet, true)
        } returns updatedWallet.right()

        repository.derivePublicKeys(userWalletId = defaultUserWalletId, currencies = currencies)

        coVerifyOrder {
            userWalletsListRepository.getSyncStrict(defaultUserWalletId)
            coldDerivationsRepository.derivePublicKeysByNetworks(
                userWallet = defaultUserWallet,
                networks = currencies.map(CryptoCurrency.Coin::network),
            )
            userWalletsListRepository.saveWithoutLock(updatedWallet, true)
        }
    }
}