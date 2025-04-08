package com.tangem.features.nft.traits.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.features.nft.component.NFTAssetTraitsComponent
import com.tangem.features.nft.traits.entity.NFTAssetTraitUM
import com.tangem.features.nft.traits.entity.NFTAssetTraitsUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@ModelScoped
internal class NFTAssetTraitsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: NFTAssetTraitsComponent.Params = paramsContainer.require()

    val state: StateFlow<NFTAssetTraitsUM> get() = _state

    private val _state = MutableStateFlow(
        value = NFTAssetTraitsUM(
            traits = params.nftAsset.transform(),
            onBackClick = ::navigateBack,
        ),
    )

    private fun NFTAsset.transform(): ImmutableList<NFTAssetTraitUM> = this
        .traits
        .mapIndexed { index, trait ->
            NFTAssetTraitUM(
                id = index.toString(),
                name = trait.name,
                value = trait.value,
            )
        }.toPersistentList()

    private fun navigateBack() {
        router.pop()
    }
}