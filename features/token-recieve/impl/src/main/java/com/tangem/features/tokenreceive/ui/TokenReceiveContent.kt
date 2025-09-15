package com.tangem.features.tokenreceive.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.tokenreceive.route.TokenReceiveRoutes

@Composable
internal fun TokenReceiveContentSheet(
    route: TokenReceiveRoutes,
    onCloseClick: () -> Unit,
    onBackClick: () -> Unit,
    contentStack: ChildStack<TokenReceiveRoutes, ComposableContentComponent>,
) {
    TangemModalBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onCloseClick,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = onBackClick,
        containerColor = TangemTheme.colors.background.tertiary,
        title = {
            Title(
                route = route,
                onBackClick = onBackClick,
                onCloseClick = onCloseClick,
            )
        },
        content = {
            TokenReceiveContent(
                stackState = contentStack,
                modifier = Modifier,
            )
        },
    )
}

@Composable
private fun Title(route: TokenReceiveRoutes, onBackClick: () -> Unit, onCloseClick: () -> Unit) {
    when (route) {
        is TokenReceiveRoutes.QrCode -> {
            TangemModalBottomSheetTitle(
                startIconRes = R.drawable.ic_back_24,
                onStartClick = onBackClick,
                endIconRes = R.drawable.ic_close_24,
                onEndClick = onCloseClick,
            )
        }
        TokenReceiveRoutes.ReceiveAssets -> {
            TangemModalBottomSheetTitle(
                title = resourceReference(R.string.domain_receive_assets_navigation_title),
                endIconRes = R.drawable.ic_close_24,
                onEndClick = onCloseClick,
            )
        }
        TokenReceiveRoutes.Warning -> {
            TangemModalBottomSheetTitle(
                endIconRes = R.drawable.ic_close_24,
                onEndClick = onCloseClick,
            )
        }
    }
}

@Composable
internal fun TokenReceiveContent(
    stackState: ChildStack<TokenReceiveRoutes, ComposableContentComponent>,
    modifier: Modifier = Modifier,
) {
    Children(
        stack = stackState,
        animation = stackAnimation(fade(animationSpec = tween(durationMillis = 100))),
        modifier = modifier
            .fillMaxSize()
            .animateContentSize(),
    ) {
        it.instance.Content(Modifier.fillMaxSize())
    }
}