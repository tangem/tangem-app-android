package com.tangem.feature.learn2earn.domain

import android.net.Uri
import com.tangem.core.analytics.api.AnalyticsEventHandler
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
[REDACTED_AUTHOR]
 */
internal class DefaultLearn2earnInteractor(
    private val featureToggleManager: Learn2earnFeatureToggleManager,
    private val repository: Learn2earnRepository,
    private val userWalletManager: UserWalletManager,
    private val derivationManager: DerivationManager,
    private val analyticsEventHandler: AnalyticsEventHandler,
    dependencyProvider: Learn2earnDependencyProvider,
) : Learn2earnInteractor {

    override var webViewResultHandler: WebViewResultHandler? = null

    private lateinit var promotion: Promotion

    private val webViewUriBuilder: WebViewUriBuilder by lazy {
        WebViewUriBuilder(
            authCredentialsProvider = dependencyProvider.getWebViewAuthCredentialsProvider(),
            localeLanguageProvider = dependencyProvider.getLocaleProvider(),
            promoCodeProvider = { repository.getUserData().promoCode },
        )
    }

    private val webViewUriParser: WebViewUriParser by lazy {
        WebViewUriParser { repository.getProgramName() }
    }

    override suspend fun init() {
        initPromotionInfo()
    }

    override fun isUserHadPromoCode(): Boolean {
        return repository.getUserData().promoCode != null
    }

    override suspend fun validateUserWallet(): PromotionError? {
        val promoCode = repository.getUserData().promoCode
        val userWalletId = userWalletManager.getWalletId()

        val domainError = if (promoCode == null) {
            repository.validate(userWalletId).error
        } else {
            repository.validateCode(userWalletId, promoCode).error
        }?.toDomainError()

        return domainError
            ?.also { handlePromotionError(it) }
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

        val domainError = if (promoCode == null) {
            requestAward(walletId, awardCurrency)
        } else {
            requestAwardWithPromoCode(walletId, awardCurrency, promoCode)
        }

        return if (domainError == null) {
            Result.success(Unit)
        } else {
            handlePromotionError(domainError)
            Result.failure(domainError)
        }
    }

    private suspend fun requestAward(walletId: String, awardCurrency: Currency): PromotionError? {
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
        }?.toDomainError()
    }

    private suspend fun requestAwardWithPromoCode(
        walletId: String,
        awardCurrency: Currency,
        promoCode: String,
    ): PromotionError? {
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
        }?.toDomainError()
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

    override fun handleRedirect(uri: Uri): WebViewAction {
        val result = webViewUriParser.parse(uri)
        when (result) {
            is WebViewResult.PromoCode -> {
                updateUserData {
                    it.copy(
                        promoCode = result.promoCode,
                        isRegisteredInPromotion = true,
                    )
                }
            }
            WebViewResult.ReadyForAward -> {
                updateUserData { it.copy(isRegisteredInPromotion = true) }
            }
            is WebViewResult.Learn2earnAnalyticsEvent -> {
                analyticsEventHandler.send(result.event)
            }
            WebViewResult.Empty -> Unit
        }
        webViewResultHandler?.handleResult(result)

        return result.toWebViewAction()
    }

    override fun isPromotionActive(): Boolean {
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

    private fun Promotion.PromotionInfo.getData(promoCode: String?): PromotionInfoResponse.Data {
        return if (promoCode == null) {
            oldCard
        } else {
            newCard
        }
    }

    private fun WebViewResult.toWebViewAction(): WebViewAction {
        return when (this) {
            WebViewResult.Empty -> WebViewAction.PROCEED
            WebViewResult.ReadyForAward -> WebViewAction.FINISH_SESSION
            is WebViewResult.Learn2earnAnalyticsEvent -> WebViewAction.NOTHING
            is WebViewResult.PromoCode -> WebViewAction.NOTHING
        }
    }
}