package com.tangem.feature.referral.domain

import arrow.core.getOrElse
import com.tangem.domain.card.DerivePublicKeysUseCase
import com.tangem.domain.tokens.AddCryptoCurrenciesUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.referral.domain.converter.TokensConverter
import com.tangem.feature.referral.domain.models.ReferralData
import com.tangem.feature.referral.domain.models.TokenData
import com.tangem.features.tester.api.TesterFeatureToggles
import com.tangem.lib.crypto.DerivationManager
import com.tangem.lib.crypto.UserWalletManager
import timber.log.Timber

@Suppress("LongParameterList")
internal class ReferralInteractorImpl(
    private val repository: ReferralRepository,
    private val derivationManager: DerivationManager,
    private val userWalletManager: UserWalletManager,
    private val tokensConverter: TokensConverter,
    private val derivePublicKeysUseCase: DerivePublicKeysUseCase,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val addCryptoCurrenciesUseCase: AddCryptoCurrenciesUseCase,
    private val testerFeatureToggles: TesterFeatureToggles,
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
        return if (tokensForReferral.isNotEmpty()) {
            if (testerFeatureToggles.isDerivePublicKeysRefactoringEnabled) {
                startReferralNew(tokenData = tokensForReferral.first())
            } else {
                // TODO: delete [REDACTED_JIRA]
                startReferralLegacy(tokenData = tokensForReferral.first())
            }
        } else {
            error("Tokens for ref is empty")
        }
    }

    private suspend fun startReferralNew(tokenData: TokenData): ReferralData {
        val userWallet = getSelectedWalletSyncUseCase().getOrElse {
            error("Failed to get selected wallet: $it")
        }

        val cryptoCurrency = repository.getCryptoCurrency(userWalletId = userWallet.walletId, tokenData = tokenData)
        derivePublicKeysUseCase(userWallet.walletId, listOfNotNull(cryptoCurrency)).getOrElse {
            Timber.e("Failed to derive public keys: $it")
            throw it
        }

        addCryptoCurrenciesUseCase(
            userWalletId = userWallet.walletId,
            currencies = listOfNotNull(cryptoCurrency),
        )

        val publicAddress = userWalletManager.getWalletAddress(
            networkId = tokenData.networkId,
            derivationPath = cryptoCurrency?.network?.derivationPath?.value,
        )

        return repository.startReferral(
            walletId = userWalletManager.getWalletId(),
            networkId = tokenData.networkId,
            tokenId = tokenData.id,
            address = publicAddress,
        )
    }

    private suspend fun startReferralLegacy(tokenData: TokenData): ReferralData {
        val currency = tokensConverter.convert(tokenData)
        val derivationPath = derivationManager.deriveAndAddTokens(currency)
        val publicAddress = userWalletManager.getWalletAddress(currency.networkId, derivationPath)
        return repository.startReferral(
            walletId = userWalletManager.getWalletId(),
            networkId = currency.networkId,
            tokenId = currency.id,
            address = publicAddress,
        )
    }

    private fun saveReferralTokens(tokens: List<TokenData>) {
        tokensForReferral.clear()
        tokensForReferral.addAll(tokens)
    }
}