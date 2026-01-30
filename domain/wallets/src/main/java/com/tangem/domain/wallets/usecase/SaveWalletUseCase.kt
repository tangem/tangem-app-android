package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.SaveWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.analytics.Settings
import com.tangem.domain.wallets.legacy.UserWalletsListError
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.repository.WalletsRepository

/**
 * Use case for saving user wallet
 *
[REDACTED_AUTHOR]
 */
class SaveWalletUseCase(
    private val userWalletsListManager: UserWalletsListManager,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val walletsRepository: WalletsRepository,
    private val useNewRepository: Boolean,
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        canOverride: Boolean = false,
        analyticsSource: AnalyticsParam.ScreensSources? = null,
    ): Either<SaveWalletError, Unit> {
        return if (useNewRepository) {
            either {
                val newUserWallet =
                    userWalletsListRepository.userWalletsSync().none { it.walletId == userWallet.walletId }
                val userWallet = userWalletsListRepository.saveWithoutLock(userWallet, canOverride)
                    .onRight { trackColdWalletAddedIfNeeded(analyticsSource, it) }
                    .bind()

                if (newUserWallet) {
                    when (userWallet) {
                        is UserWallet.Cold -> {
                            if (walletsRepository.useBiometricAuthentication()) {
                                userWalletsListRepository.setLock(
                                    userWallet.walletId,
                                    UserWalletsListRepository.LockMethod.Biometric,
                                )
                            } else {
                                Unit.right()
                            }
                        }
                        is UserWallet.Hot -> {
                            userWalletsListRepository.setLock(
                                userWallet.walletId,
                                UserWalletsListRepository.LockMethod.NoLock,
                            )
                        }
                    }.mapLeft {
                        SaveWalletError.DataError(null)
                    }.map {
                        userWalletsListRepository.select(userWallet.walletId)
                    }.bind()
                }
            }
        } else {
            either {
                userWalletsListManager.save(userWallet, canOverride)
                    .doOnSuccess { return Unit.right() }
                    .doOnFailure {
                        return when (it) {
                            is UserWalletsListError.WalletAlreadySaved -> SaveWalletError.WalletAlreadySaved(
                                it.messageResId,
                            )
                            else -> SaveWalletError.DataError(it.messageResId)
                        }.left()
                    }

                return Unit.right()
            }
        }
    }

    private suspend fun trackColdWalletAddedIfNeeded(source: AnalyticsParam.ScreensSources?, userWallet: UserWallet) {
        val hasHotWallet = userWalletsListRepository.userWalletsSync().any { it is UserWallet.Hot }
        if (hasHotWallet && userWallet is UserWallet.Cold) {
            analyticsEventHandler.send(event = Settings.ColdWalletAdded(source))
        }
    }
}