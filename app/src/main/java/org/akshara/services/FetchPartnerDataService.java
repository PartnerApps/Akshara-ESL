package org.akshara.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.Drive;
import com.opencsv.CSVReader;

import org.akshara.BuildConfig;
import org.akshara.Util.PrefUtil;
import org.akshara.activity.DriveSyncActivity;
import org.akshara.activity.Splashscreeen;
import org.akshara.db.StudentDAO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service to download the Partner Data file from Google Drive and update the Local database
 * @author vinayagasundar
 */

public class FetchPartnerDataService extends IntentService {

    private static final String TAG = "FetchPartnerDataService";

    private static final boolean DEBUG = BuildConfig.DEBUG;

    private static final String PARTNER_DATA_FILE_ID = "0B7PDjyAGQlXPbTF4Uno0ZkJta3c";


    private static final String ACTION_SYNC_STATE = "org.ekstep.partner.akshara.SYNC_STATE";


    static final String DOWNLOAD_FILE_PATH = "/Akshara-ESL/tmp/file.csv";


    public FetchPartnerDataService() {
        super(TAG);
    }


    /**
     * Get the Intent for Broadcast receiver for Sync State
     * @return {@link Intent} for Broadcast
     */
    public static IntentFilter getActionIntent() {
        return new IntentFilter(ACTION_SYNC_STATE);
    }


    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (DEBUG) {
            Log.i(TAG, "onHandleIntent: ");
        }

        downloadData();
    }

    private void downloadData() {
        if (DEBUG) {
            Log.i(TAG, "downloadData: ");
        }

        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                getApplication(), Arrays.asList(DriveSyncActivity.SCOPES))
                .setBackOff(new ExponentialBackOff());

        if (credential.getSelectedAccountName() == null) {
            String accountName = PrefUtil.getString(DriveSyncActivity.PREF_ACCOUNT_NAME);
            if (accountName != null) {
                credential.setSelectedAccountName(accountName);
            }
        }

        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        Drive service = new Drive.Builder(transport, jsonFactory, credential)
                .setApplicationName(BuildConfig.APPLICATION_ID)
                .build();


        FileOutputStream outputStream = null;
        File downloadFile = null;


        try {

            String downloadLocation = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + DOWNLOAD_FILE_PATH;

            if (DEBUG) {
                Log.i(TAG, "onHandleIntent: Location " + downloadLocation);
            }

            downloadFile = new File(downloadLocation);

            //noinspection ResultOfMethodCallIgnored
            downloadFile.getParentFile().mkdirs();

            outputStream = new FileOutputStream(downloadFile);

            service.files()
                    .get(PARTNER_DATA_FILE_ID)
                    .executeMediaAndDownloadTo(outputStream);


        } catch (IOException e) {
            if (DEBUG) {
                Log.e(TAG, "onHandleIntent: ", e);
            }
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException ignored) {
            }
        }

        if (DEBUG) {
            Log.i(TAG, "onHandleIntent: Download Completed");
        }

        if (downloadFile.exists()) {
            syncDatabase(downloadFile);
        }
    }


    private void syncDatabase(File downloadFile) {
        if (DEBUG) {
            Log.i(TAG, "syncDatabase: ");
        }

        CSVReader csvReader = null;

        try {
            csvReader = new CSVReader(new FileReader(downloadFile));

            String[] nextLine;

            if (csvReader.readNext() != null) {
                // First line always header so skip it
                List<ContentValues> valuesList = new ArrayList<>();

                while ((nextLine = csvReader.readNext()) != null) {
                    if (DEBUG) {
                        Log.i(TAG, String.format("syncDatabase: Count %d", nextLine.length));
                    }

                    if (StudentDAO.DEFAULT_INSERT_COLUMN_MAP.length == nextLine.length) {
                        ContentValues values = new ContentValues();
                        for (int index = 0; index < nextLine.length; index++) {
                            values.put(StudentDAO.DEFAULT_INSERT_COLUMN_MAP[index],
                                    nextLine[index]);
                        }

                        valuesList.add(values);
                    }
                }

                StudentDAO.getInstance().insertData(valuesList);

                PrefUtil.storeLongValue(Splashscreeen.LAST_SYNC_TIME, System.currentTimeMillis());

                LocalBroadcastManager.getInstance(this).sendBroadcast(
                        new Intent(ACTION_SYNC_STATE));
            }


        } catch (IOException e) {
            if (DEBUG) {
                Log.e(TAG, "syncDatabase: ", e);
            }
        } finally {
            //noinspection ResultOfMethodCallIgnored
            downloadFile.delete();
            if (csvReader != null) {
                try {
                    csvReader.close();
                } catch (IOException ignored) {

                }
            }
        }

    }
}
