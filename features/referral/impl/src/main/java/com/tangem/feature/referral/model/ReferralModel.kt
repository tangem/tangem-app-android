package com.tangem.feature.referral.model

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.feature.referral.analytics.ReferralEvents
import com.tangem.feature.referral.api.ReferralComponent
import com.tangem.feature.referral.domain.ReferralInteractor
import com.tangem.feature.referral.domain.errors.ReferralError
import com.tangem.feature.referral.domain.models.DiscountType
import com.tangem.feature.referral.domain.models.ReferralData
import com.tangem.feature.referral.domain.models.ReferralInfo
import com.tangem.feature.referral.models.DemoModeException
import com.tangem.feature.referral.models.ReferralStateHolder
import com.tangem.feature.referral.models.ReferralStateHolder.ErrorSnackbar
import com.tangem.feature.referral.models.ReferralStateHolder.ReferralInfoState
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class ReferralModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val referralInteractor: ReferralInteractor,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val shareManager: ShareManager,
    private val urlOpener: UrlOpener,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val appRouter: AppRouter,
) : Model() {

    private val params = paramsContainer.require<ReferralComponent.Params>()

    private var lastReferralData: ReferralData? = null

    var uiState: ReferralStateHolder by mutableStateOf(createInitiallyUiState())
        private set

    init {
        analyticsEventHandler.send(ReferralEvents.ReferralScreenOpened)
        loadReferralData()
    }

    private fun createInitiallyUiState() = ReferralStateHolder(
        headerState = ReferralStateHolder.HeaderState(
            onBackClicked = appRouter::pop,
        ),
        referralInfoState = ReferralInfoState.Loading,
        errorSnackbar = null,
        analytics = ReferralStateHolder.Analytics(
            onAgreementClicked = ::onAgreementClicked,
            onCopyClicked = ::onCopyClicked,
            onShareClicked = ::onShareClicked,
        ),
    )

    private fun loadReferralData() {
        uiState = uiState.copy(referralInfoState = ReferralInfoState.Loading)
        modelScope.launch {
            runCatching {
                referralInteractor.getReferralStatus(params.userWalletId).apply {
                    lastReferralData = this
                }
            }
                .onSuccess(::showContent)
                .onFailure(::showErrorSnackbar)
        }
    }

    private fun participate() {
        val userWallet = getUserWalletUseCase(params.userWalletId).getOrNull() ?: error("User wallet not found")

        if (isDemoCardUseCase(cardId = userWallet.cardId)) {
            showErrorSnackbar(DemoModeException())
        } else {
            analyticsEventHandler.send(ReferralEvents.ClickParticipate)
            uiState = uiState.copy(referralInfoState = ReferralInfoState.Loading)
            modelScope.launch {
                runCatching { referralInteractor.startReferral(params.userWalletId) }
                    .onSuccess(::showContent)
                    .onFailure { throwable ->
                        if (throwable is ReferralError.UserCancelledException) {
                            lastReferralData?.let { referralData ->
                                showContent(referralData)
                            }
                        } else {
                            showErrorSnackbar(throwable)
                        }
                    }
            }
        }
    }

    private fun showContent(referralData: ReferralData) {
        uiState = uiState.copy(referralInfoState = referralData.convertToReferralInfoState())
    }

    private fun showErrorSnackbar(throwable: Throwable) {
        uiState = uiState.copy(
            errorSnackbar = ErrorSnackbar(throwable = throwable, onOkClicked = appRouter::pop),
        )
    }

    private fun onAgreementClicked() {
        analyticsEventHandler.send(ReferralEvents.ClickTaC)

        lastReferralData?.tosLink?.let(urlOpener::openUrl)
    }

    private fun onCopyClicked() {
        analyticsEventHandler.send(ReferralEvents.ClickCopy)
    }

    private fun onShareClicked(text: String) {
        analyticsEventHandler.send(ReferralEvents.ClickShare)

        shareManager.shareText(text = text)
    }

    private fun ReferralData.convertToReferralInfoState(): ReferralInfoState = when (this) {
        is ReferralData.ParticipantData -> ReferralInfoState.ParticipantContent(
            award = getAwardValue(),
            networkName = getNetworkName(),
            address = referral.getAddressValue(),
            discount = getDiscountValue(),
            purchasedWalletCount = referral.walletsPurchased,
            code = referral.promocode,
            shareLink = referral.shareLink,
            url = tosLink,
            expectedAwards = expectedAwards,
        )
        is ReferralData.NonParticipantData -> ReferralInfoState.NonParticipantContent(
            award = getAwardValue(),
            networkName = getNetworkName(),
            discount = getDiscountValue(),
            url = tosLink,
            onParticipateClicked = ::participate,
        )
    }

    private fun ReferralData.getAwardValue(): String = "$award ${getToken().symbol}"

    private fun ReferralData.getNetworkName(): String = getToken().networkId.replaceFirstChar(Char::uppercase)

    @Suppress("MagicNumber")
    private fun ReferralInfo.getAddressValue(): String {
        check(address.length > 5) { "Invalid address" }
        return address.substring(startIndex = 0, endIndex = 4) + "..." +
            address.substring(startIndex = address.length - 5, endIndex = address.length)
    }

    private fun ReferralData.getDiscountValue(): String {
        val discountSymbol = when (discountType) {
            DiscountType.PERCENTAGE -> "%"
            DiscountType.VALUE -> this.getToken().symbol
        }
        return "$discount$discountSymbol"
    }

    private fun ReferralData.getToken() = requireNotNull(tokens.firstOrNull()) { "Token list is empty" }
}