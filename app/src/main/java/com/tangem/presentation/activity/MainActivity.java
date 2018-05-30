package com.tangem.presentation.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
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
import com.tangem.LogFileProvider;
import com.tangem.domain.wallet.DeviceNFCAntennaLocation;
import com.tangem.domain.wallet.LastSignStorage;
import com.tangem.domain.wallet.Logger;
import com.tangem.domain.wallet.PINStorage;
import com.tangem.util.PhoneUtility;
import com.tangem.presentation.fragment.MainFragment;
import com.tangem.wallet.BuildConfig;
import com.tangem.wallet.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    public static final int DIALOG_ENABLE_INTERNET = 1;
    private static final int REQUEST_CODE_SEND_EMAIL = 2;
    private String logTag = "MainActivity";

    public interface OnCardsClean {
        void doClean();
    }

//    public interface OnCreateNFCDialog {
//        Dialog CreateNFCDialog(int id, AlertDialogWrapper.Builder builder, LayoutInflater li);
//    }

    OnCardsClean onCardsClean;
    //    OnCreateNFCDialog onCreateNFCDialog;
    NfcAdapter.ReaderCallback onNFCReaderCallback;
    FloatingActionButton fab;


    public void setOnCardsClean(OnCardsClean onCardsClean) {
        this.onCardsClean = onCardsClean;
    }

//    public void setOnCreateNFCDialog(OnCreateNFCDialog onCreateNFCDialog) {
//        this.onCreateNFCDialog = onCreateNFCDialog;
//    }

    public void setNfcAdapterReaderCallback(NfcAdapter.ReaderCallback callback) {
        this.onNFCReaderCallback = callback;
    }

    public void showCleanButton() {

        findViewById(R.id.tvTapPrompt).setVisibility(View.INVISIBLE);
    }

    public void hideCleanButton() {
        findViewById(R.id.tvTapPrompt).setVisibility(View.VISIBLE);
    }

    public static class RootFoundDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            return new AlertDialog.Builder(getActivity())
                    .setIcon(R.drawable.tangem_logo_small_new)
                    .setTitle("Your Android device is rooted. Security at risk!")
                    .setCancelable(false)
                    .setPositiveButton("Got it",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            }
                    )
                    .create();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RootBeer rootBeer = new RootBeer(this);
        if (rootBeer.isRootedWithoutBusyBoxCheck()) {
            //we found indication of root
            new RootFoundDialog().show(getFragmentManager(), "RootFoundDialog");
        }

        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        commonInit(getApplicationContext());

        TextView tvNFCHint = findViewById(R.id.tvNFCHint);
        if (tvNFCHint != null) {
//            tvNFCHint.setText("Scan a banknote with your\n" + PhoneUtility.GetPhoneName() + "\nas shown above");
            tvNFCHint.setText("Scan a banknote with your\n smartphone as shown above");
        }

        DeviceNFCAntennaLocation antenna = new DeviceNFCAntennaLocation();
        antenna.getAntennaLocation();
        final LinearLayout hand = findViewById(R.id.llHand);
        final LinearLayout nfc = findViewById(R.id.llNFC);
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
        a.setDuration(2000); // in ms
        a.setInterpolator(new DecelerateInterpolator());
        hand.startAnimation(a);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showMenu(view);
            }
        });

        MainFragment mainFragment = (MainFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentMain);
        if (mainFragment.getCardListAdapter().getItemCount() > 0) {
            showCleanButton();
        } else {
            hideCleanButton();
        }

        final RippleBackground rippleBackground = findViewById(R.id.imNFC);
        rippleBackground.startRippleAnimation();


        Intent intent = getIntent();
        if (intent != null && (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction()))) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null && onNFCReaderCallback != null) {
                onNFCReaderCallback.onTagDiscovered(tag);
            }
        }
    }

    public static void commonInit(Context context) {
        if (PINStorage.needInit()) {
            PINStorage.Init(context);
        }
        if (LastSignStorage.needInit()) {
            LastSignStorage.Init(context);
        }
    }

    @Override
    protected void onDestroy() {
//        Logger.StopSaveToFile(getApplicationContext());
        super.onDestroy();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (BuildConfig.DEBUG) {
            for (int i = 0; i < menu.size(); i++) menu.getItem(i).setVisible(true);
        }
        return true;
    }

    public class Compress {
        private static final int BUFFER = 2048;

        private String[] _files;
        private String _zipFile;

        Compress(String[] files, String zipFile) {
            _files = files;
            _zipFile = zipFile;
        }

        void zip() {
            try {
                BufferedInputStream origin = null;
                FileOutputStream dest = new FileOutputStream(_zipFile);

                ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

                byte data[] = new byte[BUFFER];

                for (String _file : _files) {
                    Log.v("Compress", "Adding: " + _file);
                    FileInputStream fi = new FileInputStream(_file);
                    origin = new BufferedInputStream(fi, BUFFER);
                    ZipEntry entry = new ZipEntry(_file.substring(_file.lastIndexOf("/") + 1));
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(data, 0, BUFFER)) != -1) {
                        out.write(data, 0, count);
                    }
                    origin.close();
                }

                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    File zipFile = null;

    private void sendEmail(String subject, String text, File[] filelocations) {
        if (zipFile != null) return;
        try {
            Intent intent = new Intent(Intent.ACTION_SEND)
                    //.setData(new Uri.Builder().scheme("mailto").build())
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_EMAIL, new String[]{"android@tangem.com"})
                    .putExtra(Intent.EXTRA_SUBJECT, subject)
                    .putExtra(Intent.EXTRA_TEXT, text);

            if (filelocations != null && filelocations.length > 0) {
                String[] fileNames = new String[filelocations.length];
                for (int i = 0; i < filelocations.length; i++)
                    fileNames[i] = filelocations[i].getAbsolutePath();
                zipFile = File.createTempFile("tangemLogs", ".zip", filelocations[0].getParentFile());
                Compress compress = new Compress(fileNames, zipFile.getAbsolutePath());
                compress.zip();
                Log.e(logTag, String.format("Send %d bytes zip with logs", zipFile.length()));
                Uri attachment = Uri.parse("content://" + LogFileProvider.AUTHORITY + "/"
                        + zipFile.getName());

                intent.putExtra(Intent.EXTRA_STREAM, attachment);
                zipFile.deleteOnExit();
            }

            List<ResolveInfo> activities = getPackageManager().queryIntentActivities(intent, 0);
            boolean isIntentSafe = activities.size() > 0;

            if (isIntentSafe) {
                startActivity(intent);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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

    public void showMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_main, popup.getMenu());
        if (BuildConfig.DEBUG) {
            for (int i = 0; i < popup.getMenu().size(); i++)
                popup.getMenu().getItem(i).setVisible(true);
        }
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.sendLogs:
                File f = null;
                try {
                    f = Logger.collectLogs(this);
                    if (f != null) {
                        Log.e(logTag, String.format("Collect %d log bytes", f.length()));
                        sendEmail("Logs", PhoneUtility.getDeviceInfo(), new File[]{f});
                    } else {
                        Log.e(logTag, "Can't create temporaly log file");
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

    private void showLogoActivity() {
        Intent intent = new Intent(getBaseContext(), LogoActivity.class);
        intent.putExtra("skipAutoHide", true);
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
}