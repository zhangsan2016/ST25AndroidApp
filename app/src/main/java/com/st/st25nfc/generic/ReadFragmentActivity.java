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

package com.st.st25nfc.generic;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.st.st25nfc.generic.util.UIHelper;
import com.st.st25sdk.Helper;
import com.st.st25sdk.MultiAreaInterface;
import com.st.st25sdk.NFCTag;
import com.st.st25sdk.STException;
import com.st.st25nfc.R;
import com.st.st25sdk.STLog;
import com.st.st25sdk.type4a.ControlTlv;
import com.st.st25sdk.type4a.STType4Tag;
import com.st.st25sdk.type4a.Type4Tag;
import com.st.st25sdk.type5.Type5Tag;

import java.util.ArrayList;

import static com.st.st25sdk.MultiAreaInterface.AREA1;

public class ReadFragmentActivity extends STFragmentActivity
        implements NavigationView.OnNavigationItemSelectedListener, STFragment.STFragmentListener, View.OnClickListener {

    // Set here the Toolbar to use for this activity
    private int toolbar_res = R.menu.toolbar_empty;

    // The data are now read by Byte but we will still format the display by raw of 4 Bytes
    private final  int NBR_OF_BYTES_PER_RAW = 4;

    private int mStartAddress;
    private int mNumberOfBytes;

    private static final String TAG = "ReadFragmentActivity";
    private ListView lv;
    private CustomListAdapter mAdapter;
    private ContentViewAsync contentView;

    private EditText mStartAddressEditText;
    private EditText mNbrOfBytesEditText;

    // For type 4 read in case of several area
    private int mAreaId;

    private FloatingActionButton mFab;
    private View mChildView;

    private boolean mUnitInBytes;

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_layout);

        // Inflate content of FrameLayout
        FrameLayout frameLayout=(FrameLayout) findViewById(R.id.frame_content);
        mChildView = getLayoutInflater().inflate(R.layout.fragment_read_memory, null);
        frameLayout.addView(mChildView);

        if (super.getTag() == null) {
            showToast(R.string.invalid_tag);
            goBackToMainActivity();
            return;
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(this);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        mMenu = ST25Menu.newInstance(super.getTag());
        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
        mMenu.inflateMenu(navigationView);

        mUnitInBytes = true;
        // units selector
        Spinner spinnerUnit = (Spinner)findViewById(R.id.spinner);
        String[] units = getResources().getStringArray(R.array.unit_readMemory);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, units);
        spinnerUnit.setAdapter(adapter);
        spinnerUnit.setSelection(0);
        spinnerUnit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                String unit = (String) parent.getItemAtPosition(position);
                if (unit.contains("Bytes")) {
                    mUnitInBytes = true;
                } else {
                    // blocks
                    mUnitInBytes = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mUnitInBytes = true;
            }
        });

        mStartAddressEditText = (EditText) findViewById(R.id.startAddressEditText);
        mStartAddressEditText.setText(String.valueOf(0));

        mNbrOfBytesEditText = (EditText) findViewById(R.id.nbrOfBytesEditText);
        mNbrOfBytesEditText.setText(String.valueOf(64));

        toolbar.setTitle(getTag().getName());


        // Retrieve parameters for start and UI configuration
        Intent mIntent = getIntent();
        // define the start address and numbers of bytes to read for display
        int startAddress = mIntent.getIntExtra("start_address", -1);
        int nbOfBytes = mIntent.getIntExtra("nb_of_bytes", -1);
        // define the data as an array of bytes to display
        byte[] data = mIntent.getByteArrayExtra("data");
        // define the file id for the display - Type4 Tags needed - replace start address and numbers of bytes
        int areaFileID = mIntent.getIntExtra("areaFileID", -1);
        // information message
        String information = mIntent.getStringExtra("information");
        if (information == null) information = getString(R.string.hexadecimal_dump);

        // Manage the dump display for a Type4 Tag


        // Manage an hexa dump - no tag dependency
        if (data != null) {
            // UI setting
            configureUIItemsForHexaDumpOnly(information);
            // Displaying
            startDisplaying(data);
        } else if (areaFileID != -1 && getTag() instanceof Type4Tag) {
                // UI setting
                configureUIItemsForHexaDumpOnly(information);
                // start reading ...
                startType4ReadingAndDisplaying((Type4Tag) getTag(), areaFileID);


        } else if (startAddress >= 0 &&  nbOfBytes >=0 && getTag() instanceof Type5Tag) { // manage the dump with start address and number of bytes - Type5
            mStartAddress = startAddress;
            mNumberOfBytes = nbOfBytes;
            // UI setting
            configureUIItemsForHexaDumpOnly(information);
            // inform user that a read will be performed
            Snackbar snackbar = Snackbar.make(mChildView , "", Snackbar.LENGTH_LONG);
            snackbar.setAction(getString(R.string.reading_x_bytes_starting_y_address,mNumberOfBytes,mStartAddress), this);

            snackbar.setActionTextColor(getResources().getColor(R.color.white));
            snackbar.show();
            // start reading ...
            startType5ReadingAndDisplaying(mStartAddress,mNumberOfBytes);
        } else {
            // default behaviour - user have to enter read parameters
            // Manage UI for Tag Type4
            if (getTag() instanceof Type4Tag) {
                // display layout for type4
                displayType4ReadSelectionParameters();
                fillType4SpinnerForSelection((Type4Tag)getTag());
            } else {
                displayType5ReadSelectionParameters();
            }
        }

    }

    private void fillType4SpinnerForSelection(Type4Tag tag){
        Spinner spinnerUnit = (Spinner)findViewById(R.id.areaIdSpinner);
        ArrayList<String> stringArrayList = new ArrayList<String>();

        try {
            int numberOfFiles = tag.getNbrOfFiles();
            for (int i =0; i<numberOfFiles;i++) {
                stringArrayList.add(getString(R.string.area_number_to_name) + (i+1));
            }
        } catch (STException e) {
            e.printStackTrace();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, stringArrayList);
        spinnerUnit.setAdapter(adapter);
        spinnerUnit.setSelection(0);
        spinnerUnit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                mAreaId = position+1;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mAreaId = 0;
            }
        });

    }

    private void removeType4ReadSelectionParameters(){
        LinearLayout type4LayoutParameters = (LinearLayout) findViewById(R.id.areaIdLayout);
        type4LayoutParameters.setVisibility(View.GONE);
    }
    private void displayType4ReadSelectionParameters() {
        LinearLayout type4LayoutParameters = (LinearLayout) findViewById(R.id.areaIdLayout);
        type4LayoutParameters.setVisibility(View.VISIBLE);
        removeType5ReadSelectionParameters();
    }
    private void removeType5ReadSelectionParameters(){
        LinearLayout startAddressLayout = (LinearLayout) findViewById(R.id.startAddressLayout);
        LinearLayout nbrOfBytesLayout = (LinearLayout) findViewById(R.id.nbrOfBytesLayout);
        startAddressLayout.setVisibility(View.GONE);
        nbrOfBytesLayout.setVisibility(View.GONE);
        LinearLayout unitLayout = (LinearLayout) findViewById(R.id.unitLayout);
        unitLayout.setVisibility(View.GONE);
    }
    private void displayType5ReadSelectionParameters(){
        LinearLayout startAddressLayout = (LinearLayout) findViewById(R.id.startAddressLayout);
        LinearLayout nbrOfBytesLayout = (LinearLayout) findViewById(R.id.nbrOfBytesLayout);
        startAddressLayout.setVisibility(View.VISIBLE);
        nbrOfBytesLayout.setVisibility(View.VISIBLE);
        LinearLayout unitLayout = (LinearLayout) findViewById(R.id.unitLayout);
        unitLayout.setVisibility(View.VISIBLE);
        removeType4ReadSelectionParameters();
    }

    private void configureUIItemsForHexaDumpOnly (String information) {
        LinearLayout startAddressLayout = (LinearLayout) findViewById(R.id.startAddressLayout);
        LinearLayout nbrOfBytesLayout = (LinearLayout) findViewById(R.id.nbrOfBytesLayout);
        startAddressLayout.setVisibility(View.GONE);
        nbrOfBytesLayout.setVisibility(View.GONE);

        LinearLayout informationLayout = (LinearLayout) findViewById(R.id.informationLayout);
        informationLayout.setVisibility(View.VISIBLE);
        TextView informationTextView = (TextView) findViewById(R.id.informationTextView);
        if (information != null) {
            informationTextView.setText(information);
        }
        this.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        mFab.setVisibility(View.GONE);
        // remove unit selection  - default in Bytes
        LinearLayout unitLayout = (LinearLayout) findViewById(R.id.unitLayout);
        unitLayout.setVisibility(View.GONE);
    }

    private int getMemoryAreaSizeInBytes(Type4Tag myTag, int area) {
        int memoryAreaSizeInBytes = 0;
        try {
            if (myTag instanceof STType4Tag) {

                int fileId = UIHelper.getType4FileIdFromArea(area);
                ControlTlv controlTlv = ((STType4Tag) myTag).getCCFileTlv(fileId);

                memoryAreaSizeInBytes = controlTlv.getMaxFileSize();

            } else {
                if (myTag instanceof MultiAreaInterface) {
                    memoryAreaSizeInBytes = ((MultiAreaInterface) myTag).getAreaSizeInBytes(area);
                } else {
                    memoryAreaSizeInBytes = myTag.getMemSizeInBytes();
                }
            }
        } catch (STException e) {
            e.printStackTrace();
        }
        return memoryAreaSizeInBytes;
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
        getMenuInflater().inflate(toolbar_res, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        return mMenu.selectItem(this, item);
    }

    class ContentViewAsync extends AsyncTask<Void, Integer, Boolean>
    {
        byte mBuffer[] = null;
        NFCTag mTag;
        int  mArea;

        public ContentViewAsync(NFCTag myTag) {
            mTag = myTag;
        }
        public ContentViewAsync(NFCTag myTag, int myArea) {
            mTag = myTag;
            mArea = myArea;
        }

        public ContentViewAsync(byte[] buffer) {
            mBuffer = buffer;
        }

        protected Boolean doInBackground(Void...arg0) {
            if (mBuffer == null) {
                try {
                    if (mTag instanceof Type4Tag) {
                        // Tag type 4
                        int size = getMemoryAreaSizeInBytes(((Type4Tag)mTag), mArea);
                        mNumberOfBytes = size;
                        mStartAddress = 0;

                        int fileId = UIHelper.getType4FileIdFromArea(mArea);

                        // inform user that a read will be performed
                        snackBarUiThread();

                        mBuffer = ((Type4Tag)mTag).readBytes(fileId,0,size);
                        int nbrOfBytesRead = 0;
                        if (mBuffer != null) {
                            nbrOfBytesRead = mBuffer.length;
                        }
                        if (nbrOfBytesRead != mNumberOfBytes) {
                            showToast(R.string.error_during_read_operation, nbrOfBytesRead);
                        }
                    } else {
                        // Type 5
                        mBuffer = getTag().readBytes(mStartAddress, mNumberOfBytes);
                        // Warning: readBytes() may return less bytes than requested
                        int nbrOfBytesRead = 0;
                        if (mBuffer != null) {
                            nbrOfBytesRead = mBuffer.length;
                        }
                        if (nbrOfBytesRead != mNumberOfBytes) {
                            showToast(R.string.error_during_read_operation, nbrOfBytesRead);
                        }
                    }
                } catch (STException e) {
                    Log.e(TAG, e.getMessage());
                    showToast(R.string.Command_failed);
                    return false;
                }

            } else {
                // buffer already initialized by constructor - no need to read Tag.
                // Nothing to do
            }
            return true;
        }

        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (mBuffer != null) {
                mAdapter = new CustomListAdapter(mBuffer);
                lv = (ListView) findViewById(R.id.readBlocksListView);
                lv.setAdapter(mAdapter);
            }

        }

        private void snackBarUiThread(){
            runOnUiThread (new Thread(new Runnable() {
                public void run() {
                    // inform user that a read will be performed
                    Snackbar snackbar = Snackbar.make(mChildView , "", Snackbar.LENGTH_LONG);
                    snackbar.setText(getString(R.string.reading_x_bytes_starting_y_address,mNumberOfBytes,mStartAddress));
                    snackbar.setActionTextColor(getResources().getColor(R.color.white));
                    snackbar.show();
                }
            }));
        }
    }

    private int convertItemToBytesUnit(int value) {
            return value * 4;
    }

    @Override
    public void onClick(View v) {

        try {
            if (mUnitInBytes) {
                mStartAddress = Integer.parseInt(mStartAddressEditText.getText().toString());
            } else {
                int valInBlock = Integer.parseInt(mStartAddressEditText.getText().toString());
                mStartAddress = convertItemToBytesUnit(valInBlock);
            }
        } catch (Exception e) {
            STLog.e("Bad Start Address" + e.getMessage());
            showToast(R.string.bad_start_address);
            return;
        }

        try {
            if (mUnitInBytes) {
                mNumberOfBytes = Integer.parseInt(mNbrOfBytesEditText.getText().toString());
            } else {
                int valInBlock = Integer.parseInt(mNbrOfBytesEditText.getText().toString());
                mNumberOfBytes = convertItemToBytesUnit(valInBlock);
            }
        } catch (Exception e) {
            STLog.e("Bad Numbers of Bytes" + e.getMessage());
            showToast(R.string.bad_number_of_bytes);
            return;
        }
        // Hide Soft Keyboard
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        Snackbar snackbar = Snackbar.make(v, "", Snackbar.LENGTH_LONG);
        snackbar.setAction(getString(R.string.reading_x_bytes_starting_y_address, mNumberOfBytes, mStartAddress), this);

        snackbar.setActionTextColor(getResources().getColor(R.color.white));
        snackbar.show();
        if (getTag() instanceof Type5Tag) {
            startType5ReadingAndDisplaying(mStartAddress, mNumberOfBytes);
        } else {

            // by defaut - read first area
            startType4ReadingAndDisplaying(getTag(), AREA1);
        }
    }

    private void startType5ReadingAndDisplaying (int startAddress, int numberOfBytes) {
        mStartAddress = startAddress;
        mNumberOfBytes = numberOfBytes;
        mStartAddress = 0;
        mNumberOfBytes = 510;
        contentView = new ContentViewAsync(getTag());
        contentView.execute();
    }

    private void startDisplaying (byte[] data) {
        contentView = new ContentViewAsync(data);
        contentView.execute();
    }
    private void startType4ReadingAndDisplaying(NFCTag tag, int area) {
        contentView = new ContentViewAsync(tag, area);
        contentView.execute();
    }

    public void onPause() {
        if (contentView != null)
                contentView.cancel(true);

        super.onPause();
    }

    class CustomListAdapter extends BaseAdapter {

        byte[] mBuffer;

        public CustomListAdapter(byte[] buffer) {

            mBuffer = buffer;
        }

        //get read_list_items count
        @Override
        public int getCount() {
            try {
                return Helper.divisionRoundedUp(mBuffer.length, NBR_OF_BYTES_PER_RAW);
            } catch (STException e) {
                e.printStackTrace();
                return 0;
            }
        }

        //get read_list_items position
        @Override
        public Object getItem(int position) {
            return position;
        }

        //get read_list_items id at selected position
        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            View listItem = convertView;
            String data;
            Byte myByte;
            int address;
            char char1 = ' ';
            char char2 = ' ';
            char char3 = ' ';
            char char4 = ' ';
            String byte1Str = "  ";
            String byte2Str = "  ";
            String byte3Str = "  ";
            String byte4Str = "  ";

            // The data are now read by Byte but we will still format the display by raw of 4 Bytes

            // Get the 4 Bytes to display on this raw
            address = pos * NBR_OF_BYTES_PER_RAW;
            if(address < mBuffer.length) {
                myByte = mBuffer[address];
                byte1Str = Helper.convertByteToHexString(myByte).toUpperCase();
                char1 = getChar(myByte);
            }

            address = pos * NBR_OF_BYTES_PER_RAW + 1;
            if(address < mBuffer.length) {
                myByte = mBuffer[address];
                byte2Str = Helper.convertByteToHexString(myByte).toUpperCase();
                char2 = getChar(myByte);
            }

            address = pos * NBR_OF_BYTES_PER_RAW + 2;
            if(address < mBuffer.length) {
                myByte = mBuffer[address];
                byte3Str = Helper.convertByteToHexString(myByte).toUpperCase();
                char3 = getChar(myByte);
            }

            address = pos * NBR_OF_BYTES_PER_RAW + 3;
            if(address < mBuffer.length) {
                myByte = mBuffer[address];
                byte4Str = Helper.convertByteToHexString(myByte).toUpperCase();
                char4 = getChar(myByte);
            }

            if (listItem == null) {
                //set the main ListView's layout
                listItem = getLayoutInflater().inflate(R.layout.read_fragment_item, parent, false);
            }
            TextView addresssTextView = (TextView) listItem.findViewById(R.id.addrTextView);
            TextView hexValuesTextView = (TextView) listItem.findViewById(R.id.hexValueTextView);
            TextView asciiValueTextView = (TextView) listItem.findViewById(R.id.asciiValueTextView);

            String startAddress = String.format("%s %3d: ", getResources().getString(R.string.addr), mStartAddress + pos * NBR_OF_BYTES_PER_RAW);
            addresssTextView.setText(startAddress);

            data = String.format("%s %s %s %s", byte1Str, byte2Str, byte3Str, byte4Str);
            hexValuesTextView.setText(data);

            data = String.format("  %c%c%c%c", char1, char2, char3, char4);
            asciiValueTextView.setText(data);

            return listItem;
        }
    }

    private char getChar(byte myByte) {
        char myChar = ' ';

        if(myByte > 0x20) {
            myChar = (char) (myByte & 0xFF);
        }

        return myChar;
    }

}

