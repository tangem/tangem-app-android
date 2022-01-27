package com.tangem.tap.common.leapfrogWidget

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.tangem.tangem_sdk_new.ui.widget.leapfrogWidget.LeapfrogWidget
import com.tangem.tangem_sdk_new.ui.widget.leapfrogWidget.LeapfrogWidgetState
import com.tangem.tap.domain.twins.TwinsCardWidget
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.test_leapfrog_fragment.*

class TestLeapfrogFragment : Fragment(R.layout.test_leapfrog_fragment) {

    private lateinit var twinsCardWidget: TwinsCardWidget

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inflater = TransitionInflater.from(requireContext())
        exitTransition = inflater.inflateTransition(R.transition.fade)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val leapfrogContainer: FrameLayout = view.findViewById(R.id.leapfrog_views_container)
        val leapfrog = LeapfrogWidget(leapfrogContainer)
        twinsCardWidget = TwinsCardWidget(leapfrog) { 200f }

        btn_twin_welcome.setOnClickListener {
            twinsCardWidget.toWelcome()
        }
        btn_twin_to_leapfrog.setOnClickListener {
            twinsCardWidget.toLeapfrog()
        }
        btn_twin_activate.setOnClickListener {
            twinsCardWidget.toActivate()
        }


        btn_lp_init.setOnClickListener {
            leapfrog.initViews()
        }
        btn_lp_unfold.setOnClickListener {
            leapfrog.unfold()
        }
        btn_lp_fold.setOnClickListener {
            leapfrog.fold()
        }
        btn_lp_leap.setOnClickListener {
            leapfrog.leap()
        }
        btn_lp_leap_back.setOnClickListener {
            leapfrog.leapBack()
        }
    }

    override fun onStart() {
        super.onStart()
        leapfrogWidgetState?.let { twinsCardWidget.leapfrogWidget.applyState(it) }
    }

    override fun onStop() {
        super.onStop()
        leapfrogWidgetState = twinsCardWidget.leapfrogWidget.getState()
    }
}

private var leapfrogWidgetState: LeapfrogWidgetState? = null