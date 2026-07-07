package com.tangem.features.commonfeatures.impl.addtoportfolio

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.components.bottomsheets.*
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.commonfeatures.impl.R
import com.tangem.features.commonfeatures.impl.addtoportfolio.model.AddToPortfolioRoutes
import com.tangem.features.commonfeatures.impl.addtoportfolio.model.uiSpec
import com.tangem.features.commonfeatures.impl.userportfolio.model.UserPortfolioUM

@Composable
internal fun AddToPortfolioBottomSheetV2(
    childStack: State<ChildStack<AddToPortfolioRoutes, ComposableContentComponent>>,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    userPortfolioState: State<UserPortfolioUM?>? = null,
    onAddFromUserPortfolioClick: (() -> Unit)? = null,
) {
    val stack by childStack
    val contentStack = remember { mutableStateOf(stack) }
    val isNotEmpty = stack.active.configuration != AddToPortfolioRoutes.Empty
    if (isNotEmpty) {
        contentStack.value = stack
    }

    val type = if (stack.active.configuration is AddToPortfolioRoutes.TokenActions) {
        TangemBottomSheetType.Default
    } else {
        TangemBottomSheetType.Modal
    }
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        onBack = onBack,
        config = TangemBottomSheetConfig(
            isShown = isNotEmpty,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        type = type,
        containerColor = TangemTheme.colors2.surface.level2,
        title = {
            AddToPortfolioBottomSheetTitle(
                stack = stack,
                onCloseClick = onDismiss,
            )
        },
        content = {
            AnimatedContent(targetState = contentStack.value, label = "Content Animation") { animatedStack ->
                AddToPortfolioRouteContent(animatedStack = animatedStack)
            }
        },
        footer = {
            AddToPortfolioBottomSheetFooter(
                currentRoute = contentStack.value.active.configuration,
                userPortfolioState = userPortfolioState,
                onBack = onBack,
                onAddFromUserPortfolioClick = onAddFromUserPortfolioClick,
            )
        },
    )
}

@Composable
private fun AddToPortfolioRouteContent(animatedStack: ChildStack<AddToPortfolioRoutes, ComposableContentComponent>) {
    val spec = animatedStack.active.configuration.uiSpec()
    val baseModifier = if (spec.shouldApplyHorizontalPadding) {
        Modifier.padding(horizontal = TangemTheme.dimens2.x4)
    } else {
        Modifier
    }
    if (spec.isScrollable) {
        val bottomInset = LocalTangemBottomSheetContentBottomInset.current
        val scrollBottomReserve = if (bottomInset > 0.dp) bottomInset else TangemTheme.dimens2.x4
        Column(modifier = baseModifier.verticalScroll(rememberScrollState())) {
            animatedStack.active.instance.Content(modifier = Modifier)
            Spacer(modifier = Modifier.height(scrollBottomReserve))
        }
    } else {
        val isFullScreenRoute = animatedStack.active.configuration is AddToPortfolioRoutes.TokenActions
        val sizeModifier = if (isFullScreenRoute) Modifier.fillMaxSize() else Modifier
        animatedStack.active.instance.Content(modifier = baseModifier.then(sizeModifier))
    }
}

@Composable
private fun AddToPortfolioBottomSheetTitle(
    stack: ChildStack<AddToPortfolioRoutes, ComposableContentComponent>,
    onCloseClick: () -> Unit,
) {
    TangemTopBar(
        title = stack.active.configuration.uiSpec().title,
        type = TangemTopBarType.BottomSheet,
        endContent = {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_close_24),
                contentDescription = null,
                tint = TangemTheme.colors2.graphic.neutral.primary,
                modifier = Modifier
                    .size(TangemTheme.dimens2.x11)
                    .background(
                        color = TangemTheme.colors2.button.backgroundSecondary,
                        shape = CircleShape,
                    )
                    .clickableSingle(onClick = onCloseClick)
                    .padding(TangemTheme.dimens2.x2),
            )
        },
    )
}