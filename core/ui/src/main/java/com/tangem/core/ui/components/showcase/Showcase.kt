package com.tangem.core.ui.components.showcase

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.showcase.model.ShowcaseButtonModel
import com.tangem.core.ui.components.showcase.model.ShowcaseItemModel
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * @param headerIconRes big header icon
 * @param headerText header text
 * @param showcaseItems list of bullet points
 * @param primaryButton primary button
 * @param secondaryButton secondary button
 * @param modifier compose modifier
 *
 * @see <a href = "https://www.figma.com/design/Vs6SkVsFnUPsSCNwlnVf5U/Android-%E2%80%93-UI?node-id=12620-90568&t=WeCOVTgeYfkr24Ff-4"
 * >Figma component</a>
 */
@Composable
fun Showcase(
    @DrawableRes headerIconRes: Int,
    headerText: TextReference,
    showcaseItems: ImmutableList<ShowcaseItemModel>,
    primaryButton: ShowcaseButtonModel,
    secondaryButton: ShowcaseButtonModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        ShowcaseContent(
            headerIconRes = headerIconRes,
            headerText = headerText,
            showcaseItems = showcaseItems,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterHorizontally),
        )
        ShowcaseButtons(
            primaryButtonText = primaryButton.buttonText,
            onPrimaryClick = primaryButton.onClick,
            secondaryButtonText = secondaryButton.buttonText,
            onSecondaryClick = secondaryButton.onClick,
        )
    }
}

@Composable
private fun ShowcaseButtons(
    primaryButtonText: TextReference,
    secondaryButtonText: TextReference,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
    hint: TextReference? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PrimaryButton(
            text = primaryButtonText.resolveReference(),
            onClick = onPrimaryClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = TangemTheme.dimens.spacing16,
                    end = TangemTheme.dimens.spacing16,
                ),
        )
        SecondaryButton(
            text = secondaryButtonText.resolveReference(),
            onClick = onSecondaryClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = TangemTheme.dimens.spacing16,
                    end = TangemTheme.dimens.spacing16,
                    top = TangemTheme.dimens.spacing12,
                    bottom = TangemTheme.dimens.spacing16,
                ),
        )
        hint?.let {
            Text(
                text = it.resolveReference(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = TangemTheme.dimens.spacing16,
                        end = TangemTheme.dimens.spacing16,
                        bottom = TangemTheme.dimens.spacing12,
                    ),
            )
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Preview(showBackground = true, widthDp = 360, heightDp = 720, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Showcase_Preview() {
    TangemThemePreview {
        Showcase(
            headerIconRes = R.drawable.ic_notifications_unread_24,
            headerText = resourceReference(R.string.user_push_notification_agreement_header),
            showcaseItems = persistentListOf(
                ShowcaseItemModel(
                    R.drawable.ic_rocket_launch_24,
                    resourceReference(R.string.user_push_notification_agreement_argument_one),
                ),
                ShowcaseItemModel(
                    R.drawable.ic_storefront_24,
                    resourceReference(R.string.user_push_notification_agreement_argument_two),
                ),
            ),
            primaryButton = ShowcaseButtonModel(resourceReference(R.string.common_allow), {}),
            secondaryButton = ShowcaseButtonModel(resourceReference(R.string.common_later), {}),
            modifier = Modifier.background(TangemTheme.colors.background.primary),
        )
    }
}
// endregion
