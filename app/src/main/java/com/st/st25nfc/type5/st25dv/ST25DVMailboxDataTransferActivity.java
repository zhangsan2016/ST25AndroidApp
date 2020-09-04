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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.provider.DocumentFile;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.st.st25nfc.R;
import com.st.st25nfc.generic.ST25Menu;
import com.st.st25nfc.generic.STFragmentActivity;
import com.st.st25sdk.type5.st25dv.ST25DVTag;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;


public class ST25DVMailboxDataTransferActivity
        extends STFragmentActivity
        implements NavigationView.OnNavigationItemSelectedListener, ST25DVTransferTask.OnTransferListener {


    final static String TAG = "ST25DVDataTransfer";
    public ST25DVTag mST25DVTag;

    private int[] mBufferSize = new int[]{256, 512, 1024, 2048, 16384, 65536, 100000};
    private int mSize;
    private ST25DVTransferTask mTransferTask;
    private Chronometer mChronometer;

    private CustomListView mLv;
    private CustomListAdapter mAdapter;


    private ProgressBar mProgress;
    private Handler mHandler;
    private final int GET_FILE_LIST = 1;
    private int mApiVersion = Build.VERSION.SDK_INT;

    private final int SELECT_FILE = 4231;


    private enum Action {
        SEND_BUFFER,
        RECEIVED_BUFFER,
        SEND_BUFFER_FILE
    };

    private Action mCurrentAction;
    private int mTransferAction;
    byte[] mBuffer = null;
    private Uri mSelectedfileUri;

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_layout);

        // Inflate content of FrameLayout
        FrameLayout frameLayout=(FrameLayout) findViewById(R.id.frame_content);
        View childView = getLayoutInflater().inflate(R.layout.data_transfer_content_mailbox_st25dv, null);
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

        mLv = (CustomListView) findViewById(R.id.data_transfer_mailbox_list_view);

        mAdapter = new CustomListAdapter(this);
        mLv.setOnItemClickListener(mAdapter);
        mLv.setAdapter(mAdapter);

        Button startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startTranfer();
            }
        });

        Button stopButton = (Button) findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                stopTransfer();
            }
        });

        Button pauseButton = (Button) findViewById(R.id.pauseButton);
        pauseButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                pauseTransfer();
            }
        });

        Button resumeButton = (Button) findViewById(R.id.resumeButton);
        resumeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                resumeTransfer();
            }
        });

        mProgress = (ProgressBar) findViewById(R.id.progressBar);
        mHandler = new Handler();

        mChronometer = (Chronometer) findViewById(R.id.st25DvChronometer);

        mChronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            public void onChronometerTick(Chronometer chronometer) {

            }
        });

        if (mApiVersion >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        GET_FILE_LIST);
            }
        }

        toolbar.setTitle(mST25DVTag.getName());
        setContentView();
    }


    public void transferOnProgress(double progressStatus) {
        // Start lengthy operation in a background thread

        if (mProgress != null)
            mProgress.setProgress((int) progressStatus);
    }


    public void transferFinished(boolean success, final long timeTransfer, byte[] buffer) {
        mChronometer.stop();
        if (success) {
            showToast(R.string.transfer_ok);
        } else
            showToast(R.string.transfer_failed);
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
        Log.d(TAG, "Process Intent");
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        return mMenu.selectItem(this, item);
    }

    public ST25DVTag getTag() {
        return mST25DVTag;
    }

    public void setContentView() {

        class ContentView implements Runnable {

            @Override
            public void run() {

            }
        }

        new Thread(new ContentView()).start();

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

    //byte[] buffer = null;
    private void fillBuffer() {
        try {
            mBuffer = null;
            if (mSelectedfileUri != null) {

                InputStream fileInputStream = getContentResolver().openInputStream(mSelectedfileUri);

                int len = (fileInputStream.available() > 1000) ? 1000 : fileInputStream.available();
                int size;
                byte[] readBuffer = new byte[len];

                while ((size = fileInputStream.read(readBuffer, 0, len)) >= 0) {
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
            } else {
                showToast(R.string.no_file_selected);
            }

        } catch (IOException e) {
        }

    }

    public void startTranfer() {
        if (mCurrentAction != null) {
            switch (mCurrentAction) {
                case RECEIVED_BUFFER:
                    mTransferAction = ST25DVTransferTask.FAST_RANDOM_TRANSFER_FUNCTION;
                    break;
                case SEND_BUFFER:
                    if (mSize > 0) {
                        SecureRandom random = new SecureRandom();
                        mBuffer = new byte[mSize];
                        random.nextBytes(mBuffer);
                    }
                    mTransferAction = ST25DVTransferTask.FAST_BASIC_TRANSFER_FUNCTION;

                    break;
                case SEND_BUFFER_FILE:
                    mTransferAction = ST25DVTransferTask.FAST_BASIC_TRANSFER_FUNCTION;
                    fillBuffer();
                    break;
                default:
                    break;
            }

            mTransferTask = new ST25DVTransferTask(mTransferAction, mBuffer, mST25DVTag);
            mTransferTask.setTransferListener(this);
            new Thread(mTransferTask).start();
            mChronometer.start();
        } else {
            showToast(R.string.an_action_must_be_selected_first);
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


    class CustomListAdapter extends BaseAdapter implements AdapterView.OnItemClickListener, PopupMenu.OnMenuItemClickListener {

        ST25DVMailboxDataTransferActivity mActivity;
        View mlistItem;

        public CustomListAdapter(ST25DVMailboxDataTransferActivity st25DVMailboxDataTransferActivity) {
            mActivity = st25DVMailboxDataTransferActivity;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View listItem = convertView;

            if (listItem == null) {
                //set the main ListView's layout
                listItem = getLayoutInflater().inflate(R.layout.data_transfer_mailbox_items, parent, false);
            }

            TextView title = (TextView) listItem.findViewById(R.id.title);
            TextView description = (TextView) listItem.findViewById(R.id.description);
            Drawable contentImage = null;

            switch (position) {
                case 0:
                    title.setText(R.string.Writing_To_Tag);
                    if (mCurrentAction != Action.SEND_BUFFER) {
                        contentImage = getResources().getDrawable(R.drawable.st_grey_circle);
                        description.setText(R.string.Select_Buffer_Size);
                    } else {
                        description.setText(getResources().getString(R.string.maibox_data_transfer_bytes, mSize));
                        contentImage = getResources().getDrawable(R.drawable.st_green_circle);
                    }
                    break;
                case 1:
                    title.setText(R.string.Reading_From_Tag);
                    if (mCurrentAction != Action.RECEIVED_BUFFER) {
                        description.setText(R.string.Buffer_Size_SetByHost);
                        contentImage = getResources().getDrawable(R.drawable.st_grey_circle);
                    } else {
                        //description.setText(String.valueOf(mSize) + " Bytes");
                        contentImage = getResources().getDrawable(R.drawable.st_green_circle);
                    }
                    break;
                case 2:
                    title.setText(R.string.Writing_To_Tag_From_File);
                    if ((mCurrentAction == Action.SEND_BUFFER_FILE) && (mSelectedfileUri != null)) {
                        // A valid file is selected
                        //description.setText(String.valueOf(mSize) + " Bytes");
                        contentImage = getResources().getDrawable(R.drawable.st_green_circle);

                        String fileName = getFileName(mSelectedfileUri);
                        try {
                            InputStream fileInputStream = getContentResolver().openInputStream(mSelectedfileUri);
                            mSize = fileInputStream.available();

                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        description.setText(getString(R.string.source_file_and_size, fileName, mSize));
                    } else {
                        // No file selected yet
                        contentImage = getResources().getDrawable(R.drawable.st_grey_circle);
                        description.setText(R.string.Select_Buffer_From_File);
                    }
                    break;
                default:
                    break;
            }

            ImageView image = (ImageView) listItem.findViewById(R.id.thumb);
            image.setImageDrawable(contentImage);

            mlistItem = listItem;

            return listItem;

        }

        public String getFileName(Uri uri) {
            String result = null;
            if (uri.getScheme().equals("content")) {
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                try {
                    if (cursor != null && cursor.moveToFirst()) {
                        result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }
                } finally {
                    cursor.close();
                }
            }
            if (result == null) {
                result = uri.getPath();
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
            return result;
        }

        private int getSelectedFileSize(String file) {
            File f;
            int fileSize = 0;
            f = new File(file);
            if (f.exists()) fileSize = (int) f.length();
            return fileSize;
        }


        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            if (position == 0) {
                PopupMenu popupMenu = new PopupMenu(mActivity, view);
                for (int i = 0; i < mBufferSize.length; i++) {
                    popupMenu.getMenu().add(0, i, 0, String.valueOf(mBufferSize[i]));
                }
                popupMenu.setOnMenuItemClickListener(this);
                popupMenu.show();
            }

            if (position == 0) {
                mCurrentAction = Action.SEND_BUFFER;
            } else if (position == 1) {
                mCurrentAction = Action.RECEIVED_BUFFER;
            } else if (position == 2) {
                selectFile();
            }

            mAdapter.notifyDataSetChanged();
        }

        private void selectFile() {
            Intent intent;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                intent = new Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_OPEN_DOCUMENT);
                startActivityForResult(intent, SELECT_FILE);

            } else {
                String message = getString(R.string.Select_Buffer_From_File);

                // Pick up FW in phone's memory storage
                intent = new Intent()
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

                startActivityForResult(chooserIntent, SELECT_FILE);
            }
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {

            mSize = mBufferSize[item.getItemId()];
            mAdapter.notifyDataSetChanged();

            return true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==SELECT_FILE && resultCode==RESULT_OK) {
            mSelectedfileUri = data.getData();
            // A file has been selected
            mCurrentAction = Action.SEND_BUFFER_FILE;
            // Refresh the list
            mAdapter.notifyDataSetChanged();
        }
    }

    public void displayImage(byte[] buffer) {
        class DisplayImage implements Runnable {

            byte[] mBuffer;

            public DisplayImage(byte[] buffer) {
                mBuffer = buffer;
            }

            @Override
            public void run() {

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mLv.getContext());
                View v = getLayoutInflater().inflate(R.layout.transfer_dialog_layout, null);
                Bitmap bitmap = BitmapFactory.decodeByteArray(mBuffer, 0, mBuffer.length);

                ImageView imageView = (ImageView) v.findViewById(R.id.image_uploaded);
                imageView.setImageBitmap(bitmap);

                alertDialogBuilder.setView(v);


                alertDialogBuilder.show();


            }
        }
        mHandler.post(new DisplayImage(buffer));

    }
}
