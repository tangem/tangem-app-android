package com.tangem.features.details.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.details.component.UserWalletListComponent
import com.tangem.features.details.entity.UserWalletListUM
import com.tangem.features.details.impl.R
import com.tangem.features.details.model.UserWalletListModel
import com.tangem.features.details.ui.UserWalletListBlock
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update

internal class DefaultUserWalletListComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
) : UserWalletListComponent, AppComponentContext by context {

    private val model: UserWalletListModel = getOrCreateModel()
    private val state: MutableStateFlow<UserWalletListUM> = MutableStateFlow(
        value = UserWalletListUM(
            userWallets = persistentListOf(),
            isWalletSavingInProgress = false,
            addNewWalletText = TextReference.EMPTY,
            onAddNewWalletClick = model::addUserWallet,
        ),
    )

    init {
        combine(
            model.userWallets,
            model.shouldSaveUserWallets,
            model.isWalletSavingInProgress,
            transform = ::updateState,
        ).launchIn(componentScope)
    }

    private suspend fun updateState(
        userWallets: ImmutableList<UserWalletListUM.UserWalletUM>,
        shouldSaveUserWallets: Boolean,
        isWalletSavingInProgress: Boolean,
    ) = state.update { value ->
        value.copy(
            userWallets = userWallets,
            isWalletSavingInProgress = isWalletSavingInProgress,
            addNewWalletText = if (shouldSaveUserWallets) {
                resourceReference(R.string.user_wallet_list_add_button)
            } else {
                resourceReference(R.string.scan_card_settings_button)
            },
        )
    }

    @Composable
    override fun View(modifier: Modifier) {
        val state by state.collectAsStateWithLifecycle()

        UserWalletListBlock(
            modifier = modifier,
            state = state,
        )
    }

    @AssistedFactory
    interface Factory : UserWalletListComponent.Factory {

        override fun create(context: AppComponentContext): DefaultUserWalletListComponent
    }
}
