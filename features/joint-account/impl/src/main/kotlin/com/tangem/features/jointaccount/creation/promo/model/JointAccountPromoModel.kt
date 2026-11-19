package com.tangem.features.jointaccount.creation.promo.model

import androidx.compose.runtime.Stable
import com.tangem.common.ui.account.accountLimitReachedDialog
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_shield_checkmark_20
import com.tangem.core.ui.res.generated.icons.ic_user_plus_20
import com.tangem.core.ui.res.generated.icons.ic_users_20
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.commonfeatures.api.portfolioselector.AccountWalletPicker
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.LimitSource
import com.tangem.features.jointaccount.creation.model.JointAccountCreationChildParams
import com.tangem.features.jointaccount.creation.navigation.JointAccountCreationRoute
import com.tangem.features.jointaccount.creation.promo.ui.state.JointAccountPromoUM
import com.tangem.features.jointaccount.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The flow's intro. When the flow was opened without a wallet, Continue settles it first: straight away when
 * only one wallet can take a joint account, through the wallet chooser when several can, and with the limit
 * dialog when none has room left.
 */
@Stable
@ModelScoped
internal class JointAccountPromoModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val messageSender: UiMessageSender,
    accountWalletPickerFactory: AccountWalletPicker.Factory,
) : Model() {

    private val params = paramsContainer.require<JointAccountCreationChildParams>()

    val uiState: StateFlow<JointAccountPromoUM>
        field = MutableStateFlow(createState())

    val walletPicker: AccountWalletPicker = accountWalletPickerFactory.create(scope = modelScope)

    init {
        analyticsEventHandler.send(JointAccountAnalyticsEvent.IntroScreenOpened(source = params.source))
        walletPicker.picks
            .onEach { pick ->
                when (pick) {
                    is AccountWalletPicker.Pick.Wallet -> continueWith(pick.walletId)
                    AccountWalletPicker.Pick.Full -> showLimitReached()
                }
            }
            .launchIn(modelScope)
    }

    private fun createState(): JointAccountPromoUM = JointAccountPromoUM(
        title = resourceReference(R.string.common_joint_account),
        subtitle = resourceReference(R.string.joint_account_onboarding_subtitle),
        points = persistentListOf(
            JointAccountPromoUM.PointUM(
                id = "members",
                icon = TangemIconUM.Icon(
                    imageVector = Icons.ic_user_plus_20,
                    tintReference = {
                        TangemTheme.colors3.icon.primary
                    },
                ),
                lead = resourceReference(
                    id = R.string.joint_account_onboarding_members_title,
                    formatArgs = wrappedList(JointAccountConfig.MAX_MEMBERS_COUNT),
                ),
                description = resourceReference(R.string.joint_account_onboarding_members_description),
            ),
            JointAccountPromoUM.PointUM(
                id = "shared",
                icon = TangemIconUM.Icon(
                    imageVector = Icons.ic_users_20,
                    tintReference = {
                        TangemTheme.colors3.icon.primary
                    },
                ),
                lead = resourceReference(R.string.joint_account_onboarding_shared_title),
                description = resourceReference(R.string.joint_account_onboarding_shared_description),
            ),
            JointAccountPromoUM.PointUM(
                id = "approvals",
                icon = TangemIconUM.Icon(
                    imageVector = Icons.ic_shield_checkmark_20,
                    tintReference = {
                        TangemTheme.colors3.icon.primary
                    },
                ),
                lead = resourceReference(R.string.joint_account_onboarding_approvals_title),
                description = resourceReference(R.string.joint_account_onboarding_approvals_description),
            ),
        ),
        onContinueClick = ::onContinueClick,
        onCloseClick = ::onCloseClick,
    )

    private fun onContinueClick() {
        if (params.draftHolder.draft.value.walletId != null) {
            router.push(JointAccountCreationRoute.Config)
            return
        }
        if (uiState.value.isContinueLoading) return

        modelScope.launch {
            uiState.update { it.copy(isContinueLoading = true) }
            val resolution = walletPicker.resolve(hasRoom = AccountList::canAddMoreJointAccounts)
            uiState.update { it.copy(isContinueLoading = false) }

            when (resolution) {
                AccountWalletPicker.Resolution.Unavailable -> messageSender.send(
                    DialogMessage(
                        title = resourceReference(R.string.common_something_went_wrong),
                        message = resourceReference(R.string.common_unknown_error),
                    ),
                )
                AccountWalletPicker.Resolution.LimitReached -> showLimitReached()
                is AccountWalletPicker.Resolution.Single -> continueWith(resolution.walletId)
                is AccountWalletPicker.Resolution.Choose -> walletPicker.openChooser(resolution.fullWalletIds)
            }
        }
    }

    private fun showLimitReached() {
        analyticsEventHandler.send(JointAccountAnalyticsEvent.NoticeAccountsLimitReached(LimitSource.Create))
        messageSender.send(accountLimitReachedDialog(limit = AccountList.MAX_JOINT_ACCOUNTS_COUNT))
    }

    private fun continueWith(walletId: UserWalletId) {
        params.draftHolder.setWalletId(walletId)
        router.push(JointAccountCreationRoute.Config)
    }

    private fun onCloseClick() {
        router.pop()
    }
}