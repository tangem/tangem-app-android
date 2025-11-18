package com.tangem.features.hotwallet.createwalletbackup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.features.hotwallet.impl.R
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.hotwallet.createwalletbackup.routing.CreateWalletBackupRoute

@Composable
internal fun CreateWalletBackupContent(
    stackState: ChildStack<CreateWalletBackupRoute, ComposableContentComponent>,
    showTopBar: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(color = TangemTheme.colors.background.primary)
            .fillMaxSize()
            .imePadding()
            .systemBarsPadding(),
    ) {
        if (showTopBar) {
            TangemTopAppBar(
                modifier = Modifier,
                startButton = TopAppBarButtonUM.Back(
                    onBackClicked = onBackClick,
                ),
                title = stringResourceSafe(id = R.string.common_backup),
            )
        }

        Children(
            stack = stackState,
            animation = stackAnimation(slide()),
            modifier = Modifier.fillMaxSize(),
        ) {
            it.instance.Content(Modifier.fillMaxSize())
        }
    }
}