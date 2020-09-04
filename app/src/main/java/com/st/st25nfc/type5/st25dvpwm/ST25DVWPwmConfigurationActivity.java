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

package com.st.st25nfc.type5.st25dvpwm;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.st.st25nfc.R;
import com.st.st25nfc.generic.MainActivity;
import com.st.st25nfc.generic.PwdDialogFragment;
import com.st.st25nfc.generic.STFragmentActivity;
import com.st.st25nfc.generic.STType5PwdDialogFragment;
import com.st.st25nfc.type5.Type5ConfigurationProtectionActivity;
import com.st.st25sdk.type5.st25dvpwm.ST25DVPwmTag;
import com.st.st25sdk.STException;

import java.util.List;

import static com.st.st25sdk.TagHelper.ReadWriteProtection;
import static com.st.st25sdk.TagHelper.ReadWriteProtection.READABLE_AND_WRITABLE;
import static com.st.st25sdk.TagHelper.ReadWriteProtection.READABLE_AND_WRITE_PROTECTED_BY_PWD;
import static com.st.st25sdk.TagHelper.ReadWriteProtection.READ_AND_WRITE_PROTECTED_BY_PWD;
import static com.st.st25sdk.TagHelper.ReadWriteProtection.READ_PROTECTED_BY_PWD_AND_WRITE_IMPOSSIBLE;
import static com.st.st25sdk.type5.st25dvpwm.ST25DV02KWRegisterPwmRfConfiguration.DualityManagement.FULL_DUPLEX;
import static com.st.st25sdk.type5.st25dvpwm.ST25DV02KWRegisterPwmRfConfiguration.DualityManagement.PWM_FREQ_REDUCED;
import static com.st.st25sdk.type5.st25dvpwm.ST25DV02KWRegisterPwmRfConfiguration.DualityManagement.PWM_FREQ_REDUCED_AND_ONE_QUARTER_FULL_POWER_WHILE_RF_CMD;
import static com.st.st25sdk.type5.st25dvpwm.ST25DV02KWRegisterPwmRfConfiguration.DualityManagement.PWM_IN_HZ_WHILE_RF_CMD;
import static com.st.st25sdk.type5.st25dvpwm.ST25DV02KWRegisterPwmRfConfiguration.DualityManagement.PWM_ONE_QUARTER_FULL_POWER_WHILE_RF_CMD;
import static com.st.st25sdk.type5.st25dvpwm.ST25DV02KWRegisterPwmRfConfiguration.OutputDriverTrimming;
import static com.st.st25sdk.type5.st25dvpwm.ST25DV02KWRegisterPwmRfConfiguration.DualityManagement;
import static com.st.st25sdk.type5.st25dvpwm.ST25DV02KWRegisterPwmRfConfiguration.OutputDriverTrimming.HALF_FULL_POWER;
import static com.st.st25sdk.type5.st25dvpwm.ST25DV02KWRegisterPwmRfConfiguration.OutputDriverTrimming.ONE_QUARTER_FULL_POWER;
import static com.st.st25sdk.type5.st25dvpwm.ST25DV02KWRegisterPwmRfConfiguration.OutputDriverTrimming.THREE_QUARTER_FULL_POWER;
import static com.st.st25sdk.type5.st25dvpwm.ST25DV02KWRegisterPwmRfConfiguration.OutputDriverTrimming.FULL_POWER;
import static com.st.st25sdk.type5.st25dvpwm.ST25DVPwmTag.PWM1;
import static com.st.st25sdk.type5.st25dvpwm.ST25DVPwmTag.PWM2;


public class ST25DVWPwmConfigurationActivity extends STFragmentActivity implements STType5PwdDialogFragment.STType5PwdDialogListener, NavigationView.OnNavigationItemSelectedListener {

    static final String TAG = "PwmConfig";
    private Handler mHandler;
    private ST25DVPwmTag myTag;
    FragmentManager mFragmentManager;

    private String mTagName;
    private String mTagDescription;
    private String mTagType;

    private TextView mTagNameView;
    private TextView mTagTypeView;
    private TextView mTagDescriptionView;

    private Spinner mPwm1TrimmingValueSpinner;
    private Spinner mPwm2TrimmingValueSpinner;
    private Spinner mPwmDualityManagementSpinner;
    private Spinner mPwmAccessRightsSpinner;

