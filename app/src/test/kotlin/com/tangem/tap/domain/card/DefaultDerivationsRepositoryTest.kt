package com.tangem.tap.domain.card

import android.annotation.SuppressLint
import com.google.common.truth.Truth
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.CompletionResult
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.card.ScanCardException
import com.tangem.domain.common.configs.GenericCardConfig
import com.tangem.domain.common.configs.MultiWalletCardConfig
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.operations.derivation.DerivationTaskResponse
import com.tangem.tap.domain.sdk.impl.DefaultTangemSdkManager
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
* [REDACTED_AUTHOR]
 */
internal class DefaultDerivationsRepositoryTest {

    private val tangemSdkManager = mockk<DefaultTangemSdkManager>()
    private val userWalletsStore = mockk<UserWalletsStore>()
    private val repository = DefaultDerivationsRepository(
        tangemSdkManager = tangemSdkManager,
        userWalletsStore = userWalletsStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
        excludedBlockchains = ExcludedBlockchains(),
    )

    private val defaultUserWalletId = UserWalletId("011")
    private val defaultUserWallet = UserWallet(
        name = "",
        walletId = defaultUserWalletId,
        artworkUrl = "",
        cardsInWallet = setOf(),
        isMultiCurrency = false,
        scanResponse = MockScanResponseFactory.create(cardConfig = GenericCardConfig(2), derivedKeys = emptyMap()),
        hasBackupError = false,
    )

    @Test
    fun `error if userWalletId not found`() = runTest {
        coEvery { userWalletsStore.getSyncOrNull(defaultUserWalletId) } returns null

        runCatching {
            repository.derivePublicKeys(userWalletId = defaultUserWalletId, currencies = emptyList())
        }
            .onSuccess { error("Should throws exception") }
            .onFailure { Truth.assertThat(it).isInstanceOf(IllegalStateException::class.java) }

        coVerify(exactly = 1) { userWalletsStore.getSyncOrNull(defaultUserWalletId) }
        coVerify(inverse = true) { tangemSdkManager.derivePublicKeys(null, any(), any()) }
        coVerify(inverse = true) { userWalletsStore.update(defaultUserWalletId, any()) }
    }

    @SuppressLint("CheckResult")
    @Test
    fun `success if card is not supported derivations`() = runTest {
        coEvery { userWalletsStore.getSyncOrNull(defaultUserWalletId) } returns defaultUserWallet

        runCatching { repository.derivePublicKeys(userWalletId = defaultUserWalletId, currencies = emptyList()) }
            .onSuccess { Truth.assertThat(it) }
            .onFailure { error("Should returns success") }

        coVerify(exactly = 1) { userWalletsStore.getSyncOrNull(defaultUserWalletId) }
        coVerify(inverse = true) { tangemSdkManager.derivePublicKeys(null, any(), any()) }
        coVerify(inverse = true) { userWalletsStore.update(defaultUserWalletId, any()) }
    }

    @SuppressLint("CheckResult")
    @Test
    fun `success if currencies is empty`() = runTest {
        val userWallet = defaultUserWallet.copy(
            scanResponse = MockScanResponseFactory.create(cardConfig = MultiWalletCardConfig, derivedKeys = emptyMap()),
        )
        coEvery { userWalletsStore.getSyncOrNull(defaultUserWalletId) } returns userWallet

        runCatching { repository.derivePublicKeys(userWalletId = defaultUserWalletId, currencies = emptyList()) }
            .onSuccess { Truth.assertThat(it) }
            .onFailure { error("Should returns success") }

        coVerify(exactly = 1) { userWalletsStore.getSyncOrNull(defaultUserWalletId) }
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

        coEvery { userWalletsStore.getSyncOrNull(defaultUserWalletId) } returns userWallet

        runCatching {
            repository.derivePublicKeys(
                userWalletId = defaultUserWalletId,
                currencies = MockCryptoCurrencyFactory(userWallet.scanResponse).ethereum.let(::listOf),
            )
        }
            .onSuccess { Truth.assertThat(it) }
            .onFailure { error("Should returns success") }

        coVerify(exactly = 1) { userWalletsStore.getSyncOrNull(defaultUserWalletId) }
        coVerify(inverse = true) { tangemSdkManager.derivePublicKeys(null, any(), any()) }
        coVerify(inverse = true) { userWalletsStore.update(defaultUserWalletId, any()) }
    }

    @Test
    fun `error if tangemSdkManager throws exception`() = runTest {
        val userWallet = defaultUserWallet.copy(
            scanResponse = MockScanResponseFactory.create(cardConfig = MultiWalletCardConfig, derivedKeys = emptyMap()),
        )
        coEvery { userWalletsStore.getSyncOrNull(defaultUserWalletId) } returns userWallet
        coEvery { tangemSdkManager.derivePublicKeys(null, any(), any()) } throws ScanCardException.UserCancelled

        runCatching {
            repository.derivePublicKeys(
                userWalletId = defaultUserWalletId,
                currencies = MockCryptoCurrencyFactory(userWallet.scanResponse).ethereum.let(::listOf),
            )
        }
            .onSuccess { error("Should throws exception") }
            .onFailure { Truth.assertThat(it).isInstanceOf(ScanCardException.UserCancelled::class.java) }

        coVerify(exactly = 1) { userWalletsStore.getSyncOrNull(defaultUserWalletId) }
        coVerify(exactly = 1) { tangemSdkManager.derivePublicKeys(null, any(), any()) }
        coVerify(inverse = true) { userWalletsStore.update(defaultUserWalletId, any()) }
    }

    @SuppressLint("CheckResult")
    @Test
    fun `success case`() = runTest {
        val userWallet = defaultUserWallet.copy(
            scanResponse = MockScanResponseFactory.create(cardConfig = MultiWalletCardConfig, derivedKeys = emptyMap()),
        )
        coEvery { userWalletsStore.getSyncOrNull(defaultUserWalletId) } returns userWallet
        coEvery { tangemSdkManager.derivePublicKeys(null, any(), any()) } returns CompletionResult.Success(
            DerivationTaskResponse(DerivedKeysMocks.ethereumDerivedKeys),
        )
        coEvery { userWalletsStore.update(defaultUserWalletId, any()) } returns CompletionResult.Success(userWallet)

        runCatching {
            repository.derivePublicKeys(
                userWalletId = defaultUserWalletId,
                currencies = MockCryptoCurrencyFactory(userWallet.scanResponse).ethereum.let(::listOf),
            )
        }
            .onSuccess { Truth.assertThat(it) }
            .onFailure { error("Should returns success but $it") }

        coVerify(exactly = 1) { userWalletsStore.getSyncOrNull(defaultUserWalletId) }
        coVerify(exactly = 1) { tangemSdkManager.derivePublicKeys(null, any(), any()) }
        coVerify(exactly = 1) { userWalletsStore.update(defaultUserWalletId, any()) }
    }
}
