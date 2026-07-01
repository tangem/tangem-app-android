package com.tangem.features.foryou.impl

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_left_20
import com.tangem.features.foryou.ForYouComponent
import com.tangem.features.foryou.impl.model.ForYouModel
import com.tangem.features.foryou.impl.ui.ForYouContent
import com.tangem.features.promobanners.api.PromoBannersBlockComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultForYouComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Suppress("UnusedPrivateMember") @Assisted params: Unit,
    private val promoBannersBlockComponentFactory: PromoBannersBlockComponent.Factory,
) : AppComponentContext by context, ForYouComponent {

    private val model: ForYouModel = getOrCreateModel()

    private val promoBannersBlockComponent: PromoBannersBlockComponent by lazy {
        promoBannersBlockComponentFactory.create(
            context = child("promoBannersBlockComponent"),
            params = PromoBannersBlockComponent.Params(
                placeholder = PromoBannersBlockComponent.Placeholder.FEED,
                isInitiallyVisibleOnScreen = false,
            ),
        )
    }

    @Composable
    override fun Title(bottomSheetState: State<BottomSheetState>) {
        TangemTopBar(
            title = resourceReference(R.string.for_you_title),
            type = TangemTopBarType.BottomSheet,
            startContent = {
                Icon(
                    imageVector = Icons.ic_chevron_left_20,
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.primary,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .hazeEffectTangem { blurRadius = 8.dp }
                        .clickableSingle(
                            onClick = router::pop,
                            enabled = bottomSheetState.value == BottomSheetState.EXPANDED,
                        )
                        .padding(8.dp),
                )
            },
        )
    }

    @Composable
    override fun Content(
        bottomSheetState: State<BottomSheetState>,
        contentPadding: PaddingValues,
        modifier: Modifier,
    ) {
        val uiState by model.uiState.collectAsStateWithLifecycle()

        ForYouContent(
            forYouUM = uiState,
            bottomSheetState = bottomSheetState,
            promoBannersBlockComponent = promoBannersBlockComponent,
            contentPadding = contentPadding,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : ForYouComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultForYouComponent
    }
}