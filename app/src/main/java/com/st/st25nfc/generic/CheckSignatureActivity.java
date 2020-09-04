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

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.provider.DocumentFile;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.st.st25nfc.R;
import com.st.st25nfc.generic.util.UIHelper;
import com.st.st25sdk.About;
import com.st.st25sdk.Helper;
import com.st.st25sdk.STException;
import com.st.st25sdk.SignatureInterface;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Set;


public class CheckSignatureActivity extends STFragmentActivity implements NavigationView.OnNavigationItemSelectedListener {

    // Set here the Menu and the Toolbar to use for this activity
    private int toolbar_res = R.menu.toolbar_empty;

    static final String TAG = "CheckSign";
    private Handler mHandler;
    private SignatureInterface mSignatureInterface;
    FragmentManager mFragmentManager;

    private TextView mCertificatesLocationTextView;
    private TextView mKeyIdTextView;
    private TextView mWarningTextView;
    private TextView mCertificateTextView;
    private TextView mSignatureStatusTextView;
    private TextView mTagSignatureTextView;

    private Uri mCertificateLocationUri;

    private SharedPreferences mSharedPreferences;
    private final String PREFS_NAME = "CERTIFICATES_LOCATION";
    private final String SHARED_PREFERENCE_KEY = "certificatesLocationUri";

