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
import android.content.Intent;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;

import com.st.st25nfc.R;
import com.st.st25nfc.generic.AreasEditorActivity;
import com.st.st25nfc.generic.CheckSignatureActivity;
import com.st.st25nfc.generic.MemoryTestActivity;
import com.st.st25nfc.generic.PreferredApplicationActivity;
import com.st.st25nfc.generic.RegistersActivity;
import com.st.st25nfc.generic.ST25Menu;
import com.st.st25nfc.generic.ndef.NDEFEditorActivity;
import com.st.st25nfc.generic.util.UIHelper;
import com.st.st25nfc.type5.Type5ConfigurationProtectionActivity;
import com.st.st25nfc.type5.Type5LockBlockActivity;
import com.st.st25nfc.type5.st25tv.ST25TVAreaSecurityStatusActivity;
import com.st.st25nfc.type5.st25tv.ST25TVChangeMemConfActivity;
import com.st.st25sdk.NFCTag;
import com.st.st25sdk.SignatureInterface;


public class ST25DVWMenu extends ST25Menu {
    public ST25DVWMenu(NFCTag tag) {
        super(tag);
        mMenuResource.add(R.menu.menu_nfc_forum);
        mMenuResource.add(R.menu.menu_st25dv02kw);

        if (tag instanceof SignatureInterface) {
            mMenuResource.add(R.menu.menu_signature);
        }
    }

    public boolean selectItem(Activity activity, MenuItem item) {
        Intent intent;
        int itemId = item.getItemId();

        switch (itemId) {
            case R.id.preferred_application:
                intent = new Intent(activity, PreferredApplicationActivity.class);
                activity.startActivityForResult(intent, 1);
                break;
            case R.id.about:
                UIHelper.displayAboutDialogBox(activity);
                break;
            case R.id.product_name:
                // Nfc forum
            case R.id.tag_info:
                //Set tab 0 of ST25DVActivity
                intent = new Intent(activity, ST25DVWActivity.class);
                intent.putExtra("select_tab", 0);
                activity.startActivityForResult(intent, 1);
                break;
            case R.id.nfc_ndef_editor:
                intent = new Intent(activity, ST25DVWActivity.class);
                intent.putExtra("select_tab", 1);
                activity.startActivityForResult(intent, 1);
                break;
            case R.id.cc_file:
                intent = new Intent(activity, ST25DVWActivity.class);
                intent.putExtra("select_tab", 2);
                activity.startActivityForResult(intent, 1);
                break;
            // Product features
            case R.id.sys_file:
                intent = new Intent(activity, ST25DVWActivity.class);
                intent.putExtra("select_tab", 3);
                activity.startActivityForResult(intent, 1);
                break;
            case R.id.memory_dump:
                intent = new Intent(activity, ST25DVWActivity.class);
                intent.putExtra("select_tab", 4);
                activity.startActivityForResult(intent, 1);
                break;
            case R.id.configuration:
                intent = new Intent(activity, ST25TVChangeMemConfActivity.class);
                activity.startActivityForResult(intent, 1);
                break;
            case R.id.register_management:
                intent = new Intent(activity, RegistersActivity.class);
                activity.startActivityForResult(intent, 1);
                break;
            case R.id.configuration_protection:
                intent = new Intent(activity, Type5ConfigurationProtectionActivity.class);
                activity.startActivityForResult(intent, 1);
                break;
            case R.id.areas_ndef_editor:
                intent = new Intent(activity, AreasEditorActivity.class);
                activity.startActivityForResult(intent, 1);
                break;
            case R.id.area_security_status_management:
                intent = new Intent(activity, ST25TVAreaSecurityStatusActivity.class);
                activity.startActivityForResult(intent, 1);
                break;
            case R.id.signature_menu:
                intent = new Intent(activity, CheckSignatureActivity.class);
                activity.startActivityForResult(intent, 1);
                break;
            case R.id.pwm_configuration:
                intent = new Intent(activity, ST25DVWPwmConfigurationActivity.class);
                activity.startActivityForResult(intent, 1);
                break;
            case R.id.pwm_ctrl:
                intent = new Intent(activity, ST25DVWPwmCtrlActivity.class);
                activity.startActivityForResult(intent, 1);
                break;
            case R.id.lock_block_menu:
                intent = new Intent(activity, Type5LockBlockActivity.class);
                activity.startActivityForResult(intent, 1);
                break;
        }

        DrawerLayout drawer = (DrawerLayout) activity.findViewById(R.id.drawer);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
