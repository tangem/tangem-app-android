package com.tangem.features.details.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.features.details.component.DetailsComponent
import com.tangem.features.details.entity.DetailsFooterUM
import com.tangem.features.details.entity.DetailsUM
import com.tangem.features.details.ui.DetailsScreen
import com.tangem.features.details.utils.ItemsBuilder
import com.tangem.features.details.utils.SocialsBuilder
import kotlinx.coroutines.runBlocking

internal class PreviewDetailsComponent : DetailsComponent {

    private val previewBlocks = runBlocking {
        ItemsBuilder(
            walletConnectComponent = PreviewWalletConnectComponent(),
            userWalletListComponent = PreviewUserWalletListComponent(),
        ).buldAll()
    }

    private val previewFooter = DetailsFooterUM(
        socials = SocialsBuilder(urlOpener = { /* no-op */ }).buildAll(),
        appVersion = "1.0.0-preview",
    )

    val previewState = DetailsUM(
        items = previewBlocks,
        footer = previewFooter,
        popBack = { /* no-op */ },
    )

    @Composable
    @Suppress("TopLevelComposableFunctions") // TODO: Remove this check
    override fun View(modifier: Modifier) {
        DetailsScreen(state = previewState, modifier = modifier)
    }
}