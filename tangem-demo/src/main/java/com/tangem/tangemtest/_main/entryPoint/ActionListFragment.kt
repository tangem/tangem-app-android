package com.tangem.tangemtest._main.entryPoint

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tangem.tangemtest.R
import com.tangem.tangemtest._main.MainViewModel
import com.tangem.tangemtest.extensions.view.beginDelayedTransition
import com.tangem.tangemtest.ucase.getDefaultNavigationOptions
import com.tangem.tangemtest.ucase.resources.ActionType
import com.tangem.tangemtest.ucase.resources.MainResourceHolder
import com.tangem.tangemtest.ucase.ui.BaseFragment
import kotlinx.android.synthetic.main.fg_entry_point.*

/**
[REDACTED_AUTHOR]
 */
class ActionListFragment : BaseFragment() {

    private lateinit var rvActions: RecyclerView

    private val mainActivityVM: MainViewModel by activityViewModels()

    override fun getLayoutId(): Int = R.layout.fg_entry_point

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

        rvActions.adapter = RvActionsAdapter(vhDataWrapper) { type, position, data ->
            navigateTo(data, options = getDefaultNavigationOptions())
        }
        mainActivityVM.ldDescriptionSwitch.observe(viewLifecycleOwner, Observer {
            vhDataWrapper.descriptionIsVisible = it
            rvActions.beginDelayedTransition()
            rvActions.adapter?.notifyDataSetChanged()
        })
    }

    override fun onResume() {
        super.onResume()
        val adapter: RvActionsAdapter = rvActions.adapter as? RvActionsAdapter ?: return

        adapter.setItemList(getNavigateOptions())
        adapter.notifyDataSetChanged()
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