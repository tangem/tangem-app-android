package com.tangem.presentation.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.scottyab.rootbeer.RootBeer;
import com.skyfishjy.library.RippleBackground;
import com.tangem.domain.wallet.DeviceNFCAntennaLocation;
import com.tangem.domain.wallet.LastSignStorage;
import com.tangem.domain.wallet.Logger;
import com.tangem.domain.wallet.PINStorage;
import com.tangem.presentation.dialog.RootFoundDialog;
import com.tangem.presentation.fragment.Main;
import com.tangem.util.CommonUtil;
import com.tangem.util.PhoneUtility;
import com.tangem.wallet.BuildConfig;
import com.tangem.wallet.R;

import java.io.File;

public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
    public static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_CODE_SEND_EMAIL = 2;

    private File zipFile = null;
    private NfcAdapter.ReaderCallback onNFCReaderCallback;
    private OnCardsClean onCardsClean;
    private FloatingActionButton fab;
    private LinearLayout llTapPrompt;

    public interface OnCardsClean {
        void doClean();
    }

    public void setOnCardsClean(OnCardsClean onCardsClean) {
        this.onCardsClean = onCardsClean;
    }

    public void setNfcAdapterReaderCallback(NfcAdapter.ReaderCallback callback) {
        this.onNFCReaderCallback = callback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tvNFCHint = findViewById(R.id.tvNFCHint);
        llTapPrompt = findViewById(R.id.llTapPrompt);
        LinearLayout hand = findViewById(R.id.llHand);
        LinearLayout nfc = findViewById(R.id.llNFC);
        RippleBackground rippleBackground = findViewById(R.id.imNFC);
        fab = findViewById(R.id.fab);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        commonInit(getApplicationContext());

        rippleBackground.startRippleAnimation();

//        tvNFCHint.setText("Scan a banknote with your\n" + PhoneUtility.GetPhoneName() + "\nas shown above");
        tvNFCHint.setText(R.string.scan_banknote);

        DeviceNFCAntennaLocation antenna = new DeviceNFCAntennaLocation();
        antenna.getAntennaLocation();

        // animate
        final RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) hand.getLayoutParams();
        final RelativeLayout.LayoutParams lp2 = (RelativeLayout.LayoutParams) nfc.getLayoutParams();
        final float dp = getResources().getDisplayMetrics().density;
        final float lm = dp * (69 + antenna.X * 75);
        lp.topMargin = (int) (dp * (-100 + antenna.Y * 250));
        lp2.topMargin = (int) (dp * (-125 + antenna.Y * 250));
        nfc.setLayoutParams(lp2);
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                lp.leftMargin = (int) (lm * interpolatedTime);
                hand.setLayoutParams(lp);
            }
        };
        a.setDuration(2000);
        a.setInterpolator(new DecelerateInterpolator());
        hand.startAnimation(a);

        // set listeners
        fab.setOnClickListener(this::showMenu);

        // add fragment
        Main main = (Main) getSupportFragmentManager().findFragmentById(R.id.fragmentMain);
        if (main.getCardListAdapter().getItemCount() > 0)
            showCleanButton();
        else
            hideCleanButton();

        // NFC
        Intent intent = getIntent();
        if (intent != null && (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction()))) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null && onNFCReaderCallback != null) {
                onNFCReaderCallback.onTagDiscovered(tag);
            }
        }

        // check if device root
        RootBeer rootBeer = new RootBeer(this);
        if (rootBeer.isRootedWithoutBusyBoxCheck())
            new RootFoundDialog().show(getFragmentManager(), RootFoundDialog.TAG);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction()))) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null && onNFCReaderCallback != null) {
                onNFCReaderCallback.onTagDiscovered(tag);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        switch (keycode) {
            case KeyEvent.KEYCODE_MENU:
                fab.requestFocus();
                showMenu(fab);
                return true;
        }
        return super.onKeyDown(keycode, e);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (BuildConfig.DEBUG) {
            for (int i = 0; i < menu.size(); i++) menu.getItem(i).setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.sendLogs:
                File f = null;
                try {
                    f = Logger.collectLogs(this);
                    if (f != null) {
                        Log.e(TAG, String.format("Collect %d log bytes", f.length()));
                        CommonUtil.sendEmail(this, zipFile, TAG, "Logs", PhoneUtility.getDeviceInfo(), new File[]{f});
                    } else {
                        Log.e(TAG, "Can't create temporaly log file");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (f != null && f.exists()) {
                        f.delete();
                    }
                }
                return true;
            case R.id.managePIN:
                showSavePinActivity();
                return true;

            case R.id.managePIN2:
                showSavePin2Activity();
                return true;

            case R.id.cleanCards:
                if (onCardsClean != null) onCardsClean.doClean();
                hideCleanButton();
                return true;

            case R.id.about:
                showLogoActivity();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SEND_EMAIL) {
            if (zipFile != null) {
                zipFile.delete();
                zipFile = null;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static void commonInit(Context context) {
        if (PINStorage.needInit()) {
            PINStorage.Init(context);
        }
        if (LastSignStorage.needInit()) {
            LastSignStorage.Init(context);
        }
    }

    public void showCleanButton() {
        llTapPrompt.setVisibility(View.INVISIBLE);
    }

    public void hideCleanButton() {
        llTapPrompt.setVisibility(View.VISIBLE);
    }

    private void showLogoActivity() {
        Intent intent = new Intent(getBaseContext(), LogoActivity.class);
        intent.putExtra(LogoActivity.TAG, true);
        intent.putExtra(LogoActivity.EXTRA_AUTO_HIDE, false);
        startActivity(intent);
    }

    private void showSavePinActivity() {
        Intent intent = new Intent(getBaseContext(), SavePINActivity.class);
        intent.putExtra("PIN2", false);
        startActivity(intent);
    }

    private void showSavePin2Activity() {
        Intent intent = new Intent(getBaseContext(), SavePINActivity.class);
        intent.putExtra("PIN2", true);
        startActivity(intent);
    }

    private void showMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_main, popup.getMenu());

        if (BuildConfig.DEBUG) {
            popup.getMenu().findItem(R.id.managePIN).setEnabled(true);
            popup.getMenu().findItem(R.id.managePIN2).setEnabled(true);
            popup.getMenu().findItem(R.id.sendLogs).setVisible(true);
        }

        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

}