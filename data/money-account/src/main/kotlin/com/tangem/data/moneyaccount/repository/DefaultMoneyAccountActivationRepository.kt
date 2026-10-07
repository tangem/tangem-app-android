package com.tangem.data.moneyaccount.repository

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.model.MoneyAccountActivationError
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.moneyaccount.repository.MoneyAccountActivationRepository
import com.tangem.domain.visa.model.MoneyAccountDepositCredentials
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultMoneyAccountActivationRepository @Inject constructor(
    private val authDataSource: TangemPayAuthDataSource,
    private val derivationsRepository: DerivationsRepository,
    private val onboardingRepository: OnboardingRepository,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) : MoneyAccountActivationRepository {

    override suspend fun activateMoneyAccount(
        userWalletId: UserWalletId,
    ): Either<MoneyAccountActivationError, MoneyAccountDepositCredentials> = withContext(dispatchers.io) {
        either {
            val userWallet = runSuspendCatching { userWalletsListRepository.getSyncStrict(userWalletId) }
                .getOrElse { raise(it.toActivationError()) }
            val activationData = authDataSource.produceMoneyAccountData(userWallet)
                .mapLeft { it.toActivationError() }
                .bind()

            runSuspendCatching {
                onboardingRepository.storeInitialCredentials(
                    userWalletId = userWalletId,
                    initialCredentials = activationData.initialCredentials,
                )
                derivationsRepository.storeDerivedKeys(
                    userWalletId = userWalletId,
                    derivedKeys = activationData.derivedKeys,
                )
            }.getOrElse { raise(it.toActivationError()) }

            MoneyAccountDepositCredentials(
                address = activationData.address,
                signature = activationData.signature,
            )
        }
    }

    private fun Throwable.toActivationError(): MoneyAccountActivationError = when (this) {
        is TangemSdkError.UserCancelled -> MoneyAccountActivationError.UserCancelled
        else -> MoneyAccountActivationError.Unknown
    }
}