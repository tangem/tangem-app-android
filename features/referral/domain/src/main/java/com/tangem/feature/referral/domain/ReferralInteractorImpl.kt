package com.tangem.feature.referral.domain

import arrow.core.getOrElse
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.models.PortfolioId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.AddCryptoCurrenciesUseCase
import com.tangem.domain.wallets.usecase.DerivePublicKeysUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.feature.referral.domain.errors.ReferralError
import com.tangem.feature.referral.domain.models.ReferralData
import com.tangem.feature.referral.domain.models.TokenData
import com.tangem.lib.crypto.UserWalletManager
import timber.log.Timber

@Suppress("LongParameterList")
internal class ReferralInteractorImpl(
    private val repository: ReferralRepository,
    private val userWalletManager: UserWalletManager,
    private val derivePublicKeysUseCase: DerivePublicKeysUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val addCryptoCurrenciesUseCase: AddCryptoCurrenciesUseCase,
) : ReferralInteractor {

    private val tokensForReferral = mutableListOf<TokenData>()

    override suspend fun getReferralStatus(userWalletId: UserWalletId): ReferralData {
        val referralData = repository.getReferralData(userWalletId.stringValue)

        saveReferralTokens(referralData.tokens)

        return referralData
    }

    override suspend fun startReferral(portfolioId: PortfolioId): ReferralData {
        if (tokensForReferral.isEmpty()) error("Tokens for ref is empty")

        val tokenData = tokensForReferral.first()
        val userWalletId = portfolioId.userWalletId
        val userWallet = getUserWalletUseCase(userWalletId).getOrElse {
            error("Failed to get user wallet $userWalletId: $it")
        }

        val cryptoCurrency = repository.getCryptoCurrency(userWalletId = userWallet.walletId, tokenData = tokenData)
            ?: error("Failed to create crypto currency")

        // todo account different for account?
        derivePublicKeysUseCase(userWallet.walletId, listOfNotNull(cryptoCurrency)).getOrElse {
            Timber.e("Failed to derive public keys: $it")
            throw it.mapToDomainError()
        }

        when (portfolioId) {
            is PortfolioId.Account -> TODO("account")
            is PortfolioId.Wallet -> addCryptoCurrenciesUseCase(
                userWalletId = userWallet.walletId,
                currency = cryptoCurrency,
            )
        }

        val publicAddress = when (portfolioId) {
            is PortfolioId.Account -> TODO("account")
            is PortfolioId.Wallet -> userWalletManager.getWalletAddress(
                networkId = tokenData.networkId,
                derivationPath = cryptoCurrency.network.derivationPath.value,
            )
        }

        return repository.startReferral(
            walletId = userWalletManager.getWalletId(),
            networkId = tokenData.networkId,
            tokenId = tokenData.id,
            address = publicAddress,
        )
    }

    override suspend fun getCryptoCurrency(userWalletId: UserWalletId, tokenData: TokenData): CryptoCurrency? =
        repository.getCryptoCurrency(userWalletId = userWalletId, tokenData = tokenData)

    private fun saveReferralTokens(tokens: List<TokenData>) {
        tokensForReferral.clear()
        tokensForReferral.addAll(tokens)
    }

    private fun Throwable.mapToDomainError(): ReferralError {
        if (this !is TangemSdkError) return ReferralError.DataError(this)
        return if (this is TangemSdkError.UserCancelled) {
            ReferralError.UserCancelledException
        } else {
            ReferralError.SdkError
        }
    }
}