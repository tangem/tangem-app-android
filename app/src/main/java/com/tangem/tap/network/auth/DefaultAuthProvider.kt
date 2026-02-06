package com.tangem.tap.network.auth

import com.tangem.common.extensions.toHexString
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.utils.Provider
import com.tangem.utils.ProviderSuspend

internal class DefaultAuthProvider(
    private val userWalletsListManager: UserWalletsListManager,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val shouldUseNewListRepository: Boolean = false,
    private val environmentConfigStorage: EnvironmentConfigStorage,
) : AuthProvider {

    override suspend fun getCardPublicKey(): String {
        val userWallet = getSelectedWallet()

        if (userWallet !is UserWallet.Cold) {
            return ""
        }

        return userWallet.scanResponse.card.cardPublicKey.toHexString()
    }

    override suspend fun getCardId(): String {
        val userWallet = getSelectedWallet()

        if (userWallet !is UserWallet.Cold) {
            return ""
        }

        return userWallet.scanResponse.card.cardId
    }

    override suspend fun getCardsPublicKeys(): Map<String, String> {
        return getWallets().filterIsInstance<UserWallet.Cold>().associate {
            it.scanResponse.card.cardId to it.scanResponse.card.cardPublicKey.toHexString()
        }
    }

    override fun getApiKey(apiEnvironment: Provider<ApiEnvironment>): ProviderSuspend<String> {
        return ProviderSuspend {
            when (apiEnvironment.invoke()) {
                ApiEnvironment.MOCK,
                ApiEnvironment.DEV,
                ApiEnvironment.DEV_2,
                ApiEnvironment.DEV_3,
                -> environmentConfigStorage.getConfigSync().tangemApiKeyDev
                ApiEnvironment.STAGE_2,
                ApiEnvironment.STAGE,
                -> environmentConfigStorage.getConfigSync().tangemApiKeyStage
                ApiEnvironment.PROD -> environmentConfigStorage.getConfigSync().tangemApiKey
            } ?: error("No tangem tech api config provided")
        }
    }

    override fun getGaslessServiceApiKey(apiEnvironment: Provider<ApiEnvironment>): ProviderSuspend<String> {
        return ProviderSuspend {
            when (apiEnvironment.invoke()) {
                ApiEnvironment.DEV,
                -> environmentConfigStorage.getConfigSync().gaslessTxApiKeyDev
                ApiEnvironment.PROD -> environmentConfigStorage.getConfigSync().gaslessTxApiKey
                else -> error("No gasless tx api config provided for ${apiEnvironment.invoke()}")
            } ?: error("No gasless tx api config provided")
        }
    }

    private suspend fun getWallets(): List<UserWallet> {
        return if (shouldUseNewListRepository) {
            userWalletsListRepository.userWalletsSync()
        } else {
            userWalletsListManager.userWalletsSync
        }
    }

    private suspend fun getSelectedWallet(): UserWallet? {
        return if (shouldUseNewListRepository) {
            userWalletsListRepository.selectedUserWalletSync()
        } else {
            userWalletsListManager.selectedUserWalletSync
        }
    }
}