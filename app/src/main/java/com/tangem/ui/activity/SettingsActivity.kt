package com.tangem.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.tangem.wallet.R

class SettingsActivity : AppCompatActivity() {
    companion object {
        fun callingIntent(context: Context) = Intent(context, SettingsActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initToolbar()
    }

    private fun initToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setTitle(R.string.settings)
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_close_clear_cancel)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

}