    // Integers indicating the item currently set in the Tag for each spinner
    private int mTagPwm1TrimmingValue = -1;
    private int mTagPwm2TrimmingValue = -1;
    private int mTagDualityManagementValue = -1;
    private int mTagPwmAccessRightsValue = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_layout);

        // Inflate content of FrameLayout
        FrameLayout frameLayout=(FrameLayout) findViewById(R.id.frame_content);
        View childView = getLayoutInflater().inflate(R.layout.fragment_st25dv02kw_pwm_configuration, null);
        frameLayout.addView(childView);

        myTag = (ST25DVPwmTag) MainActivity.getTag();
        if (myTag == null) {
            showToast(R.string.invalid_tag);
            goBackToMainActivity();
            return;
        }

        mHandler = new Handler();
        mFragmentManager = getSupportFragmentManager();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
        mMenu.inflateMenu(navigationView);

        mTagNameView = (TextView) findViewById(R.id.model_header);
        mTagTypeView = (TextView) findViewById(R.id.model_type);
        mTagDescriptionView = (TextView) findViewById(R.id.model_description);

        mPwm1TrimmingValueSpinner = (Spinner)findViewById(R.id.pwm1TrimmingValueSpinner);
        String[] trimmingValues = getResources().getStringArray(R.array.pwm_trimming_values);
        ArrayAdapter<String> trimmingAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, trimmingValues);
        trimmingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPwm1TrimmingValueSpinner.setAdapter(trimmingAdapter);

        mPwm2TrimmingValueSpinner = (Spinner)findViewById(R.id.pwm2TrimmingValueSpinner);
        mPwm2TrimmingValueSpinner.setAdapter(trimmingAdapter);

        mPwmDualityManagementSpinner = (Spinner)findViewById(R.id.pwmDualityManagementSpinner);
        String[] dualityValues = getResources().getStringArray(R.array.pwm_duality_values);
        ArrayAdapter<String> dualityAdapter = new MultiLinesArrayAdapter(this, R.layout.spinner_item, android.R.id.text1, dualityValues);
        // Specify the layout to use when the list of choices appears
        dualityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPwmDualityManagementSpinner.setAdapter(dualityAdapter);

        mPwmAccessRightsSpinner = (Spinner)findViewById(R.id.pwmAccessRightsSpinner);
        String[] permissions = getResources().getStringArray(R.array.permisisons);
        ArrayAdapter<String> accessRightsAdapter = new MultiLinesArrayAdapter(this, R.layout.spinner_item, android.R.id.text1, permissions);
        // Specify the layout to use when the list of choices appears
        accessRightsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPwmAccessRightsSpinner.setAdapter(accessRightsAdapter);

        LinearLayout pwm2TrimmingValueLayout = (LinearLayout)findViewById(R.id.pwm2TrimmingValueLayout);
        if (myTag.getNumberOfPwm() == 2) {
            pwm2TrimmingValueLayout.setVisibility(View.VISIBLE);
        } else {
            pwm2TrimmingValueLayout.setVisibility(View.GONE);
        }

        Button presentPwmPasswordButton = (Button) findViewById(R.id.presentPwmPasswordButton);
        presentPwmPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presentPwmPassword();
            }
        });

        Button changePwmPasswordButton = (Button) findViewById(R.id.changePwmPasswordButton);
        changePwmPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePwmPassword();
            }
        });

        readCurrentPwmConfiguration();
    }

    private void presentPwmPassword() {
        new Thread(new Runnable() {
            public void run() {
                int passwordNumber = ST25DVPwmTag.ST25DVPWM_PWM_PASSWORD_ID;
                STType5PwdDialogFragment.STPwdAction pwdAction = STType5PwdDialogFragment.STPwdAction.PRESENT_CURRENT_PWD;
                String message = getResources().getString(R.string.enter_pwm_password);

                Log.v(TAG, "presentPwmPassword");

                STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(pwdAction, passwordNumber, message, new STType5PwdDialogFragment.STType5PwdDialogListener() {

                    @Override
                    public void onSTType5PwdDialogFinish(int result) {
                        Log.v(TAG, "onSTType5PwdDialogFinish. result = " + result);
                        if (result == PwdDialogFragment.RESULT_OK) {
                            showToast(R.string.present_pwd_succeeded);
                        } else {
                            Log.e(TAG, "Action failed! Tag not updated!");
                        }
                    }
                });
                pwdDialogFragment.show(getSupportFragmentManager(), "pwdDialogFragment");
            }
        }).start();
    }

    private void changePwmPassword() {
        new Thread(new Runnable() {
            public void run() {
                int passwordNumber = ST25DVPwmTag.ST25DVPWM_PWM_PASSWORD_ID;
                STType5PwdDialogFragment.STPwdAction pwdAction = STType5PwdDialogFragment.STPwdAction.PRESENT_CURRENT_PWD;
                String message = getResources().getString(R.string.enter_pwm_password);

                Log.v(TAG, "enter Old PWM Password");

                STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(pwdAction, passwordNumber, message, new STType5PwdDialogFragment.STType5PwdDialogListener() {

                    @Override
                    public void onSTType5PwdDialogFinish(int result) {
                        Log.v(TAG, "onSTType5PwdDialogFinish. result = " + result);
                        if (result == PwdDialogFragment.RESULT_OK) {
                            enterNewPwmPassword();
                        } else {
                            Log.e(TAG, "Action failed! Tag not updated!");
                        }
                    }
                });
                pwdDialogFragment.show(getSupportFragmentManager(), "pwdDialogFragment");

            }
        }).start();
    }


    private void enterNewPwmPassword() {
        new Thread(new Runnable() {
            public void run() {
                int passwordNumber = ST25DVPwmTag.ST25DVPWM_PWM_PASSWORD_ID;
                STType5PwdDialogFragment.STPwdAction pwdAction = STType5PwdDialogFragment.STPwdAction.ENTER_NEW_PWD;
                String message = getResources().getString(R.string.enter_new_pwm_password);

                Log.v(TAG, "enterNewPwmPassword");

                STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(pwdAction, passwordNumber, message, new STType5PwdDialogFragment.STType5PwdDialogListener() {

                    @Override
                    public void onSTType5PwdDialogFinish(int result) {
                        Log.v(TAG, "onSTType5PwdDialogFinish. result = " + result);
                        if (result == PwdDialogFragment.RESULT_OK) {
                            showToast(R.string.present_pwd_succeeded);
                        } else {
                            Log.e(TAG, "Action failed! Tag not updated!");
                        }
                    }
                });
                pwdDialogFragment.show(getSupportFragmentManager(), "pwdDialogFragment");
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds read_list_items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_pwm_config, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                writePwmConfiguration();
                return true;

            case R.id.action_refresh:
                readCurrentPwmConfiguration();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        return mMenu.selectItem(this, item);
    }

    /**
     * Array Adapter supportings multilines items
     */
    class MultiLinesArrayAdapter extends ArrayAdapter<String> {
        public MultiLinesArrayAdapter(@NonNull Context context, @LayoutRes int resource) {
            super(context, resource);
        }

        public MultiLinesArrayAdapter(@NonNull Context context, @LayoutRes int resource, @IdRes int textViewResourceId) {
            super(context, resource, textViewResourceId);
        }

        public MultiLinesArrayAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull String[] objects) {
            super(context, resource, objects);
        }

        public MultiLinesArrayAdapter(@NonNull Context context, @LayoutRes int resource, @IdRes int textViewResourceId, @NonNull String[] objects) {
            super(context, resource, textViewResourceId, objects);
        }

        public MultiLinesArrayAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<String> objects) {
            super(context, resource, objects);
        }

        public MultiLinesArrayAdapter(@NonNull Context context, @LayoutRes int resource, @IdRes int textViewResourceId, @NonNull List<String> objects) {
            super(context, resource, textViewResourceId, objects);
        }

        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            final View view = super.getDropDownView(position,convertView,parent);

            view.post(new Runnable()
            {
                @Override
                public void run()
                {
                    ((TextView)view.findViewById(android.R.id.text1)).setSingleLine(false);
                }
            });
            return view;
        }
    }

    private void readCurrentPwmConfiguration() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    mTagName = myTag.getName();
                    mTagDescription = myTag.getDescription();
                    mTagType = myTag.getTypeDescription();

                    runOnUiThread(new Runnable() {
                                      public void run() {
                                          mTagNameView.setText(mTagName);
                                          mTagTypeView.setText(mTagDescription);
                                          mTagDescriptionView.setText(mTagType);
                                      }
                    });

                    OutputDriverTrimming pwm1Trimming;
                    OutputDriverTrimming pwm2Trimming;
                    DualityManagement dualityManagement;
                    ReadWriteProtection pwmReadWriteProtection;

                    pwm1Trimming = myTag.getPwmOutputDriverTrimming(PWM1);
                    if (myTag.getNumberOfPwm() == 2) {
                        pwm2Trimming = myTag.getPwmOutputDriverTrimming(PWM2);
                    } else {
                        pwm2Trimming = null;
                    }

                    dualityManagement = myTag.getDualityManagement();
                    pwmReadWriteProtection = myTag.getPwmCtrlAccessRights();

                    // Get the item number that should be selected in the Spinners
                    mTagPwm1TrimmingValue = getPwmTrimmingValue(pwm1Trimming);
                    mTagPwm2TrimmingValue = (myTag.getNumberOfPwm() == 2) ?  getPwmTrimmingValue(pwm2Trimming) : 0;
                    mTagDualityManagementValue = getDualityManagementValue(dualityManagement);
                    mTagPwmAccessRightsValue = getPwmAccessRightsValue(pwmReadWriteProtection);

                    // Post an action to UI Thead to update the spinners
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mPwm1TrimmingValueSpinner.setSelection(mTagPwm1TrimmingValue);
                            if (myTag.getNumberOfPwm() == 2) {
                                mPwm2TrimmingValueSpinner.setSelection(mTagPwm2TrimmingValue);
                            }
                            mPwmDualityManagementSpinner.setSelection(mTagDualityManagementValue);
                            mPwmAccessRightsSpinner.setSelection(mTagPwmAccessRightsValue);
                        }
                    });

                } catch (STException e) {
                    switch (e.getError()) {
                        case TAG_NOT_IN_THE_FIELD:
                            showToast(R.string.tag_not_in_the_field);
                            break;
                        default:
                            e.printStackTrace();
                            showToast(R.string.error_while_reading_the_tag);
                    }
                }
            }
        }).start();
    }

    private void writePwmConfiguration() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    //////////////////////////////////////////////////////
                    // Read current PWM Config                          //
                    //////////////////////////////////////////////////////
                    OutputDriverTrimming currentPwm1Trimming = myTag.getPwmOutputDriverTrimming(PWM1);
                    OutputDriverTrimming currentPwm2Trimming = (myTag.getNumberOfPwm() == 2) ?  myTag.getPwmOutputDriverTrimming(PWM2) : null;
                    DualityManagement currentDualityManagement = myTag.getDualityManagement();
                    ReadWriteProtection currentPwmReadWriteProtection = myTag.getPwmCtrlAccessRights();

                    //////////////////////////////////////////////////////
                    // Read PWM Config set in the Spinners              //
                    //////////////////////////////////////////////////////
                    OutputDriverTrimming newPwm1Trimming = getOutputDriverTrimming(mPwm1TrimmingValueSpinner.getSelectedItemPosition());
                    OutputDriverTrimming newPwm2Trimming = getOutputDriverTrimming(mPwm2TrimmingValueSpinner.getSelectedItemPosition());
                    DualityManagement newDualityManagement = getDualityManagement(mPwmDualityManagementSpinner.getSelectedItemPosition());
                    ReadWriteProtection newPwmReadWriteProtection = getPwmAccessRights(mPwmAccessRightsSpinner.getSelectedItemPosition());

                    //////////////////////////////////////////////////////
                    // Update the tag with the values that have changed //
                    //////////////////////////////////////////////////////
                    if (currentPwm1Trimming != newPwm1Trimming) {
                        myTag.setPwmOutputDriverTrimming(PWM1, newPwm1Trimming);
                    }
                    if ((currentPwm2Trimming != newPwm2Trimming) && (myTag.getNumberOfPwm() == 2)) {
                        myTag.setPwmOutputDriverTrimming(PWM2, newPwm2Trimming);
                    }
                    if (currentDualityManagement != newDualityManagement) {
                        myTag.setDualityManagement(newDualityManagement);
                    }
                    if (currentPwmReadWriteProtection != newPwmReadWriteProtection) {
                        myTag.setPwmCtrlAccessRights(newPwmReadWriteProtection);
                    }

                    showToast(R.string.tag_updated);

                } catch (STException e) {
                    switch (e.getError()) {
                        case TAG_NOT_IN_THE_FIELD:
                            showToast(R.string.tag_not_in_the_field);
                            break;
                        case CONFIG_PASSWORD_NEEDED:
                            displayPasswordDialogBox();
                            break;
                        default:
                            e.printStackTrace();
                            showToast(R.string.error_while_updating_the_tag);
                    }
                }
            }
        }).start();
    }

    private void displayPasswordDialogBox() {
        Log.v(TAG, "displayPasswordDialogBox");

        // Warning: Function called from background thread! Post a request to the UI thread
        runOnUiThread(new Runnable() {
            public void run() {
                STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(
                        STType5PwdDialogFragment.STPwdAction.PRESENT_CURRENT_PWD,
                        ST25DVPwmTag.ST25DVPWM_CONFIGURATION_PASSWORD_ID,
                        getResources().getString(R.string.enter_configuration_pwd));
                pwdDialogFragment.show(mFragmentManager, "pwdDialogFragment");
            }
        });
    }

    public void onSTType5PwdDialogFinish(int result) {
        Log.v(TAG, "onSTType5PwdDialogFinish. result = " + result);
        if (result == PwdDialogFragment.RESULT_OK) {
            // Config password has been entered successfully so we can now retry to update the tag
            writePwmConfiguration();
        } else {
            Log.e(TAG, "Action failed! Tag not updated!");
        }
    }

    /**
     * Convert an OutputDriverTrimming value into the corresponding spinner index.
     * @param pwmTrimming
     * @return
     */

    private int getPwmTrimmingValue(OutputDriverTrimming pwmTrimming) {
        int pwmTrimmingValue;

        switch (pwmTrimming) {
            case ONE_QUARTER_FULL_POWER:
                pwmTrimmingValue = 0;
                break;
            case HALF_FULL_POWER:
                pwmTrimmingValue = 1;
                break;
            case THREE_QUARTER_FULL_POWER:
                pwmTrimmingValue = 2;
                break;
            default:
            case FULL_POWER:
                pwmTrimmingValue = 3;
                break;
        }

        return pwmTrimmingValue;
    }

    /**
     * Convert a spinner index into the corresponding OutputDriverTrimming value
     * @param pwmTrimmingValue
     * @return
     */
    private OutputDriverTrimming getOutputDriverTrimming(int pwmTrimmingValue) {
        OutputDriverTrimming outputDriverTrimming;

        switch(pwmTrimmingValue) {
            case 0:
                outputDriverTrimming = ONE_QUARTER_FULL_POWER;
                break;
            case 1:
                outputDriverTrimming = HALF_FULL_POWER;
                break;
            case 2:
                outputDriverTrimming = THREE_QUARTER_FULL_POWER;
                break;
            default:
            case 3:
                outputDriverTrimming = FULL_POWER;
                break;
        }

        return outputDriverTrimming;
    }

    /**
     * Convert a DualityManagement value into the corresponding spinner index.
     * @param dualityManagement
     * @return
     */
    private int getDualityManagementValue(DualityManagement dualityManagement) {
        int dualityManagementValue;

        switch (dualityManagement) {
            default:
            case FULL_DUPLEX:
                dualityManagementValue = 0;
                break;
            case PWM_IN_HZ_WHILE_RF_CMD:
                dualityManagementValue = 1;
                break;
            case PWM_ONE_QUARTER_FULL_POWER_WHILE_RF_CMD:
                dualityManagementValue = 2;
                break;
            case PWM_FREQ_REDUCED:
                dualityManagementValue = 3;
                break;
            case PWM_FREQ_REDUCED_AND_ONE_QUARTER_FULL_POWER_WHILE_RF_CMD:
                dualityManagementValue = 4;
                break;
        }

        return dualityManagementValue;
    }

    /**
     * Convert a spinner index into the corresponding DualityManagement value
     * @param dualityManagementValue
     * @return
     */
    private DualityManagement getDualityManagement(int dualityManagementValue) {
        DualityManagement dualityManagement;

        switch(dualityManagementValue) {
            default:
            case 0:
                dualityManagement = FULL_DUPLEX;
                break;
            case 1:
                dualityManagement = PWM_IN_HZ_WHILE_RF_CMD;
                break;
            case 2:
                dualityManagement = PWM_ONE_QUARTER_FULL_POWER_WHILE_RF_CMD;
                break;
            case 3:
                dualityManagement = PWM_FREQ_REDUCED;
                break;
            case 4:
                dualityManagement = PWM_FREQ_REDUCED_AND_ONE_QUARTER_FULL_POWER_WHILE_RF_CMD;
                break;
        }

        return dualityManagement;
    }

    /**
     * Convert a ReadWriteProtection value into the corresponding spinner index.
     * @param pwmReadWriteProtection
     * @return
     */
    private int getPwmAccessRightsValue(ReadWriteProtection pwmReadWriteProtection) {
        int pwmAccessRightsValue;

        switch (pwmReadWriteProtection) {
            default:
            case READABLE_AND_WRITABLE:
                pwmAccessRightsValue = 0;
                break;
            case READABLE_AND_WRITE_PROTECTED_BY_PWD:
                pwmAccessRightsValue = 1;
                break;
            case READ_AND_WRITE_PROTECTED_BY_PWD:
                pwmAccessRightsValue = 2;
                break;
            case READ_PROTECTED_BY_PWD_AND_WRITE_IMPOSSIBLE:
                pwmAccessRightsValue = 3;
                break;
        }

        return pwmAccessRightsValue;
    }

    /**
     * Convert a spinner index into the corresponding ReadWriteProtection value
     * @param pwmAccessRightsValue
     * @return
     */
    private ReadWriteProtection getPwmAccessRights(int pwmAccessRightsValue) {
        ReadWriteProtection readWriteProtection;

        switch(pwmAccessRightsValue) {
            default:
            case 0:
                readWriteProtection = READABLE_AND_WRITABLE;
                break;
            case 1:
                readWriteProtection = READABLE_AND_WRITE_PROTECTED_BY_PWD;
                break;
            case 2:
                readWriteProtection = READ_AND_WRITE_PROTECTED_BY_PWD;
                break;
            case 3:
                readWriteProtection = READ_PROTECTED_BY_PWD_AND_WRITE_IMPOSSIBLE;
                break;
        }

        return readWriteProtection;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (areSpinnersChanged()) {
                askConfirmationWhenLeaving();
            } else {
                super.onBackPressed();
            }
        }
    }

    /**
     * Function checking if one or more Spinner has been changed
     * @return True if a Spinner has been changed.
     *         False if all the Spinners are still in line with the tag configuration.
     */
    private boolean areSpinnersChanged() {

        int newPwm1TrimmingValue = mPwm1TrimmingValueSpinner.getSelectedItemPosition();
        int newPwm2TrimmingValue = mPwm2TrimmingValueSpinner.getSelectedItemPosition();
        int newDualityManagementValue = mPwmDualityManagementSpinner.getSelectedItemPosition();
        int newPwmAccessRightsValue = mPwmAccessRightsSpinner.getSelectedItemPosition();

        if ( (newPwm1TrimmingValue != mTagPwm1TrimmingValue) ||
             (newPwm2TrimmingValue != mTagPwm2TrimmingValue) ||
             (newDualityManagementValue != mTagDualityManagementValue) ||
             (newPwmAccessRightsValue != mTagPwmAccessRightsValue) ) {
            // One or more Spinner has been changed
            return true;
        } else {
            return false;
        }
    }

    private void askConfirmationWhenLeaving() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ST25DVWPwmConfigurationActivity.this);

        // set title
        alertDialogBuilder.setTitle(getString(R.string.confirmation_needed));

        // set dialog message
        alertDialogBuilder
                .setMessage(getString(R.string.data_not_written_to_tag_do_you_want_to_leave_screen))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.yes),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        ST25DVWPwmConfigurationActivity.this.finish();
                    }
                })
                .setNegativeButton(getString(R.string.no),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

}
