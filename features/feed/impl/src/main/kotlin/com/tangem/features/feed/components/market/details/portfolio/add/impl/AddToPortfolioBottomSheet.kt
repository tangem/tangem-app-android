package com.tangem.features.feed.components.market.details.portfolio.add.impl

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.account.PortfolioSelectorComponent
import com.tangem.features.feed.components.market.details.portfolio.add.impl.model.AddToPortfolioRoutes
import com.tangem.features.feed.impl.R

@Composable
internal fun AddToPortfolioBottomSheet(
    childStack: State<ChildStack<AddToPortfolioRoutes, ComposableContentComponent>>,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    val stack by childStack
    val contentStack = remember { mutableStateOf(stack) }
    val currentRoute = stack.active.configuration
    val isNotEmpty = currentRoute != AddToPortfolioRoutes.Empty
    if (isNotEmpty) {
        contentStack.value = stack
    }

    TangemModalBottomSheet<TangemBottomSheetConfigContent.Empty>(
        scrollableContent = false,
        onBack = onBack,
        config = TangemBottomSheetConfig(
            isShown = isNotEmpty,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        containerColor = TangemTheme.colors.background.tertiary,
        title = {
            AnimatedContent(targetState = contentStack.value, label = "Title Animation") { animatedStack ->
                AddToPortfolioBottomSheetTitle(
                    stack = animatedStack,
                    onBackClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        content = {
            AnimatedContent(targetState = contentStack.value, label = "Content Animation") { animatedStack ->
                val paddingModifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                )
                val isScrollableContent = when (animatedStack.active.configuration) {
                    AddToPortfolioRoutes.PortfolioSelector -> false
                    AddToPortfolioRoutes.AddToken,
                    AddToPortfolioRoutes.Empty,
                    is AddToPortfolioRoutes.NetworkSelector,
                    AddToPortfolioRoutes.TokenActions,
                    -> true
                }
                if (isScrollableContent) {
                    Column(
                        modifier = paddingModifier.verticalScroll(rememberScrollState()),
                    ) {
                        animatedStack.active.instance.Content(modifier = Modifier)
                    }
                } else {
                    animatedStack.active.instance.Content(modifier = paddingModifier)
                }
            }
        },
    )
}

@Composable
private fun AddToPortfolioBottomSheetTitle(
    stack: ChildStack<AddToPortfolioRoutes, ComposableContentComponent>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title: TextReference = when (stack.active.configuration) {
        AddToPortfolioRoutes.AddToken -> resourceReference(R.string.common_add_token)
        AddToPortfolioRoutes.Empty -> TextReference.EMPTY
        is AddToPortfolioRoutes.NetworkSelector -> resourceReference(R.string.common_choose_network)
        AddToPortfolioRoutes.TokenActions -> resourceReference(R.string.common_get_token)
        AddToPortfolioRoutes.PortfolioSelector -> (stack.active.instance as PortfolioSelectorComponent)
            .title.collectAsStateWithLifecycle().value
    }
    val startIconRes: Int?
    val endIconRes: Int?
    if (stack.backStack.isNotEmpty()) {
        startIconRes = R.drawable.ic_back_24
        endIconRes = null
    } else {
        startIconRes = null
        endIconRes = R.drawable.ic_close_24
    }
    TangemModalBottomSheetTitle(
        modifier = modifier,
        title = title,
        startIconRes = startIconRes,
        endIconRes = endIconRes,
        onStartClick = onBackClick,
        onEndClick = onBackClick,
    )
}