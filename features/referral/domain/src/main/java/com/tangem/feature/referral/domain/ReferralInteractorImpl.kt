package com.tangem.feature.referral.domain

import arrow.core.getOrElse
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.account.producer.SingleAccountProducer
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.models.PortfolioId
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.AddCryptoCurrenciesUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.usecase.DerivePublicKeysUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.feature.referral.domain.errors.ReferralError
import com.tangem.feature.referral.domain.models.ReferralData
import com.tangem.feature.referral.domain.models.TokenData
import timber.log.Timber

@Suppress("LongParameterList")
internal class ReferralInteractorImpl(
    private val repository: ReferralRepository,
    private val derivePublicKeysUseCase: DerivePublicKeysUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val addCryptoCurrenciesUseCase: AddCryptoCurrenciesUseCase,
    private val manageCryptoCurrenciesUseCase: ManageCryptoCurrenciesUseCase,
    private val singleAccountSupplier: SingleAccountSupplier,
    private val walletManagersFacade: WalletManagersFacade,
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

        val accountIndex = when (portfolioId) {
            is PortfolioId.Account -> {
                val account = singleAccountSupplier.getSyncOrNull(
                    params = SingleAccountProducer.Params(accountId = portfolioId.accountId),
                )
                    ?: error("Account not found: ${portfolioId.accountId}")

                when (account) {
                    is Account.Crypto.Portfolio -> account.derivationIndex
                    is Account.Payment -> TODO("[REDACTED_JIRA]")
                }
            }
            is PortfolioId.Wallet -> null
        }

        val cryptoCurrency = getCryptoCurrency(
            userWalletId = portfolioId.userWalletId,
            tokenData = tokenData,
            accountIndex = accountIndex,
        )
            ?: error("Failed to create crypto currency")

        when (portfolioId) {
            is PortfolioId.Account -> {
                manageCryptoCurrenciesUseCase(accountId = portfolioId.accountId, add = cryptoCurrency)
            }
            is PortfolioId.Wallet -> {
                derivePublicKeysUseCase(userWallet.walletId, listOf(cryptoCurrency)).getOrElse { throwable ->
                    Timber.e("Failed to derive public keys: $throwable")
                    throw throwable.mapToDomainError()
                }

                addCryptoCurrenciesUseCase(
                    userWalletId = userWallet.walletId,
                    currency = cryptoCurrency,
                )
            }
        }
            .onLeft(Timber::e)

        val publicAddress = walletManagersFacade.getDefaultAddress(
            userWalletId = userWalletId,
            network = cryptoCurrency.network,
        )
            ?: error("Address not found: ${cryptoCurrency.network.id}")

        return repository.startReferral(
            walletId = userWalletId.stringValue,
            networkId = tokenData.networkId,
            tokenId = tokenData.id,
            address = publicAddress,
        )
    }

    override suspend fun getCryptoCurrency(
        userWalletId: UserWalletId,
        tokenData: TokenData,
        accountIndex: DerivationIndex?,
    ): CryptoCurrency? {
        return repository.getCryptoCurrency(
            userWalletId = userWalletId,
            tokenData = tokenData,
            accountIndex = accountIndex,
        )
    }

    private fun saveReferralTokens(tokens: List<TokenData>) {
        tokensForReferral.clear()
        tokensForReferral.addAll(tokens)
    }

    private fun Throwable.mapToDomainError(): ReferralError {
        if (this !is TangemSdkError) return ReferralError.DataError(this)
        return if (this is TangemSdkError.UserCancelled) {
            ReferralError.UserCancelledException()
        } else {
            ReferralError.SdkError()
        }
    }
}