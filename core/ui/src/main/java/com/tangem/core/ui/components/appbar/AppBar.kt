package com.tangem.core.ui.components.appbar

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * App bar without buttons
 *
 * @param text        title
 * @param modifier    modifier
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=123%3A287&t=WdN5XpixzZLlQAZO-4"
 * >Figma component</a>
 */
@Composable
fun AppBar(text: TextReference, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        Text(
            text = text.resolveReference(),
            color = TangemTheme.colors.text.primary1,
            maxLines = 1,
            style = TangemTheme.typography.subtitle1,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(vertical = TangemTheme.dimens.spacing10),
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppBar_Preview() {
    TangemThemePreview {
        AppBar(
            text = stringReference("Tangem"),
            modifier = Modifier.background(TangemTheme.colors.background.primary),
        )
    }
}
// endregion