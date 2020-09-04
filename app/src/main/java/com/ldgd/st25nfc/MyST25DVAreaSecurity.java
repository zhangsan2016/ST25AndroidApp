package com.ldgd.st25nfc;

import android.support.v4.app.FragmentManager;
import android.util.Log;

import com.st.st25nfc.generic.STType5PwdDialogFragment;
import com.st.st25sdk.MultiAreaInterface;
import com.st.st25sdk.STException;
import com.st.st25sdk.TagHelper;
import com.st.st25sdk.type5.st25dv.ST25DVRegisterRfAiSS;
import com.st.st25sdk.type5.st25dv.ST25DVTag;

/**
 * Created by ldgd on 2018/12/12.
 * 功能：nfc ST25DV区块安全
 * 说明：
 * //显示密码  --  需要先显示密码（验证）后，才能修改密码
 */

public class MyST25DVAreaSecurity {

    public static final int AREA1 = 1;
    public static final int AREA2 = 2;
    public static final int AREA3 = 3;
    public static final int AREA4 = 4;



    /**
     *  设置区密码
     * @param passwordId 密码编号,一共三个密码，设置其中一个
     * @param areaPassword 要设置的区域密码，有三个例如： ST25DVTag.ST25DV_PASSWORD_1;
     */
    public void setAreaPassword(int passwordId,byte areaPassword, final ST25DVTag myTag) {

        // 全部密码设置成
        try {
            myTag.setPasswordNumber(AREA1, areaPassword);
            myTag.setPasswordNumber(MultiAreaInterface.AREA2, areaPassword);
            myTag.setPasswordNumber(MultiAreaInterface.AREA3, areaPassword);
            myTag.setPasswordNumber(MultiAreaInterface.AREA4, areaPassword);
            Log.e("mima ", "密码设置成功");
        } catch (STException e) {
            e.printStackTrace();
        }

    }


    /**
     * 更改读写许可
     *
     * @param protection   权限类型 （可读可写、可读,写入密码保护、读写密码保护） READABLE_AND_WRITE_PROTECTED_BY_PWD
     * @param selectedArea 选中的分区 AREA1、AREA2、AREA3、AREA4
     */
    public void changePermission(final TagHelper.ReadWriteProtection protection, final int selectedArea, final ST25DVTag myTag) {
        new Thread(new Runnable() {
            public void run() {
                try {

                    // 获得设置许可类型（可读、写入密码保护、读写密码保护）,当前设置可读
                    TagHelper.ReadWriteProtection readWriteProtection = protection;
                    int area = selectedArea;
                    ST25DVRegisterRfAiSS rfAiSSRegister = getRFAiSSRegister(area, myTag);
                    rfAiSSRegister.setSSReadWriteProtection(readWriteProtection);

                    ST25DVRegisterRfAiSS.ST25DVSecurityStatusPWDControl pwdNbr = rfAiSSRegister.getSSPWDControl();

           /*        if((readWriteProtection != READABLE_AND_WRITABLE) && (pwdNbr == NO_PWD_SELECTED)) {
                       // The area has some protections but not password has been chosen for this area. Display a warning
                       showWarningWhenNoPassword();
                   }*/


                } catch (STException e) {
                  /* switch (e.getError()) {
                       case TAG_NOT_IN_THE_FIELD:
                           showToast(R.string.tag_not_in_the_field);
                           break;
                       case CONFIG_PASSWORD_NEEDED:
                           displayPasswordDialogBox();
                           break;
                       default:
                           e.printStackTrace();
                           showToast(R.string.error_while_updating_the_tag);
                   }*/
                }
            }
        }).start();
    }

    private ST25DVRegisterRfAiSS getRFAiSSRegister(int area, ST25DVTag myTag) throws STException {
        ST25DVRegisterRfAiSS register;

        switch (area) {
            case AREA1:
                register = (ST25DVRegisterRfAiSS) myTag.getRegister(ST25DVTag.REGISTER_RFA1SS_ADDRESS);
                break;
            case AREA2:
                register = (ST25DVRegisterRfAiSS) myTag.getRegister(ST25DVTag.REGISTER_RFA2SS_ADDRESS);
                break;
            case AREA3:
                register = (ST25DVRegisterRfAiSS) myTag.getRegister(ST25DVTag.REGISTER_RFA3SS_ADDRESS);
                break;
            case AREA4:
                register = (ST25DVRegisterRfAiSS) myTag.getRegister(ST25DVTag.REGISTER_RFA4SS_ADDRESS);
                break;
            default:
                throw new STException(STException.STExceptionCode.BAD_PARAMETER);
        }

        return register;
    }


    /**
     * 更改密码
     *
     * @param mFragmentManager
     */
    public void changePassword(final FragmentManager mFragmentManager) {
        new Thread(new Runnable() {
            public void run() {
                // 密码编号，三个的其中一个 ST25DVTag.ST25DV_PASSWORD_1
                int passwordNumber = ST25DVTag.ST25DV_PASSWORD_3;
                // message = “请输入当前密码%1$s”
                String message = "请输入当前密码";

                // 枚举类型，修改当前密码
                STType5PwdDialogFragment.STPwdAction mCurrentAction;
                mCurrentAction = STType5PwdDialogFragment.STPwdAction.PRESENT_CURRENT_PWD;

                STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(mCurrentAction, passwordNumber, message);
                pwdDialogFragment.show(mFragmentManager, "pwdDialogFragment");

            }
        }).start();
    }


    /**
     * 显示密码
     * @param myTag
     */
    public void presentPassword(final ST25DVTag myTag,final FragmentManager mFragmentManager) {

        new Thread(new Runnable() {
            public void run() {
                int passwordNumber = 0;
                try {
                    passwordNumber = myTag.getPasswordNumber(MultiAreaInterface.AREA4);
                } catch (STException e) {
                    e.printStackTrace();
                }

                STType5PwdDialogFragment.STPwdAction pwdAction = STType5PwdDialogFragment.STPwdAction.PRESENT_CURRENT_PWD;
                String message = "输入区域1密码";

                // 参数 pwdAction : Dialog标识，passwordNumber ：得到的当前密码，message ： Dialog提示消息
                STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(pwdAction, passwordNumber, message);
                pwdDialogFragment.show(mFragmentManager, "pwdDialogFragment");
            }
        }).start();

    }


}
