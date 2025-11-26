package com.tangem.features.createwalletselection

import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.components.label.entity.LabelStyle
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.card.analytics.IntroductionProcess
import com.tangem.domain.card.analytics.Shop
import com.tangem.domain.wallets.usecase.GenerateBuyTangemCardLinkUseCase
import com.tangem.features.createwalletselection.entity.CreateWalletSelectionUM
import com.tangem.features.createwalletselection.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class CreateWalletSelectionModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val generateBuyTangemCardLinkUseCase: GenerateBuyTangemCardLinkUseCase,
    private val urlOpener: UrlOpener,
) : Model() {

    internal val uiState: StateFlow<CreateWalletSelectionUM>
        field = MutableStateFlow(
            CreateWalletSelectionUM(
                onBackClick = { router.pop() },
                blocks = persistentListOf(
                    CreateWalletSelectionUM.Block(
                        title = resourceReference(R.string.wallet_create_hardware_title),
                        titleLabel = LabelUM(
                            text = resourceReference(R.string.common_recommended),
                            style = LabelStyle.ACCENT,
                        ),
                        description = resourceReference(R.string.wallet_add_hardware_description),
                        features = persistentListOf(
                            CreateWalletSelectionUM.Feature(
                                iconResId = R.drawable.ic_add_wallet_16,
                                title = resourceReference(R.string.wallet_add_hardware_info_create),
                            ),
                            CreateWalletSelectionUM.Feature(
                                iconResId = R.drawable.ic_import_seed_16,
                                title = resourceReference(R.string.wallet_add_import_seed_phrase),
                            ),
                        ),
                        onClick = ::onHardwareWalletClick,
                    ),
                    CreateWalletSelectionUM.Block(
                        title = resourceReference(R.string.wallet_create_mobile_title),
                        titleLabel = null,
                        description = resourceReference(R.string.wallet_add_mobile_description),
                        features = persistentListOf(
                            CreateWalletSelectionUM.Feature(
                                iconResId = R.drawable.ic_mobile_wallet_16,
                                title = resourceReference(R.string.hw_create_title),
                            ),
                            CreateWalletSelectionUM.Feature(
                                iconResId = R.drawable.ic_import_seed_16,
                                title = resourceReference(R.string.wallet_add_import_seed_phrase),
                            ),
                        ),
                        onClick = ::onMobileWalletClick,
                    ),
                ),
                onBuyClick = ::onBuyClick,
                showAlreadyHaveWallet = true,
            ),
        )

    init {
        // temporarily disabled by timer and enabled by default
        // showAlreadyHaveWalletWithDelay()
    }

    @Suppress("UnusedPrivateMember")
    private fun showAlreadyHaveWalletWithDelay() {
        modelScope.launch {
            delay(SHOW_ALREADY_HAVE_WALLET_DELAY)
            uiState.update { it.copy(showAlreadyHaveWallet = true) }
        }
    }

    private fun onMobileWalletClick() {
        router.push(AppRoute.CreateMobileWallet)
    }

    private fun onHardwareWalletClick() {
        router.push(AppRoute.CreateHardwareWallet)
    }

    private fun onBuyClick() {
        analyticsEventHandler.send(IntroductionProcess.ButtonBuyCards)
        analyticsEventHandler.send(Shop.ScreenOpened)
        modelScope.launch {
            generateBuyTangemCardLinkUseCase.invoke().let { urlOpener.openUrl(it) }
        }
    }

    companion object {
        private const val SHOW_ALREADY_HAVE_WALLET_DELAY = 3000L
    }
}