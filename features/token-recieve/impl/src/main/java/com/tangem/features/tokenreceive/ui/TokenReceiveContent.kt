package com.tangem.features.tokenreceive.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_left_24
import com.tangem.core.ui.res.generated.icons.ic_cross_24
import com.tangem.features.tokenreceive.route.TokenReceiveRoutes
import dev.chrisbanes.haze.rememberHazeState

@Composable
internal fun TokenReceiveContentSheet(
    route: TokenReceiveRoutes,
    onCloseClick: () -> Unit,
    onBackClick: () -> Unit,
    contentStack: ChildStack<TokenReceiveRoutes, ComposableContentComponent>,
) {
    CompositionLocalProvider(LocalHazeState provides rememberHazeState()) {
        TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
            type = TangemBottomSheetType.Modal,
            onBack = onBackClick,
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = onCloseClick,
                content = TangemBottomSheetConfigContent.Empty,
            ),
            containerColor = TangemTheme.colors3.bg.secondary,
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
}

@Composable
private fun Title(route: TokenReceiveRoutes, onBackClick: () -> Unit, onCloseClick: () -> Unit) {
    when (route) {
        is TokenReceiveRoutes.QrCode -> {
            TangemTopBar(
                type = TangemTopBarType.BottomSheet,
                endContent = {
                    TangemButton(
                        iconStart = TangemIconUM.Icon(Icons.ic_cross_24),
                        onClick = onCloseClick,
                        size = TangemButton.Size.X11,
                        variant = TangemButton.Variant.Material,
                    )
                },
                startContent = {
                    TangemButton(
                        iconStart = TangemIconUM.Icon(imageVector = Icons.ic_chevron_left_24),
                        onClick = onBackClick,
                        size = TangemButton.Size.X11,
                        variant = TangemButton.Variant.Material,
                    )
                },
            )
        }
        TokenReceiveRoutes.ReceiveAssets -> {
            TangemTopBar(
                title = resourceReference(R.string.domain_receive_assets_navigation_title),
                type = TangemTopBarType.BottomSheet,
                endContent = {
                    TangemButton(
                        iconStart = TangemIconUM.Icon(Icons.ic_cross_24),
                        onClick = onCloseClick,
                        size = TangemButton.Size.X11,
                        variant = TangemButton.Variant.Material,
                    )
                },
            )
        }
        TokenReceiveRoutes.Warning -> {
            TangemTopBar(
                type = TangemTopBarType.BottomSheet,
                endContent = {
                    TangemButton(
                        iconStart = TangemIconUM.Icon(Icons.ic_cross_24),
                        onClick = onCloseClick,
                        size = TangemButton.Size.X11,
                        variant = TangemButton.Variant.Material,
                    )
                },
            )
        }
    }
}

@Composable
private fun TokenReceiveContent(
    stackState: ChildStack<TokenReceiveRoutes, ComposableContentComponent>,
    modifier: Modifier = Modifier,
) {
    Children(
        stack = stackState,
        animation = stackAnimation(fade(animationSpec = tween(durationMillis = 100))),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        it.instance.Content(Modifier.fillMaxWidth())
    }
}