    private final int SELECT_ST25_CERTIFICATES_DIRECTORY = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_layout);

        // Inflate content of FrameLayout
        FrameLayout frameLayout=(FrameLayout) findViewById(R.id.frame_content);
        View childView = getLayoutInflater().inflate(R.layout.fragment_check_signature, null);
        frameLayout.addView(childView);

        if (super.getTag() == null) {
            showToast(R.string.invalid_tag);
            goBackToMainActivity();
            return;
        }

        try {
            mSignatureInterface = (SignatureInterface) MainActivity.getTag();
        } catch (ClassCastException e) {
            showToast(R.string.tag_not_implementing_signature);
            return;
        }

        mHandler = new Handler();
        mFragmentManager = getSupportFragmentManager();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
        mMenu.inflateMenu(navigationView);

        mCertificatesLocationTextView = (TextView) findViewById(R.id.certificatesLocationTextView);
        Button changeCertificatesLocationButton = (Button) findViewById(R.id.changeCertificatesLocationButton);
        changeCertificatesLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeCertificatesLocation();
            }
        });

        Button checkSignatureButton = (Button) findViewById(R.id.checkSignatureButton);
        checkSignatureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkSignature();
            }
        });

        mKeyIdTextView = (TextView) findViewById(R.id.keyIdTextView);
        mCertificateTextView = (TextView) findViewById(R.id.certificateTextView);
        mSignatureStatusTextView = (TextView) findViewById(R.id.signatureStatusTextView);
        mTagSignatureTextView = (TextView) findViewById(R.id.tagSignatureTextView);

        mWarningTextView = (TextView) findViewById(R.id.warningTextView);
        mWarningTextView.setVisibility(View.GONE);

        mSharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        mCertificateLocationUri = getLastCertificatesLocation();
        if (mCertificateLocationUri != null) {
            displayCertificateLocation();
        }
    }

    /**
     * Get the Certificates Location saved in SharedPreferences (if any)
     * @return
     */
    private Uri getLastCertificatesLocation() {
        if (mSharedPreferences == null) return null;

        try {
            String location = mSharedPreferences.getString(SHARED_PREFERENCE_KEY, "");

            if ((location == null) || location.equals("")) {
                return null;
            } else {
                Uri uri = Uri.parse(location);
                return uri;
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Save Certificates Location in SharedPreferences
     */
    private void saveCertificatesLocation(Uri certificatesLocationUri) {
        if (mSharedPreferences == null) return;

        SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();

        if (sharedPreferencesEditor != null) {
            sharedPreferencesEditor.putString(SHARED_PREFERENCE_KEY, certificatesLocationUri.toString());
            sharedPreferencesEditor.commit();
        }
    }

    private void changeCertificatesLocation() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        startActivityForResult(Intent.createChooser(i, "Select the directory containing ST25 Certificates"), SELECT_ST25_CERTIFICATES_DIRECTORY);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case SELECT_ST25_CERTIFICATES_DIRECTORY:
                if (data != null) {
                    mCertificateLocationUri = data.getData();
                    saveCertificatesLocation(mCertificateLocationUri);
                    displayCertificateLocation();
                }
                break;
        }
    }

    private void displayCertificateLocation() {
        if (mCertificateLocationUri == null) {
            return;
        }

        // The URI contains:
        // "%2F" for '/'
        // "%3A" for ':'
        // Convert them to a readable format
        String certificateLocationString = "";
        try {
            certificateLocationString = URLDecoder.decode(mCertificateLocationUri.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        mCertificatesLocationTextView.setText(certificateLocationString);
    }

    private void checkSignature() {

        if (!isSdkWithSignature()) {
            showToast(R.string.feature_under_nda);
            return;
        }

        new Thread(new Runnable() {
            public void run() {
                try {
                    final byte keyId = mSignatureInterface.getKeyIdNDA();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mTagSignatureTextView.setText("");
                            mSignatureStatusTextView.setText("");
                            mCertificateTextView.setText("");

                            int key = (keyId & 0xFF);
                            String keyIdStr = String.format("%02d", key);
                            mKeyIdTextView.setText(keyIdStr);

                            if (key == 0) {
                                mWarningTextView.setVisibility(View.VISIBLE);
                            } else {
                                mWarningTextView.setVisibility(View.GONE);
                            }
                        }
                    });

                    InputStream certificateInputStream = findCertificate(keyId);

                    if(certificateInputStream == null) {
                        displayCertificateError(keyId);
                        return;
                    }

                    boolean isSignatureValid = mSignatureInterface.isSignatureOkNDA(certificateInputStream);

                    displaySignatureStatus(isSignatureValid);

                    // Read tag' signature and display it
                    byte[] signature = mSignatureInterface.readSignatureNDA();
                    displaySignature(signature);

                } catch (STException e) {
                    switch (e.getError()) {
                        case TAG_NOT_IN_THE_FIELD:
                            showToast(R.string.tag_not_in_the_field);
                            break;
                        case IMPLEMENTED_IN_NDA_VERSION:
                            showToast(R.string.feature_under_nda);
                            break;
                        case MISSING_LIBRARY:
                            showToast(R.string.spongy_castle_crypto_libraries_are_missing);
                            break;
                        default:
                            e.printStackTrace();
                            showToast(R.string.error_while_checking_the_signature);
                            // Reset the satus field
                            displaySignatureStatus(false);
                    }
                }
            }
        }).start();
    }


    private boolean isSdkWithSignature() {
        Set<String> sdkFeatureList = About.getExtraFeatureList();

        for (String featureString: sdkFeatureList) {
            if (featureString.equals("signature")) {
                return true;
            }
        }

        return false;
    }

    /**
     * This function will check all the files present in the "Certificate Location" folder
     * and look for one with the requested keyId
     *
     * @param keyId
     * @return an InputStream to handle the file or null if no file was found
     */
    private InputStream findCertificate(byte keyId) {
        String searchedKeyIdString = "KID " + String.format("%02x", keyId);

        if (mCertificateLocationUri == null) {
            return null;
        }

        DocumentFile directoryDocumentFile = DocumentFile.fromTreeUri(getApplicationContext(), mCertificateLocationUri);
        DocumentFile[] files = directoryDocumentFile.listFiles();

        if (files == null) {
            return null;
        }

        for (DocumentFile file : files) {
            if (file.isFile())
            {
                String fileName = file.getName();
                if (fileName.contains(".crt")) {

                    Uri fileUri = file.getUri();
                    try {
                        InputStream fileInputStream = getContentResolver().openInputStream(fileUri);

                        String certificateString = UIHelper.convertInputStreamToString(fileInputStream);
                        if (certificateString.contains(searchedKeyIdString)) {

                            displayCertificateName(fileName);

                            // Return a fresh new InputStream
                            fileInputStream = getContentResolver().openInputStream(fileUri);

                            return fileInputStream;
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }

            }

        }

        // KeyId not found!
        return null;
    }

    private void displayCertificateError(final byte keyId) {
        runOnUiThread(new Runnable() {
            public void run() {
                String message = getResources().getString(R.string.certificate_with_keyid_not_found, String.format("%02x", keyId));

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CheckSignatureActivity.this);

                // set title
                alertDialogBuilder.setTitle(getString(R.string.error));

                // set dialog message
                alertDialogBuilder
                        .setMessage(message)
                        .setCancelable(true)
                        .setPositiveButton(getString(R.string.continue_message),new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });

                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();
            }
        });
    }

    private void displayCertificateName(final String certificateName) {
        runOnUiThread(new Runnable() {
            public void run() {
                mCertificateTextView.setText(certificateName);
            }
        });
    }

    private void displaySignatureStatus(final boolean isSignatureValid) {
        // Warning: Function called from background thread! Post a request to the UI thread
        mHandler.post(new Runnable() {
            public void run() {
                if(isSignatureValid) {
                    mSignatureStatusTextView.setTextColor(getResources().getColor(R.color.st_light_green));
                    mSignatureStatusTextView.setText(R.string.signature_ok);
                } else {
                    mSignatureStatusTextView.setTextColor(getResources().getColor(R.color.st_light_purple));
                    mSignatureStatusTextView.setText(R.string.signature_nok);

                }
            }
        });
    }

    private void displaySignature(final byte[] signature) {
        // Warning: Function called from background thread! Post a request to the UI thread
        mHandler.post(new Runnable() {
            public void run() {
                mTagSignatureTextView.setText(Helper.convertHexByteArrayToString(signature));
            }
        });
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
}
