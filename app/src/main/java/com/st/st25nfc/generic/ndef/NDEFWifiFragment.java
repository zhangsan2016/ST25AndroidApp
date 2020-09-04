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

package com.st.st25nfc.generic.ndef;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.st.st25nfc.R;
import com.st.st25sdk.ndef.NDEFMsg;
import com.st.st25sdk.ndef.WifiRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class NDEFWifiFragment extends NDEFRecordFragment implements AdapterView.OnItemSelectedListener {

    final static String TAG = "NDEFWifiFragment";

    class AccessPoint {
        private int statusFlag;
        private String ssid;
        private int authentication;     // See WIFI_AUTH_* in WifiRecord
        private int encryption;         // See WIFI_ENCR_* in WifiRecord
        private String key;

        public static final int ACCESS_POINT_SAVED_IN_PHONE_MEMORY = 0x01;
        public static final int ACCESS_POINT_CURRENTLY_AVAILABLE = 0x02;
        public static final int ACCESS_POINT_DEFINED_IN_NFC_TAG = 0x04;

        public AccessPoint(String ssid, int authentication, int encryption, int flag) {
            this(ssid, authentication, encryption, flag, "");
        }

        public AccessPoint(String ssid, int authentication, int encryption, int flag, String key) {
            this.ssid = ssid;
            this.authentication = authentication;
            this.encryption = encryption;
            this.key = key;
            this.statusFlag = flag;
        }

        public int getAuthentication() {
            return authentication;
        }

        public void setAuthentication(int authentication) {
            this.authentication = authentication;
        }

        public String getSsid() {
            return ssid;
        }

        public void setSsid(String ssid) {
            this.ssid = ssid;
        }

        public int getEncryption() {
            return encryption;
        }

        public void setEncryption(int encryption) {
            this.encryption = encryption;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public int getStatusFlag() {
            return statusFlag;
        }

        public void setStatusFlag(int statusFlag) {
            this.statusFlag = statusFlag;
        }

        // The toString() method is extremely important to making this class work with a Spinner
        public String toString()
        {
            return(ssid);
        }

        // Override equals() and hashCode() so that Access Points with the same SSID are considered
        // as identical
        @Override
        public boolean equals(Object obj) {
            // if both the object references are referring to the same object.
            if(this == obj)
                return true;

            // Check if the argument is of the right type by comparing the classes.
            // if(!(obj instanceof AccessPoint)) return false; ---> avoid.
            if(obj == null || obj.getClass()!= this.getClass())
                return false;

            // type casting of the argument.
            AccessPoint accessPoint = (AccessPoint) obj;

            boolean result = (accessPoint.ssid.equals(this.ssid));
            return result;
        }

        @Override
        public int hashCode() {
            return ssid.hashCode();
        }
    }

    private View mView;

    private ArrayAdapter<AccessPoint> mSpinnerAdapter;

    private WifiManager mWifiManager = null;

    // This list is the concatenation of:
    // - the access point defined in the tag (if any)
    // - the access points saved in the phone's memory
    // - the access points currently available
    private List<AccessPoint> mAccessPointList = new ArrayList<AccessPoint>();

    private AccessPoint mTagAccessPoint = null;

    // We should keep track of the SSID currently selected in the Spinner because we need
    // this info when the list is updated (to keep the same item selected).
    private String mSelectedAccessPoint = null;

    // List of Wifi Access Points saved in the phone's memory
    private List<WifiConfiguration> mSavedAccessPoints = null;

    // List of Wifi Access Points currently available
    private List<ScanResult> mAvailableAccessPoints = null;

    private WifiRecord mWifiRecord;
    private int mAction;
    private WifiReceiver mWifiReceiver = null;

    private Spinner mSsidSpinner;
    private Spinner mAuthTypeSpinner;
    private Spinner mEncrTypeSpinner;
    private EditText mNetKeyEditText;

    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1001;


    public static NDEFWifiFragment newInstance(Context context) {
        NDEFWifiFragment f = new NDEFWifiFragment();
        /* If needed, pass some argument to the fragment
        Bundle args = new Bundle();
        args.putInt("index", index);
        f.setArguments(args);
        */
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_ndef_wifi, container, false);
        mView = view;

        Bundle bundle = getArguments();
        if (bundle == null) {
            Log.e(TAG, "Fatal error! Arguments are missing!");
            return null;
        }

        NDEFMsg ndefMsg = (NDEFMsg) bundle.getSerializable(NDEFRecordFragment.NDEFKey);
        int recordNbr = bundle.getInt(NDEFRecordFragment.RecordNbrKey);
        mWifiRecord = (WifiRecord) ndefMsg.getNDEFRecord(recordNbr);

        mSsidSpinner = (Spinner) mView.findViewById(R.id.ssidSpinner);
        mAuthTypeSpinner = (Spinner) mView.findViewById(R.id.authTypeList);
        mEncrTypeSpinner = (Spinner) mView.findViewById(R.id.encrTypeList);
        mNetKeyEditText = (EditText) mView.findViewById(R.id.netKeyTxt);

        Context context = getActivity().getApplicationContext();
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (!mWifiManager.isWifiEnabled()) {
            Toast toast = Toast.makeText(context, R.string.wifi_is_not_enabled, Toast.LENGTH_LONG);
            toast.show();

        } else {
            startWifiScanning();
        }

        mSpinnerAdapter = new WifiSpinnerAdapter(context, R.layout.wifi_spinner_row);
        mSsidSpinner.setAdapter(mSpinnerAdapter);

        mAction = bundle.getInt(NDEFEditorFragment.EditorKey);

        if( (mAction == NDEFEditorFragment.EDIT_NDEF_RECORD) ||
            (mAction == NDEFEditorFragment.VIEW_NDEF_RECORD) ) {
            // There is an Access Point defined in the Tag
            mTagAccessPoint = new AccessPoint(mWifiRecord.getSSID(),
                                              mWifiRecord.getAuthType(),
                                              mWifiRecord.getEncrType(),
                                              AccessPoint.ACCESS_POINT_DEFINED_IN_NFC_TAG,
                                              mWifiRecord.getEncrKey());
        } else {
            mTagAccessPoint = null;
        }

        getSavedAccessPoints();

        // NB: The Access Point List will be updated later on when we receive the list of available Access Points
        buildAccessPointList();

        if (mWifiManager.isWifiEnabled()) {
            if(mAction == NDEFEditorFragment.VIEW_NDEF_RECORD) {
                // We are displaying an existing record. By default it is not editable
                ndefRecordEditable(false);
            } else {
                // We are adding a new TextRecord or editing an existing record
                ndefRecordEditable(true);
            }

        } else {
            ndefRecordEditable(false);
        }

        return mView;
    }

    private void selectDefaultAccessPoint() {
        // By default, we select the SSID present in the tag (if any).
        if (mTagAccessPoint != null) {
            mSelectedAccessPoint = mTagAccessPoint.getSsid();

        } else {
            // Otherwise we select the Access Point to which the phone is currently connected (if any)
            if (mWifiManager.isWifiEnabled()) {
                WifiInfo info = mWifiManager.getConnectionInfo ();
                if (info != null) mSelectedAccessPoint = info.getSSID().replaceAll("\"", "");  // Remove the "" around the SSID
            }
        }
    }


    private void getSavedAccessPoints() {
        mSavedAccessPoints = mWifiManager.getConfiguredNetworks();
    }

    // The mAccessPointList is the concatenation of:
    // - the access point defined in the tag (if any)
    // - the access points saved in the phone's memory
    // - the access points currently available
    private void buildAccessPointList() {

        mAccessPointList.clear();

        if (mSpinnerAdapter == null) return;

        if (mTagAccessPoint != null) {
            addAccessPoint(mTagAccessPoint);
        }

        // Iterate through the list of Saved Access Points
        if (mSavedAccessPoints != null) {
            for (WifiConfiguration wifiConfiguration : mSavedAccessPoints) {

                String ssid = wifiConfiguration.SSID.replaceAll("\"", "");  // Remove the "" around the SSID
                int auth = WifiRecord.WIFI_AUTH_OPEN;
                int encr = WifiRecord.WIFI_ENCR_NONE;

                if ((wifiConfiguration.preSharedKey == null) &&
                        (wifiConfiguration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE) == true) &&
                        (wifiConfiguration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK) == false) ) {
                    // we have an open network
                    auth = WifiRecord.WIFI_AUTH_OPEN;
                    encr = WifiRecord.WIFI_ENCR_NONE;

                } else if ((wifiConfiguration.preSharedKey != null) &&
                        (wifiConfiguration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE) == false) &&
                        (wifiConfiguration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK) == true) ) {
                    // we have a WPAPSK or WPA2PSK
                    auth = WifiRecord.WIFI_AUTH_WPA2PSK;
                    encr = WifiRecord.WIFI_ENCR_AES;
                } else {
                    Log.w(TAG, "Unknown authentication or encryption");
                    // Fallback scenario
                    if (wifiConfiguration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE)) {
                        auth = WifiRecord.WIFI_AUTH_OPEN;
                        encr = WifiRecord.WIFI_ENCR_NONE;
                    } else {
                        auth = WifiRecord.WIFI_AUTH_WPA2PSK;
                        encr = WifiRecord.WIFI_ENCR_AES;
                    }
                }

                addAccessPoint(new AccessPoint(ssid, auth, encr, AccessPoint.ACCESS_POINT_SAVED_IN_PHONE_MEMORY));
            }
        }

        // Iterate through the list of Available Access Points
        if (mAvailableAccessPoints != null) {
            for (ScanResult scanResult : mAvailableAccessPoints) {
                String ssid = scanResult.SSID;
                int auth = WifiRecord.WIFI_AUTH_OPEN;
                int encr = WifiRecord.WIFI_ENCR_NONE;

                if (scanResult.capabilities.contains("WPA2-PSK")) {
                    auth = WifiRecord.WIFI_AUTH_WPA2PSK;
                    encr = WifiRecord.WIFI_ENCR_AES;
                } else if (scanResult.capabilities.contains("WPA-PSK")) {
                    auth = WifiRecord.WIFI_AUTH_WPAPSK;
                    encr = WifiRecord.WIFI_ENCR_AES;
                } else if (scanResult.capabilities.contains("WPA2")) {
                    auth = WifiRecord.WIFI_AUTH_WPA2;
                    encr = WifiRecord.WIFI_ENCR_AES;
                } else if (scanResult.capabilities.contains("WPA")) {
                    auth = WifiRecord.WIFI_AUTH_WPA;
                    encr = WifiRecord.WIFI_ENCR_AES;
                } else {
                    Log.w(TAG, "Unknown authentication or encryption. We keep the default values (Open and None)");
                }

                addAccessPoint(new AccessPoint(ssid, auth, encr, AccessPoint.ACCESS_POINT_CURRENTLY_AVAILABLE));
            }
        }

        // Update the SSID spinner
        mSpinnerAdapter.clear();

        // Sort the Access Points list by alphabetical order
        Collections.sort(mAccessPointList, new Comparator<AccessPoint>() {
            @Override
            public int compare(AccessPoint accessPoint1, AccessPoint accessPoint2) {
                return accessPoint1.toString().compareToIgnoreCase(accessPoint2.toString());
            }
        });

        mSpinnerAdapter.addAll(mAccessPointList);
        mSpinnerAdapter.notifyDataSetChanged();

        setContent();
    }

    /**
     * Add an Access Point to mAccessPointList.
     * This function will check that it is not a duplicate
     */
    private void addAccessPoint(AccessPoint newAccessPoint) {

        int position = getAccessPointPosition(newAccessPoint.getSsid());
        if (position == -1) {
            // This is a new Item
            mAccessPointList.add(newAccessPoint);
        } else {
            // This is an existing Item. Only copy the statusFlag
            AccessPoint existingAccesPoint = mAccessPointList.get(position);

            int statusFlag = existingAccesPoint.getStatusFlag() | newAccessPoint.getStatusFlag();
            existingAccesPoint.setStatusFlag(statusFlag);
        }
    }

    /**
     * This function will go through the mAccessPointList and look for an SSID with the given name.
     * @param name
     * @return The position in the list or (-1) if not found
     */
    private int getAccessPointPosition(String name) {
        int position = 0;
        int hashCode = name.hashCode();

        if ((name == null) || (name.length() == 0) ) {
            return (-1);
        }

        for (AccessPoint accessPoint : mAccessPointList) {
            if (accessPoint.hashCode() == hashCode) {
                return position;
            }
            position++;
        }

        // String not found!
        return (-1);
    }

    private void printAccessPointConfig(WifiConfiguration config) {
        Log.d("WifiPreference", "SSID " + config.SSID);
        Log.d("WifiPreference", "PASSWORD " + config.preSharedKey);
        Log.d("WifiPreference", "ALLOWED ALGORITHMS -------------");
        Log.d("WifiPreference", "LEAP " + config.allowedAuthAlgorithms.get(WifiConfiguration.AuthAlgorithm.LEAP));
        Log.d("WifiPreference", "OPEN " + config.allowedAuthAlgorithms.get(WifiConfiguration.AuthAlgorithm.OPEN));
        Log.d("WifiPreference", "SHARED " + config.allowedAuthAlgorithms.get(WifiConfiguration.AuthAlgorithm.SHARED));
        Log.d("WifiPreference", "GROUP CIPHERS--------------------");
        Log.d("WifiPreference", "CCMP " + config.allowedGroupCiphers.get(WifiConfiguration.GroupCipher.CCMP));
        Log.d("WifiPreference", "TKIP " + config.allowedGroupCiphers.get(WifiConfiguration.GroupCipher.TKIP));
        Log.d("WifiPreference", "WEP104 " + config.allowedGroupCiphers.get(WifiConfiguration.GroupCipher.WEP104));
        Log.d("WifiPreference", "WEP40  " + config.allowedGroupCiphers.get(WifiConfiguration.GroupCipher.WEP40));
        Log.d("WifiPreference", "KEYMGMT -------------------------");
        Log.d("WifiPreference", "IEEE8021X " + config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X));
        Log.d("WifiPreference", "NONE " + config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE));
        Log.d("WifiPreference", "WPA_EAP " + config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP));
        Log.d("WifiPreference", "WPA_PSK " + config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK));
        Log.d("WifiPreference", "PairWiseCipher-------------------");
        Log.d("WifiPreference", "CCMP " + config.allowedPairwiseCiphers.get(WifiConfiguration.PairwiseCipher.CCMP));
        Log.d("WifiPreference", "NONE " + config.allowedPairwiseCiphers.get(WifiConfiguration.PairwiseCipher.NONE));
        Log.d("WifiPreference", "TKIP " + config.allowedPairwiseCiphers.get(WifiConfiguration.PairwiseCipher.TKIP));
        Log.d("WifiPreference", "Protocols-------------------------");
        Log.d("WifiPreference", "RSN " + config.allowedProtocols.get(WifiConfiguration.Protocol.RSN));
        Log.d("WifiPreference", "WPA " + config.allowedProtocols.get(WifiConfiguration.Protocol.WPA));
        Log.d("WifiPreference", "WEP Key Strings--------------------");
        String[] wepKeys = config.wepKeys;
        Log.d("WifiPreference", "WEP KEY 0 " + wepKeys[0]);
        Log.d("WifiPreference", "WEP KEY 1 " + wepKeys[1]);
        Log.d("WifiPreference", "WEP KEY 2 " + wepKeys[2]);
        Log.d("WifiPreference", "WEP KEY 3 " + wepKeys[3]);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        stopBroadcastReceiver();
    }

    private void stopBroadcastReceiver() {
        if (mWifiReceiver != null) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.unregisterReceiver(mWifiReceiver);
                mWifiReceiver = null;
            }
        }
    }

    /**
     * Scan the currently available Wifi Access Points.
     */
    private  void startWifiScanning() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && getActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
            // Wait for permission. The callback onRequestPermissionsResult will be called
        }else{
            // Permission was previously granted or legacy device with permission defined at installation time

            mWifiReceiver = new WifiReceiver();

            Toast toast = Toast.makeText(getActivity().getApplicationContext(), R.string.wifi_scan_starting, Toast.LENGTH_SHORT);
            toast.show();

            // Register a broadcast receiver that will be called when the number of wifi connections changed
            getActivity().registerReceiver(mWifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

            mWifiManager.startScan();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startWifiScanning();
            } else {
                showToast(R.string.location_permission_not_granted);
            }
        }
    }

    /**
     * The content from the NDEF Record is displayed in the Fragment
     */
    public void setContent() {

        if ((mAccessPointList != null) && (mAccessPointList.size() > 0)) {
            int position = -1;

            if (mSelectedAccessPoint != null){
                position = getAccessPointPosition(mSelectedAccessPoint);
            }

            // If no access point currently selected, we select the first access point of the list.
            if (position == -1) {
                position = 0;
            }

            AccessPoint accessPoint = mAccessPointList.get(position);

            mSsidSpinner.setSelection(position);
            setAuthTypeSpinnerSelection(accessPoint.getAuthentication());
            setEncrTypeSpinnerSelection(accessPoint.getEncryption());
            mNetKeyEditText.setText(accessPoint.getKey());
        }
    }

    private void setAuthTypeSpinnerSelection(int authType) {

        /*
            Spinner items:
            0: "OPEN"
            1: "WPAPSK"
            2: "SHARED"
            3: "WPA"
            4: "WPA2"
            5: "WPA2PSK
         */
        if ((authType & WifiRecord.WIFI_AUTH_OPEN) == WifiRecord.WIFI_AUTH_OPEN) {
            mAuthTypeSpinner.setSelection(0);
        } else if ((authType & WifiRecord.WIFI_AUTH_WPAPSK) == WifiRecord.WIFI_AUTH_WPAPSK) {
            mAuthTypeSpinner.setSelection(1);
        } else if ((authType & WifiRecord.WIFI_AUTH_SHARED) == WifiRecord.WIFI_AUTH_SHARED) {
            mAuthTypeSpinner.setSelection(2);
        } else if ((authType & WifiRecord.WIFI_AUTH_WPA) == WifiRecord.WIFI_AUTH_WPA) {
            mAuthTypeSpinner.setSelection(3);
        } else if ((authType & WifiRecord.WIFI_AUTH_WPA2) == WifiRecord.WIFI_AUTH_WPA2) {
            mAuthTypeSpinner.setSelection(4);
        } else if ((authType & WifiRecord.WIFI_AUTH_WPA2PSK) == WifiRecord.WIFI_AUTH_WPA2PSK) {
            mAuthTypeSpinner.setSelection(5);
        } else {
            mAuthTypeSpinner.setSelection(0);
        }
    }

    private void setEncrTypeSpinnerSelection(int encrType) {
        /*
            Spinner items:
            0: "NONE"
            1: "WEP"
            2: "TKIP"
            3: "AES"
         */

        if ((encrType & WifiRecord.WIFI_ENCR_NONE) == WifiRecord.WIFI_ENCR_NONE) {
            mEncrTypeSpinner.setSelection(0);
        } else if ((encrType & WifiRecord.WIFI_ENCR_WEP) == WifiRecord.WIFI_ENCR_WEP) {
            mEncrTypeSpinner.setSelection(1);
        } else if ((encrType & WifiRecord.WIFI_ENCR_TKIP) == WifiRecord.WIFI_ENCR_TKIP) {
            mEncrTypeSpinner.setSelection(2);
        } else if ((encrType & WifiRecord.WIFI_ENCR_AES) == WifiRecord.WIFI_ENCR_AES) {
            mEncrTypeSpinner.setSelection(3);
        } else {
            mEncrTypeSpinner.setSelection(0);
        }
    }

    /**
     * The content from the fragment is saved into the NDEF Record
     */
    @Override
    public void updateContent() {
        String ssid;
        if (mSsidSpinner.getSelectedItem() != null) {
            ssid = mSsidSpinner.getSelectedItem().toString();
        } else {
            ssid = "NotDefined";
        }

        int authType = getSelectedAuthenticationType();
        int encrType = getSelectedEncryptionType();

        String encrKey;
        if (mNetKeyEditText.getText() != null) {
            encrKey = mNetKeyEditText.getText().toString();
        } else {
            encrKey = "NotDefined";
        }

        mWifiRecord.setSSID(ssid);
        mWifiRecord.setAuthType(authType);
        mWifiRecord.setEncrType(encrType);
        mWifiRecord.setEncrKey(encrKey);
    }

    private int getSelectedAuthenticationType() {
        int authenticatinType;

        String selectedAuthType = (String) mAuthTypeSpinner.getSelectedItem();

        switch(selectedAuthType) {
            case "OPEN":
                authenticatinType = WifiRecord.WIFI_AUTH_OPEN;
                break;
            case "WPAPSK":
                authenticatinType = WifiRecord.WIFI_AUTH_WPAPSK;
                break;
            case "SHARED":
                authenticatinType = WifiRecord.WIFI_AUTH_SHARED;
                break;
            case "WPA":
                authenticatinType = WifiRecord.WIFI_AUTH_WPA;
                break;
            case "WPA2":
                authenticatinType = WifiRecord.WIFI_AUTH_WPA2;
                break;
            default:
            case "WPA2PSK":
                authenticatinType = WifiRecord.WIFI_AUTH_WPA2PSK;
                break;
        }

        return authenticatinType;
    }

    private int getSelectedEncryptionType() {
        int encryptionType;

        String selectedEncrType = (String) mEncrTypeSpinner.getSelectedItem();

        switch(selectedEncrType) {
            case "NONE":
                encryptionType = WifiRecord.WIFI_ENCR_NONE;
                break;
            case "WEP":
                encryptionType = WifiRecord.WIFI_ENCR_WEP;
                break;
            case "TKIP":
                encryptionType = WifiRecord.WIFI_ENCR_TKIP;
                break;
            default:
            case "AES":
                encryptionType = WifiRecord.WIFI_ENCR_AES;
                break;
        }

        return encryptionType;
    }

    public void ndefRecordEditable(boolean editable) {
        mSsidSpinner.setFocusable(editable);
        mSsidSpinner.setEnabled(editable);
        mSsidSpinner.setClickable(editable);

        mAuthTypeSpinner.setFocusable(editable);
        mAuthTypeSpinner.setEnabled(editable);
        mAuthTypeSpinner.setClickable(editable);

        mEncrTypeSpinner.setFocusable(editable);
        mEncrTypeSpinner.setEnabled(editable);
        mEncrTypeSpinner.setClickable(editable);

        mNetKeyEditText.setClickable(editable);
        mNetKeyEditText.setFocusable(editable);
        mNetKeyEditText.setFocusableInTouchMode(editable);

        if (editable) {
            mSsidSpinner.setOnItemSelectedListener(this);
        } else {
            // The Fragment is no more editable. Reload its content
            selectDefaultAccessPoint();
            setContent();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        if ((mAccessPointList != null) && (mAccessPointList.size() > position)) {
            AccessPoint accessPoint = mAccessPointList.get(position);

            mSelectedAccessPoint = accessPoint.getSsid();

            setAuthTypeSpinnerSelection(accessPoint.getAuthentication());
            setEncrTypeSpinnerSelection(accessPoint.getEncryption());
            mNetKeyEditText.setText(accessPoint.getKey());
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    // Broadcast receiver notified when the number of wifi connections changed
    class WifiReceiver extends BroadcastReceiver {

        public void onReceive(Context c, Intent intent) {
            mAvailableAccessPoints = mWifiManager.getScanResults();
            buildAccessPointList();
            showToast(R.string.ssid_list_updated);

            stopBroadcastReceiver();
        }
    }

    public class WifiSpinnerAdapter extends ArrayAdapter<AccessPoint> {

        private Context context;
        private LayoutInflater mLayoutInflater;

        public WifiSpinnerAdapter(Context context, int resource) {
            super(context,  R.layout.wifi_spinner_row);
            this.context = context;

             this.mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getDropDownView(int position, View convertView,ViewGroup parent) {
            return getCustomView(position, convertView, parent, true);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent, false);
        }

        public View getCustomView(int position, View convertView, ViewGroup parent, boolean displayIcon) {

            AccessPoint accessPoint = mAccessPointList.get(position);

            View row = mLayoutInflater.inflate(R.layout.wifi_spinner_row, parent, false);

            TextView accessPointTextView = (TextView) row.findViewById(R.id.accessPointTextView);
            accessPointTextView.setText(accessPoint.getSsid());

            ImageView wifiImageView = (ImageView)row.findViewById(R.id.wifiImageView);
            ImageView memoryImageView = (ImageView)row.findViewById(R.id.memoryImageView);
            ImageView nfcImageView = (ImageView)row.findViewById(R.id.nfcImageView);

            if (displayIcon) {
                if ((accessPoint.getStatusFlag() & AccessPoint.ACCESS_POINT_CURRENTLY_AVAILABLE) == AccessPoint.ACCESS_POINT_CURRENTLY_AVAILABLE) {
                    wifiImageView.setVisibility(View.VISIBLE);
                } else {
                    wifiImageView.setVisibility(View.INVISIBLE);
                }

                if ((accessPoint.getStatusFlag() & AccessPoint.ACCESS_POINT_SAVED_IN_PHONE_MEMORY) == AccessPoint.ACCESS_POINT_SAVED_IN_PHONE_MEMORY) {
                    memoryImageView.setVisibility(View.VISIBLE);
                } else {
                    memoryImageView.setVisibility(View.INVISIBLE);
                }

                if ((accessPoint.getStatusFlag() & AccessPoint.ACCESS_POINT_DEFINED_IN_NFC_TAG) == AccessPoint.ACCESS_POINT_DEFINED_IN_NFC_TAG) {
                    nfcImageView.setVisibility(View.VISIBLE);
                } else {
                    nfcImageView.setVisibility(View.INVISIBLE);
                }

            } else {
                wifiImageView.setVisibility(View.GONE);
                memoryImageView.setVisibility(View.GONE);
                nfcImageView.setVisibility(View.GONE);
            }

            return row;
        }
    }

}


