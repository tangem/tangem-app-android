package com.tangem.features.commonfeatures.impl.addfunds

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.features.commonfeatures.api.addfunds.AddFundsComponent
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenComponent
import com.tangem.features.commonfeatures.impl.R
import com.tangem.features.commonfeatures.impl.addfunds.model.AddFundsModel
import com.tangem.features.commonfeatures.impl.addtoportfolio.TokenActionsComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultAddFundsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: AddFundsComponent.Params,
    chooseTokenComponentFactory: ChooseTokenComponent.Factory,
    tokenActionsComponentFactory: TokenActionsComponent.Factory,
) : AppComponentContext by appComponentContext, AddFundsComponent {

    private val model: AddFundsModel = getOrCreateModel(params)

    private val chooseTokenComponent: ChooseTokenComponent = chooseTokenComponentFactory.create(
        context = child(key = "addFundsChooseToken"),
        params = ChooseTokenComponent.Params(bridge = model.chooseTokenBridge),
    )

    private val tokenActionsComponent: TokenActionsComponent = tokenActionsComponentFactory.create(
        context = child(key = "addFundsTokenActions"),
        params = TokenActionsComponent.Params(
            data = model.tokenActionsData,
            callbacks = model,
            bottomAction = TokenActionsComponent.BottomAction.GoToToken,
            isRedesignForced = true,
        ),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        chooseTokenComponent.Content(modifier)
        val isTokenActionsShown by model.isTokenActionsShown.collectAsStateWithLifecycle()
        if (isTokenActionsShown) {
            // force use redesign theme here according to the task requirements, will be reworked in the next release
            TangemThemeRedesign {
                TangemModalBottomSheet<TangemBottomSheetConfigContent.Empty>(
                    config = TangemBottomSheetConfig(
                        isShown = true,
                        onDismissRequest = model::onTokenActionsDismiss,
                        content = TangemBottomSheetConfigContent.Empty,
                    ),
                    containerColor = TangemTheme.colors2.surface.level2,
                    scrollableContent = true,
                    title = {
                        TangemModalBottomSheetTitle(
                            modifier = Modifier.fillMaxWidth(),
                            title = resourceReference(R.string.common_get_token),
                            endIconRes = R.drawable.ic_close_24,
                            onEndClick = model::onTokenActionsDismiss,
                        )
                    },
                    content = { _ ->
                        Column(
                            modifier = Modifier.padding(
                                start = TangemTheme.dimens2.x4,
                                top = TangemTheme.dimens2.x2,
                                end = TangemTheme.dimens2.x4,
                                bottom = TangemTheme.dimens2.x4,
                            ),
                        ) {
                            tokenActionsComponent.Content(Modifier)
                        }
                    },
                )
            }
        }
    }

    @AssistedFactory
    interface Factory : AddFundsComponent.Factory {
        override fun create(context: AppComponentContext, params: AddFundsComponent.Params): DefaultAddFundsComponent
    }
}