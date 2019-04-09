package com.tangem.ui.fragment

import android.os.Bundle
import android.preference.PreferenceFragment

import com.tangem.wallet.R

class Settings : PreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.pref_main)
    }

}
