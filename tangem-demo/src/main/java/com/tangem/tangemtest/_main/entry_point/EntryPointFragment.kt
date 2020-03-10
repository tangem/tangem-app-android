package com.tangem.tangemtest._main.entry_point

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tangem.tangemtest.R
import com.tangem.tangemtest.card_use_cases.CardContext
import com.tangem.tangemtest.card_use_cases.actions.Action
import com.tangem.tangemtest.commons.NavigateAction
import com.tangem.tangemtest.commons.getDefaultNavigationOptions
import com.tangem.tangemtest.commons.getDiManager
import kotlinx.android.synthetic.main.fg_entry_point.*

/**
[REDACTED_AUTHOR]
 */
class EntryPointFragment : Fragment() {

    private val navController: NavController by lazy { findNavController() }
    private lateinit var rvActions: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fg_entry_point, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = activity ?: throw Exception("Fragment isn't associated with activity ")

        val cardContext = getDiManager().getCardContext().init(activity)
        initRecyclerView(cardContext)
    }

    private fun initRecyclerView(cardContext: CardContext) {
        rvActions = rv_actions
        rvActions.layoutManager = LinearLayoutManager(activity)

        val adapter = RvActionsAdapter(cardContext) { position, destinationId -> navigate(destinationId as Int) }
        adapter.setItems(mutableListOf(
                NavigateAction(Action.Scan(), R.id.nav_scan),
                NavigateAction(Action.Sign(), R.id.nav_sign)
        ))
        rvActions.adapter = adapter
    }

    override fun onResume() {
        super.onResume()

        rvActions.adapter?.notifyDataSetChanged()
    }

    private fun navigate(destinationId: Int) {
        navController.navigate(destinationId, null, getDefaultNavigationOptions())
    }
}