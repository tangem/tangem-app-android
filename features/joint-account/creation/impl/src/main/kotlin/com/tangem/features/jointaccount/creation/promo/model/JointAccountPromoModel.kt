package com.tangem.features.jointaccount.creation.promo.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_lightning_24
import com.tangem.core.ui.res.generated.icons.ic_shield_checkmark_24
import com.tangem.features.jointaccount.creation.impl.R
import com.tangem.features.jointaccount.creation.navigation.JointAccountCreationRoute
import com.tangem.features.jointaccount.creation.promo.ui.state.JointAccountPromoUM
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
        benefits = persistentListOf(
            JointAccountPromoUM.BenefitUM(
                id = "members",
                icon = TangemIconUM.Icon(
                    imageVector = Icons.ic_lightning_24,
                    tintReference = { TangemTheme.colors3.icon.status.info },
                ),
                // TODO([REDACTED_TASK_KEY]) will be added
                title = stringReference(value = "Up to 5 members"),
                subtitle = stringReference(value = "Everyone shares one balance and history"),
            ),
            JointAccountPromoUM.BenefitUM(
                id = "control",
                icon = TangemIconUM.Icon(
                    imageVector = Icons.ic_shield_checkmark_24,
                    tintReference = { TangemTheme.colors3.icon.status.info },
                ),
                // TODO([REDACTED_TASK_KEY]) will be added
                title = stringReference(value = "No single point of control"),
                subtitle = stringReference(value = "Funds move only with enough signatures"),
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