package com.tangem.presentation.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import com.tangem.wallet.BuildConfig;
import com.tangem.wallet.R;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class LogoActivity extends AppCompatActivity {
    public static final String TAG = LogoActivity.class.getSimpleName();

    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private ImageView imgLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logo);

        imgLogo = findViewById(R.id.imgLogo);

        // set up the user interaction to manually show or hide the system UI.
        imgLogo.setOnClickListener(view -> hide());
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // trigger the initial hide() shortly after the activity has been created, to briefly hint to the user that UI controls are available.
        TextView AppVersion = findViewById(R.id.AppVersion);

        if (BuildConfig.DEBUG)
            AppVersion.setText("BETA v." + BuildConfig.VERSION_NAME + "\n" + "dev" + "\n" + "build " + BuildConfig.VERSION_CODE);
        else
            AppVersion.setText("BETA v." + BuildConfig.VERSION_NAME);

        if (!getIntent().getBooleanExtra("skipAutoHide", false)) {
            delayedHide(1000);
        }
    }

    private void hide() {
        Intent intent = new Intent(getBaseContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        //imgLogo.removeCallbacks(mHideRunnable);
        imgLogo.postDelayed(mHideRunnable, delayMillis);
    }
}
