/*
  * @author STMicroelectronics MMY Application team
  *
  ******************************************************************************
  * @attention
  *
  * <h2><center>&copy; COPYRIGHT 2017 STMicroelectronics</center></h2>
  *
  * Licensed under ST MIX_MYLIBERTY SOFTWARE LICENSE AGREEMENT (the "License");
  * You may not use this file except in compliance with the License.
  * You may obtain a copy of the License at:
  *
  *        http://www.st.com/Mix_MyLiberty
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied,
  * AND SPECIFICALLY DISCLAIMING THE IMPLIED WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  ******************************************************************************
*/

package com.st.st25nfc.type5.st25dv;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import com.st.st25nfc.R;
import com.st.st25nfc.generic.ST25Menu;
import com.st.st25nfc.generic.STFragmentActivity;
import com.st.st25sdk.Helper;
import com.st.st25sdk.type5.st25dv.ST25DVTag;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


public class ST25DVFirmwareUpdateDemoActivity
        extends STFragmentActivity
        implements NavigationView.OnNavigationItemSelectedListener, ST25DVTransferTask.OnTransferListener {


    public ST25DVTag mST25DVTag;

    private ST25DVTransferTask mTransferTask;
    private Chronometer mChronometer;

    private AsyncTaskUpload mUploadTask;

    private TextView mSourceFileTextView;
    private InputStream mFirmwareInputStream;

    private byte[] mPassword;

    private ProgressBar mProgress;
    private double mProgressStatus;
    //private Handler mHandler;

    private enum Action {
        INIT,
        PRESENT_PASSWORD,
        UPLOAD_FIRMWARE,
        START_CHRONOMETER
    };

    private final int GET_FIRMWARE_LIST = 1;
    private Thread mThread;
    private int mTransferAction;
    private Action mCurrentAction;

    private int mApiVersion = Build.VERSION.SDK_INT;

    private byte[] mBuffer;

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_layout);

        // Inflate content of FrameLayout
        FrameLayout frameLayout=(FrameLayout) findViewById(R.id.frame_content);
        View childView = getLayoutInflater().inflate(R.layout.firmware_update_demo_content_st25dv, null);
        frameLayout.addView(childView);

        mST25DVTag = (ST25DVTag) super.getTag();
        if (mST25DVTag == null) {
            showToast(R.string.invalid_tag);
            goBackToMainActivity();
            return;
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        //Button fab = (Button) findViewById(R.id.button);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.button);
        //fab.setOnClickListener(this);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        mMenu = ST25Menu.newInstance(super.getTag());
        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
        mMenu.inflateMenu(navigationView);

        Button startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {startTranfer();
            }
        });

        Button stopButton = (Button) findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {stopTransfer();
            }
        });

        Button pauseButton = (Button) findViewById(R.id.pauseButton);
        pauseButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                pauseTransfer();
                mChronometer.pause();
            }
        });

        Button resumeButton = (Button) findViewById(R.id.resumeButton);
        resumeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                resumeTransfer();
                mChronometer.resume();
            }
        });

        mProgress = (ProgressBar) findViewById(R.id.progressBar);
        //mHandler = new Handler();

        mChronometer = (Chronometer) findViewById(R.id.st25DvChronometer);

        mChronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            public void onChronometerTick(Chronometer chronometer) {

            }
        });

        mCurrentAction = Action.INIT;

        toolbar.setTitle(mST25DVTag.getName());

        if (mApiVersion >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        GET_FIRMWARE_LIST);
            }
        }

        mSourceFileTextView = (TextView) findViewById(R.id.sourceFileTextView);
        mFirmwareInputStream = null;

        Button selectSourceFileButton = (Button) findViewById(R.id.selectSourceFileButton);
        selectSourceFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseFirmware();
            }
        });

        //setContentView();
    }

    /**
     * Display an AlertDialog to choose between:
     * - the demo FW embedded in the APK
     * - a FW present in phone's memory storage
     */
    private void chooseFirmware() {
        final AlertDialog alertDialog = new AlertDialog.Builder(ST25DVFirmwareUpdateDemoActivity.this).create();

        // set title
        alertDialog.setTitle(R.string.firmware_update_message);

        // inflate XML content
        View dialogView = getLayoutInflater().inflate(R.layout.st25dv_firmware_selection_popup, null);
        alertDialog.setView(dialogView);

        final RadioButton embeddedFirmwareRadioButton = (RadioButton) dialogView.findViewById(R.id.embeddedFirmwareRadioButton);

        Button continueButton = (Button) dialogView.findViewById(R.id.continueButton);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (embeddedFirmwareRadioButton.isChecked()) {
                    // Select the demo FW embedded in the APK
                    selectEmbeddedDemoFirmware();

                } else {
                    String message = getString(R.string.please_select_firmware_file);

                    // Pick up FW in phone's memory storage
                    Intent intent = new Intent()
                            .setType("*/*")
                            .setAction(Intent.ACTION_GET_CONTENT);

                    // Special intent for Samsung file manager
                    Intent sIntent = new Intent("com.sec.android.app.myfiles.PICK_DATA");
                    sIntent.addCategory(Intent.CATEGORY_DEFAULT);

                    Intent chooserIntent;
                    if (getPackageManager().resolveActivity(sIntent, 0) != null){
                        // it is device with samsung file manager
                        chooserIntent = Intent.createChooser(sIntent, message);
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { intent});
                    }
                    else {
                        chooserIntent = Intent.createChooser(intent, message);
                    }

                    startActivityForResult(chooserIntent, 0);
                }

                // Leave the dialog box
                alertDialog.dismiss();
            }
        });

        // show it
        alertDialog.show();
    }

    private void selectEmbeddedDemoFirmware() {
        mFirmwareInputStream = ST25DVFirmwareUpdateDemoActivity.class.getResourceAsStream("/assets/ST25DVDemo_FwUpgrd.bin");

        String filePath = "Embedded demo Firmware";

        int fileSize = 0;
        try {
            fileSize = mFirmwareInputStream.available();
        } catch (IOException e) {
            showToast(R.string.failed_to_red_file_size);
            e.printStackTrace();
            return;
        }

        mSourceFileTextView.setText(getString(R.string.source_file_and_size, filePath, fileSize));

        // Ready for FW upload
        mCurrentAction = Action.UPLOAD_FIRMWARE;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==0 && resultCode==RESULT_OK) {
            Uri selectedfileUri = data.getData(); //The uri with the location of the file

            try {
                mFirmwareInputStream = getContentResolver().openInputStream(selectedfileUri);
            } catch (FileNotFoundException e) {
                showToast(R.string.failled_to_open_file);
                e.printStackTrace();
                return;
            }

            String filePath = selectedfileUri.getPath();
            int fileSize = 0;
            try {
                fileSize = mFirmwareInputStream.available();
            } catch (IOException e) {
                showToast(R.string.failed_to_red_file_size);
                e.printStackTrace();
                return;
            }
            mSourceFileTextView.setText(getString(R.string.source_file_and_size, filePath, fileSize));

            // Ready for FW upload
            mCurrentAction = Action.UPLOAD_FIRMWARE;
        }
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message m) {
                Action action = (Action) m.obj;
                 switch (action) {
                     case PRESENT_PASSWORD:
                         sendPassword();
                         break;
                     case UPLOAD_FIRMWARE:
                         startUpload();
                         break;
                     case START_CHRONOMETER:
                         mChronometer.start();
                         break;
                     default:
                         break;
            }
        }
    };


    public void transferOnProgress(double progressStatus) {
        // Start lengthy operation in a background thread

        if (mTransferAction != ST25DVTransferTask.FAST_PRESENT_PWD_FUNCTION) {
            if (mProgress != null) {
                if (mProgressStatus == 0 && progressStatus != 0) {
                    Message message = mHandler.obtainMessage();
                    message.obj = Action.START_CHRONOMETER;
                    mHandler.sendMessage(message);
                }
                mProgress.setProgress((int) progressStatus);
                mProgressStatus = progressStatus;
            }
        }
    }


    public void transferFinished(boolean success, final long timeTransfer, byte[] buffer) {

        if (mTransferAction == ST25DVTransferTask.FAST_PRESENT_PWD_FUNCTION) {
            if (success) {
                showToast(R.string.password_ok);
                Message message = mHandler.obtainMessage();
                message.obj = Action.UPLOAD_FIRMWARE;
                mHandler.sendMessage(message);

            } else {
                showToast(R.string.wrong_pwd_or_mailbox_enabled);
            }
        } else if (mTransferAction == ST25DVTransferTask.FAST_FIRMWARE_UPDATE_FUNCTION) {
            mChronometer.stop();
            if (success) {
                showToast(R.string.transfer_ok);
            } else {
                showToast(R.string.transfer_failed);
            }
        }

    }


    @Override
    public byte[] getDataToWrite() {
        return null;
    }



    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
        stopTransfer();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds read_list_items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_empty, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long

        // as you specify a parent activity in AndroidManifest.xml.


        return super.onOptionsItemSelected(item);
    }


    void processIntent(Intent intent) {

    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        return mMenu.selectItem(this, item);
    }

    public ST25DVTag getTag() {
        return mST25DVTag;
    }

    public void onPause() {
        /*if (mThread != null)
            try {
                mThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "Issue joining thread");
            }*/
        super.onPause();
    }


    private void fillBuffer() {
        try {
            mBuffer = null;
            int len = (mFirmwareInputStream.available() > 1000) ? 1000 : mFirmwareInputStream.available();
            int size;
            byte[] readBuffer = new byte[len];

            while ((size = mFirmwareInputStream.read(readBuffer, 0, len)) >= 0) {
                byte[] tmpBuffer = null;

                if (mBuffer != null) {
                    tmpBuffer = new byte[mBuffer.length];
                    System.arraycopy(mBuffer, 0, tmpBuffer, 0, mBuffer.length);
                    mBuffer = new byte[mBuffer.length + size];
                } else {
                    mBuffer = new byte[size];
                }

                if (tmpBuffer != null)
                    System.arraycopy(tmpBuffer, 0, mBuffer, 0, tmpBuffer.length);

                System.arraycopy(readBuffer, 0, mBuffer, mBuffer.length - size, size);

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void startTranfer() {

        switch (mCurrentAction) {
            case UPLOAD_FIRMWARE:
                //when password is ok the transfer start
                presentPassword();
                break;
            case INIT:
                showToast(R.string.please_select_a_firmware);
                break;
            default:
                break;
        }


    }

    public void stopTransfer() {
        mChronometer.stop();
        if (mTransferTask != null) mTransferTask.stop();
    }

    public void pauseTransfer() {
        mChronometer.pause();
        if (mTransferTask != null) mTransferTask.pause();
    }

    public void resumeTransfer() {
        mChronometer.resume();
        if (mTransferTask != null) mTransferTask.resume();
    }

    /* Checks if external storage is available to at least read */
    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    private void presentPassword() {

        View promptView = getLayoutInflater().inflate(R.layout.present_firmware_password, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptView);
        final EditText passwordEditText = (EditText) promptView.findViewById(R.id.user_input);

        alertDialogBuilder.setCancelable(false).setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String password = passwordEditText.getText().toString();
                mPassword =  Helper.convertHexStringToByteArray(password);
                sendPassword();
            }
        }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }


    private void sendPassword() {
        mTransferAction = ST25DVTransferTask.FAST_PRESENT_PWD_FUNCTION;
        mTransferTask = new ST25DVTransferTask(mTransferAction, mPassword, mST25DVTag);
        mTransferTask.setTransferListener(this);
        new Thread(mTransferTask).start();
    }

    private void startUpload() {
        // The long-running operation is run on a worker thread for fillBuffer
        if (mUploadTask != null && mUploadTask.getStatus() == AsyncTask.Status.RUNNING) {
            mUploadTask.cancel(true);
        }
        stopTransfer();
        mUploadTask = new AsyncTaskUpload(this);
        mUploadTask.execute();
    }

    private class AsyncTaskUpload extends AsyncTask<Void, Void, Void> {
        ST25DVFirmwareUpdateDemoActivity mFirmwareUpdateActivity;

        public AsyncTaskUpload(ST25DVFirmwareUpdateDemoActivity firmwareUpdateActivity) {
            mFirmwareUpdateActivity = firmwareUpdateActivity;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            fillBuffer();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mTransferAction = ST25DVTransferTask.FAST_FIRMWARE_UPDATE_FUNCTION;
            mTransferTask = new ST25DVTransferTask(mTransferAction, mBuffer, mST25DVTag);
            mTransferTask.setTransferListener(mFirmwareUpdateActivity);
            mProgressStatus = 0;
            new Thread(mTransferTask).start();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            stopTransfer();
        }

    }
}
