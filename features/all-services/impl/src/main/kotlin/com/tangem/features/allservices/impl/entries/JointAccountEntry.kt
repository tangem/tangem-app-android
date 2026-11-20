package com.tangem.features.allservices.impl.entries

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_users_24
import com.tangem.features.allservices.impl.R
import com.tangem.features.allservices.impl.entries.model.JointAccountEntryModel
import com.tangem.features.allservices.impl.ui.AllServicesEntryIcon
import com.tangem.features.allservices.impl.ui.AllServicesEntryRow
import kotlinx.coroutines.flow.StateFlow

internal class JointAccountEntry(
    context: AppComponentContext,
) : AllServicesEntryComponent(context = context, key = KEY) {

    private val model: JointAccountEntryModel = getOrCreateModel()

    override val isVisible: StateFlow<Boolean> = model.isVisible

    @Composable
    override fun Content(modifier: Modifier) {
        AllServicesEntryRow(
            title = resourceReference(R.string.common_joint_account),
            subtitle = resourceReference(R.string.all_services_joint_account_subtitle),
            onClick = model::onClick,
            modifier = modifier,
            icon = { AllServicesEntryIcon(imageVector = Icons.ic_users_24) },
        )
    }

    companion object {
        const val KEY = "joint_account"
    }
}