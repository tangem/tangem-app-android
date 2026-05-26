package com.tangem.data.account.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.data.account.converter.CryptoPortfolioConverter
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import javax.inject.Inject

/**
 * Factory to create default [GetWalletAccountsResponse].
 *
 * @property userWalletsListRepository repository to get user wallets
 * @property cryptoPortfolioCF         converter factory to convert crypto portfolio accounts
 * @property userTokensResponseFactory factory to create [UserTokensResponse]
 * @property networkFactory            factory to create network derivation path
 *
[REDACTED_AUTHOR]
 */
internal class DefaultWalletAccountsResponseFactory @Inject constructor(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val cryptoPortfolioCF: CryptoPortfolioConverter.Factory,
    private val userTokensResponseFactory: UserTokensResponseFactory,
    private val networkFactory: NetworkFactory,
    private val featureTogglesManager: FeatureTogglesManager,
) {

    fun create(userWalletId: UserWalletId, userTokensResponse: UserTokensResponse?): GetWalletAccountsResponse {
        val userWallet = userWalletsListRepository.getSyncOrNull(userWalletId)

        val accountDTOs = userWallet?.let(::createDefaultAccountDTOs).orEmpty()
        val response = userTokensResponse.orDefault(userWallet = userWallet)

        return GetWalletAccountsResponse(
            wallet = GetWalletAccountsResponse.Wallet(
                group = response.group,
                sort = response.sort,
                totalAccounts = accountDTOs.size,
                totalArchivedAccounts = 0,
            ),
            accounts = accountDTOs.assignTokens(userWalletId = userWalletId, tokens = response.tokens),
            unassignedTokens = emptyList(),
        )
    }

    private fun createDefaultAccountDTOs(userWallet: UserWallet): List<WalletAccountDTO> {
        val accounts = AccountList.empty(userWallet.walletId).accounts
            .filterIsInstance<Account.CryptoPortfolio>()

        val converter = cryptoPortfolioCF.create(userWallet = userWallet)

        return converter.convertListBack(input = accounts)
    }

    private fun UserTokensResponse?.orDefault(userWallet: UserWallet?): UserTokensResponse {
        if (this != null) return this

        return userTokensResponseFactory.createDefaultResponse(
            userWallet = userWallet,
            networkFactory = networkFactory,
            accountId = userWallet?.let {
                AccountId.forCryptoPortfolio(userWalletId = it.walletId, derivationIndex = DerivationIndex.Main)
            },
            extraBlockchains = userWallet?.extraDefaultBlockchains().orEmpty(),
        )
    }

    private fun UserWallet.extraDefaultBlockchains(): List<Blockchain> {
        val batchId = (this as? UserWallet.Cold)?.scanResponse?.card?.batchId ?: return emptyList()
        return when {
            batchId == ADI_PROMO_BATCH_ID &&
                featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_15402_ADI_MAIN_SCREEN_DEFAULT_ENABLED) ->
                listOf(Blockchain.Adi)
            else -> emptyList()
        }
    }

    private companion object {
        const val ADI_PROMO_BATCH_ID = "BB000053"
    }
}