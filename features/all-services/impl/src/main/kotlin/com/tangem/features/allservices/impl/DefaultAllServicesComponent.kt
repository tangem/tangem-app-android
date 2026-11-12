package com.tangem.features.allservices.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.allservices.api.AllServicesComponent
import com.tangem.features.allservices.impl.ui.AllServicesScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("UnusedPrivateProperty")
internal class DefaultAllServicesComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted params: Unit,
) : AllServicesComponent, AppComponentContext by context {

    @Composable
    override fun Content(modifier: Modifier) {
        AllServicesScreen(onBackClick = router::pop, modifier = modifier)
    }

    @AssistedFactory
    interface Factory : AllServicesComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultAllServicesComponent
    }
}