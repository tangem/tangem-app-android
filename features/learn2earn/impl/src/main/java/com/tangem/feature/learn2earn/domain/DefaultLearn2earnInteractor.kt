package com.tangem.feature.learn2earn.domain

import android.net.Uri
import com.tangem.common.Provider
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.common.locale.LocaleProvider
import com.tangem.datasource.api.promotion.models.PromotionInfoResponse
import com.tangem.datasource.demo.DemoModeDatasource
import com.tangem.feature.learn2earn.analytics.AnalyticsParam
import com.tangem.feature.learn2earn.analytics.Learn2earnEvents.*
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
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
import com.tangem.utils.coroutines.FeatureCoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList", "LargeClass")
internal class DefaultLearn2earnInteractor(
    private val featureToggleManager: Learn2earnFeatureToggleManager,
    private val repository: Learn2earnRepository,
    private val userWalletManager: UserWalletManager,
    private val derivationManager: DerivationManager,
    private val localeProvider: LocaleProvider,
    private val analytics: AnalyticsEventHandler,
    private val demoModeDatasource: DemoModeDatasource,
    private val dependencyProvider: Learn2earnDependencyProvider,
    dispatchers: AppCoroutineDispatcherProvider,
) : Learn2earnInteractor {

    override var webViewResultHandler: WebViewResultHandler? = null

    private val promoUserData: PromoUserData
        get() = repository.getUserData()

    private lateinit var promotion: Promotion

    private val scope = CoroutineScope(Job() + dispatchers.io + FeatureCoroutineExceptionHandler.create("mainScope"))

    private val isTangemWalletFlow = MutableStateFlow(false)
    private var isTangemWalletFlowJob: Job? = null
    private var isTangemWalletSync = false

    private val webViewUriBuilder: WebViewUriBuilder by lazy {
        WebViewUriBuilder(
            authCredentialsProvider = dependencyProvider.getWebViewAuthCredentialsProvider(),
            localeProvider = localeProvider,
            promoCodeProvider = Provider { promoUserData.promoCode },
        )
    }

    private val webViewUriParser: WebViewUriParser by lazy {
        WebViewUriParser { repository.getProgramName() }
    }

    override suspend fun init() {
        initPromotionInfo()
    }

    override fun getIsTangemWalletFlow(): Flow<Boolean> = isTangemWalletFlow

    override fun isUserHadPromoCode(): Boolean {
        return promoUserData.promoCode != null
    }

    override suspend fun validateUserWallet(): Result<Unit> {
        val promoCode = promoUserData.promoCode
        val userWalletId = userWalletManager.getWalletId()
        if (userWalletId.isEmpty()) return Result.failure(PromotionError.EmptyUserWalletId)

        val domainError = if (promoCode == null) {
            repository.validate(userWalletId).error
        } else {
            repository.validateCode(userWalletId, promoCode).error
        }?.toDomainError()

        return if (domainError == null) {
            Result.success(Unit)
        } else {
            handlePromotionError(domainError)
            Result.failure(domainError)
        }
    }

    override fun isUserRegisteredInPromotion(): Boolean {
        return promoUserData.isRegisteredInPromotion
    }

    override fun getAwardAmount(): Int = try {
        promotion.getPromotionInfo().getCardData().award.toInt()
    } catch (ex: NullPointerException) {
        Timber.w(ex)
        0
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
        val promoCode = promoUserData.promoCode

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
        return webViewUriBuilder.buildUriForNewUser(promoUserData.isLearningStageFinished)
    }

    override fun buildUriForOldUser(): Uri {
        return webViewUriBuilder.buildUriForOldUser(promoUserData.isLearningStageFinished)
    }

    override fun getBasicAuthHeaders(): ArrayList<String> {
        return HeadersConverter().convert(webViewUriBuilder.getBasicAuthHeaders())
    }

    override fun handleRedirect(uri: Uri): WebViewAction {
        val result = webViewUriParser.parse(uri)
        when (result) {
            is WebViewResult.NewUserLearningFinished -> {
                analytics.send(PromoScreen.SuccessScreenOpened(AnalyticsParam.ClientType.New()))
                updateUserData {
                    it.copy(
                        promoCode = result.promoCode,
                        isRegisteredInPromotion = true,
                        isLearningStageFinished = true,
                    )
                }
            }
            is WebViewResult.OldUserLearningFinished -> {
                analytics.send(PromoScreen.SuccessScreenOpened(AnalyticsParam.ClientType.Old()))
                updateUserData {
                    it.copy(
                        promoCode = null,
                        isLearningStageFinished = true,
                    )
                }
            }
            WebViewResult.ReadyForAward -> {
                updateUserData { it.copy(isRegisteredInPromotion = true) }
            }
            is WebViewResult.Learn2earnAnalyticsEvent -> {
                analytics.send(result.event)
            }
            WebViewResult.Empty -> Unit
        }
        webViewResultHandler?.handleResult(result)

        return result.toWebViewAction()
    }

    override fun isPromotionActive(): Boolean {
        subscribeToCardTypeResolverFlow()

        val isActive = when {
            demoModeDatasource.isDemoModeActive -> false
            !featureToggleManager.isLearn2earnEnabled -> false
            promoUserData.isAlreadyReceivedAward -> false
            else -> promotion.isReadyToUse()
        }

        return isActive
    }

    override fun isPromotionActiveOnStories(): Boolean {
        return if (isPromotionActive()) {
            promotion.getPromotionInfo().newCard.status == PromotionInfoResponse.Status.ACTIVE
        } else {
            false
        }
    }

    override fun isPromotionActiveOnMain(): Boolean {
        if (!isPromotionActive() || !isTangemWalletSync) return false

        val promotionInfo = promotion.getPromotionInfo()
        return if (isUseLogicForOldCards()) {
            promotionInfo.oldCard.status == PromotionInfoResponse.Status.ACTIVE
        } else {
            when (promotionInfo.newCard.status) {
                PromotionInfoResponse.Status.PENDING -> !promoUserData.isAlreadyReceivedAward
                PromotionInfoResponse.Status.ACTIVE -> true
                // disable promotion for a new user only if they have received an award
                PromotionInfoResponse.Status.FINISHED -> !promoUserData.isAlreadyReceivedAward
            }
        }
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

    private fun subscribeToCardTypeResolverFlow() {
        if (isTangemWalletFlowJob == null || isTangemWalletFlowJob?.isCancelled == true) {
            isTangemWalletFlowJob = dependencyProvider.getCardTypeResolverFlow()
                .onEach {
                    val isTangemWallet = it?.isTangemWallet() ?: false
                    isTangemWalletSync = isTangemWallet
                    isTangemWalletFlow.emit(isTangemWallet)
                }
                .launchIn(scope)
        }
    }

    // region Utils
    private fun updateUserData(updateBlock: (PromoUserData) -> PromoUserData): PromoUserData {
        return updateBlock(promoUserData).apply {
            repository.updateUserData(this)
        }
    }

    private fun isUseLogicForOldCards(): Boolean {
        return promoUserData.promoCode == null
    }

    private fun Promotion.isReadyToUse(): Boolean {
        return info != null
    }

    private fun Promotion.PromotionInfo.getCardData(): PromotionInfoResponse.Data {
        return if (isUseLogicForOldCards()) {
            oldCard
        } else {
            newCard
        }
    }

    private fun WebViewResult.toWebViewAction(): WebViewAction {
        return when (this) {
            is WebViewResult.NewUserLearningFinished,
            WebViewResult.OldUserLearningFinished,
            is WebViewResult.Learn2earnAnalyticsEvent,
            -> WebViewAction.NOTHING
            WebViewResult.ReadyForAward -> WebViewAction.FINISH_SESSION
            WebViewResult.Empty -> WebViewAction.PROCEED
        }
    }
    // endregion Utils
}