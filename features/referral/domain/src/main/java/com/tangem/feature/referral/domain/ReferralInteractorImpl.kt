package com.tangem.feature.referral.domain

import com.tangem.common.core.TangemSdkError
import com.tangem.domain.account.producer.SingleAccountProducer
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.account.derivationIndex
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.feature.referral.domain.errors.ReferralError
import com.tangem.feature.referral.domain.models.ReferralData
import com.tangem.feature.referral.domain.models.TokenData
import com.tangem.utils.logging.TangemLogger

internal class ReferralInteractorImpl(
    private val repository: ReferralRepository,
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

    override suspend fun startReferral(accountId: AccountId): ReferralData {
        if (tokensForReferral.isEmpty()) error("Tokens for ref is empty")

        val tokenData = tokensForReferral.first()
        val userWalletId = accountId.userWalletId

        val account = singleAccountSupplier.getSyncOrNull(
            params = SingleAccountProducer.Params(accountId = accountId),
        ) ?: error("Account not found: $accountId")

        val cryptoCurrency = getCryptoCurrency(
            userWalletId = accountId.userWalletId,
            tokenData = tokenData,
            accountIndex = account.derivationIndex,
        ) ?: error("Failed to create crypto currency")

        manageCryptoCurrenciesUseCase(
            accountId = accountId,
            add = cryptoCurrency,
            skipDerivationErrors = false,
        )
            .mapLeft { it.mapToDomainError() }
            .onLeft { error ->
                TangemLogger.e("Error", error)
                if (error is ReferralError.UserCancelledException) {
                    throw error
                }
            }

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