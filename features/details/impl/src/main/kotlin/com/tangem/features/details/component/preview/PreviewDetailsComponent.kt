package com.tangem.features.details.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.navigation.DummyRouter
import com.tangem.core.navigation.url.DummyUrlOpener
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
            router = DummyRouter(),
            urlOpener = DummyUrlOpener(),
        ).buildAll(isWalletConnectAvailable = true, onSupportClick = {})
    }

    private val previewFooter = DetailsFooterUM(
        socials = SocialsBuilder(DummyUrlOpener()).buildAll(),
        appVersion = "1.0.0-preview",
    )

    val previewState = DetailsUM(
        items = previewBlocks,
        footer = previewFooter,
        popBack = { /* no-op */ },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        DetailsScreen(
            modifier = modifier,
            state = previewState,
            userWalletListBlockContent = PreviewUserWalletListComponent(),
        )
    }
}