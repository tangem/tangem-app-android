package com.tangem.data.virtualaccount.repository

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.virtualaccount.repository.VirtualAccountActivationRepository
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultVirtualAccountActivationRepository @Inject constructor(
    private val authDataSource: TangemPayAuthDataSource,
    private val derivationsRepository: DerivationsRepository,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) : VirtualAccountActivationRepository {

    override suspend fun activateVirtualAccount(userWalletId: UserWalletId) {
        withContext(dispatchers.io) {
            val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)
            val activationData = authDataSource.produceVirtualAccountData(userWallet)
                .fold(
                    ifLeft = { error("Can not activate virtual account: ${it.message}") },
                    ifRight = { it },
                )

            // Persist the derived VA key so the on-chain balance can be read without re-deriving (no extra tap).
            derivationsRepository.storeDerivedKeys(
                userWalletId = userWalletId,
                derivedKeys = activationData.derivedKeys,
            )

            // TODO([REDACTED_TASK_KEY]): register activationData.address with the VA backend once the endpoint is available.
        }
    }
}