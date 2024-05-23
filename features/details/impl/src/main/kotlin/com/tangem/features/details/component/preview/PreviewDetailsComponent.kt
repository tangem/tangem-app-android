package com.tangem.features.details.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.features.details.component.DetailsComponent
import com.tangem.features.details.entity.DetailsFooter
import com.tangem.features.details.utils.BlocksBuilder
import com.tangem.features.details.utils.SocialsBuilder
import kotlinx.coroutines.flow.MutableStateFlow

internal class PreviewDetailsComponent : DetailsComponent {

    private val previewBlocks = BlocksBuilder(
        walletConnectComponent = PreviewWalletConnectComponent(),
        userWalletListComponent = PreviewUserWalletListComponent(),
    ).buldAll()

    private val previewFooter = DetailsFooter(
        socials = SocialsBuilder(urlOpener = { /* no-op */ }).buildAll(),
        appVersion = "1.0.0-preview",
    )

    private val previewState = DetailsComponent.State(
        blocks = previewBlocks,
        footer = previewFooter,
        popBack = { /* no-op */ },
    )

    private val state = MutableStateFlow(previewState)

    @Composable
    @Suppress("TopLevelComposableFunctions") // TODO: Remove this check
    override fun View(modifier: Modifier) {
        TODO("Will be implemented in AND-7107")
    }
}
