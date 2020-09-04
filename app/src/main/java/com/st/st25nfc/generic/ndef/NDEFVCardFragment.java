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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.st.st25nfc.R;
import com.st.st25nfc.generic.util.ContactHelper;
import com.st.st25sdk.ndef.NDEFMsg;
import com.st.st25sdk.ndef.VCardRecord;

import java.io.ByteArrayOutputStream;

public class NDEFVCardFragment extends NDEFRecordFragment {

    final static String TAG = "NDEFVCardFragment";

    private View mView;
    private int mSeekPhotoCurPos;

    private TextView mSeekTextView;
    private EditText mContactAddressEditText;
    private EditText mContactNameEditText;
    private EditText mContactEmailEditText;
    private EditText mContactNumberEditText;
    private EditText mContactWebsiteEditText;
    private ImageView mPhotoImageView;
    private CheckBox mPhotoCheckBox;
    private Button mCapturePhotoButton;
    private Button mGetContactButton;
    private SeekBar mPhotoQualitySeekBar;

    private int mApiVersion = android.os.Build.VERSION.SDK_INT;

    private int mCompressRate = 50;

    private boolean mPictureExport = false;
    private VCardRecord mVCardRecord;
    private int mAction;

    private final int CAMERA_CAPTURE = 0;
    private final int PICK_CONTACT = 1;
    private final int PICTURE_CROP = 4;

    private Uri mPictureUri = null;

    private final int mDefaultPhotoDisplayHSize = 256;
    private final int mDefaultPhotoDisplayWSize = 256;


    public static NDEFVCardFragment newInstance(Context context) {
        NDEFVCardFragment f = new NDEFVCardFragment();
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

        View view = inflater.inflate(R.layout.fragment_ndef_vcard, container, false);
        mView = view;

        Bundle bundle = getArguments();
        if (bundle == null) {
            Log.e(TAG, "Fatal error! Arguments are missing!");
            return null;
        }

        NDEFMsg ndefMsg = (NDEFMsg) bundle.getSerializable(NDEFRecordFragment.NDEFKey);
        int recordNbr = bundle.getInt(NDEFRecordFragment.RecordNbrKey);
        mVCardRecord = (VCardRecord) ndefMsg.getNDEFRecord(recordNbr);

        initFragmentWidgets();

        mAction = bundle.getInt(NDEFEditorFragment.EditorKey);
        if(mAction == NDEFEditorFragment.VIEW_NDEF_RECORD) {
            // We are displaying an existing record. By default it is not editable
            ndefRecordEditable(false);
        } else {
            // We are adding a new TextRecord or editing an existing record
            ndefRecordEditable(true);
        }

        return view;
    }

    private int estimatedVCardSize(Bitmap bitmap) {

        String encodedImage = tranformPhoto(bitmap);
        return encodedImage.length();
    }
    private void updatePhotoInformationFields() {
        int imgSize = 0;

        if (mPhotoImageView != null) {
            Bitmap bitmap = mPhotoImageView.getDrawable() == null ? null : ((BitmapDrawable) mPhotoImageView.getDrawable()).getBitmap();
            if (bitmap != null) {
                imgSize = estimatedVCardSize(bitmap);
            }

        }
        mSeekTextView.setText(getResources().getString(R.string.photo_in_bytes,(int)mCompressRate,imgSize));

    }

