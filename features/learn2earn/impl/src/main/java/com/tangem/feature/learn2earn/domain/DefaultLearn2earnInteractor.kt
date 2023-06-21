package com.tangem.feature.learn2earn.domain

import android.net.Uri
import com.tangem.datasource.api.promotion.models.PromotionInfoResponse
import com.tangem.feature.learn2earn.data.api.Learn2earnRepository
import com.tangem.feature.learn2earn.domain.api.Learn2earnInteractor
import com.tangem.feature.learn2earn.domain.models.CardType
import com.tangem.feature.learn2earn.domain.models.Promotion
import com.tangem.feature.learn2earn.domain.models.PromotionError
import com.tangem.feature.learn2earn.domain.models.PromotionError.Companion.toDomainError
import com.tangem.feature.learn2earn.domain.models.RedirectConsequences
import com.tangem.lib.auth.BasicAuthProvider

/**
[REDACTED_AUTHOR]
 */
class DefaultLearn2earnInteractor(
    private val repository: Learn2earnRepository,
    basicAuthProvider: BasicAuthProvider,
    userCountryCodeProvider: () -> String,
) : Learn2earnInteractor {

    private val webViewUriBuilder = WebViewUriBuilder(
        basicAuthProvider = basicAuthProvider,
        userCountryCodeProvider = userCountryCodeProvider,
        promoCodeProvider = { repository.getPromoCode() },
        promoNameProvider = { repository.getProgramName() },
    )

    private lateinit var promotion: Promotion

    override suspend fun init() {
        initPromotionInfo()
    }

    override fun isNeedToShowViewOnStoriesScreen(): Boolean {
        return promotionIsActive()
    }

    override fun getBasicAuthHeaders(): Map<String, String> {
        return webViewUriBuilder.getBasicAuthHeaders()
    }

    override suspend fun isNeedToShowViewOnWalletScreen(walletId: String): Boolean {
        val response = repository.validate(walletId)

        return if (response.valid == false && response.isError()) {
            false
        } else {
            promotionIsActive()
        }
    }

    override fun buildUriForStories(): Uri {
        val type = if (repository.isHadActivatedCards()) {
            CardType.NEW
        } else {
            CardType.EXISTED
        }

        return webViewUriBuilder.buildUriForStories(type)
    }

    override fun buildUriForMainPage(walletId: String, cardId: String, cardPubKey: String): Uri {
        return webViewUriBuilder.buildUriForMainPage(walletId, cardId, cardPubKey)
    }

    override fun handleWebViewRedirect(uri: Uri): RedirectConsequences {
        if (webViewUriBuilder.isReadyForExistedCardAwardRedirect(uri)) {
            return RedirectConsequences.FINISH_SESSION
        }

        return if (webViewUriBuilder.isPromoCodeRedirect(uri)) {
            webViewUriBuilder.extractPromoCode(uri)?.let { repository.savePromoCode(it) }
            RedirectConsequences.NOTHING
        } else {
            RedirectConsequences.PROCEED
        }
    }

    private fun promotionIsActive(): Boolean {
        val isActive = when {
            repository.isAlreadyReceivedAward() -> false
            promotion.isError() -> false
            else -> promotion.getInfo().status == PromotionInfoResponse.Status.ACTIVE
        }

        return isActive
    }

    private suspend fun initPromotionInfo() {
        promotion = repository.getPromotionInfo()
            .fold(
                onSuccess = { response ->
                    val responseError = response.error
                    if (responseError == null) {
                        Promotion(
                            info = Promotion.PromotionInfo(
                                status = response.status!!,
                                awardForNewCard = response.awardForNewCard!!,
                                awardForOldCard = response.awardForOldCard!!,
                                awardPaymentToken = response.awardPaymentToken!!,
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
    }
}