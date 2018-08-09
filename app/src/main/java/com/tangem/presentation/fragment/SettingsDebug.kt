package com.tangem.presentation.fragment

import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.SwitchPreference
import com.tangem.wallet.R

class SettingsDebug : PreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.fr_settings_debug)

        val debug_new_request_verify = findPreference(getString(R.string.key_debug_new_request_verify)) as SwitchPreference
        val debug_new_request_verify_show_json = findPreference(getString(R.string.key_debug_new_request_verify_show_json)) as SwitchPreference

        debug_new_request_verify.title = "Новый запрос verify card"
        debug_new_request_verify_show_json.title = "Показать ответ"

    }

}