package com.tangem.tangemtest._main.entryPoint

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.tangem.tangemtest.R
import com.tangem.tangemtest._main.MainViewModel
import com.tangem.tangemtest.cardUseCase.resources.ActionType
import com.tangem.tangemtest.cardUseCase.resources.MainResourceHolder
import com.tangem.tangemtest.commons.getDefaultNavigationOptions
import kotlinx.android.synthetic.main.fg_entry_point.*

/**
[REDACTED_AUTHOR]
 */
class ActionListFragment : Fragment() {

    private val navController: NavController by lazy { findNavController() }
    private lateinit var rvActions: RecyclerView

    private val mainActivityVM: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fg_entry_point, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
    }

    private fun initRecyclerView() {
        val layoutManager = LinearLayoutManager(activity)
        rvActions = rv_actions
        rvActions.layoutManager = layoutManager
        rvActions.addItemDecoration(DividerItemDecoration(activity, layoutManager.orientation))

        val vhDataWrapper = VhExDataWrapper(MainResourceHolder, false)

        rvActions.adapter = RvActionsAdapter(vhDataWrapper) { type, position, data -> navigate(data) }
        mainActivityVM.ldDescriptionSwitch.observe(viewLifecycleOwner, Observer {
            vhDataWrapper.descriptionIsVisible = it
            TransitionManager.beginDelayedTransition(rvActions as ViewGroup, AutoTransition())
            rvActions.adapter?.notifyDataSetChanged()
        })
    }

    override fun onResume() {
        super.onResume()
        val adapter: RvActionsAdapter = rvActions.adapter as? RvActionsAdapter ?: return

        adapter.setItemList(getNavigateOptions())
        adapter.notifyDataSetChanged()
    }

    private fun navigate(destinationId: Int) {
        navController.navigate(destinationId, null, getDefaultNavigationOptions())
    }

    private fun getNavigateOptions(): MutableList<ActionType> {
        return mutableListOf(
                ActionType.Scan,
                ActionType.Sign,
                ActionType.Personalize,
                ActionType.Depersonalize
//                ActionType.CreateWallet,
//                ActionType.PurgeWallet,
//                ActionType.ReadIssuerData,
//                ActionType.WriteIssuerData,
//                ActionType.ReadIssuerExData,
//                ActionType.WriteIssuerExData,
//                ActionType.ReadUserData,
//                ActionType.WriteUserData
        )
    }
}