package com.tangem.tap.features.tokens.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionInflater
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.tokens.redux.TokensAction
import com.tangem.tap.features.tokens.redux.TokensState
import com.tangem.tap.features.tokens.ui.adapters.CurrenciesAdapter
import com.tangem.tap.mainScope
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_add_tokens.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import org.rekotlin.StoreSubscriber


class AddTokensFragment : Fragment(R.layout.fragment_add_tokens),
        StoreSubscriber<TokensState> {

    private lateinit var viewAdapter: CurrenciesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                store.dispatch(NavigationAction.PopBackTo())
            }
        })
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_right)
        exitTransition = inflater.inflateTransition(R.transition.fade)
    }

    override fun onStart() {
        super.onStart()
        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.tokensState == newState.tokensState
            }.select { it.tokensState }
        }
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { activity?.onBackPressed() }
        setupPopularTokensRecyclerView()
        btn_tokens_save_changes.setOnClickListener {
            store.dispatch(TokensAction.SaveChanges(viewAdapter.getAddedItems()))
        }
    }

    private fun setupPopularTokensRecyclerView() {
        viewAdapter = CurrenciesAdapter()
        viewAdapter.setOnItemAddListener {
            btn_tokens_save_changes.isEnabled = viewAdapter.getAddedItems().isNotEmpty()
        }

        rv_popular_tokens.layoutManager = LinearLayoutManager(context)
        rv_popular_tokens.adapter = viewAdapter
    }

    override fun newState(state: TokensState) {
        if (activity == null) return

        viewAdapter.addedCurrencies = state.addedCurrencies
        viewAdapter.submitUnfilteredList(state.shownCurrencies)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.popular_tokens, menu)

        val menuItem = menu.findItem(R.id.menu_search)
        val searchView: SearchView = menuItem.actionView as SearchView
        searchView.queryHint = "Type here to search"
        searchView.maxWidth = android.R.attr.width
        searchView.inputtedTextAsFlow()
                .debounce(400)
                .distinctUntilChanged()
                .onEach {
                    viewAdapter.filter(searchView.query)
                }
                .launchIn(mainScope)
        return super.onCreateOptionsMenu(menu, inflater);
    }

    override fun onDestroy() {
        store.dispatch(TokensAction.ResetState)
        super.onDestroy()
    }
}

fun SearchView.inputtedTextAsFlow(): Flow<String> = callbackFlow {
    val watcher = setOnQueryTextListener(object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            return false
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            trySend(newText ?: "")
            return false
        }
    })
    awaitClose { (watcher) }
}