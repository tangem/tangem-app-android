package com.tangem.card_android.android.nfc

import android.content.Context
import android.os.Build
import android.view.View
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.Transformation
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.widget.LinearLayoutCompat

class NfcDeviceAntennaLocation(private var context: Context,
                               private var ivHandCardHorizontal: ImageView,
                               private var ivHandCardVertical: ImageView,
                               private var llHand: LinearLayoutCompat,
                               private var llNfc: LinearLayoutCompat) {

    companion object {
        const val CARD_ON_BACK = 0
        const val CARD_ON_FRONT = 1
        const val CARD_ORIENTATION_HORIZONTAL = 0
        const val CARD_ORIENTATION_VERTICAL = 1
    }

    var orientation: Int = 0
    var fullName: String = ""
    var x: Float = 0.toFloat()
    var y: Float = 0.toFloat()
    var z: Int = 0

    fun init() {
        getAntennaLocation()
        setCardOrientation()
        animate()
    }

    fun animate() {
        val lp = llHand.layoutParams as RelativeLayout.LayoutParams
        val lp2 = llNfc.layoutParams as RelativeLayout.LayoutParams
        val dp = context.resources.displayMetrics.density
        val lm = dp * (69 + x * 75)
        lp.topMargin = (dp * (-100 + y * 250)).toInt()
        lp2.topMargin = (dp * (-125 + y * 250)).toInt()
        llNfc.layoutParams = lp2

        val a = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                lp.leftMargin = (lm * interpolatedTime).toInt()
                llHand.layoutParams = lp
            }
        }
        a.duration = 2000
        a.interpolator = DecelerateInterpolator()
        llHand.startAnimation(a)
    }

    private fun getAntennaLocation() {
        val codename = Build.DEVICE

        // default values
        this.orientation = 0
        this.fullName = ""
        this.x = 0.5f
        this.y = 0.35f
        this.z = 0

        for (nfcLocation in NfcLocation.values()) {
            if (codename.startsWith(nfcLocation.codename)) {
                this.fullName = nfcLocation.fullName
                this.orientation = nfcLocation.orientation
                this.x = nfcLocation.x / 100f
                this.y = nfcLocation.y / 100f
                this.z = nfcLocation.z
            }
        }
    }

    private fun setCardOrientation() {
        when (orientation) {
            CARD_ORIENTATION_HORIZONTAL -> {
                ivHandCardHorizontal.visibility = View.VISIBLE
                ivHandCardVertical.visibility = View.GONE
            }

            CARD_ORIENTATION_VERTICAL -> {
                ivHandCardVertical.visibility = View.VISIBLE
                ivHandCardHorizontal.visibility = View.GONE
            }
        }

        // set card z position
        when (z) {
            NfcDeviceAntennaLocation.CARD_ON_BACK -> llHand.elevation = 0.0f
            NfcDeviceAntennaLocation.CARD_ON_FRONT -> llHand.elevation = 30.0f
        }
    }

}