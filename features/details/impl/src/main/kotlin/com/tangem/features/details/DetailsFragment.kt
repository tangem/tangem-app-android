package com.tangem.features.details

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.features.details.component.preview.PreviewDetailsComponent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// TODO: Remove after https://tangem.atlassian.net/browse/AND-7170
@AndroidEntryPoint
internal class DetailsFragment : ComposeFragment() {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        // TODO: Update to use the actual component in AND-7165
        PreviewDetailsComponent().View(modifier = modifier)
    }

    companion object : DetailsEntryPoint {

        override fun entryFragment(): Fragment = DetailsFragment()
    }
}
