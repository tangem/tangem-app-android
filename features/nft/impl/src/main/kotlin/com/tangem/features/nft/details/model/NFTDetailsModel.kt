package com.tangem.features.nft.details.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.features.nft.component.NFTDetailsComponent
import com.tangem.features.nft.details.entity.NFTDetailsUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@ModelScoped
internal class NFTDetailsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    paramsContainer: ParamsContainer,
) : Model() {

    val state: StateFlow<NFTDetailsUM> get() = _state

    private val _state = MutableStateFlow(
        value = NFTDetailsUM(
            onBackClick = ::navigateBack,
        ),
    )

    @Suppress("UnusedPrivateMember")
    private val params: NFTDetailsComponent.Params = paramsContainer.require()

    private fun navigateBack() {
        router.pop()
    }
}