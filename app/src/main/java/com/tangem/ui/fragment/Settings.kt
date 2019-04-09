package com.tangem.ui.fragment

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.tangem.wallet.R

class Settings : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_main)
    }

}