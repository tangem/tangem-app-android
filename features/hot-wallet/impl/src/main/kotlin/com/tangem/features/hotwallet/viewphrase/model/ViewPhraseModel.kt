package com.tangem.features.hotwallet.viewphrase.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.grid.entity.SeedPhraseGridItem
import com.tangem.features.hotwallet.ViewPhraseComponent
import com.tangem.features.hotwallet.viewphrase.entity.ViewPhraseUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@Stable
@ModelScoped
internal class ViewPhraseModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
) : Model() {

    private val params = paramsContainer.require<ViewPhraseComponent.Params>()

    internal val uiState: StateFlow<ViewPhraseUM>
    field = MutableStateFlow(
        ViewPhraseUM(
            onBackClick = { router.pop() },
        ),
    )

    init {
        uiState.update {
            it.copy(
                words = params.words.mapIndexed { index, s ->
                    SeedPhraseGridItem(index + 1, s)
                }.toImmutableList(),
            )
        }
    }
}