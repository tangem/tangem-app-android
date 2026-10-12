package com.tangem.features.jointaccount.creation.promo.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_shield_checkmark_20
import com.tangem.core.ui.res.generated.icons.ic_user_plus_20
import com.tangem.core.ui.res.generated.icons.ic_users_20
import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.features.jointaccount.creation.navigation.JointAccountCreationRoute
import com.tangem.features.jointaccount.creation.promo.ui.state.JointAccountPromoUM
import com.tangem.features.jointaccount.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
@ModelScoped
internal class JointAccountPromoModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
) : Model() {

    val uiState: StateFlow<JointAccountPromoUM>
        field = MutableStateFlow(createState())

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
        router.push(JointAccountCreationRoute.Config)
    }

    private fun onCloseClick() {
        router.pop()
    }
}