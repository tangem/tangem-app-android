package com.tangem.feature.tester.presentation.actions

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.findActivity
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.feature.tester.impl.R
import com.tangem.feature.tester.presentation.actions.TesterActionsContentState.HideAllCurrenciesUM
import com.tangem.feature.tester.presentation.actions.TesterActionsContentState.ToggleAppThemeUM
import timber.log.Timber
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TesterActionsScreen(state: TesterActionsContentState, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.secondary),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        stickyHeader {
            AppBarWithBackButton(
                text = stringResourceSafe(R.string.tester_actions),
                onBackClick = state.onBackClick,
            )
        }

        item {
            val onClick = remember(state.hideAllCurrenciesUM) {
                { (state.hideAllCurrenciesUM as? HideAllCurrenciesUM.Clickable)?.onClick?.invoke() ?: Unit }
            }
            TesterActionItem(
                name = stringResourceSafe(R.string.hide_all_currencies),
                progress = state.hideAllCurrenciesUM is HideAllCurrenciesUM.Progress,
                onClick = onClick,
            )
        }

        item {
            val config = state.toggleAppThemeUM

            TesterActionItem(
                name = stringResourceSafe(id = R.string.toggle_app_theme, config.currentAppTheme.name),
                onClick = config.onClick,
            )
        }

        item {
            val activity = LocalContext.current.findActivity()

            TesterActionItem(
                name = stringResourceSafe(id = R.string.share_logs),
                onClick = { activity.shareFile(file = state.shareLogsUM.file) },
                enabled = state.shareLogsUM.file != null,
            )
        }
    }
}

@Composable
private fun TesterActionItem(name: String, onClick: () -> Unit, progress: Boolean = false, enabled: Boolean = true) {
    PrimaryButton(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        text = name,
        onClick = onClick,
        showProgress = progress,
        enabled = enabled,
    )
}

private fun Activity.shareFile(file: File?) {
    val originalIntent = createEmailShareIntent(activity = this, file = file)

    try {
        val chooserIntent = Intent.createChooser(originalIntent, "Share logs...")

        ContextCompat.startActivity(this, chooserIntent, null)
    } catch (ex: Exception) {
        Timber.e("Failed to share file: $ex")
    }
}

private fun createEmailShareIntent(activity: Activity, file: File?): Intent {
    val builder = ShareCompat.IntentBuilder(activity)
        .setType("text/plain")

    file?.let {
        builder.setStream(
            FileProvider.getUriForFile(activity, "${activity.packageName}.provider", it),
        )
    }

    return builder.intent
}

// region Preview
@Composable
private fun TesterActionsScreenSample(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary),
    ) {
        TesterActionsScreen(
            state = TesterActionsContentState(
                hideAllCurrenciesUM = HideAllCurrenciesUM.Clickable {},
                toggleAppThemeUM = ToggleAppThemeUM(AppThemeMode.DEFAULT) {},
                shareLogsUM = TesterActionsContentState.ShareLogsUM(file = null),
                onBackClick = {},
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TesterActionsScreenPreview() {
    TangemThemePreview {
        TesterActionsScreenSample()
    }
}
// endregion Preview