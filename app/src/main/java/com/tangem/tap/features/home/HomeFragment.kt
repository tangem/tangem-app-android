package com.tangem.tap.features.home

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.tangem.tap.common.extensions.safeStartActivity
import com.tangem.tap.common.extensions.toast
import com.tangem.tap.common.redux.global.StateDialog
import com.tangem.tap.common.redux.navigation.DetailsTransition
import com.tangem.tap.common.redux.navigation.FragmentShareTransition
import com.tangem.tap.common.redux.navigation.ShareElement
import com.tangem.tap.common.toggleWidget.IndeterminateProgressButtonWidget
import com.tangem.tap.common.toggleWidget.ViewStateWidget
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.features.home.redux.HomeDialog
import com.tangem.tap.features.home.redux.HomeState
import com.tangem.tap.features.wallet.ui.dialogs.ScanFailsDialog
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_home.*
import org.rekotlin.StoreSubscriber
import java.lang.ref.WeakReference

class HomeFragment : Fragment(R.layout.fragment_home), StoreSubscriber<HomeState> {

    lateinit var btnScanCard: ViewStateWidget

    private var dialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inflater = TransitionInflater.from(requireContext())
        exitTransition = inflater.inflateTransition(R.transition.fade)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val transitionViews = mutableListOf<ShareElement>()
        transitionViews.add(ShareElement(WeakReference(imv_front_card), "imv_front_card"))
        val shareTransition = FragmentShareTransition(transitionViews, DetailsTransition(), DetailsTransition())

        store.dispatch(HomeAction.SetFragmentShareTransition(shareTransition))
        store.dispatch(HomeAction.Init)

        btn_scan_card?.setOnClickListener { store.dispatch(HomeAction.ReadCard) }
        btn_get_new_card?.setOnClickListener { store.dispatch(HomeAction.GoToShop) }

        btnScanCard = IndeterminateProgressButtonWidget(btn_scan_card, progress)
    }

    override fun onStart() {
        super.onStart()
        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.homeState == newState.homeState
            }.select { it.homeState }
        }
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
    }


    override fun newState(state: HomeState) {
        if (activity == null) return

        handleOpenUrl(state)
        handleDialog(state.dialog)

        btnScanCard.changeState(state.btnScanState.progressState)
        btnScanCard.mainView.isEnabled = state.btnScanState.enabled
    }

    private fun handleOpenUrl(state: HomeState) {
        if (state.openUrl == null) return
        val context = context ?: return

        val uri = Uri.parse(state.openUrl)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.safeStartActivity(intent, null, { toast(it.toString()) }, {
            store.dispatch(HomeAction.SetOpenUrl(null))
        })
    }

    private fun handleDialog(stateDialog: StateDialog?) {
        when (stateDialog) {
            is HomeDialog.ScanFailsDialog -> {
                if (dialog == null) dialog = ScanFailsDialog.create(requireContext()).apply { show() }
            }
            else -> {
                dialog?.dismiss()
                dialog = null
            }
        }
    }

    override fun onDestroyView() {
        store.dispatch(HomeAction.SetFragmentShareTransition(null))
        super.onDestroyView()
    }
}