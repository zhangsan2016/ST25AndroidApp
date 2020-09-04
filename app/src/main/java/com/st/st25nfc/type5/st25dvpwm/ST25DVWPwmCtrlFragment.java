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

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.st.st25nfc.R;
import com.st.st25nfc.generic.MainActivity;
import com.st.st25nfc.generic.PwdDialogFragment;
import com.st.st25nfc.generic.STFragment;
import com.st.st25nfc.generic.STFragmentActivity;
import com.st.st25nfc.generic.STType5PwdDialogFragment;
import com.st.st25sdk.NFCTag;
import com.st.st25sdk.STException;
import com.st.st25sdk.STLog;
import com.st.st25sdk.type5.st25dvpwm.ST25DVPwmTag;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import static com.st.st25nfc.R.id.textView;
import static com.st.st25sdk.type5.st25dvpwm.ST25DVPwmTag.PWM1;
import static com.st.st25sdk.type5.st25dvpwm.ST25DVPwmTag.PWM2;


public class ST25DVWPwmCtrlFragment extends STFragment{

    public ST25DVPwmTag myTag = null;
    static final String TAG = "PwmCtrlConfig";

    private View mView;
    private FragmentManager mFragmentManager;

    TextView mTagNameTextView;
    TextView mTagTypeTextView;
    TextView mTagDescriptionTextView;

    boolean mAreTagPwmSettingsRead = false;
    boolean mWaitingForPwmPassword = false;

    //////////////// PWM1 /////////////
    private boolean mPwm1Enable;
    private int mPwm1Frequency;
    private int mPwm1DutyCycle;

    private RadioButton mPwm1EnableButton;
    private EditText mPwm1FreqEditText;
    private EditText mPwm1DutyCycleEditText;
    private TextView mPwm1ResolutionEditText;
    private TextView mPwm1PeriodTextView;
    private TextView mPwm1PulseWidthTextView;

    //////////////// PWM2 /////////////
    private boolean mPwm2Enable;
    private int mPwm2Frequency;
    private int mPwm2DutyCycle;

    private RadioButton mPwm2EnableButton;
    private EditText mPwm2FreqEditText;
    private EditText mPwm2DutyCycleEditText;
    private TextView mPwm2ResolutionEditText;
    private TextView mPwm2PeriodTextView;
    private TextView mPwm2PulseWidthTextView;

    /////////////////////////////////////

    private VerticalSeekBar mPwm1SeekBarDutyCycle;
    private VerticalSeekBar mPwm2SeekBarDutyCycle;

    private AutomaticPwmControl mAutoDemo;

    private PlayMusic mPlayMusicDemo;


    public static ST25DVWPwmCtrlFragment newInstance(Context context) {
        ST25DVWPwmCtrlFragment f = new ST25DVWPwmCtrlFragment();
        /* If needed, pass some argument to the fragment
        Bundle args = new Bundle();
        args.putInt("index", index);
        f.setArguments(args);
        */

        // Set the title of this fragment
        f.setTitle(context.getResources().getString(R.string.configuration));
        return f;
    }

    public ST25DVWPwmCtrlFragment() {
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_st25dv02kw_pwm_ctrl, container, false);

