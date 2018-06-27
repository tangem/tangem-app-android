package com.tangem.presentation.fragment;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tangem.domain.cardReader.NfcManager;
import com.tangem.domain.wallet.TangemCard;
import com.tangem.wallet.R;

import java.io.IOException;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class VerifyCard extends Fragment implements SwipeRefreshLayout.OnRefreshListener, NfcAdapter.ReaderCallback {

    private TangemCard mCard;
    private TextView tvCardID, tvManufacturer, tvRegistrationDate, tvCardIdentity, tvLastSigned, tvRemainingSignatures, tvReusable, tvError, tvMessage,
            tvIssuer, tvIssuerData, tvFeatures, tvBlockchain, tvSignedTx, tvSigningMethod, tvFirmware, tvWalletIdentity, tvWallet;
    private ImageView ivBlockchain, ivPIN, ivPIN2orSecurityDelay, ivDeveloperVersion;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private NfcManager mNfcManager;

    public VerifyCard() {

    }

    public void onRefresh() {
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fr_verify_card, container, false);

        mNfcManager = new NfcManager(getActivity(), this);

        mSwipeRefreshLayout = v.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mCard = new TangemCard(getActivity().getIntent().getStringExtra("UID"));
        mCard.LoadFromBundle(getActivity().getIntent().getExtras().getBundle("Card"));
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

        btnOk.setOnClickListener(v1 -> {
            Intent data = prepareResultIntent();
            data.putExtra("modification", "update");
            getActivity().setResult(Activity.RESULT_OK, data);
            getActivity().finish();
        });


        FloatingActionButton fabMenu = v.findViewById(R.id.fabMenu);


        tvWallet = v.findViewById(R.id.tvWallet);
        tvWalletIdentity = v.findViewById(R.id.tvWalletIdentity);

        updateViews();

//        if (NeedUpdate) {
//            mSwipeRefreshLayout.setRefreshing(true);
//            mSwipeRefreshLayout.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    onRefresh();
//                }
//            }, 1000);
//        }

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

    void updateViews() {
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

            //tvBlockchain.setText(mCard.getBlockchain().getOfficialName());
            tvBlockchain.setText(mCard.getBlockchainName());
            ivBlockchain.setImageResource(mCard.getBlockchain().getImageResource(this.getContext(), mCard.getTokenSymbol()));

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

    Timer timerHideErrorAndMessage = null;

    public Intent prepareResultIntent() {
        Intent data = new Intent();
        data.putExtra("UID", mCard.getUID());
        data.putExtra("Card", mCard.getAsBundle());
        return data;
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        try {
            Log.w(getClass().getName(), "Ignore discovered tag!");
            mNfcManager.ignoreTag(tag);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}