package com.tangem.presentation.fragment;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.tangem.domain.cardReader.NfcManager;
import com.tangem.domain.wallet.CoinEngine;
import com.tangem.domain.wallet.CoinEngineFactory;
import com.tangem.domain.wallet.PINStorage;
import com.tangem.domain.wallet.TangemCard;
import com.tangem.presentation.activity.CreateNewWalletActivity;
import com.tangem.presentation.activity.PurgeActivity;
import com.tangem.presentation.activity.RequestPINActivity;
import com.tangem.presentation.activity.SwapPINActivity;
import com.tangem.presentation.dialog.PINSwapWarningDialog;
import com.tangem.wallet.BuildConfig;
import com.tangem.wallet.R;

import java.io.IOException;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class VerifyCard extends Fragment implements NfcAdapter.ReaderCallback {
    public static final String TAG = VerifyCard.class.getSimpleName();

    private static final int REQUEST_CODE_SEND_PAYMENT = 1;
    private static final int REQUEST_CODE_PURGE = 2;
    private static final int REQUEST_CODE_REQUEST_PIN2_FOR_PURGE = 3;
    private static final int REQUEST_CODE_VERIFY_CARD = 4;
    private static final int REQUEST_CODE_ENTER_NEW_PIN = 5;
    private static final int REQUEST_CODE_ENTER_NEW_PIN2 = 6;
    private static final int REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN = 7;
    private static final int REQUEST_CODE_SWAP_PIN = 8;

    private NfcManager mNfcManager;
    private TangemCard mCard;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private TextView tvCardID, tvManufacturer, tvRegistrationDate, tvCardIdentity, tvLastSigned, tvRemainingSignatures, tvReusable, tvError, tvMessage, tvIssuer, tvIssuerData, tvFeatures, tvBlockchain, tvSignedTx, tvSigningMethod, tvFirmware, tvWalletIdentity, tvWallet;
    private ImageView ivBlockchain, ivPIN, ivPIN2orSecurityDelay, ivDeveloperVersion;

    private int requestPIN2Count = 0;
    private Timer timerHideErrorAndMessage = null;
    private String newPIN = "", newPIN2 = "";

    public VerifyCard() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNfcManager = new NfcManager(getActivity(), this);

        mCard = new TangemCard(Objects.requireNonNull(getActivity()).getIntent().getStringExtra("UID"));
        mCard.LoadFromBundle(Objects.requireNonNull(getActivity().getIntent().getExtras()).getBundle("Card"));
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fr_verify_card, container, false);

        mSwipeRefreshLayout = v.findViewById(R.id.swipe_container);
        tvCardID = v.findViewById(R.id.tvCardID);
        tvLastSigned = v.findViewById(R.id.tvLastSigned);
        tvRemainingSignatures = v.findViewById(R.id.tvRemainingSignatures);
        tvReusable = v.findViewById(R.id.tvReusable);
        tvManufacturer = v.findViewById(R.id.tvManufacturerInfo);
        tvCardIdentity = v.findViewById(R.id.tvCardIdentity);
        tvRegistrationDate = v.findViewById(R.id.tvCardRegistredDate);
        ivBlockchain = v.findViewById(R.id.imgBlockchain);
        ivPIN = v.findViewById(R.id.imgPIN);
        ivPIN2orSecurityDelay = v.findViewById(R.id.imgPIN2orSecurityDelay);
        ivDeveloperVersion = v.findViewById(R.id.imgDeveloperVersion);
        tvError = v.findViewById(R.id.tvError);
        tvMessage = v.findViewById(R.id.tvMessage);
        tvIssuer = v.findViewById(R.id.tvIssuer);
        tvIssuerData = v.findViewById(R.id.tvIssuerData);
        tvFirmware = v.findViewById(R.id.tvFirmware);
        tvFeatures = v.findViewById(R.id.tvFeatures);
        tvBlockchain = v.findViewById(R.id.tvBlockchain);
        tvSignedTx = v.findViewById(R.id.tvSignedTx);
        tvSigningMethod = v.findViewById(R.id.tvSigningMethod);
        Button btnOk = v.findViewById(R.id.btnOk);
        FloatingActionButton fabMenu = v.findViewById(R.id.fabMenu);
        tvWallet = v.findViewById(R.id.tvWallet);
        tvWalletIdentity = v.findViewById(R.id.tvWalletIdentity);

        updateViews();

        btnOk.setOnClickListener(v1 -> {
            Intent data = prepareResultIntent();
            data.putExtra("modification", "update");
            if (getActivity() != null)
                getActivity().finish();
        });

        mSwipeRefreshLayout.setOnRefreshListener(() -> mSwipeRefreshLayout.setRefreshing(false));

        fabMenu.setOnClickListener(v16 -> showMenu(fabMenu));

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        mNfcManager.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mNfcManager.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mNfcManager.onStop();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_ENTER_NEW_PIN:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        if (Objects.requireNonNull(data.getExtras()).containsKey("confirmPIN")) {
                            Intent intent = new Intent(getContext(), RequestPINActivity.class);
                            intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString());
                            intent.putExtra("UID", mCard.getUID());
                            intent.putExtra("Card", mCard.getAsBundle());
                            newPIN = data.getStringExtra("newPIN");
                            startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN);
                        } else {
                            Intent intent = new Intent(getContext(), RequestPINActivity.class);
                            intent.putExtra("newPIN", data.getStringExtra("newPIN"));
                            intent.putExtra("mode", RequestPINActivity.Mode.ConfirmNewPIN.toString());
                            startActivityForResult(intent, REQUEST_CODE_ENTER_NEW_PIN);
                        }
                    }
                }
                break;
            case REQUEST_CODE_ENTER_NEW_PIN2:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        if (Objects.requireNonNull(data.getExtras()).containsKey("confirmPIN2")) {
                            Intent intent = new Intent(getContext(), RequestPINActivity.class);
                            intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString());
                            intent.putExtra("UID", mCard.getUID());
                            intent.putExtra("Card", mCard.getAsBundle());
                            newPIN2 = data.getStringExtra("newPIN2");
                            startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN);
                        } else {
                            Intent intent = new Intent(getContext(), RequestPINActivity.class);
                            intent.putExtra("newPIN2", data.getStringExtra("newPIN2"));
                            intent.putExtra("mode", RequestPINActivity.Mode.ConfirmNewPIN2.toString());
                            startActivityForResult(intent, REQUEST_CODE_ENTER_NEW_PIN2);
                        }
                    }
                }
                break;
            case REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN:
                if (resultCode == Activity.RESULT_OK) {
                    if (newPIN.equals("")) newPIN = mCard.getPIN();

                    if (newPIN2.equals("")) newPIN2 = PINStorage.getPIN2();

                    PINSwapWarningDialog pinSwapWarningDialog = new PINSwapWarningDialog();
                    pinSwapWarningDialog.setOnRefreshPage(this::startSwapPINActivity);
                    Bundle bundle = new Bundle();
                    if (!PINStorage.isDefaultPIN(newPIN) || !PINStorage.isDefaultPIN2(newPIN2))
                        bundle.putString(PINSwapWarningDialog.EXTRA_MESSAGE, getString(R.string.if_you_forget));
                    else
                        bundle.putString(PINSwapWarningDialog.EXTRA_MESSAGE, getString(R.string.if_you_use_default));
                    pinSwapWarningDialog.setArguments(bundle);
                    pinSwapWarningDialog.show(Objects.requireNonNull(getActivity()).getFragmentManager(), PINSwapWarningDialog.TAG);
                }
                break;

            case REQUEST_CODE_SWAP_PIN:
                if (resultCode == Activity.RESULT_OK) {
                    if (data == null) {
                        data = new Intent();

                        data.putExtra("UID", mCard.getUID());
                        data.putExtra("Card", mCard.getAsBundle());
                        data.putExtra("modification", "delete");
                    } else {
                        data.putExtra("modification", "update");
                    }
                    Objects.requireNonNull(getActivity()).setResult(Activity.RESULT_OK, data);
                    getActivity().finish();
                } else {
                    if (data != null && data.getExtras().containsKey("UID") && data.getExtras().containsKey("Card")) {
                        TangemCard updatedCard = new TangemCard(data.getStringExtra("UID"));
                        updatedCard.LoadFromBundle(data.getBundleExtra("Card"));
                        mCard = updatedCard;
                    }
                    if (resultCode == CreateNewWalletActivity.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                        requestPIN2Count++;
                        Intent intent = new Intent(getContext(), RequestPINActivity.class);
                        intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString());
                        intent.putExtra("UID", mCard.getUID());
                        intent.putExtra("Card", mCard.getAsBundle());
                        startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN);
                        return;
                    } else {
                        if (data != null && data.getExtras().containsKey("message")) {
                            mCard.setError(data.getStringExtra("message"));
                        }
                    }
                }
                break;
            case REQUEST_CODE_REQUEST_PIN2_FOR_PURGE:
                if (resultCode == Activity.RESULT_OK) {
                    Intent intent = new Intent(getContext(), PurgeActivity.class);
                    intent.putExtra("UID", mCard.getUID());
                    intent.putExtra("Card", mCard.getAsBundle());
                    startActivityForResult(intent, REQUEST_CODE_PURGE);
                }
                break;
            case REQUEST_CODE_PURGE:
                if (resultCode == Activity.RESULT_OK) {
                    if (data == null) {
                        data = new Intent();

                        data.putExtra("UID", mCard.getUID());
                        data.putExtra("Card", mCard.getAsBundle());
                        data.putExtra("modification", "delete");
                    } else {
                        data.putExtra("modification", "update");
                    }
                    Objects.requireNonNull(getActivity()).setResult(Activity.RESULT_OK, data);
                    getActivity().finish();
                } else {
                    if (data != null && data.getExtras().containsKey("UID") && data.getExtras().containsKey("Card")) {
                        TangemCard updatedCard = new TangemCard(data.getStringExtra("UID"));
                        updatedCard.LoadFromBundle(data.getBundleExtra("Card"));
                        mCard = updatedCard;
                    }
                    if (resultCode == CreateNewWalletActivity.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                        requestPIN2Count++;
                        Intent intent = new Intent(getContext(), RequestPINActivity.class);
                        intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString());
                        intent.putExtra("UID", mCard.getUID());
                        intent.putExtra("Card", mCard.getAsBundle());
                        startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_PURGE);
                        return;
                    } else {
                        if (data != null && data.getExtras().containsKey("message")) {
                            mCard.setError(data.getStringExtra("message"));
                        }
                    }
                    updateViews();
                }
                break;
            case REQUEST_CODE_SEND_PAYMENT:
                if (resultCode == Activity.RESULT_OK) {
//                    mSwipeRefreshLayout.postDelayed(this::onRefresh, 10000);
                    mSwipeRefreshLayout.setRefreshing(true);
                    mCard.clearInfo();
                    updateViews();
                }

                if (data != null) {
                    if (data.getExtras().containsKey("UID") && data.getExtras().containsKey("Card")) {
                        TangemCard updatedCard = new TangemCard(data.getStringExtra("UID"));
                        updatedCard.LoadFromBundle(data.getBundleExtra("Card"));
                        mCard = updatedCard;
                    }
                    if (data.getExtras().containsKey("message")) {
                        if (resultCode == Activity.RESULT_OK) {
                            mCard.setMessage(data.getStringExtra("message"));
                        } else {
                            mCard.setError(data.getStringExtra("message"));
                        }
                    }
                    updateViews();
                }

                break;
        }
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        try {
//            Log.w(getClass().getName(), "Ignore discovered tag!");
            mNfcManager.ignoreTag(tag);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateViews() {
        try {
            if (timerHideErrorAndMessage != null) {
                timerHideErrorAndMessage.cancel();
                timerHideErrorAndMessage = null;
            }
            tvCardID.setText(mCard.getCIDDescription());

            if (mCard.getError() == null || mCard.getError().isEmpty()) {
                tvError.setVisibility(View.GONE);
                tvError.setText("");
            } else {
                tvError.setVisibility(View.VISIBLE);
                tvError.setText(mCard.getError());
            }
            if (mCard.getMessage() == null || mCard.getMessage().isEmpty()) {
                tvMessage.setVisibility(View.GONE);
                tvMessage.setText("");
            } else {
                tvMessage.setVisibility(View.VISIBLE);
                tvMessage.setText(mCard.getMessage());
            }

            tvManufacturer.setText(mCard.getManufacturer().getOfficialName());

            if (mCard.isManufacturerConfirmed() && mCard.isCardPublicKeyValid()) {
                tvCardIdentity.setText(R.string.attested);
                tvCardIdentity.setTextColor(ContextCompat.getColor(Objects.requireNonNull(getContext()), R.color.confirmed));

            } else {
                tvCardIdentity.setText(R.string.not_confirmed);
                tvCardIdentity.setTextColor(ContextCompat.getColor(Objects.requireNonNull(getContext()), R.color.not_confirmed));
            }

            tvIssuer.setText(mCard.getIssuerDescription());
            tvIssuerData.setText(mCard.getIssuerDataDescription());

            tvRegistrationDate.setText(mCard.getPersonalizationDateTimeDescription());

            tvBlockchain.setText(mCard.getBlockchainName());

            ivBlockchain.setImageResource(mCard.getBlockchain().getImageResource(getContext(), mCard.getTokenSymbol()));


            if (mCard.isReusable())
                tvReusable.setText(R.string.reusable);
            else
                tvReusable.setText(R.string.one_off_banknote);

            tvSigningMethod.setText(mCard.getSigningMethod().getDescription());

            if (mCard.getStatus() == TangemCard.Status.Loaded || mCard.getStatus() == TangemCard.Status.Purged) {

                tvLastSigned.setText(mCard.getLastSignedDescription());
                if (mCard.getRemainingSignatures() == 0) {
                    tvRemainingSignatures.setTextColor(ContextCompat.getColor(Objects.requireNonNull(getContext()), R.color.not_confirmed));
                    tvRemainingSignatures.setText(R.string.none);
                } else if (mCard.getRemainingSignatures() == 1) {
                    tvRemainingSignatures.setTextColor(ContextCompat.getColor(Objects.requireNonNull(getContext()), R.color.not_confirmed));
                    tvRemainingSignatures.setText(R.string.last_one);
                } else if (mCard.getRemainingSignatures() > 1000) {
                    tvRemainingSignatures.setTextColor(ContextCompat.getColor(Objects.requireNonNull(getContext()), R.color.confirmed));
                    tvRemainingSignatures.setText(R.string.unlimited);
                } else {
                    tvRemainingSignatures.setTextColor(ContextCompat.getColor(Objects.requireNonNull(getContext()), R.color.confirmed));
                    tvRemainingSignatures.setText(String.valueOf(mCard.getRemainingSignatures()));
                }
                tvSignedTx.setText(String.valueOf(mCard.getMaxSignatures() - mCard.getRemainingSignatures()));
            } else {
                tvLastSigned.setText("");
                tvRemainingSignatures.setText("");
                tvSignedTx.setText("");
            }

            tvFirmware.setText(mCard.getFirmwareVersion());

            String features = "";

            if (mCard.allowSwapPIN() && mCard.allowSwapPIN2()) {
                features += "Allows change PIN1 and PIN2\n";
            } else if (mCard.allowSwapPIN()) {
                features += "Allows change PIN1\n";
            } else if (mCard.allowSwapPIN2()) {
                features += "Allows change PIN2\n";
            } else {
                features += "Fixed PIN1 and PIN2\n";
            }

            if (mCard.needCVC()) {
                features += "Requires CVC\n";
            }

            if (mCard.supportDynamicNDEF()) {
                features += "Dynamic NDEF for iOS\n";
            } else if (mCard.supportNDEF()) {
                features += "NDEF\n";
            }

            if (mCard.supportBlock()) {
                features += "Blockable\n";
            }

            if (mCard.supportOnlyOneCommandAtTime()) {
                features += "Atomic command mode";
            }

            if (features.endsWith("\n")) {
                features = features.substring(0, features.length() - 1);
            }
            tvFeatures.setText(features);

            if (mCard.useDefaultPIN1()) {
                ivPIN.setImageResource(R.drawable.unlock_pin1);
                ivPIN.setOnClickListener(v -> Toast.makeText(getContext(), R.string.this_banknote_protected_default_PIN1_code, Toast.LENGTH_LONG).show());
            } else {
                ivPIN.setImageResource(R.drawable.lock_pin1);
                ivPIN.setOnClickListener(v -> Toast.makeText(getContext(), R.string.this_banknote_protected_user_PIN1_code, Toast.LENGTH_LONG).show());
            }


            if (mCard.getPauseBeforePIN2() > 0 && (mCard.useDefaultPIN2() || !mCard.useSmartSecurityDelay())) {
                ivPIN2orSecurityDelay.setImageResource(R.drawable.timer);
                ivPIN2orSecurityDelay.setOnClickListener(v -> Toast.makeText(getContext(), String.format("This banknote will enforce %.0f seconds security delay for all operations requiring PIN2 code", mCard.getPauseBeforePIN2() / 1000.0), Toast.LENGTH_LONG).show());
            } else if (mCard.useDefaultPIN2()) {
                ivPIN2orSecurityDelay.setImageResource(R.drawable.unlock_pin2);
                ivPIN2orSecurityDelay.setOnClickListener(v -> Toast.makeText(getContext(), R.string.this_banknote_protected_default_PIN2_code, Toast.LENGTH_LONG).show());
            } else {
                ivPIN2orSecurityDelay.setImageResource(R.drawable.lock_pin2);
                ivPIN2orSecurityDelay.setOnClickListener(v -> Toast.makeText(getContext(), R.string.this_banknote_protected_user_PIN2_code, Toast.LENGTH_LONG).show());
            }

            if (mCard.useDevelopersFirmware()) {
                ivDeveloperVersion.setImageResource(R.drawable.ic_developer_version);
                ivDeveloperVersion.setVisibility(View.VISIBLE);
                ivDeveloperVersion.setOnClickListener(v -> Toast.makeText(getContext(), R.string.unlocked_banknote_only_development_use, Toast.LENGTH_LONG).show());
            } else {
                ivDeveloperVersion.setVisibility(View.INVISIBLE);
            }

            if (mCard.getStatus() == TangemCard.Status.Loaded) {
                tvWallet.setText(mCard.getShortWalletString());
                if (mCard.isWalletPublicKeyValid()) {
                    tvWalletIdentity.setText(R.string.possession_proved);
                    tvWalletIdentity.setTextColor(ContextCompat.getColor(Objects.requireNonNull(getContext()), R.color.confirmed));
                } else {
                    tvWalletIdentity.setText(R.string.possession_not_proved);
                    tvWalletIdentity.setTextColor(ContextCompat.getColor(Objects.requireNonNull(getContext()), R.color.not_confirmed));
                }
            } else {
                tvWallet.setText(R.string.not_available);
                tvWalletIdentity.setText(R.string.no_data_string);
            }

            timerHideErrorAndMessage = new Timer();
            timerHideErrorAndMessage.schedule(new TimerTask() {
                @Override
                public void run() {
                    tvError.post(() -> {
                        tvMessage.setVisibility(View.GONE);
                        tvError.setVisibility(View.GONE);
                        mCard.setError(null);
                        mCard.setMessage(null);
                    });
                }
            }, 5000);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doSetPin() {
        requestPIN2Count = 0;
        Intent intent = new Intent(getContext(), RequestPINActivity.class);
        intent.putExtra("mode", RequestPINActivity.Mode.RequestNewPIN.toString());
        newPIN = "";
        newPIN2 = "";
        startActivityForResult(intent, REQUEST_CODE_ENTER_NEW_PIN);
    }

    private void doResetPin() {
        requestPIN2Count = 0;
        Intent intent = new Intent(getContext(), RequestPINActivity.class);
        intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString());
        intent.putExtra("UID", mCard.getUID());
        intent.putExtra("Card", mCard.getAsBundle());
        newPIN = PINStorage.getDefaultPIN();
        newPIN2 = "";
        startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN);
    }

    private void doResetPin2() {
        requestPIN2Count = 0;
        Intent intent = new Intent(getContext(), RequestPINActivity.class);
        intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString());
        intent.putExtra("UID", mCard.getUID());
        intent.putExtra("Card", mCard.getAsBundle());
        newPIN = "";
        newPIN2 = PINStorage.getDefaultPIN2();
        startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN);
    }

    private void doResetPins() {
        requestPIN2Count = 0;
        Intent intent = new Intent(getContext(), RequestPINActivity.class);
        intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString());
        intent.putExtra("UID", mCard.getUID());
        intent.putExtra("Card", mCard.getAsBundle());
        newPIN = PINStorage.getDefaultPIN();
        newPIN2 = PINStorage.getDefaultPIN2();
        startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN);
    }

    private void doSetPin2() {
        requestPIN2Count = 0;
        Intent intent = new Intent(getContext(), RequestPINActivity.class);
        intent.putExtra("mode", RequestPINActivity.Mode.RequestNewPIN2.toString());
        newPIN = "";
        newPIN2 = "";
        startActivityForResult(intent, REQUEST_CODE_ENTER_NEW_PIN2);
    }

    private void doPurge() {
        requestPIN2Count = 0;
        final CoinEngine engine = CoinEngineFactory.Create(mCard.getBlockchain());
        if (!mCard.hasBalanceInfo()) {
            return;
        } else if (engine != null && engine.IsBalanceNotZero(mCard)) {
            Toast.makeText(getContext(), R.string.cannot_erase_wallet_with_non_zero_balance, Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(getContext(), RequestPINActivity.class);
        intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString());
        intent.putExtra("UID", mCard.getUID());
        intent.putExtra("Card", mCard.getAsBundle());
        startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_PURGE);
    }

    private void startSwapPINActivity() {
        Intent intent = new Intent(getContext(), SwapPINActivity.class);
        intent.putExtra("UID", mCard.getUID());
        intent.putExtra("Card", mCard.getAsBundle());
        intent.putExtra("newPIN", newPIN);
        intent.putExtra("newPIN2", newPIN2);
        startActivityForResult(intent, REQUEST_CODE_SWAP_PIN);
    }

    public Intent prepareResultIntent() {
        Intent data = new Intent();
        data.putExtra("UID", mCard.getUID());
        data.putExtra("Card", mCard.getAsBundle());
        return data;
    }

    private void showMenu(View v) {
        final PopupMenu popup = new PopupMenu(getActivity(), v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_loaded_wallet, popup.getMenu());

        popup.getMenu().findItem(R.id.action_set_PIN1).setVisible(mCard.allowSwapPIN());
        popup.getMenu().findItem(R.id.action_reset_PIN1).setVisible(mCard.allowSwapPIN() && !mCard.useDefaultPIN1());
        popup.getMenu().findItem(R.id.action_set_PIN2).setVisible(mCard.allowSwapPIN2());
        popup.getMenu().findItem(R.id.action_reset_PIN2).setVisible(mCard.allowSwapPIN2() && !mCard.useDefaultPIN2());
        popup.getMenu().findItem(R.id.action_reset_PINs).setVisible(mCard.allowSwapPIN() && mCard.allowSwapPIN2() && !mCard.useDefaultPIN1() && !mCard.useDefaultPIN2());
        if (!mCard.isReusable())
            popup.getMenu().findItem(R.id.action_purge).setVisible(false);

        popup.setOnMenuItemClickListener(item -> {

            int id = item.getItemId();
            switch (id) {
                case R.id.action_set_PIN1:
                    doSetPin();
                    return true;

                case R.id.action_reset_PIN1:
                    doResetPin();
                    return true;

                case R.id.action_set_PIN2:
                    doSetPin2();
                    return true;

                case R.id.action_reset_PIN2:
                    doResetPin2();
                    return true;

                case R.id.action_reset_PINs:
                    doResetPins();
                    return true;

                case R.id.action_purge:
                    doPurge();
                    return true;

                default:
                    return false;
            }
        });

        if (BuildConfig.DEBUG) {
            popup.getMenu().findItem(R.id.action_set_PIN2).setEnabled(true);
            popup.getMenu().findItem(R.id.action_reset_PIN2).setEnabled(true);
        }

        popup.show();
    }

}