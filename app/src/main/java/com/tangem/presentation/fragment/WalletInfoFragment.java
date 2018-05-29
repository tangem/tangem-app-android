package com.tangem.presentation.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.tangem.wallet.R;
import com.tangem.wallet.Tangem_Card;

import java.util.Hashtable;

import static android.content.Context.CLIPBOARD_SERVICE;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link WalletInfoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WalletInfoFragment extends Fragment {

    // TODO: Rename and change types of parameters
    private Tangem_Card mCard;

    private OnFragmentInteractionListener mListener;

    public WalletInfoFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment WalletInfoFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static WalletInfoFragment newInstance(Tangem_Card card) {
        WalletInfoFragment fragment = new WalletInfoFragment();
        Bundle args = new Bundle();
        args.putString("UID",card.getUID());
        card.SaveToBundle(args);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mCard = new Tangem_Card(getArguments().getString("UID"));
            mCard.LoadFromBundle(getArguments());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View result=inflater.inflate(R.layout.fragment_wallet_info, container, false);

        ImageView mImage= (ImageView)result.findViewById(R.id.qrWallet);
        try {
            mImage.setImageBitmap(generateQrCode(mCard.getWallet()));
        } catch (WriterException e) {
            e.printStackTrace();
        }
        TextView mText=(TextView)result.findViewById(R.id.strWallet);
        mText.setText(mCard.getWallet());

        mText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView mText = (TextView) view;
                ClipboardManager clipboard = (ClipboardManager)getActivity().getSystemService(CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText(mText.getText(), mText.getText()));
                Toast.makeText(getContext(),"Copied to clipboard",Toast.LENGTH_LONG).show();
            }
        });
        return result;
    }

    public static Bitmap generateQrCode(String myCodeText) throws WriterException {
        Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<EncodeHintType, ErrorCorrectionLevel>();
        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // H = 30% damage

        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        int size = 256;

        BitMatrix bitMatrix = qrCodeWriter.encode(myCodeText, BarcodeFormat.QR_CODE, size, size, hintMap);
        int width = bitMatrix.getWidth();
        Bitmap bmp = Bitmap.createBitmap(width, width, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < width; y++) {
                bmp.setPixel(y, x, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bmp;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
//        void onFragmentInteraction(Uri uri);
    }
}
