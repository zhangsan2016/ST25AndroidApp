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

package com.st.st25nfc.generic.util;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.st.st25nfc.R;
import com.st.st25nfc.generic.MainActivity;
import com.st.st25nfc.generic.RawReadWriteFragment;
import com.st.st25nfc.generic.STFragment;
import com.st.st25nfc.generic.TagInfoFragment;
import com.st.st25nfc.generic.ndef.NDEFEditorFragment;
import com.st.st25nfc.type4.CCFileType4Fragment;
import com.st.st25nfc.type4.st25ta.SysFileST25TAFragment;
import com.st.st25nfc.type4.stm24sr.SysFileM24SRFragment;
import com.st.st25nfc.type4.stm24tahighdensity.SysFileST25TAHighDensityFragment;
import com.st.st25nfc.type5.CCFileType5Fragment;
import com.st.st25nfc.type5.SysFileType5Fragment;
import com.st.st25sdk.About;
import com.st.st25sdk.NFCTag;

import com.st.st25sdk.type4a.Type4Tag;
import com.st.st25sdk.type5.Type5Tag;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Scanner;


public class UIHelper {

    static final String TAG = "UIHelper";

    /**
     * Function indicating if the current thread is the UI Thread
     *
     * @return
     */
    public static boolean isUIThread() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            // On UI thread.
            return true;
        } else {
            // Not on UI thread.
            return false;
        }
    }

    // Identifiers of STFragments. Each id correspond to a STFragment
    public enum STFragmentId {
        // Generic Fragments
        TAG_INFO_FRAGMENT_ID,
        CC_FILE_TYPE5_FRAGMENT_ID,
        CC_FILE_TYPE4_FRAGMENT_ID,
        SYS_FILE_TYP5_FRAGMENT_ID,
        SYS_FILE_M24SR_FRAGMENT_ID,
        SYS_FILE_ST25TA_HIGH_DENSITY_FRAGMENT_ID,
        SYS_FILE_ST25TA_FRAGMENT_ID,
        RAW_DATA_FRAGMENT_ID,
        NDEF_DETAILS_FRAGMENT_ID,

        // M24SR Fragments
        M24SR_NDEF_DETAILS_FRAGMENT_ID,
        M24SR_EXTRA_FRAGMENT_ID,

        // ST25TV Fragments
        ST25TV_CONFIG_FRAGMENT_ID,

        // NDEF Fragments
        NDEF_MULTI_RECORD_FRAGMENT_ID,
        NDEF_SMS_FRAGMENT_ID,
        NDEF_TEXT_FRAGMENT_ID,
        NDEF_URI_FRAGMENT_ID
    }

    /**
     * This function instantiate a STFragment from its STFragmentId
     *
     * @param context
     * @param stFragmentId
     * @return
     */
    public static STFragment getSTFragment(Context context, STFragmentId stFragmentId) {
        STFragment fragment = null;

        switch (stFragmentId) {
            // Generic Fragments
            case TAG_INFO_FRAGMENT_ID:
                fragment = TagInfoFragment.newInstance(context);
                break;
            case CC_FILE_TYPE5_FRAGMENT_ID:
                fragment = CCFileType5Fragment.newInstance(context);
                break;
            case CC_FILE_TYPE4_FRAGMENT_ID:
                fragment = CCFileType4Fragment.newInstance(context);
                break;
            case SYS_FILE_TYP5_FRAGMENT_ID:
                fragment = SysFileType5Fragment.newInstance(context);
                break;
            case SYS_FILE_M24SR_FRAGMENT_ID:
                fragment = SysFileM24SRFragment.newInstance(context);
                break;
            case SYS_FILE_ST25TA_HIGH_DENSITY_FRAGMENT_ID:
                fragment = SysFileST25TAHighDensityFragment.newInstance(context);
                break;
            case SYS_FILE_ST25TA_FRAGMENT_ID:
                fragment = SysFileST25TAFragment.newInstance(context);
                break;
            case RAW_DATA_FRAGMENT_ID:
                fragment = RawReadWriteFragment.newInstance(context);
                break;
            case NDEF_DETAILS_FRAGMENT_ID:
                fragment = NDEFEditorFragment.newInstance(context);
                break;
            default:
                Log.e(TAG, "Invalid stFragmentId: " + stFragmentId);
                break;

        }

        return fragment;
    }


    // Convert the area number into an area name
    public static String getAreaName(int area) {
        String areaName = getApplicationResources().getString(R.string.area_number_to_name) + area;
        return areaName;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean isAType4Tag(NFCTag tag) {
        if (tag instanceof Type4Tag) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isAType5Tag(NFCTag tag) {
        if (tag instanceof Type5Tag) {
            return true;
        } else {
            return false;
        }
    }

    public static void invalidateCache(NFCTag tag) {
        if (tag instanceof Type4Tag) {
            Type4Tag type4Tag = (Type4Tag) tag;
            type4Tag.invalidateCache();

        } else if (tag instanceof Type5Tag) {
            Type5Tag type5Tag = (Type5Tag) tag;
            type5Tag.invalidateCache();
        } else {
            Log.e(TAG, "Tag not supported yet!");
        }
    }

    /**
     * Function returning the Type4 fileId corresponding to an Area.*
     * @param area
     */
    public static int getType4FileIdFromArea(int area) {
        return area;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static void displayAboutDialogBox(Context context) {

        //set up dialog
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.application_version_dialog);
        dialog.setTitle(context.getResources().getString(R.string.version_dialog_header));
        dialog.setCancelable(true);

        String versionName = "???";
        try {
            versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


        //set up text
        String message;
        message = context.getResources().getString(R.string.app_version_v) + versionName;

        TextView text = (TextView) dialog.findViewById(R.id.versionTextView);
        text.setText(message);

        message = "";
        message = message + "ST25SDK" + ": " + About.getFullVersion() + "\n";
        message = message + context.getResources().getString(R.string.product_features) + ": " + About.getExtraFeatureList() + "\n";
        text = (TextView) dialog.findViewById(R.id.featuresTextView);
        text.setText(message);

        message = context.getResources().getString(R.string.app_description);
        text = (TextView) dialog.findViewById(R.id.TextView02);
        text.setText(message);

        //set up image view
        ImageView img = (ImageView) dialog.findViewById(R.id.versionImageView);
        img.setImageResource(R.drawable.logo_st25_transp);

        //set up button
        Button button = (Button) dialog.findViewById(R.id.Button01);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        //now that the dialog is set up, it's time to show it
        dialog.show();
    }

    public static Resources getApplicationResources() {
        return MainActivity.mResources;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static String convertInputStreamToString(InputStream inputStream) {
        Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");

        return scanner.hasNext() ? scanner.next() : "";
    }

}

