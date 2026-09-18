package com.tangem.data.moneyaccount.repository

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.moneyaccount.repository.MoneyAccountActivationRepository
import com.tangem.domain.visa.model.MoneyAccountDepositCredentials
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultMoneyAccountActivationRepository @Inject constructor(
    private val authDataSource: TangemPayAuthDataSource,
    private val derivationsRepository: DerivationsRepository,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) : MoneyAccountActivationRepository {

    override suspend fun activateMoneyAccount(userWalletId: UserWalletId): MoneyAccountDepositCredentials {
        return withContext(dispatchers.io) {
            val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)
            val activationData = authDataSource.produceMoneyAccountData(userWallet)
                .fold(
                    ifLeft = { error("Can not activate money account: ${it.message}") },
                    ifRight = { it },
                )

            derivationsRepository.storeDerivedKeys(
                userWalletId = userWalletId,
                derivedKeys = activationData.derivedKeys,
            )

            MoneyAccountDepositCredentials(
                address = activationData.address,
                signature = activationData.signature,
            )
        }
    }
}