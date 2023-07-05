package com.tangem.feature.learn2earn.domain

import android.net.Uri
import com.tangem.datasource.api.promotion.models.AbstractPromotionResponse
import com.tangem.datasource.api.promotion.models.PromotionInfoResponse
import com.tangem.feature.learn2earn.data.api.Learn2earnRepository
import com.tangem.feature.learn2earn.data.models.PromoUserData
import com.tangem.feature.learn2earn.data.toggles.Learn2earnFeatureToggleManager
import com.tangem.feature.learn2earn.domain.api.*
import com.tangem.feature.learn2earn.domain.models.Promotion
import com.tangem.feature.learn2earn.domain.models.PromotionError
import com.tangem.feature.learn2earn.domain.models.toDomainError
import com.tangem.lib.crypto.DerivationManager
import com.tangem.lib.crypto.UserWalletManager
import com.tangem.lib.crypto.models.Currency

/**
* [REDACTED_AUTHOR]
 */
class DefaultLearn2earnInteractor(
    private val featureToggleManager: Learn2earnFeatureToggleManager,
    private val repository: Learn2earnRepository,
    private val userWalletManager: UserWalletManager,
    private val derivationManager: DerivationManager,
    dependencyProvider: Learn2earnDependencyProvider,
) : Learn2earnInteractor {

    override var webViewResultHandler: WebViewResultHandler? = null

    private lateinit var promotion: Promotion

    private val webViewUriBuilder: WebViewUriBuilder = WebViewUriBuilder(
        authCredentialsProvider = dependencyProvider.getWebViewAuthCredentialsProvider(),
        userCountryCodeProvider = dependencyProvider.getUserCountryCodeProvider(),
        promoCodeProvider = { repository.getUserData().promoCode },
    )

    override suspend fun init() {
        initPromotionInfo()
    }

    override fun isUserHadPromoCode(): Boolean {
        return repository.getUserData().promoCode != null
    }

    override fun isNeedToShowViewOnStoriesScreen(): Boolean {
        return promotionIsActive()
    }

    override suspend fun isNeedToShowViewOnMainScreen(): Boolean {
        if (!promotionIsActive()) return false

        val promoCode = repository.getUserData().promoCode
        val userWalletId = userWalletManager.getWalletId()
        return if (promoCode == null) {
            val response = repository.validate(userWalletId)
            response.valid == true
        } else {
            val response = repository.validateCode(userWalletId, promoCode)
            when (val error = response.error?.toDomainError()) {
                null -> response.valid == true
                else -> error !is PromotionError.CodeWasNotAppliedInShop
            }
        }
    }

    override fun isUserRegisteredInPromotion(): Boolean {
        return repository.getUserData().isRegisteredInPromotion
    }

    override fun getAwardAmount(): Int {
        val promoCode = repository.getUserData().promoCode
        val awardAmount = promotion.getPromotionInfo().getData(promoCode).award.toInt()

        return awardAmount
    }

    override fun getAwardNetworkName(): String {
        val networkId = promotion.getPromotionInfo().awardPaymentToken.networkId
        return userWalletManager.getNativeTokenForNetwork(networkId).name
    }

    @Throws(IllegalArgumentException::class)
    override suspend fun requestAward(): Result<Unit> {
        val awardCurrency = getCurrencyForAward()
            ?: return Result.failure(PromotionError.UnknownError("Currency for award is null"))

        val walletId = userWalletManager.getWalletId()
        val promoCode = repository.getUserData().promoCode

        val error = if (promoCode == null) {
            requestAward(walletId, awardCurrency)
        } else {
            requestAwardWithPromoCode(walletId, awardCurrency, promoCode)
        }

        return if (error == null) {
            Result.success(Unit)
        } else {
            val domainError = error.toDomainError()
            handlePromotionError(domainError)
            Result.failure(domainError)
        }
    }

    private suspend fun requestAward(walletId: String, awardCurrency: Currency): AbstractPromotionResponse.Error? {
        val validateResponse = repository.validate(walletId)
        return if (validateResponse.valid == true) {
            val address = getWalletAddressForAward(awardCurrency)
            val awardResponse = repository.requestAward(walletId, address)
            if (awardResponse.status == true) {
                updateUserData { it.copy(isAlreadyReceivedAward = true) }
                null
            } else {
                awardResponse.error
            }
        } else {
            validateResponse.error
        }
    }

    private suspend fun requestAwardWithPromoCode(
        walletId: String,
        awardCurrency: Currency,
        promoCode: String,
    ): AbstractPromotionResponse.Error? {
        val codeValidateResponse = repository.validateCode(walletId, promoCode)
        return if (codeValidateResponse.valid == true) {
            val address = getWalletAddressForAward(awardCurrency)
            val awardWithCodeResponse = repository.requestAwardByCode(walletId, address, promoCode)
            if (awardWithCodeResponse.status == true) {
                updateUserData { it.copy(isAlreadyReceivedAward = true) }
                null
            } else {
                awardWithCodeResponse.error
            }
        } else {
            codeValidateResponse.error
        }
    }

    private suspend fun getWalletAddressForAward(currency: Currency): String {
        val derivationPath = deriveOrAddTokens(currency)
        return userWalletManager.getWalletAddress(currency.networkId, derivationPath)
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

    private fun handlePromotionError(error: PromotionError) {
        when (error) {
            is PromotionError.CodeNotFound -> {
                updateUserData { it.copy(promoCode = null) }
            }
            is PromotionError.CardAlreadyHasAward, is PromotionError.WalletAlreadyHasAward,
            is PromotionError.CodeWasAlreadyUsed,
            -> {
                updateUserData { it.copy(isAlreadyReceivedAward = true) }
            }
            else -> Unit
        }
    }

    override fun buildUriForNewUser(): Uri {
        return webViewUriBuilder.buildUriForNewUser()
    }

    override fun buildUriForOldUser(): Uri {
        return webViewUriBuilder.buildUriForOldUser()
    }

    override fun getBasicAuthHeaders(): ArrayList<String> {
        return HeadersConverter().convert(webViewUriBuilder.getBasicAuthHeaders())
    }

    override fun handleRedirect(uri: Uri): RedirectConsequences {
        if (webViewUriBuilder.isReadyForExistedCardAwardRedirect(uri)) {
            updateUserData { it.copy(isRegisteredInPromotion = true) }
            webViewResultHandler?.handleResult(WebViewResult.ReadyForAward)
            return RedirectConsequences.FINISH_SESSION
        }

        return if (webViewUriBuilder.isPromoCodeRedirect(uri)) {
            webViewUriBuilder.extractPromoCode(uri)?.let { code ->
                updateUserData {
                    it.copy(
                        promoCode = code,
                        isRegisteredInPromotion = true,
                    )
                }
                webViewResultHandler?.handleResult(WebViewResult.PromoCodeReceived)
            }
            RedirectConsequences.NOTHING
        } else {
            RedirectConsequences.PROCEED
        }
    }

    private fun promotionIsActive(): Boolean {
        val userData = repository.getUserData()
        val isActive = when {
            !featureToggleManager.isLearn2earnEnabled -> false
            userData.isAlreadyReceivedAward -> false
            promotion.isError() -> false
            else -> {
                val data = promotion.getPromotionInfo().getData(userData.promoCode)
                data.status == PromotionInfoResponse.Status.ACTIVE
            }
        }

        return isActive
    }

    private suspend fun initPromotionInfo() {
        promotion = if (featureToggleManager.isLearn2earnEnabled) {
            repository.getPromotionInfo()
                .fold(
                    onSuccess = { response ->
                        val responseError = response.error
                        if (responseError == null) {
                            val npeMessage = { "Shouldn't be null" }
                            Promotion(
                                info = Promotion.PromotionInfo(
                                    newCard = requireNotNull(response.newCard, npeMessage),
                                    oldCard = requireNotNull(response.oldCard, npeMessage),
                                    awardPaymentToken = requireNotNull(response.awardPaymentToken, npeMessage),
                                ),
                                error = null,
                            )
                        } else {
                            Promotion(
                                info = null,
                                error = responseError.toDomainError(),
                            )
                        }
                    },
                    onFailure = {
                        Promotion(
                            info = null,
                            error = PromotionError.NetworkUnreachable,
                        )
                    },
                )
        } else {
            Promotion(
                info = null,
                error = null,
            )
        }
    }

    private fun getCurrencyForAward(): Currency? {
        val token = promotion.info?.awardPaymentToken ?: return null

        return Currency.NonNativeToken(
            id = token.id,
            name = token.name,
            symbol = token.symbol,
            networkId = token.networkId,
            contractAddress = token.contractAddress,
            decimalCount = token.decimalCount,
        )
    }

    private fun updateUserData(updateBlock: (PromoUserData) -> PromoUserData): PromoUserData {
        return updateBlock(repository.getUserData()).apply {
            repository.updateUserData(this)
        }
    }
}

private fun Promotion.PromotionInfo.getData(promoCode: String?): PromotionInfoResponse.Data {
    return if (promoCode == null) {
        newCard
    } else {
        oldCard
    }
}
