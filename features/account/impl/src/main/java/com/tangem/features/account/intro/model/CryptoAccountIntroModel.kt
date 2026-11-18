package com.tangem.features.account.intro.model

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.common.routing.AppRoute
import com.tangem.common.ui.account.accountLimitReachedDialog
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_right_left_20
import com.tangem.core.ui.res.generated.icons.ic_shield_checkmark_20
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.account.intro.ui.state.CryptoAccountIntroUM
import com.tangem.features.commonfeatures.api.portfolioselector.AccountWalletPicker
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The crypto account intro. Continue settles the wallet: straight away when only one wallet has room for
 * another account, through the wallet chooser when several do, and with the limit dialog when none does.
 * The intro then gives its place to account creation, so finishing or closing it never lands back here.
 */
@Stable
@ModelScoped
internal class CryptoAccountIntroModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val messageSender: UiMessageSender,
    accountWalletPickerFactory: AccountWalletPicker.Factory,
) : Model() {

    val uiState: StateFlow<CryptoAccountIntroUM>
        field = MutableStateFlow(createState())

    val walletPicker: AccountWalletPicker = accountWalletPickerFactory.create(scope = modelScope)

    init {
        walletPicker.picks
            .onEach { pick ->
                when (pick) {
                    is AccountWalletPicker.Pick.Wallet -> openCreation(pick.walletId)
                    AccountWalletPicker.Pick.Full -> showLimitReached()
                }
            }
            .launchIn(modelScope)
    }

    // TODO: subtitle and points are stub copy until their keys are added to Lokalise
    private fun createState(): CryptoAccountIntroUM = CryptoAccountIntroUM(
        title = resourceReference(R.string.common_crypto_account),
        subtitle = stringReference("Organize your crypto into separate accounts within one wallet"),
        points = persistentListOf(
            point(
                id = "addresses",
                icon = Icons.ic_shield_checkmark_20,
                lead = "New addresses.",
                description = "Get a fresh set for savings, trading, or payments.",
            ),
            point(
                id = "balances",
                icon = Icons.ic_arrow_right_left_20,
                lead = "Separate balances.",
                description = "Move funds between your accounts anytime.",
            ),
            point(
                id = "backup",
                icon = Icons.ic_shield_checkmark_20,
                lead = "One backup.",
                description = "Your wallet's backup covers every account.",
            ),
        ),
        onContinueClick = ::onContinueClick,
        onCloseClick = router::pop,
    )

    private fun point(id: String, icon: ImageVector, lead: String, description: String) = CryptoAccountIntroUM.PointUM(
        id = id,
        icon = TangemIconUM.Icon(imageVector = icon, tintReference = { TangemTheme.colors3.icon.primary }),
        lead = stringReference(lead),
        description = stringReference(description),
    )

    private fun onContinueClick() {
        if (uiState.value.isContinueLoading) return
        modelScope.launch {
            uiState.update { it.copy(isContinueLoading = true) }
            val resolution = walletPicker.resolve(hasRoom = AccountList::canAddMoreCryptoAccounts)
            uiState.update { it.copy(isContinueLoading = false) }

            when (resolution) {
                AccountWalletPicker.Resolution.Unavailable -> messageSender.send(
                    DialogMessage(
                        title = resourceReference(R.string.common_something_went_wrong),
                        message = resourceReference(R.string.common_unknown_error),
                    ),
                )
                AccountWalletPicker.Resolution.LimitReached -> showLimitReached()
                is AccountWalletPicker.Resolution.Single -> openCreation(resolution.walletId)
                is AccountWalletPicker.Resolution.Choose -> walletPicker.openChooser(resolution.fullWalletIds)
            }
        }
    }

    private fun showLimitReached() {
        messageSender.send(accountLimitReachedDialog(limit = AccountList.MAX_CRYPTO_PORTFOLIO_ACCOUNTS_COUNT))
    }

    private fun openCreation(walletId: UserWalletId) {
        router.replaceCurrent(AppRoute.CreateAccount(userWalletId = walletId))
    }
}