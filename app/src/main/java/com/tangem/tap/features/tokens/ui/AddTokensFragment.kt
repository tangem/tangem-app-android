package com.tangem.tap.features.tokens.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.transition.TransitionInflater
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.accompanist.appcompattheme.AppCompatTheme
import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.common.analytics.Analytics
import com.tangem.tap.common.analytics.events.ManageTokens
import com.tangem.tap.common.extensions.copyToClipboard
import com.tangem.tap.common.extensions.dispatchNotification
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.getString
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.BaseFragment
import com.tangem.tap.features.addBackPressHandler
import com.tangem.tap.features.tokens.redux.ContractAddress
import com.tangem.tap.features.tokens.redux.TokenWithBlockchain
import com.tangem.tap.features.tokens.redux.TokensAction
import com.tangem.tap.features.tokens.redux.TokensState
import com.tangem.tap.features.tokens.ui.compose.CurrenciesScreen
import com.tangem.tap.mainScope
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.FragmentAddTokensBinding
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.rekotlin.StoreSubscriber

class AddTokensFragment : BaseFragment(R.layout.fragment_add_tokens), StoreSubscriber<TokensState> {

    private val binding: FragmentAddTokensBinding by viewBinding(FragmentAddTokensBinding::bind)
    private var tokensState: MutableState<TokensState> = mutableStateOf(store.state.tokensState)
    private var searchInput = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        Analytics.send(ManageTokens.ScreenOpened())
        addBackPressHandler(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(binding) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { activity?.onBackPressed() }

        val onSaveChanges = { tokens: List<TokenWithBlockchain>, blockchains: List<Blockchain> ->
            Analytics.send(ManageTokens.ButtonSaveChanges())
            store.dispatch(TokensAction.SaveChanges(tokens, blockchains))
        }
        val onNetworkItemClicked = { contractAddress: ContractAddress ->
            context?.copyToClipboard(contractAddress)
            store.dispatchNotification(R.string.contract_address_copied_message)
        }

        val onLoadMore = {
            store.dispatch(TokensAction.LoadMore(scanResponse = store.state.globalState.scanResponse))
        }

        cvCurrencies.setContent {
            AppCompatTheme {
                CurrenciesScreen(
                    tokensState = tokensState,
                    onSaveChanges = onSaveChanges,
                    onNetworkItemClicked = onNetworkItemClicked,
                    onLoadMore = onLoadMore,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        store.subscribe(this) { state ->
            state
                .skipRepeats { oldState, newState -> oldState.tokensState == newState.tokensState }
                .select { it.tokensState }
        }
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
    }

    override fun onDestroy() {
        store.dispatch(TokensAction.ResetState)
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_popular_tokens, menu)

        val addCustomTokenAllowed = store.state.tokensState.allowToAdd
        menu.findItem(R.id.menu_navigate_add_custom_token).isVisible = addCustomTokenAllowed

        val menuItem = menu.findItem(R.id.menu_search)
        val searchView: SearchView = menuItem.actionView as SearchView
        searchView.queryHint = searchView.getString(R.string.common_search)
        searchView.maxWidth = android.R.attr.width
        searchView.inputtedTextAsFlow()
            .debounce(800)
            .distinctUntilChanged()
            .onEach {
                searchInput.value = it.lowercase()
                dispatchOnMain(TokensAction.SetSearchInput(searchInput.value))
            }
            .launchIn(mainScope)
        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search -> true
            R.id.menu_navigate_add_custom_token -> {
                Analytics.send(ManageTokens.ButtonCustomToken())
                store.dispatch(TokensAction.PrepareAndNavigateToAddCustomToken)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun configureTransitions() {
        super.configureTransitions()
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_right)
        exitTransition = inflater.inflateTransition(R.transition.fade)
    }

    override fun handleOnBackPressed() {
        super.handleOnBackPressed()
        store.dispatch(NavigationAction.PopBackTo())
        store.dispatch(TokensAction.ResetState)
    }

    override fun newState(state: TokensState) {
        if (activity == null || view == null) return
        val toolbarTitle =
            if (state.allowToAdd) R.string.main_manage_tokens else R.string.search_tokens_title
        tokensState.value = state
        binding.toolbar.title = getString(toolbarTitle)
    }
}

fun SearchView.inputtedTextAsFlow(): Flow<String> = callbackFlow {
    val watcher = setOnQueryTextListener(
        object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                trySend(newText ?: "")
                return false
            }
        },
    )
    awaitClose { (watcher) }
}
