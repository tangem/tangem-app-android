package com.tangem.feature.referral.domain

import com.tangem.lib.crypto.DerivationManager
import com.tangem.lib.crypto.UserWalletManager
import com.tangem.lib.crypto.models.Token
import com.tangem.feature.referral.domain.converter.TokensConverter
import com.tangem.feature.referral.domain.models.ReferralData
import com.tangem.feature.referral.domain.models.TokenData

internal class ReferralInteractorImpl(
    private val repository: ReferralRepository,
    private val derivationManager: DerivationManager,
    private val userWalletManager: UserWalletManager,
    private val tokensConverter: TokensConverter,
) : ReferralInteractor {

    private val tokensForReferral = mutableListOf<TokenData>()

    override suspend fun getReferralStatus(): ReferralData {
        val refStatus = repository.getReferralStatus(userWalletManager.getWalletId())
        saveRefTokens(refStatus.tokens)
        return refStatus
    }

    override suspend fun startReferral(): ReferralData {
        if (tokensForReferral.isNotEmpty()) {
            val token = tokensConverter.convert(tokensForReferral.first())
            deriveOrAddTokens(token)
            val publicAddress = userWalletManager.getWalletAddress(token)
            return repository.startReferral(
                walletId = userWalletManager.getWalletId(),
                networkId = token.networkId ?: "",
                tokenId = token.id,
                address = publicAddress,
            )
        } else {
            throw IllegalStateException("tokens for ref is empty")
        }
    }

    private fun deriveOrAddTokens(token: Token) {
        if (!derivationManager.hasDerivation()) {
            derivationManager.deriveMissingBlockchains(token.networkId)
        }
        if (!userWalletManager.isTokenAdded(token)) {
            userWalletManager.addToken(token)
        }
    }

    private fun saveRefTokens(tokens: List<TokenData>) {
        tokensForReferral.clear()
        tokensForReferral.addAll(tokens)
    }
}