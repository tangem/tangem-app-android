package com.tangem.feature.referral.domain

import com.tangem.feature.referral.domain.converter.TokensConverter
import com.tangem.feature.referral.domain.models.ReferralData
import com.tangem.feature.referral.domain.models.TokenData
import com.tangem.lib.crypto.DerivationManager
import com.tangem.lib.crypto.UserWalletManager

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
        val referralData = repository.getReferralData(userWalletManager.getWalletId())

        saveReferralTokens(referralData.tokens)

        return referralData
    }

    override suspend fun startReferral(): ReferralData {
        if (tokensForReferral.isNotEmpty()) {
            val currency = tokensConverter.convert(tokensForReferral.first())
            val derivationPath = derivationManager.deriveAndAddTokens(currency)
            val publicAddress = userWalletManager.getWalletAddress(currency.networkId, derivationPath)
            return repository.startReferral(
                walletId = userWalletManager.getWalletId(),
                networkId = currency.networkId,
                tokenId = currency.id,
                address = publicAddress,
            )
        } else {
            error("Tokens for ref is empty")
        }
    }

    private fun saveReferralTokens(tokens: List<TokenData>) {
        tokensForReferral.clear()
        tokensForReferral.addAll(tokens)
    }
}
