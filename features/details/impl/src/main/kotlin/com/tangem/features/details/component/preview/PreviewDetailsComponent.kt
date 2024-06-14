package com.tangem.features.details.component.preview

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.navigation.DummyRouter
import com.tangem.features.details.component.DetailsComponent
import com.tangem.features.details.entity.DetailsFooterUM
import com.tangem.features.details.entity.DetailsUM
import com.tangem.features.details.ui.DetailsScreen
import com.tangem.features.details.utils.ItemsBuilder
import com.tangem.features.details.utils.SocialsBuilder
import kotlinx.coroutines.runBlocking

internal class PreviewDetailsComponent : DetailsComponent {

    override val snackbarHostState: SnackbarHostState = SnackbarHostState()

    private val previewBlocks = runBlocking {
        ItemsBuilder(
            router = DummyRouter(),
        ).buldAll(isWalletConnectAvailable = true)
    }

    private val previewFooter = DetailsFooterUM(
        socials = SocialsBuilder(DummyRouter()).buildAll(),
        appVersion = "1.0.0-preview",
    )

    val previewState = DetailsUM(
        items = previewBlocks,
        footer = previewFooter,
        popBack = { /* no-op */ },
    )

    @Composable
    @Suppress("TopLevelComposableFunctions") // TODO: Remove this check
    override fun Content(modifier: Modifier) {
        DetailsScreen(
            modifier = modifier,
            state = previewState,
            snackbarHostState = snackbarHostState,
            userWalletListBlockContent = PreviewUserWalletListComponent(),
        )
    }
}