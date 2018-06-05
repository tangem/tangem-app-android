package com.tangem.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;

import com.tangem.wallet.R;

import java.io.File;
import java.util.List;

public class CommonUtil {

    public static void sendEmail(Context context, File zipFile, String logTag, String subject, String text, File[] fileLocations) {
        if (zipFile != null) return;
        try {
            Intent intent = new Intent(Intent.ACTION_SEND)
                    //.setData(new Uri.Builder().scheme("mailto").build())
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_EMAIL, new String[]{"android@tangem.com"})
                    .putExtra(Intent.EXTRA_SUBJECT, subject)
                    .putExtra(Intent.EXTRA_TEXT, text);

            if (fileLocations != null && fileLocations.length > 0) {
                String[] fileNames = new String[fileLocations.length];
                for (int i = 0; i < fileLocations.length; i++)
                    fileNames[i] = fileLocations[i].getAbsolutePath();
                zipFile = File.createTempFile("tangemLogs", ".zip", fileLocations[0].getParentFile());
                Compress compress = new Compress(fileNames, zipFile.getAbsolutePath());
                compress.zip();
                Log.e(logTag, String.format("Send %d bytes zip with logs", zipFile.length()));
                Uri attachment = Uri.parse("content://" + context.getString(R.string.log_file_provider_authorities) + "/" + zipFile.getName());

                intent.putExtra(Intent.EXTRA_STREAM, attachment);
                zipFile.deleteOnExit();
            }

            List<ResolveInfo> activities = context.getPackageManager().queryIntentActivities(intent, 0);
            boolean isIntentSafe = activities.size() > 0;

            if (isIntentSafe) {
                context.startActivity(intent);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
