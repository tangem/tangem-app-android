package com.tangem.features.commonfeatures.impl.addtoportfolio

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.core.ui.ds.button.SecondaryTangemButton
import com.tangem.core.ui.ds.button.TangemButtonShape
import com.tangem.core.ui.ds.button.TangemButtonSize
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.commonfeatures.impl.R
import com.tangem.features.commonfeatures.impl.addtoportfolio.model.AddToPortfolioFooterKind
import com.tangem.features.commonfeatures.impl.addtoportfolio.model.AddToPortfolioRoutes
import com.tangem.features.commonfeatures.impl.addtoportfolio.model.uiSpec
import com.tangem.features.commonfeatures.impl.addtoportfolio.userportfolio.model.UserPortfolioUM
import dev.chrisbanes.haze.rememberHazeState

@Composable
internal fun AddToPortfolioBottomSheetFooter(
    currentRoute: AddToPortfolioRoutes,
    userPortfolioState: State<UserPortfolioUM?>?,
    onBack: () -> Unit,
    onAddFromUserPortfolioClick: (() -> Unit)?,
) {
    when (currentRoute.uiSpec().footer) {
        AddToPortfolioFooterKind.Cancel -> WithLocalHaze {
            CancelFooterButton(onClick = onBack)
        }
        AddToPortfolioFooterKind.UserPortfolioAdd -> {
            val state = userPortfolioState?.value ?: return
            val onClick = onAddFromUserPortfolioClick ?: return
            WithLocalHaze {
                UserPortfolioAddFooter(
                    isEnabled = state.isAddEnabled,
                    onClick = onClick,
                )
            }
        }
        AddToPortfolioFooterKind.None -> Unit
    }
}

@Composable
private fun WithLocalHaze(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalHazeState provides rememberHazeState(), content = content)
}

@Composable
private fun CancelFooterButton(onClick: () -> Unit) {
    SecondaryTangemButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens2.x4)
            .padding(bottom = TangemTheme.dimens2.x4),
        onClick = onClick,
        text = resourceReference(R.string.common_cancel),
        size = TangemButtonSize.X12,
        shape = TangemButtonShape.Rounded,
    )
}

@Composable
private fun UserPortfolioAddFooter(isEnabled: Boolean, onClick: () -> Unit) {
    TangemRowContainer(
        contentPadding = PaddingValues(
            horizontal = TangemTheme.dimens2.x6,
            vertical = TangemTheme.dimens2.x5,
        ),
    ) {
        Text(
            modifier = Modifier.layoutId(TangemRowLayoutId.START_TOP),
            text = stringResourceSafe(R.string.common_add_token),
            style = TangemTheme.typography2.bodyMedium16,
            color = TangemTheme.colors2.text.neutral.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            modifier = Modifier.layoutId(TangemRowLayoutId.START_BOTTOM),
            text = stringResourceSafe(R.string.markets_token_add_subtitle),
            style = TangemTheme.typography2.captionMedium12,
            color = TangemTheme.colors2.text.neutral.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        SecondaryTangemButton(
            modifier = Modifier
                .layoutId(TangemRowLayoutId.TAIL)
                .padding(start = TangemTheme.dimens2.x2),
            onClick = onClick,
            text = resourceReference(R.string.common_add),
            size = TangemButtonSize.X9,
            shape = TangemButtonShape.Rounded,
            isEnabled = isEnabled,
        )
    }
}