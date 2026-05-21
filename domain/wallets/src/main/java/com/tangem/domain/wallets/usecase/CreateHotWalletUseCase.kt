package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.builder.HotUserWalletBuilder
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.MnemonicType
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class CreateHotWalletUseCase @Inject constructor(
    private val tangemHotSdk: TangemHotSdk,
    private val hotUserWalletBuilderFactory: HotUserWalletBuilder.Factory,
    private val saveWalletUseCase: SaveWalletUseCase,
    private val syncWalletWithRemoteUseCase: SyncWalletWithRemoteUseCase,
    private val appCoroutineScope: AppCoroutineScope,
) {
    suspend operator fun invoke(auth: HotAuth, mnemonicType: MnemonicType): Either<Throwable, UserWallet.Hot> {
        return Either.catch {
            val hotWalletId = tangemHotSdk.generateWallet(auth, mnemonicType)
            val userWallet = hotUserWalletBuilderFactory.create(hotWalletId).build()

            saveWalletUseCase(userWallet)

            appCoroutineScope.launch {
                syncWalletWithRemoteUseCase(userWalletId = userWallet.walletId)
            }

            userWallet
        }
    }
}