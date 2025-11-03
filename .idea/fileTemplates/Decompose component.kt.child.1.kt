#if (${PACKAGE_NAME} != "")package ${PACKAGE_NAME}.ui#end

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import ${PACKAGE_NAME}.entity.${NAME}UM

@Composable
internal fun ${NAME}Content(state: ${NAME}UM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color = TangemTheme.colors.background.secondary)
            .fillMaxSize()
            .imePadding()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppBarWithBackButton(
            onBackClick = state.onBackClick,
            modifier = Modifier.height(TangemTheme.dimens.size56),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = TangemTheme.dimens.spacing16)
                .weight(1f),

        ) {
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ${NAME}ContentPreview(@PreviewParameter(PreviewStateProvider::class) state: ${NAME}UM) {
    TangemThemePreview {
        ${NAME}Content(state = state)
    }
}

private class PreviewStateProvider : CollectionPreviewParameterProvider<${NAME}UM>(
    buildList {
        TODO("Not yet implemented")
    },
)