package com.tangem.feature.referral.domain

import com.tangem.feature.referral.domain.converter.TokensConverter
import com.tangem.feature.referral.domain.models.ReferralData
import com.tangem.feature.referral.domain.models.TokenData
import com.tangem.lib.crypto.DerivationManager
import com.tangem.lib.crypto.UserWalletManager
import com.tangem.lib.crypto.models.Currency

internal class ReferralInteractorImpl(
    private val repository: ReferralRepository,
    private val derivationManager: DerivationManager,
    private val userWalletManager: UserWalletManager,
    private val tokensConverter: TokensConverter,
) : ReferralInteractor {

    private val tokensForReferral = mutableListOf<TokenData>()

    override val isDemoMode: Boolean
        get() = repository.isDemoMode

    override suspend fun getReferralStatus(): ReferralData {
        val refStatus = repository.getReferralStatus(userWalletManager.getWalletId())
        saveRefTokens(refStatus.tokens)
        return refStatus
    }

    override suspend fun startReferral(): ReferralData {
        if (tokensForReferral.isNotEmpty()) {
            val currency = tokensConverter.convert(tokensForReferral.first())
            val derivationPath = deriveOrAddTokens(currency)
            val publicAddress = userWalletManager.getWalletAddress(currency.networkId, derivationPath)
            return repository.startReferral(
                walletId = userWalletManager.getWalletId(),
                networkId = currency.networkId,
                tokenId = currency.id,
                address = publicAddress,
            )
        } else {
            error("tokens for ref is empty")
        }
    }

    private suspend fun deriveOrAddTokens(currency: Currency): String {
        val derivationPath = derivationManager.getDerivationPathForBlockchain(currency.networkId)
        if (derivationPath.isNullOrEmpty()) error("derivationPath shouldn't be empty")
        if (!derivationManager.hasDerivation(currency.networkId, derivationPath)) {
            derivationManager.deriveMissingBlockchains(currency)
        }
        if (!userWalletManager.isTokenAdded(currency, derivationPath)) {
            userWalletManager.addToken(currency, derivationPath)
        }
        return derivationPath
    }

    private fun saveRefTokens(tokens: List<TokenData>) {
        tokensForReferral.clear()
        tokensForReferral.addAll(tokens)
    }
}
