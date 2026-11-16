package com.tangem.features.allservices.impl.entries

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_stack_24
import com.tangem.domain.models.earn.EarnType
import com.tangem.domain.models.earn.PreselectedEarnType
import com.tangem.features.allservices.impl.R
import com.tangem.features.allservices.impl.entries.model.EarnOfferEntryModel
import com.tangem.features.allservices.impl.ui.AllServicesEntryIcon
import com.tangem.features.allservices.impl.ui.AllServicesEntryRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class StakingEntry(
    context: AppComponentContext,
) : AllServicesEntryComponent(context = context, key = KEY) {

    private val model: EarnOfferEntryModel = getOrCreateModel(params = EarnType.STAKING)

    override val isVisible: StateFlow<Boolean> = MutableStateFlow(true)

    @Composable
    override fun Content(modifier: Modifier) {
        val bestApy by model.bestApy.collectAsStateWithLifecycle()

        AllServicesEntryRow(
            title = resourceReference(R.string.common_staking),
            subtitle = bestApy?.let { apy ->
                resourceReference(R.string.all_services_staking_subtitle, wrappedList(apy))
            },
            onClick = { router.push(AppRoute.Earn(preselectedEarnType = PreselectedEarnType.Staking)) },
            modifier = modifier,
            icon = { AllServicesEntryIcon(imageVector = Icons.ic_stack_24) },
        )
    }

    companion object {
        const val KEY = "staking"
    }
}