package com.tangem.tap.common.toggleWidget

import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.widget.ViewSwitcher
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
class RefreshBalanceWidget(
    private val root: ViewGroup
) : ViewStateWidget {

    var isShowing: Boolean? = null
        private set

    override val mainView: View = root.findViewById(R.id.btn_refresh_balance)


    private val viewSwitcher: ViewSwitcher by lazy { mainView.findViewById(R.id.switcher_refresh_arrow_to_progress) }

    private val arrowView = mainView.findViewById<View>(R.id.imv_arrow_refresh)

    private var progressViewAnimation: Animation? = null

    init {
        viewSwitcher.inAnimation = AlphaAnimation(0f, 1f).apply {
            duration = 400
            interpolator = LinearInterpolator()
        }
        viewSwitcher.outAnimation = AlphaAnimation(1f, 0f).apply {
            duration = 400
            interpolator = LinearInterpolator()
        }
    }

    override fun changeState(state: WidgetState) {
        val progressState = state as? ProgressState ?: return

        val currentStateByView = getState()
        when {
            currentStateByView == progressState -> return
            currentStateByView == ProgressState.Done -> if (progressState == ProgressState.Error) return
        }

        animateState(progressState)
        viewSwitcher.showNext()
    }

    private fun animateState(state: ProgressState) {
        when (state) {
            ProgressState.Done, ProgressState.Error -> {
                progressViewAnimation?.cancel()
                progressViewAnimation = null
            }
            ProgressState.Loading -> {
                progressViewAnimation = RotateAnimation(
                    0f, 360f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
                )
                progressViewAnimation?.duration = 700
                progressViewAnimation?.interpolator = AccelerateInterpolator()
                progressViewAnimation?.repeatCount = -1
                progressViewAnimation?.let { arrowView.startAnimation(it) }
            }
        }
    }

    fun show(show: Boolean) {
        if (show == isShowing) return

        isShowing = show
        if (show) {
            mainView.startAnimation(ShowAnimation())
        } else {
            mainView.startAnimation(AlphaAnimation(1f, 0f).apply { fillAfter = true })
        }
    }

    private fun isArrowRefreshActive(): Boolean = viewSwitcher.currentView.id == R.id.fl_arrow_refresh_container

    private fun getState(): ProgressState = if (isArrowRefreshActive()) ProgressState.Done else ProgressState.Loading
}

class ShowAnimation : AnimationSet(true) {
    init {
        addAnimation(ScaleAnimation(
            0f, 1f,
            0f, 1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f)
        )
        addAnimation(AlphaAnimation(0f, 1f))
        duration = 300
        interpolator = AnticipateOvershootInterpolator()
    }
}