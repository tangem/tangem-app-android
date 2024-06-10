package com.tangem.managetokens.presentation.managetokens.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.components.buttons.PrimarySmallButton
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.managetokens.impl.R
import com.tangem.managetokens.presentation.managetokens.state.TokenButtonType

@Composable
internal fun TokenButton(type: TokenButtonType, onClick: () -> Unit, modifier: Modifier = Modifier) {
    when (type) {
        TokenButtonType.ADD -> PrimarySmallButton(
            config = SmallButtonConfig(
                text = resourceReference(R.string.manage_tokens_add),
                onClick = onClick,
            ),
            modifier = modifier,
        )
        TokenButtonType.EDIT -> SecondarySmallButton(
            config = SmallButtonConfig(
                text = resourceReference(R.string.manage_tokens_edit),
                onClick = onClick,
            ),
            modifier = modifier,
        )
        TokenButtonType.NOT_AVAILABLE -> {
            Box(
                modifier = modifier
                    .size(height = TangemTheme.dimens.size24, width = TangemTheme.dimens.size46)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(
                            bounded = false,
                            radius = TangemTheme.dimens.size20,
                        ),
                        onClick = onClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier
                        .size(TangemTheme.dimens.size20),
                    painter = painterResource(id = R.drawable.ic_information_24),
                    tint = TangemTheme.colors.icon.informative,
                    contentDescription = null,
                )
            }
        }
    }
}

@Preview(backgroundColor = 0xffffff, showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TokenButton_Preview(@PreviewParameter(TokenButtonTypeProvider::class) type: TokenButtonType) {
    TangemThemePreview {
        TokenButton(type = type, {})
    }
}

private class TokenButtonTypeProvider : PreviewParameterProvider<TokenButtonType> {
    override val values = sequenceOf(
        TokenButtonType.ADD,
        TokenButtonType.EDIT,
        TokenButtonType.NOT_AVAILABLE,
    )
}