    private String tranformPhoto(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, mCompressRate, outputStream);
        byte[] b = outputStream.toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }


    public void setPhotoContact(Bitmap picture) {
        mPhotoImageView.setImageBitmap(picture);
    }

    private void initFragmentWidgets() {

        mContactAddressEditText = (EditText) mView.findViewById(R.id.edit_contact_address);
        mContactNameEditText = (EditText) mView.findViewById(R.id.edit_contact_name);
        mContactEmailEditText = (EditText) mView.findViewById(R.id.edit_contact_email);
        mContactNumberEditText = (EditText) mView.findViewById(R.id.edit_contact_number);
        mContactWebsiteEditText = (EditText) mView.findViewById(R.id.edit_contact_website);
        mPhotoImageView = (ImageView) mView.findViewById(R.id.photoView);
        mPhotoCheckBox = (CheckBox) mView.findViewById(R.id.capture_photo_checkbox);
        mCapturePhotoButton = (Button) mView.findViewById(R.id.capturePhotoButton);
        mGetContactButton = (Button) mView.findViewById(R.id.getContactButton);
        mPhotoQualitySeekBar = (SeekBar) mView.findViewById(R.id.vcard_photo_quality_slider);
        mSeekTextView = (TextView) mView.findViewById(R.id.vcard_seekbar_quality);

        mCapturePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureFrame();
            }
        });

        mGetContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getContact();
            }
        });

        mSeekPhotoCurPos = 80;    //you need to give starting position value of SeekBar
        mCompressRate = mSeekPhotoCurPos;
        //TextView seekText = (TextView) mView.findViewById(R.id.SeekBarLabel);
        mPhotoQualitySeekBar.setProgress((int) mSeekPhotoCurPos);


        mPhotoQualitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mSeekPhotoCurPos = progress;
                mCompressRate = mSeekPhotoCurPos;
                updatePhotoInformationFields();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(getContext(), getResources().getString(R.string.discrete_value, mSeekPhotoCurPos), Toast.LENGTH_SHORT).show();
            }
        });

        setContent();
    }

    /**
     * The content from the fragment is saved into the NDEF Record
     */
    @Override
    public void updateContent() {
        String contactAddress = mContactAddressEditText.getText().toString();
        String contactName = mContactNameEditText.getText().toString();
        String contactEmail = mContactEmailEditText.getText().toString();
        String contactNumber = mContactNumberEditText.getText().toString();
        String contactWebsite = mContactWebsiteEditText.getText().toString();

        BitmapDrawable drawable = (BitmapDrawable) mPhotoImageView.getDrawable();
        Bitmap photo = null;
        if (drawable != null) {
            photo = drawable.getBitmap();
        }

        mVCardRecord.setSPAddr(contactAddress);
        mVCardRecord.setName(contactName);
        mVCardRecord.setEmail(contactEmail);
        mVCardRecord.setNumber(contactNumber);
        mVCardRecord.setWebSite(contactWebsite);

        if (photo != null && mPhotoCheckBox.isChecked()) {
            mVCardRecord.setPhoto(tranformPhoto(photo));
        } else {
            mVCardRecord.setPhoto(null);
        }
    }

    /**
     * The content from the NDEF Record is displayed in the Fragment
     */
    public void setContent() {
        String address = mVCardRecord.getStructPostalAddr();
        mContactAddressEditText.setText(address);

        String name = mVCardRecord.getFormattedName();
        mContactNameEditText.setText(name);

        String email = mVCardRecord.getEmail();
        mContactEmailEditText.setText(email);

        String number = mVCardRecord.getNumber();
        mContactNumberEditText.setText(number);

        String webSite = mVCardRecord.getWebSiteAddr();
        mContactWebsiteEditText.setText(webSite);

        String photoString = mVCardRecord.getPhoto();
        Bitmap decodedByte = null;
        if (photoString != null) {
            byte[] decodedString = Base64.decode(photoString, Base64.DEFAULT);
            decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        }

        if (decodedByte != null) {
            mPhotoImageView.setImageBitmap(decodedByte);
        }
    }

    public void ndefRecordEditable(boolean editable) {
        mContactNameEditText.setClickable(editable);
        mContactNameEditText.setFocusable(editable);
        mContactNameEditText.setFocusableInTouchMode(editable);

        mContactNumberEditText.setClickable(editable);
        mContactNumberEditText.setFocusable(editable);
        mContactNumberEditText.setFocusableInTouchMode(editable);

        mContactEmailEditText.setClickable(editable);
        mContactEmailEditText.setFocusable(editable);
        mContactEmailEditText.setFocusableInTouchMode(editable);

        mContactAddressEditText.setClickable(editable);
        mContactAddressEditText.setFocusable(editable);
        mContactAddressEditText.setFocusableInTouchMode(editable);

        mContactWebsiteEditText.setClickable(editable);
        mContactWebsiteEditText.setFocusable(editable);
        mContactWebsiteEditText.setFocusableInTouchMode(editable);

        if(!editable) {
            // The Fragment is no more editable. Reload its content
            setContent();
        }
    }

    public void getContact() {
        if (mApiVersion >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PICK_CONTACT);
            }
        }

        startActivityForResult(new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI), PICK_CONTACT);
    }

    public void captureFrame() {
        try {
            if (mApiVersion >= Build.VERSION_CODES.KITKAT) {
                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    startImageCapture();
                } else {
                    // onRequestPermissionsResult() will be notified with the result
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_CAPTURE);
                }
            } else {
                showToast(R.string.device_doesnt_support_capturing);
            }

        } catch (ActivityNotFoundException anfe) {
            showToast(R.string.device_doesnt_support_capturing);
        }
    }

    private void performCrop() {
        try {
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            cropIntent.setDataAndType(mPictureUri, "image/*");
            cropIntent.putExtra("crop", true);
            cropIntent.putExtra("aspectX", 1);
            cropIntent.putExtra("aspectY", 1);
            cropIntent.putExtra("outputX", mDefaultPhotoDisplayWSize);
            cropIntent.putExtra("outputY", mDefaultPhotoDisplayHSize);
            cropIntent.putExtra("return-data", true);
            startActivityForResult(cropIntent, PICTURE_CROP);

        } catch (ActivityNotFoundException anfe) {
            showToast(R.string.device_doesnt_support_crop_feature);
        }
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, String[] permissions, int[] grantResults) {
        switch(requestCode) {
            case CAMERA_CAPTURE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startImageCapture();
                } else {
                    showToast(R.string.cannot_continue_without_camera_permission);
                }
                break;
        }
    }

    private void startImageCapture() {
        Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(captureIntent, CAMERA_CAPTURE);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case CAMERA_CAPTURE:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        mPictureUri = data.getData();
                        performCrop();
                    } else {
                        showToast(R.string.device_doesnt_support_capturing);
                    }
                    //Bundle extras = data.getExtras();
                    //thePic = extras.getParcelable("data");
                    //Bitmap photo = (Bitmap) data.getExtras().get("data");
                    //if (_curFragment != null ) {
                    //    ((NDEFVCardFragment) _curFragment).setPhotoContact(photo);
                    //}
                }
                break;

            case PICTURE_CROP:
                if (data != null) {
                    Bundle extras = data.getExtras();
                    Bitmap photo = extras.getParcelable("data");
                    if (photo != null) {
                        mPhotoImageView.setImageBitmap(photo);
                        mPhotoCheckBox.setChecked(true);
                    }
                }
                break;

            case PICK_CONTACT: {
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    ContactHelper contactHelper = new ContactHelper(uri, getActivity().getContentResolver());
                    String id = contactHelper.getId();

                    String address = contactHelper.retrieveContactStructurePostAddr(id);
                    String name = contactHelper.getDisplayName(id);
                    String email = contactHelper.retrieveContactEmail(id);
                    String number = contactHelper.retrieveContactNumber(id);
                    String addressWebsite = contactHelper.retrieveContactWebSite(id);
                    Bitmap photo = contactHelper.retrieveContactPhoto(id);

                    mContactAddressEditText.setText(address);
                    mContactNameEditText.setText(name);
                    mContactEmailEditText.setText(email);
                    mContactNumberEditText.setText(number);
                    mContactWebsiteEditText.setText(addressWebsite);

                    if (photo != null)
                        mPhotoImageView.setImageBitmap(photo);

                }

                break;

            }
        }
    }

}


