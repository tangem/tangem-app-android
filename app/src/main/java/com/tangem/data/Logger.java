package com.tangem.data;

import android.content.Context;
import android.util.Log;

import com.tangem.tangem_card.util.Util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

public class Logger {

    public static File collectLogs(Context context) {
        File f = new File(context.getCacheDir().getAbsolutePath() + "/Wallet_" + Util.formatDateTimeToFileName(new Date()) + ".log");
        try {
            if (f.createNewFile()) {
                f.setReadable(true);
                FileWriter fileWriter = new FileWriter(f, true);
                Process process = Runtime.getRuntime().exec("logcat -d -b main -v time");
                try {
                    InputStream is = process.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader bufferedReader = new BufferedReader(isr);
                    BufferedWriter buf = new BufferedWriter(fileWriter);
                    buf.append("Tangem Wallet logs");
                    buf.newLine();

                    int i = 0;
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        buf.append(line);
                        buf.newLine();
                        i++;
                    }
                    Log.e("Logger", String.format("%d log lines collected", i));
                    buf.newLine();
                    buf.flush();
                    buf.close();

                } finally {
                    process.destroy();
                }

                return f;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


}

//public class Logger {
//
//    public static File[] getLastLogFiles() {
//        File path = new File(Environment.getExternalStorageDirectory(), "Tangem/logs");
//        if (!path.exists()) {
//            return null;
//        }
//        File[] files = path.listFiles();
//        Arrays.sort(files, new Comparator<File>() {
//            @Override
//            public int compare(File o1, File o2) {
//                if (o1.lastModified() < o2.lastModified()) {
//                    return -1;
//                } else if (o1.lastModified() > o2.lastModified()) {
//                    return 1;
//                }
//                return 0;
//            }
//        });
//        if (files.length < 5) return files;
//        return Arrays.copyOfRange(files, files.length - 5, files.length);
//    }
//
//    private static File logFile = null;
//
//    private static void initLogFile(Context context) {
//        try {
//            File path = new File(Environment.getExternalStorageDirectory(), "Tangem/logs");
//            if (!path.exists()) {
//                path.mkdirs();
//                MediaScannerConnection.scanFile(context, new String[]{path.getParentFile().toString()}, null, null);
//            }
//            logFile = new File(path, String.format("wallet_%s.log", Util.formatDateTimeToFileName(new Date())));
//            logFile.createNewFile();
//            logFile.setReadable(true);
//
//            // initiate media scan and put the new things into the path array to
//            // make the scanner aware of the location and the files you want to see
//            MediaScannerConnection.scanFile(context, new String[]{logFile.getAbsolutePath()}, null, null);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    public static boolean isCurrent(File f) {
//        if (f == null || logFile == null) return false;
//        return f.getAbsolutePath().equals(logFile.getAbsolutePath());
//    }
//
//
//    private static class LogCatThread extends Thread {
//        private boolean Terminated;
//
//        private static final Object oSync = new Object();
//
//        public void Terminate() {
//            Terminated = true;
//            synchronized (oSync) {
//                oSync.notifyAll();
//            }
//            try {
//                join(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//                interrupt();
//            }
//        }
//
//        public void collectLogs(Writer out) {
//            try {
//                Process process = Runtime.getRuntime().exec("logcat -d -b main -v time");
//                try {
//                    InputStream is = process.getInputStream();
//                    InputStreamReader isr = new InputStreamReader(is);
//                    BufferedReader bufferedReader = new BufferedReader(isr);
//                    try {
//
//                        try {
//                            //BufferedWriter for performance, true to set append to file flag
//                            BufferedWriter buf = new BufferedWriter(out);
//
//                            while (!Terminated && isr.ready()) {
//                                String line = bufferedReader.readLine();
//                                buf.append(line);
//                                buf.newLine();
//                            }
//                            //Log.i("Logger",String.format("%d lines added",i));
//                            buf.newLine();
//                            buf.close();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//
//
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//
//                } finally {
//                    process.destroy();
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//        }
//
//        @Override
//        public void run() {
//            Process process = null;
//            try {
//                if (logFile == null) return;
//                process = Runtime.getRuntime().exec("logcat -b main -v time");
//                try {
//                    InputStream is = process.getInputStream();
//                    InputStreamReader isr = new InputStreamReader(is);
//                    BufferedReader bufferedReader = new BufferedReader(isr);
//                    while (!Terminated) {
//                        try {
//                            synchronized (oSync) {
//                                oSync.wait(1000);
//                            }
//                            if (!logFile.exists()) {
//                                try {
//                                    logFile.createNewFile();
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                            try {
//                                //BufferedWriter for performance, true to set append to file flag
//                                BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
//
//                                while (isr.ready()) {
//                                    String line = bufferedReader.readLine();
//                                    buf.append(line);
//                                    buf.newLine();
//                                }
//                                //Log.i("Logger",String.format("%d lines added",i));
//                                buf.newLine();
//                                buf.close();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//
//
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                } finally {
//                    process.destroy();
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    static LogCatThread t = new LogCatThread();
//
//    public static void StartSaveToFile(Activity activity) {
//        try {
//            if (logFile != null) return;
//
//            verifyStoragePermissions(activity);
//            initLogFile(activity.getApplicationContext());
//            t.init();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static void StopSaveToFile(Context context) {
//        final Object oSync = new Object();
//        if (t.isAlive()) {
//            t.Terminate();
//        }
//        if (logFile != null) {
//            MediaScannerConnection.scanFile(context, new String[]{logFile.getAbsolutePath().toString()}, null, new MediaScannerConnection.OnScanCompletedListener() {
//                @Override
//                public void onScanCompleted(String path, Uri uri) {
////                    synchronized (oSync) {
////                        oSync.notifyAll();
////                    }
//                }
//            });
////            try {
////                synchronized (oSync) {
////                    oSync.wait(10000);
//            logFile = null;
////                }
////            } catch (InterruptedException e) {
////                e.printStackTrace();
////            }
//        }
//
//    }
//
//    // Storage Permissions
//    private static final int REQUEST_EXTERNAL_STORAGE = 1;
//    private static String[] PERMISSIONS_STORAGE = {
//            Manifest.permission.READ_EXTERNAL_STORAGE,
//            Manifest.permission.WRITE_EXTERNAL_STORAGE
//    };
//
//    //Checks if the app has permission to write to device storage
//    //If the app does not has permission then the user will be prompted to grant permissions
//    public static void verifyStoragePermissions(Activity activity) {
//        // check if we have write permission
//        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
//
//        if (permission != PackageManager.PERMISSION_GRANTED) {
//            // We don't have permission so prompt the user
//            ActivityCompat.requestPermissions(
//                    activity,
//                    PERMISSIONS_STORAGE,
//                    REQUEST_EXTERNAL_STORAGE
//            );
//        }
//    }
//
//}

