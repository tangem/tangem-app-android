package com.tangem.data.wallets.derivations

import android.annotation.SuppressLint
import com.google.common.truth.Truth
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.CompletionResult
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.data.wallets.cold.DefaultColdMapDerivationsRepository
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.card.ScanCardException
import com.tangem.domain.card.configs.GenericCardConfig
import com.tangem.domain.card.configs.MultiWalletCardConfig
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.operations.derivation.DerivationTaskResponse
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
internal class DefaultDerivationsRepositoryTest {

    private val tangemSdkManager = mockk<TangemSdkManager>()
    private val userWalletsStore = mockk<UserWalletsStore>()
    private val repository = DefaultDerivationsRepository(
        userWalletsStore = userWalletsStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
        hotDerivationsRepository = mockk(),
        coldDerivationsRepository = DefaultColdMapDerivationsRepository(
            tangemSdkManager = tangemSdkManager,
            networkFactory = NetworkFactory(excludedBlockchains = ExcludedBlockchains()),
            dispatchers = TestingCoroutineDispatcherProvider(),
        ),
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

    @Test
    fun `error if userWalletId not found`() = runTest {
        coEvery { userWalletsStore.getSyncStrict(defaultUserWalletId) } throws IllegalStateException()

        runCatching {
            repository.derivePublicKeys(userWalletId = defaultUserWalletId, currencies = emptyList())
        }
            .onSuccess { error("Should throws exception") }
            .onFailure { Truth.assertThat(it).isInstanceOf(IllegalStateException::class.java) }

        coVerify(exactly = 1) { userWalletsStore.getSyncStrict(defaultUserWalletId) }
        coVerify(inverse = true) { tangemSdkManager.derivePublicKeys(null, any(), any()) }
        coVerify(inverse = true) { userWalletsStore.update(defaultUserWalletId, any()) }
    }

    @SuppressLint("CheckResult")
    @Test
    fun `success if card is not supported derivations`() = runTest {
        coEvery { userWalletsStore.getSyncStrict(defaultUserWalletId) } returns defaultUserWallet

        repository.derivePublicKeys(userWalletId = defaultUserWalletId, currencies = emptyList())

        runCatching { }
            .onSuccess { Truth.assertThat(it) }
            .onFailure {
                error("Should returns success")
            }

        coVerify(exactly = 1) { userWalletsStore.getSyncStrict(defaultUserWalletId) }
        coVerify(inverse = true) { tangemSdkManager.derivePublicKeys(null, any(), any()) }
        coVerify(inverse = true) { userWalletsStore.update(defaultUserWalletId, any()) }
    }

    @SuppressLint("CheckResult")
    @Test
    fun `success if currencies is empty`() = runTest {
        val userWallet = defaultUserWallet.copy(
            scanResponse = MockScanResponseFactory.create(cardConfig = MultiWalletCardConfig, derivedKeys = emptyMap()),
        )
        coEvery { userWalletsStore.getSyncStrict(defaultUserWalletId) } returns userWallet

        runCatching { repository.derivePublicKeys(userWalletId = defaultUserWalletId, currencies = emptyList()) }
            .onSuccess { Truth.assertThat(it) }
            .onFailure { error("Should returns success") }

        coVerify(exactly = 1) { userWalletsStore.getSyncStrict(defaultUserWalletId) }
        coVerify(inverse = true) { tangemSdkManager.derivePublicKeys(null, any(), any()) }
        coVerify(inverse = true) { userWalletsStore.update(defaultUserWalletId, any()) }
    }

    @SuppressLint("CheckResult")
    @Test
    fun `success if card already has derivations`() = runTest {
        val userWallet = defaultUserWallet.copy(
            scanResponse = MockScanResponseFactory.create(
                cardConfig = MultiWalletCardConfig,
                derivedKeys = DerivedKeysMocks.ethereumDerivedKeys,
            ),
        )

        coEvery { userWalletsStore.getSyncStrict(defaultUserWalletId) } returns userWallet

        runCatching {
            repository.derivePublicKeys(
                userWalletId = defaultUserWalletId,
                currencies = MockCryptoCurrencyFactory(userWallet).ethereum.let(::listOf),
            )
        }
            .onSuccess { Truth.assertThat(it) }
            .onFailure { error("Should returns success") }

        coVerify(exactly = 1) { userWalletsStore.getSyncStrict(defaultUserWalletId) }
        coVerify(inverse = true) { tangemSdkManager.derivePublicKeys(null, any(), any()) }
        coVerify(inverse = true) { userWalletsStore.update(defaultUserWalletId, any()) }
    }

    @Test
    fun `error if tangemSdkManager throws exception`() = runTest {
        val userWallet = defaultUserWallet.copy(
            scanResponse = MockScanResponseFactory.create(cardConfig = MultiWalletCardConfig, derivedKeys = emptyMap()),
        )
        coEvery { userWalletsStore.getSyncStrict(defaultUserWalletId) } returns userWallet
        coEvery { tangemSdkManager.derivePublicKeys(null, any(), any()) } throws ScanCardException.UserCancelled

        runCatching {
            repository.derivePublicKeys(
                userWalletId = defaultUserWalletId,
                currencies = MockCryptoCurrencyFactory(userWallet).ethereum.let(::listOf),
            )
        }
            .onSuccess { error("Should throws exception") }
            .onFailure { Truth.assertThat(it).isInstanceOf(ScanCardException.UserCancelled::class.java) }

        coVerify(exactly = 1) { userWalletsStore.getSyncStrict(defaultUserWalletId) }
        coVerify(exactly = 1) { tangemSdkManager.derivePublicKeys(null, any(), any()) }
        coVerify(inverse = true) { userWalletsStore.update(defaultUserWalletId, any()) }
    }

    @SuppressLint("CheckResult")
    @Test
    fun `success case`() = runTest {
        val userWallet = defaultUserWallet.copy(
            scanResponse = MockScanResponseFactory.create(cardConfig = MultiWalletCardConfig, derivedKeys = emptyMap()),
        )
        coEvery { userWalletsStore.getSyncStrict(defaultUserWalletId) } returns userWallet
        coEvery { tangemSdkManager.derivePublicKeys(null, any(), any()) } returns CompletionResult.Success(
            DerivationTaskResponse(DerivedKeysMocks.ethereumDerivedKeys),
        )
        coEvery { userWalletsStore.update(defaultUserWalletId, any()) } returns CompletionResult.Success(userWallet)

        runCatching {
            repository.derivePublicKeys(
                userWalletId = defaultUserWalletId,
                currencies = MockCryptoCurrencyFactory(userWallet).ethereum.let(::listOf),
            )
        }
            .onSuccess { Truth.assertThat(it) }
            .onFailure { error("Should returns success but $it") }

        coVerify(exactly = 1) { userWalletsStore.getSyncStrict(defaultUserWalletId) }
        coVerify(exactly = 1) { tangemSdkManager.derivePublicKeys(null, any(), any()) }
        coVerify(exactly = 1) { userWalletsStore.update(defaultUserWalletId, any()) }
    }
}