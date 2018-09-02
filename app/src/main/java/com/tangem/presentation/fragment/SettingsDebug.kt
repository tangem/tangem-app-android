package com.tangem.presentation.fragment

import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.SwitchPreference
import com.tangem.wallet.R

class SettingsDebug : PreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.fr_settings_debug)

    }

}