package com.tangem.core.ui.components.bottomsheets.modal

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.buttons.small.TangemIconButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * Title component for [TangemModalBottomSheet] with [TangemIconButton] for buttons.
 */
@Composable
fun TangemModalBottomSheetTitle(
    modifier: Modifier = Modifier,
    title: TextReference? = null,
    @DrawableRes startIconRes: Int? = null,
    onStartClick: (() -> Unit)? = null,
    @DrawableRes endIconRes: Int? = null,
    onEndClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        if (startIconRes != null && onStartClick != null) {
            TangemIconButton(
                iconRes = startIconRes,
                onClick = onStartClick,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterStart),
            )
        }
        if (title != null) {
            Text(
                text = title.resolveReference(),
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        if (endIconRes != null && onEndClick != null) {
            TangemIconButton(
                iconRes = endIconRes,
                onClick = onEndClick,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterEnd),
            )
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_TangemModalBottomSheetTitle(
    @PreviewParameter(TangemModalBottomSheetTitleProvider::class) params: TangemModalBottomSheetTitleData,
) {
    TangemThemePreview {
        TangemModalBottomSheetTitle(
            title = params.title,
            startIconRes = params.startIconRes,
            onStartClick = params.onStartClick,
            endIconRes = params.endIconRes,
            onEndClick = params.onEndClick,
            modifier = Modifier.background(TangemTheme.colors.background.secondary),
        )
    }
}

private data class TangemModalBottomSheetTitleData(
    val title: TextReference?,
    val startIconRes: Int?,
    val onStartClick: (() -> Unit)?,
    val endIconRes: Int?,
    val onEndClick: (() -> Unit)?,
)

private class TangemModalBottomSheetTitleProvider : PreviewParameterProvider<TangemModalBottomSheetTitleData> {
    override val values = sequenceOf(
        TangemModalBottomSheetTitleData(
            title = resourceReference(R.string.wallet_title),
            startIconRes = R.drawable.ic_back_24,
            onStartClick = {},
            endIconRes = null,
            onEndClick = null,
        ),
        TangemModalBottomSheetTitleData(
            title = resourceReference(R.string.wallet_title),
            startIconRes = null,
            onStartClick = null,
            endIconRes = R.drawable.ic_close_24,
            onEndClick = {},
        ),
        TangemModalBottomSheetTitleData(
            title = resourceReference(R.string.wallet_title),
            startIconRes = R.drawable.ic_back_24,
            onStartClick = {},
            endIconRes = R.drawable.ic_close_24,
            onEndClick = {},
        ),
        TangemModalBottomSheetTitleData(
            title = null,
            startIconRes = R.drawable.ic_back_24,
            onStartClick = {},
            endIconRes = R.drawable.ic_close_24,
            onEndClick = {},
        ),
    )
}
// endregion Preview
