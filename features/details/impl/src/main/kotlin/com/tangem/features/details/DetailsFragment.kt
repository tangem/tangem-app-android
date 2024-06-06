package com.tangem.features.details

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import com.arkivanov.decompose.defaultComponentContext
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.di.RootAppComponentContext
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.message.EventMessageEffect
import com.tangem.core.ui.message.EventMessageHandler
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.details.component.DetailsComponent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// TODO: Remove after [REDACTED_JIRA]
@AndroidEntryPoint
internal class DetailsFragment : ComposeFragment() {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    @Inject
    internal lateinit var componentFactory: DetailsComponent.Factory

    @Inject
    internal lateinit var detailsRouter: DetailsRouter

    @Inject
    @RootAppComponentContext
    internal lateinit var rootContext: AppComponentContext

    private val component: DetailsComponent by lazy { initComponent() }

    private val messageHandler = EventMessageHandler()

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        component.Content(modifier = modifier)

        EventMessageEffect(
            messageHandler = messageHandler,
            snackbarHostState = component.snackbarHostState,
        )
    }

    private fun initComponent(): DetailsComponent {
        val selectedUserWalletId = arguments?.getString(DetailsEntryPoint.USER_WALLET_ID_KEY)
            ?.let(::UserWalletId)

        requireNotNull(selectedUserWalletId) { "UserWalletId must be provided" }

        val context = rootContext.childByContext(
            componentContext = defaultComponentContext(requireActivity().onBackPressedDispatcher),
            messageHandler = messageHandler,
            router = detailsRouter,
        )

        return componentFactory.create(
            context = context,
            params = DetailsComponent.Params(
                selectedUserWalletId = selectedUserWalletId,
            ),
        )
    }

    companion object : DetailsEntryPoint {

        override fun entryFragment(): Fragment = DetailsFragment()
    }
}