package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.common.wallets.error.SaveWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.builder.HotUserWalletBuilder
import com.tangem.domain.wallets.models.CreateHotWalletError
import com.tangem.domain.wallets.models.HotWalletAdditionRestriction
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.hot.sdk.model.MnemonicType
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@Suppress("LongParameterList")
class CreateHotWalletUseCase @Inject constructor(
    private val tangemHotSdk: TangemHotSdk,
    private val hotUserWalletBuilderFactory: HotUserWalletBuilder.Factory,
    private val saveWalletUseCase: SaveWalletUseCase,
    private val syncWalletWithRemoteUseCase: SyncWalletWithRemoteUseCase,
    private val getHotWalletAdditionRestrictionUseCase: GetHotWalletAdditionRestrictionUseCase,
    private val discardUnsavedHotWalletUseCase: DiscardUnsavedHotWalletUseCase,
    private val appCoroutineScope: AppCoroutineScope,
) {
    suspend operator fun invoke(
        auth: HotAuth,
        mnemonicType: MnemonicType,
    ): Either<CreateHotWalletError, UserWallet.Hot> = either {
        val restriction = runSuspendCatching { getHotWalletAdditionRestrictionUseCase(isSocialLogin = false) }
            .getOrElse { raise(CreateHotWalletError.Failed(it)) }
        if (restriction == HotWalletAdditionRestriction.LimitReached) raise(CreateHotWalletError.LimitReached)

        val hotWalletId = runSuspendCatching { tangemHotSdk.generateWallet(auth, mnemonicType) }
            .getOrElse { raise(CreateHotWalletError.Failed(it)) }

        val result = try {
            buildAndSave(hotWalletId)
        } catch (e: CancellationException) {
            withContext(NonCancellable) { discardUnsavedHotWalletUseCase(hotWalletId) }
            throw e
        }

        result.onRight { userWallet ->
            appCoroutineScope.launch {
                syncWalletWithRemoteUseCase(userWalletId = userWallet.walletId)
            }
        }
        // the save is not cancellable, so a caller that left meanwhile must not get a result to act on
        currentCoroutineContext().ensureActive()
        val userWallet = result.bind()

        // Auth Service registration is triggered centrally by SaveWalletUseCase for every new hot
        // wallet, so no explicit call is needed here.

        userWallet
    }

    private suspend fun buildAndSave(hotWalletId: HotWalletId): Either<CreateHotWalletError, UserWallet.Hot> = either {
        val userWallet = runSuspendCatching { hotUserWalletBuilderFactory.create(hotWalletId).build() }
            .getOrElse { error ->
                withContext(NonCancellable) { discardUnsavedHotWalletUseCase(hotWalletId) }
                raise(CreateHotWalletError.Failed(error))
            }

        // A save cancelled half way leaves the wallet on disk but not yet listed, so the cleanup would take its keys
        val saveResult = withContext(NonCancellable) { runSuspendCatching { saveWalletUseCase(userWallet) } }
        val saveError = saveResult.getOrNull()?.leftOrNull()
        val saveException = saveResult.exceptionOrNull()
        if (saveError == null && saveException == null) return@either userWallet
        TangemLogger.e("Unable to save the created mobile wallet: $saveError", saveException)

        val isListedAnyway = withContext(NonCancellable) { discardUnsavedHotWalletUseCase(hotWalletId) }
        when {
            saveError is SaveWalletError.HotWalletsLimitReached -> raise(CreateHotWalletError.LimitReached)
            // Setting the lock can fail after the wallet got listed; reporting a failure would make a retry add a twin
            isListedAnyway -> userWallet
            else -> raise(CreateHotWalletError.Failed(saveException))
        }
    }
}