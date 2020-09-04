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

package com.st.st25nfc.type4.st25ta;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;

import com.st.st25nfc.generic.CheckSignatureActivity;

import com.st.st25nfc.generic.PreferredApplicationActivity;
import com.st.st25nfc.generic.ndef.NDEFEditorActivity;
import com.st.st25nfc.generic.util.UIHelper;
import com.st.st25nfc.type4.AreasPwdActivity;
import com.st.st25nfc.type4.GpoConfigActivity;
import com.st.st25sdk.NFCTag;
import com.st.st25nfc.R;
import com.st.st25nfc.generic.ST25Menu;
import com.st.st25sdk.SignatureInterface;
import com.st.st25sdk.type4a.STType4CounterInterface;
import com.st.st25sdk.type4a.STType4GpoInterface;
import com.st.st25sdk.type4a.st25ta.ST25TA02KDTag;
import com.st.st25sdk.type4a.st25ta.ST25TA02KPTag;


public class ST25TAMenu extends ST25Menu {
    public ST25TAMenu(NFCTag tag) {
        super(tag);

        mMenuResource.add(R.menu.menu_nfc_forum);
        mMenuResource.add(R.menu.menu_st25ta);

        if (tag instanceof STType4CounterInterface) {
            mMenuResource.add(R.menu.menu_st25ta_counter);
        }

        if (tag instanceof STType4GpoInterface) {
            mMenuResource.add(R.menu.menu_st25ta_gpo);
        }

        if (tag instanceof SignatureInterface) {
            mMenuResource.add(R.menu.menu_signature);
        }
    }

    @Override
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
                intent = new Intent(activity, ST25TAActivity.class);
                intent.putExtra("select_tab", 0);
                activity.startActivityForResult(intent, 1);
                break;
            case R.id.nfc_ndef_editor:
                intent = new Intent(activity, ST25TAActivity.class);
                intent.putExtra("select_tab", 1);
                activity.startActivityForResult(intent, 1);
                break;
            case R.id.cc_file:
                intent = new Intent(activity, ST25TAActivity.class);
                intent.putExtra("select_tab", 2);
                activity.startActivityForResult(intent, 1);
                break;
            // Product features
            case R.id.sys_file:
                intent = new Intent(activity, ST25TAActivity.class);
                intent.putExtra("select_tab", 3);
                activity.startActivityForResult(intent, 1);
                break;
            case R.id.memory_dump:
                intent = new Intent(activity, ST25TAActivity.class);
                intent.putExtra("select_tab", 4);
                activity.startActivityForResult(intent, 1);
                break;
            case R.id.security_status_management:
                intent = new Intent(activity, AreasPwdActivity.class);
                activity.startActivityForResult(intent, 1);
                break;
            case R.id.counter_status_management:
                intent = new Intent(activity, CounterConfigActivity.class);
                activity.startActivityForResult(intent, 1);
                break;
            case R.id.gpo_status_management:
                intent = new Intent(activity, GpoConfigActivity.class);
                activity.startActivityForResult(intent, 1);
                break;

            case R.id.signature_menu:
                intent = new Intent(activity, CheckSignatureActivity.class);
                activity.startActivityForResult(intent, 1);
                break;

            default:
                break;

        }

        DrawerLayout drawer = (DrawerLayout) activity.findViewById(R.id.drawer);
        drawer.closeDrawer(GravityCompat.START);
        return true;

    }

}