        LinearLayout musicConfigLinearLayout = (LinearLayout) view.findViewById(R.id.pwmMusicConfig);
        musicConfigLinearLayout.setVisibility(View.GONE);

        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.pwm_mode);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // checkedId is the RadioButton selected
                if (mAutoDemo != null) mAutoDemo.stop();
                stopMusic();

                switch(checkedId) {
                    case R.id.radio_light:
                        mView.findViewById(R.id.pwm1LinearControlTable).setVisibility(View.GONE);
                        mView.findViewById(R.id.pwm2LinearControlTable).setVisibility(View.GONE);
                        mView.findViewById(R.id.pwmMusicConfig).setVisibility(View.GONE);
                        break;
                    case R.id.radio_auto:
                        mView.findViewById(R.id.pwm1LinearControlTable).setVisibility(View.GONE);
                        mView.findViewById(R.id.pwm2LinearControlTable).setVisibility(View.GONE);
                        mView.findViewById(R.id.pwmMusicConfig).setVisibility(View.GONE);
                        if (mAutoDemo != null) mAutoDemo.stop();
                        mAutoDemo = new AutomaticPwmControl();
                        mAutoDemo.initialyze(50);
                        mAutoDemo.start();
                        break;
                    case R.id.radio_musics:
                        mView.findViewById(R.id.pwmMusicConfig).setVisibility(View.VISIBLE);
                        break;
                    case R.id.radio_expert:
                        mView.findViewById(R.id.pwmMusicConfig).setVisibility(View.GONE);
                        mView.findViewById(R.id.pwm1LinearControlTable).setVisibility(View.VISIBLE);
                        if (myTag.getNumberOfPwm() == 2) {
                            mView.findViewById(R.id.pwm2LinearControlTable).setVisibility(View.VISIBLE);
                        }
                        break;
                }
            }
        });

        mView = view;
        mFragmentManager = getActivity().getSupportFragmentManager();

        Musics myMusics = new Musics();
        fillMusicSpinnerForSelection(mView,myMusics);

        // by defaut set the layout in light mode - do not display all informations
        mView.findViewById(R.id.pwm1LinearControlTable).setVisibility(View.GONE);
        mView.findViewById(R.id.pwm2LinearControlTable).setVisibility(View.GONE);

        mPwm1EnableButton = (RadioButton) mView.findViewById(R.id.EnableValueRadioButton);
        mPwm1Enable = false;
        mPwm1EnableButton.setChecked(mPwm1Enable);
        mPwm1EnableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPwm1Enable(!mPwm1Enable);
            }
        });

        mPwm2EnableButton = (RadioButton) mView.findViewById(R.id.EnableValueRadioButton2);
        mPwm2Enable = false;
        mPwm2EnableButton.setChecked(mPwm2Enable );
        mPwm2EnableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPwm2Enable(!mPwm2Enable);
            }
        });

        mTagNameTextView = (TextView) mView.findViewById(R.id.model_header);
        mTagTypeTextView = (TextView) mView.findViewById(R.id.model_type);
        mTagDescriptionTextView = (TextView) mView.findViewById(R.id.model_description);

        mPwm1FreqEditText = (EditText) mView.findViewById(R.id.FreqValueEditText);
        mPwm2FreqEditText = (EditText) mView.findViewById(R.id.FreqValueEditText2);
        mPwm1DutyCycleEditText = (EditText) mView.findViewById(R.id.DutyCycleValueEditText);
        mPwm2DutyCycleEditText = (EditText) mView.findViewById(R.id.DutyCycleValueEditText2);
        mPwm1ResolutionEditText = (TextView) mView.findViewById(R.id.ResolutionValueTextView);
        mPwm2ResolutionEditText = (TextView) mView.findViewById(R.id.ResolutionValueTextView2);
        mPwm1PeriodTextView = (TextView) mView.findViewById(R.id.PeriodValueTextView);
        mPwm2PeriodTextView = (TextView) mView.findViewById(R.id.PeriodValueTextView2);
        mPwm1PulseWidthTextView = (TextView) mView.findViewById(R.id.PulseWidthValueTextView);
        mPwm2PulseWidthTextView = (TextView) mView.findViewById(R.id.PulseWidthValueTextView2);


        mPwm1FreqEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                // check range
                String currentText = s.toString();
                int freq = getFrequencyValueInTable(currentText);

                setPwm1Frequency(freq, false);
            }
        });

        mPwm1DutyCycleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                String currentText = s.toString();
                int dutyCycle = getDutyCycleValueInTable(currentText);
                setPwm1DutyCycle(dutyCycle, false);
            }
        });


        mPwm2FreqEditText.addTextChangedListener(new TextWatcher() {
            boolean ignoreChange = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                // check range
                String currentText = s.toString();
                int freq = getFrequencyValueInTable(currentText);
                setPwm2Frequency(freq, false);
            }
        });

        mPwm2DutyCycleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String currentText = s.toString();
                int dutyCycle = getDutyCycleValueInTable(currentText);
                setPwm2DutyCycle(dutyCycle, false);
            }
        });

        // Initialise seekbars for pwm1 and 2
        mPwm1SeekBarDutyCycle =(VerticalSeekBar) mView.findViewById(R.id.pwm1_dutycycle_slider);
        // perform seek bar change listener event used for getting the progress value
        mPwm1SeekBarDutyCycle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int newPwm1DutyCycleValue = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                newPwm1DutyCycleValue = progress;
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!mAreTagPwmSettingsRead) {
                    // Ignore the slider changes until the Tag's PWM settings have been read
                    return;
                }

                if (newPwm1DutyCycleValue != mPwm1DutyCycle) {
                    setPwm1DutyCycle(newPwm1DutyCycleValue, true);
                    updateTag(PWM1, true);
                }
            }
        });

        mPwm2SeekBarDutyCycle =(VerticalSeekBar)mView.findViewById(R.id.pwm2_dutycycle_slider);
        // perform seek bar change listener event used for getting the progress value
        mPwm2SeekBarDutyCycle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int newPwm2DutyCycleValue = 0;
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                newPwm2DutyCycleValue = progress;
            }

            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!mAreTagPwmSettingsRead) {
                    // Ignore the slider changes until the Tag's PWM settings have been read
                    return;
                }

                if (newPwm2DutyCycleValue != mPwm2DutyCycle) {
                    setPwm2DutyCycle(newPwm2DutyCycleValue, true);
                    updateTag(PWM2, true);
                }
            }
        });

        if (myTag.getNumberOfPwm() == 2) {
            mPwm2SeekBarDutyCycle.setEnabled(true);
        } else {
            mPwm2SeekBarDutyCycle.setEnabled(false);
        }

        mPwm1ResolutionEditText.setText(String.format("%.1f", ST25DVPwmTag.ST25DVPWM_PWM_RESOLUTION_NS));
        mPwm2ResolutionEditText.setText(String.format("%.1f", ST25DVPwmTag.ST25DVPWM_PWM_RESOLUTION_NS));

        return (View) view;
    }

    private void fillMusicSpinnerForSelection(View view, final Musics musics){
        Spinner spinnerMusics = (Spinner)view.findViewById(R.id.spinnerMusics);
        final ArrayList<String> stringArrayList = new ArrayList<String>();
        Enumeration e = musics.mMusicsList.keys();
        while (e.hasMoreElements()) {
            stringArrayList.add(e.nextElement().toString());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_item, stringArrayList);
        spinnerMusics.setAdapter(adapter);
        spinnerMusics.setSelection(0);
        spinnerMusics.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String musicTitle = stringArrayList.get(position);

                int[] notes = (int[]) musics.mMusicsList.get(musicTitle);
                int[] notesTicks = (int[]) musics.mMusicsListTick.get(musicTitle);

                startMusic(notes, notesTicks);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                stopMusic();
            }
        });

    }

    private class AutomaticPwmControl {
        private int mDutyCycleRatio;
        private boolean mDemoLoop;
        final Handler handlerAutomaticLedFlashing = new Handler();
        int mStepsForLight;

        public AutomaticPwmControl() {
            mDutyCycleRatio = 0;
            mDemoLoop = true;
            mStepsForLight = 0;
        }

        public void initialyze(int dutyCycleRatio) {
            if (dutyCycleRatio <0 || dutyCycleRatio > 100) {
                dutyCycleRatio = 50;
            }
            initDefaultPwmSetting(dutyCycleRatio);
            mDutyCycleRatio = dutyCycleRatio;
            mStepsForLight = 10;
        }

        public void start() {
            handlerAutomaticLedFlashing.postDelayed(new Runnable() {
                @Override
                public void run() {
                    computeNextPWMsDutyCycleValue();

                    // PWM1
                    setPwm1DutyCycle(mDutyCycleRatio, true);
                    updateTag(PWM1, false);

                    if (myTag.getNumberOfPwm() == 2) {
                        // PWM2
                        int pwm2DutyCycle = 100 - mDutyCycleRatio;
                        setPwm2DutyCycle(pwm2DutyCycle, true);
                        updateTag(PWM2, false);
                    }

                    // update sliders
                    initSlidersValues();
                    if (mDemoLoop) {//demoTick();
                        handlerAutomaticLedFlashing.postDelayed(this, 1500);
                    }
                }
            }, 1500);
        }

        public void stop() {
            mDemoLoop = false;
            handlerAutomaticLedFlashing.removeCallbacksAndMessages(null);
        }

        private void computeNextPWMsDutyCycleValue () {
            if (mDutyCycleRatio >= 100 && mStepsForLight > 0) {
                mStepsForLight = -10;
                mDutyCycleRatio = 100;
            }
            if (mDutyCycleRatio <= 0 && mStepsForLight < 0) {
                mStepsForLight = 10;
                mDutyCycleRatio = 0;
            }
            mDutyCycleRatio = mDutyCycleRatio + mStepsForLight;
        }
    }

    private int getFrequencyValueInTable(String text) {
        int frequency = myTag.ST25DVPWM_PWM_MIN_FREQ;
        if (!text.equals(""))
            try {
                frequency = Integer.parseInt(text);
            } catch (Exception e) {
                STLog.e("Bad frequence" + e.getMessage());
                showToast(R.string.incorrect_frequency);
            }
        else
            frequency = 0;
        return frequency;
    }

    private int getDutyCycleValueInTable(String text) {
        int dutyCycle = 0;
        if (!text.equals(""))
            try {
                dutyCycle = Integer.parseInt(text);
            } catch (Exception e) {
                STLog.e("Bad dutyCycle" + e.getMessage());
                showToast(R.string.incorrect_duty_cycle);
            }
        else
            dutyCycle = 0;
        return dutyCycle;
    }


    private void initSlidersValues() {
        mPwm1SeekBarDutyCycle.setProgress(mPwm1DutyCycle);

        if (myTag.getNumberOfPwm() == 2) {
            mPwm2SeekBarDutyCycle.setProgress(mPwm2DutyCycle);
        } else {
            // Set to 0 so that the sliders appears as greyed out
            mPwm2SeekBarDutyCycle.setProgress(0);
        }
    }

    @Override
    public void onResume() {

        if (!STFragmentActivity.tagChanged(getActivity(), myTag)) {
            // The same tag has been taped again

            // Read Tag PWM settings if not yet done
            if (!mAreTagPwmSettingsRead) {
                fillView();
            }
        }

        super.onResume();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    public void onAttach(Context context) {
        super.onAttach(context);
        myTag = (ST25DVPwmTag) ((STFragmentListener) context).getTag();
    }

    /**
     * Function checking if all the PWMs are enabled and set with a valid frequency
     * @return
     */
    private boolean arePwmsRunning() throws STException {

        readPwmSettings(PWM1);

        if (!mPwm1Enable) {
            return false;
        }

        if (!isPwmFrequencyOk(mPwm1Frequency)) {
            // PWM1 frequency is not valid
            return false;
        }

        if (myTag.getNumberOfPwm() == 2) {
            readPwmSettings(PWM2);

            if (!mPwm2Enable) {
                return false;
            }

            if (!isPwmFrequencyOk(mPwm2Frequency)) {
                // PWM2 frequency is not valid
                return false;
            }
        }

        // Every PWMs are enabled and set with a valid frequency
        return true;
    }

    private boolean isPwmFrequencyOk(int frequency) {
        if ((frequency < myTag.ST25DVPWM_PWM_MIN_FREQ) || (frequency > myTag.ST25DVPWM_PWM_MAX_FREQ)) {
            return false;
        } else {
            return true;
        }
    }

    private boolean isPwmDutyCycleOk(int dutyCycle) {
        if ((dutyCycle < 0) || (dutyCycle > 100)) {
            return false;
        } else {
            return true;
        }
    }

    private void showPwmSettingAlert() {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

                // set title
                alertDialogBuilder.setTitle(getResources().getString(R.string.warning));

                // set dialog message
                alertDialogBuilder
                        .setMessage(getResources().getString(R.string.pwm_not_enabled_warning))
                        .setCancelable(true)
                        .setPositiveButton(getResources().getString(R.string.yes),new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                setDefaultPwmSetting();
                                dialog.cancel();
                            }
                        })
                        .setNegativeButton(getResources().getString(R.string.no),new DialogInterface.OnClickListener() {
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

    private void initDefaultPwmSetting(int dutyCycleRatio) {

        if (dutyCycleRatio < 0 || dutyCycleRatio > 100) {
            dutyCycleRatio = 0;
        }

        setPwm1Enable(true);
        setPwm1DutyCycle(dutyCycleRatio, true);
        setPwm1Frequency(2000, true);

        updateTag(PWM1, true);

        if (myTag.getNumberOfPwm() == 2) {
            setPwm2Enable(true);
            setPwm2DutyCycle(dutyCycleRatio, true);
            setPwm2Frequency(16000, true);

            updateTag(PWM2, true);
        }

    }

    /**
     * Function enabling PWM1 and PWM2 and setting the default frequency
     */
    private void setDefaultPwmSetting() {
        initDefaultPwmSetting(50);
        initSlidersValues();
        showToast(R.string.pwm_controls_update_success);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mAutoDemo != null) mAutoDemo.stop();
    }

    @Override
    public void fillView() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (myTag == null) {
                        // Fatal error: We can't go further if myTag is null!
                        showToast(R.string.invalid_tag);
                        return;
                    }

                    // Retrieve all the info from the tag...
                    final String mTagName = myTag.getName();
                    final String mTagDescription = myTag.getDescription();
                    final String mTagType = myTag.getTypeDescription();

                    readPwmSettings(PWM1);
                    if (myTag.getNumberOfPwm() == 2) {
                        readPwmSettings(PWM2);
                    }

                    if (!mAreTagPwmSettingsRead) {
                        // This is the first time that we read the Tag PWM settings. Check if PWMs are running
                        if (!arePwmsRunning()) {
                            showPwmSettingAlert();
                        }
                    }

                    // PWMs settings read successfully
                    mAreTagPwmSettingsRead = true;

                    // ...and update all the UI widgets
                    final Activity activity = getActivity();
                    if (activity != null) {
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                mTagNameTextView.setText(mTagName);
                                mTagTypeTextView.setText(mTagDescription);
                                mTagDescriptionTextView.setText(mTagType);

                                initSlidersValues();
                            }
                        });
                    }

                }
                catch (STException e) {
                    switch (e.getError()) {
                        case TAG_NOT_IN_THE_FIELD:
                            showToast(R.string.tag_not_in_the_field);
                            break;
                        case ISO15693_BLOCK_PROTECTED:
                            // PWM Control Block is password protected
                            presentPwmPassword();
                            break;
                        default:
                            e.printStackTrace();
                            showToast(R.string.failed_to_initialize_pwm_control);
                            break;
                    }

                }
            }
        }).start();
    }

    public void updateTag()  {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    myTag.setPwmConfiguration(PWM1, mPwm1Frequency, mPwm1DutyCycle, mPwm1Enable);

                    if (myTag.getNumberOfPwm() == 2) {
                        myTag.setPwmConfiguration(PWM2, mPwm2Frequency, mPwm2DutyCycle, mPwm2Enable);
                    }

                    initSlidersValues();
                    showToast(R.string.pwm_controls_update_success);
                }
                catch (STException e) {
                    switch (e.getError()) {
                        case TAG_NOT_IN_THE_FIELD:
                            showToast(R.string.tag_not_in_the_field);
                            break;
                        case ISO15693_BLOCK_IS_LOCKED:
                            // PWM Control Block is password protected
                            presentPwmPassword();
                            break;
                        default:
                            e.printStackTrace();
                            showToast(R.string.failed_to_update_pwm_controls);
                            break;
                    }
                }
            }
        }).start();
    }

    public void updateTag(final int pwmNumber, boolean toast)  {
        if ((pwmNumber < 1) || (pwmNumber > myTag.getNumberOfPwm())) {
            Log.e(TAG, getString(R.string.invalid_pwm_number, pwmNumber));
            return;
        }

        final boolean withTrace = toast;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (pwmNumber == PWM1) {
                        myTag.setPwmConfiguration(PWM1, mPwm1Frequency, mPwm1DutyCycle, mPwm1Enable);
                    }
                    else {
                        myTag.setPwmConfiguration(PWM2, mPwm2Frequency, mPwm2DutyCycle, mPwm2Enable);
                    }
                }
                catch (STException e) {
                    switch (e.getError()) {
                        case TAG_NOT_IN_THE_FIELD:
                            if (withTrace) showToast(R.string.tag_not_in_the_field);
                            break;
                        case ISO15693_BLOCK_IS_LOCKED:
                            // PWM Control Block is password protected
                            presentPwmPassword();
                            break;
                        default:
                            e.printStackTrace();
                            if (withTrace) showToast(R.string.failed_to_update_pwm_controls);
                            break;
                    }
                    return;
                }
            }
        }).start();

    }

    private void readPwmSettings(int pwmNumber) throws STException {
        boolean enable;
        int frequency, dutyCycle;

        if ((pwmNumber < 1) || (pwmNumber > myTag.getNumberOfPwm())) {
            Log.e(TAG, getString(R.string.invalid_pwm_number, pwmNumber));
            throw new STException(STException.STExceptionCode.BAD_PARAMETER);
        }

        if (pwmNumber == PWM1) {
            enable = myTag.isPwmEnable(PWM1);
            frequency = myTag.getPwmFrequency(PWM1);
            dutyCycle = myTag.getPwmDutyCycle(PWM1);

            setPwm1Enable(enable);
            setPwm1Frequency(frequency, true);
            setPwm1DutyCycle(dutyCycle, true);

        } else if (pwmNumber == PWM2) {
            enable = myTag.isPwmEnable(PWM2);
            frequency = myTag.getPwmFrequency(PWM2);
            dutyCycle = myTag.getPwmDutyCycle(PWM2);

            setPwm2Enable(enable);
            setPwm2Frequency(frequency, true);
            setPwm2DutyCycle(dutyCycle, true);
        }
    }

    private void presentPwmPassword() {
        if (mWaitingForPwmPassword) {
            // We are already waiting for PWM password so there is nothing to do
            return;
        }

        mWaitingForPwmPassword = true;

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
                            fillView();
                        } else {
                            Log.e(TAG, "Action failed! Tag not updated!");
                        }
                    }
                });
                pwdDialogFragment.show(mFragmentManager, "pwdDialogFragment");
            }
        }).start();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public void setPwm1Enable(boolean enable) {
        this.mPwm1Enable = enable;

        // Update Table in UI Thread
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    mPwm1EnableButton.setChecked(mPwm1Enable);
                }
            });
        }
    }

    public void setPwm1Frequency(int frequency, final boolean updateTableValue) {
        if (this.mPwm1Frequency != frequency) {
            this.mPwm1Frequency = frequency;

            setPwm1PeriodAndPulseWidth();

            // Update Table in UI Thread
            final Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        if (updateTableValue) {
                            mPwm1FreqEditText.setText(String.format("%d", mPwm1Frequency));
                        }

                        if (isPwmFrequencyOk(mPwm1Frequency)) {
                            mPwm1FreqEditText.setTextColor(getResources().getColor(R.color.st_dark_blue));
                        } else {
                            showToast(R.string.bad_frequency_value, String.valueOf(mPwm1Frequency));
                            mPwm1FreqEditText.setTextColor(getResources().getColor(R.color.red));
                        }
                    }
                });
            }

        }
    }

    public void setPwm1DutyCycle(int dutyCycle, final boolean updateTableValue) {
        if (this.mPwm1DutyCycle != dutyCycle) {
            this.mPwm1DutyCycle = dutyCycle;

            setPwm1PeriodAndPulseWidth();

            // Update Table in UI Thread
            final Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        if (updateTableValue) {
                            mPwm1DutyCycleEditText.setText(String.format("%d", mPwm1DutyCycle));
                        }

                        if (isPwmDutyCycleOk(mPwm1DutyCycle)) {
                            mPwm1DutyCycleEditText.setTextColor(getResources().getColor(R.color.st_dark_blue));
                        } else {
                            showToast(R.string.bad_duty_cycle_value, String.valueOf(mPwm1DutyCycle));
                            mPwm1DutyCycleEditText.setTextColor(getResources().getColor(R.color.red));
                        }
                    }
                });
            }

        }
    }

    private void setPwm1PeriodAndPulseWidth() {
        // Update Table in UI Thread
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    if (isPwmFrequencyOk(mPwm1Frequency) && isPwmDutyCycleOk(mPwm1DutyCycle)) {
                        int period = (int) (1000000000 / (mPwm1Frequency * ST25DVPwmTag.ST25DVPWM_PWM_RESOLUTION_NS));
                        int pulseWidth = period * mPwm1DutyCycle /100;

                        mPwm1PeriodTextView.setText(String.format("0x%x", period));
                        mPwm1PulseWidthTextView.setText(String.format("0x%x", pulseWidth));

                    } else {
                        mPwm1PeriodTextView.setText(R.string.general_na);
                        mPwm1PulseWidthTextView.setText(R.string.general_na);
                    }
                }
            });
        }
    }

    public void setPwm2Enable(boolean enable) {
        this.mPwm2Enable = enable;

        // Update Table in UI Thread
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    mPwm2EnableButton.setChecked(mPwm2Enable);
                }
            });
        }
    }

    public void setPwm2Frequency(int frequency, final boolean updateTableValue) {
        if (this.mPwm2Frequency != frequency) {
            this.mPwm2Frequency = frequency;

            setPwm2PeriodAndPulseWidth();

            // Update Table in UI Thread
            final Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        if (updateTableValue) {
                            mPwm2FreqEditText.setText(String.format("%d", mPwm2Frequency));
                        }

                        if (isPwmFrequencyOk(mPwm2Frequency)) {
                            mPwm2FreqEditText.setTextColor(getResources().getColor(R.color.st_dark_blue));
                        } else {
                            showToast(R.string.bad_frequency_value, String.valueOf(mPwm2Frequency));
                            mPwm2FreqEditText.setTextColor(getResources().getColor(R.color.red));
                        }
                    }
                });
            }

        }
    }

    public void setPwm2DutyCycle(int dutyCycle, final boolean updateTableValue) {
        if (this.mPwm2DutyCycle != dutyCycle) {
            this.mPwm2DutyCycle = dutyCycle;

            setPwm2PeriodAndPulseWidth();

            // Update Table in UI Thread
            final Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        if (updateTableValue) {
                            mPwm2DutyCycleEditText.setText(String.format("%d", mPwm2DutyCycle));
                        }

                        if (isPwmDutyCycleOk(mPwm2DutyCycle)) {
                            mPwm2DutyCycleEditText.setTextColor(getResources().getColor(R.color.st_dark_blue));
                        } else {
                            showToast(R.string.bad_duty_cycle_value, String.valueOf(mPwm2DutyCycle));
                            mPwm2DutyCycleEditText.setTextColor(getResources().getColor(R.color.red));
                        }
                    }
                });
            }

        }
    }

    private void setPwm2PeriodAndPulseWidth() {
        // Update Table in UI Thread
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    if (isPwmFrequencyOk(mPwm2Frequency) && isPwmDutyCycleOk(mPwm2DutyCycle)) {
                        int period = (int) (1000000000 / (mPwm2Frequency * ST25DVPwmTag.ST25DVPWM_PWM_RESOLUTION_NS));
                        int pulseWidth = period * mPwm2DutyCycle /100;

                        mPwm2PeriodTextView.setText(String.format("0x%x", period));
                        mPwm2PulseWidthTextView.setText(String.format("0x%x", pulseWidth));

                    } else {
                        mPwm2PeriodTextView.setText(R.string.general_na);
                        mPwm2PulseWidthTextView.setText(R.string.general_na);
                    }
                }
            });
        }


    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void stopMusic() {
        if (mPlayMusicDemo != null) {
            mPlayMusicDemo.stop();
            mPlayMusicDemo = null;
        }
    }

    private void startMusic(int[] notes, int[] notesTicks) {
        stopMusic();

        mPlayMusicDemo = new PlayMusic(notes, notesTicks);
        int volume = mPwm1DutyCycle;
        mPlayMusicDemo.initialyze(volume);
        mPlayMusicDemo.start();

    }

    private class PlayMusic {
        private int[] mMusicNotes;
        private int[] mDurationNotes;
        private boolean mDemoLoop;

        final Handler handlerAutomaticLedMusic = new Handler();
        int mStepsForMusic;

        private boolean rightMusicDefined;

        public PlayMusic(int[] musicNotes, int[] durationNotes) {
            rightMusicDefined = true;
            if (musicNotes.length != durationNotes.length) {
                rightMusicDefined = false;
                showToast(R.string.bad_music_defined);
            }
            mMusicNotes = musicNotes;
            mDurationNotes = durationNotes;
            mDemoLoop = true;
            mStepsForMusic = 0;
        }

        public void initialyze(int dutyCycleRatio) {
            if (dutyCycleRatio < 0 || dutyCycleRatio > 100) {
                dutyCycleRatio = 50;
            }

            setPwm1Enable(false);
            setPwm1DutyCycle(dutyCycleRatio, true);
            setPwm1Frequency(500, true);

            mStepsForMusic = 0;
        }

        public void start() {
            if (!rightMusicDefined) {
                showToast(R.string.bad_music_defined);
                return;
            }

            handlerAutomaticLedMusic.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (computeNextNote()) {
                        // PWM1
                        // update register values
                        int frequency = mMusicNotes[mStepsForMusic];
                        setPwm1Frequency(frequency, true);
                        setPwm1Enable(true);

                        if (frequency == Musics.NOP) {
                            writePwmConfiguration(frequency, 0, false);
                        } else {
                            writePwmConfiguration(frequency, mPwm1DutyCycle, true);
                        }

                        if (mDemoLoop) {
                            handlerAutomaticLedMusic.postDelayed(this, mDurationNotes[mStepsForMusic]);
                        }
                        mStepsForMusic++;
                    } else {
                        stop();
                    }

                }
            }, mDurationNotes[mStepsForMusic]);
        }

        private void writePwmConfiguration(final int freq, final int dutyCycle, final boolean enable) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        myTag.setPwmConfiguration(PWM1, freq, dutyCycle, enable);
                    }
                    catch (STException e) {
                        switch (e.getError()) {
                            case TAG_NOT_IN_THE_FIELD:
                                showToast(R.string.tag_not_in_the_field);
                                break;
                            default:
                                e.printStackTrace();
                                showToast(R.string.failed_to_update_pwm_controls);
                                break;
                        }
                        return;
                    }
                }
            }).start();
        }

        public void stop() {
            mDemoLoop = false;
            mStepsForMusic = 0;
            handlerAutomaticLedMusic.removeCallbacksAndMessages(null);
            setPwm1Enable(false);
            updateTag(PWM1, false);
        }

        private boolean computeNextNote () {
            if (mStepsForMusic >= 0 && mStepsForMusic < mMusicNotes.length) {
                return true;
            } else {
                mDemoLoop = false;
                return false;
            }
        }
    }

    private static class Musics {
        static int NOP = 489; // In order to generate a pause between notes when needed.
        int DO4 = 523;
        int DOdi4 = 554;
        int RE4 = 587;
        int REdi4 = 622;
        int MI4 = 659;
        int FA4 = 698;
        int FAdi4 = 740;
        int SOL4 = 784;
        int SOLdi4 = 831;
        int LA4 = 880;
        int LAdi4 = 932;
        int Sib4 = 932;
        int SI4 = 988;
        int DO5 = 1047;
        int DOdi5 = 1109;
        int RE5 = 1175;
        int REdi5 = 1245;
        int MI5 = 1319;
        int FA5 = 1397;
        int FAdi5 = 1480;
        int SOL5 = 1568;
        int SOLdi5 = 1661;
        int LA5 = 1760;
        int LAdi5 = 1865;
        int Sib5 = 1869;
        int SI5 = 1976;

        int Rd = 1600;  // ronde
        int B = 800;    // blanche
        int N = 400;    // noire
        int C = 200;    // croche
        int D = 100;    // double croche
        int T = 50;     // triple croche
        int TBB = 10;   //	pause

        int notesMarseillaise[] = {RE4, NOP, RE4, NOP, RE4, SOL4, NOP, SOL4, LA4, NOP, LA4, RE5, SI4, SOL4, NOP, SOL4, SI4, SOL4};
        int notesMarseillaiseD[] = {D, TBB, C, TBB, D, N, TBB, N, N, TBB, N, N, C, C, TBB, D, C, D};

        int notesFrereJacques1[] = {FA4, SOL4, LA4, FA4, NOP, FA4, SOL4, LA4, FA4, LA4, LAdi4, DO5, LA4, LAdi4, DO5, DO5, RE5, DO5, LAdi4, LA4, FA4, DO5, RE5, DO5, LAdi4, LA4, FA4, FA4, DO4, FA4, NOP, FA4, DO4, FA4};
        int notesFrereJacques1D[] = {N, N, N, N, C, N, N, N, N, N, N, B, N, N, B, 300, D, C, C, N, N, 300, D, C, C, N, N, N, N, N, N, N, N, N};

        int notesHappyBirthday[] = {SOL4, NOP, SOL4, LA4, SOL4, DO5, SI4, SOL4, NOP, SOL4, LA4, SOL4, DO5, NOP, DO5, SOL4, NOP, SOL4, SOL5, MI5, DO5, SI4, LA4, FA5, NOP, FA5, MI5, DO4, RE4, DO5};
        int notesHappyBirthdayD[] = {C, TBB, D, N, N, N, B, C, TBB, D, N, N, N, TBB, B, C, TBB, D, N, N, N, N, N, C, TBB, D, N, N, N, B};

        Hashtable mMusicsList;
        Hashtable mMusicsListTick;

        int notesLittleFatherChristmas[] = {DO4, FA4, NOP, FA4, NOP, FA4, SOL4, FA4, NOP, FA4, SOL4, LA5, NOP, LA5, NOP, LA5, SI5, LA5, SOL5, FA5, NOP, FA5, NOP, FA5, NOP, FA5, MI5, RE5, DO5, NOP, DO5, NOP, DO5, FA5, NOP, FA5, NOP, FA5, MI5, FA5, SOL5};
        int notesLittleFatherChristmasD[] = {N, N, TBB, N, TBB, N, N, B, TBB, C, C, N, TBB, N, TBB, N, N, B, N, N, TBB, C, TBB, C, TBB, C, C, C, B, TBB, C, TBB, C, B, TBB, C, TBB, C, C, C, B};

        enum MusicListTitle {
            FrereJacques,
            Marseillaise,
            HappyBirthday,
            LittleFatherChristmas
        }

        public Musics() {
            mMusicsList = new Hashtable();
            mMusicsList.put(MusicListTitle.FrereJacques.toString(), notesFrereJacques1);
            mMusicsList.put(MusicListTitle.Marseillaise.toString(), notesMarseillaise);
            mMusicsList.put(MusicListTitle.HappyBirthday.toString(), notesHappyBirthday);
            mMusicsList.put(MusicListTitle.LittleFatherChristmas.toString(), notesLittleFatherChristmas);
            mMusicsListTick = new Hashtable();
            mMusicsListTick.put(MusicListTitle.FrereJacques.toString(), notesFrereJacques1D);
            mMusicsListTick.put(MusicListTitle.Marseillaise.toString(), notesMarseillaiseD);
            mMusicsListTick.put(MusicListTitle.HappyBirthday.toString(), notesHappyBirthdayD);
            mMusicsListTick.put(MusicListTitle.LittleFatherChristmas.toString(), notesLittleFatherChristmasD);
        }
    }

}
