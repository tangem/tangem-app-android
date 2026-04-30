package com.tangem.features.commonfeatures.impl.addtoportfolio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.features.commonfeatures.impl.addtoportfolio.model.AddToPortfolioRoutes
import com.tangem.features.commonfeatures.impl.addtoportfolio.userportfolio.model.UserPortfolioUM

@Composable
internal fun AddToPortfolioBottomSheetSwitch(
    childStack: State<ChildStack<AddToPortfolioRoutes, ComposableContentComponent>>,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    userPortfolioState: State<UserPortfolioUM?>? = null,
    onAddFromUserPortfolioClick: (() -> Unit)? = null,
) {
    if (LocalRedesignEnabled.current) {
        AddToPortfolioBottomSheetV2(
            childStack = childStack,
            onBack = onBack,
            onDismiss = onDismiss,
            userPortfolioState = userPortfolioState,
            onAddFromUserPortfolioClick = onAddFromUserPortfolioClick,
        )
    } else {
        AddToPortfolioBottomSheet(
            childStack = childStack,
            onBack = onBack,
            onDismiss = onDismiss,
        )
    }
}