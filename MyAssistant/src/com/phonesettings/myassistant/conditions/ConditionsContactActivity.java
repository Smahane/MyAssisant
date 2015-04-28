package com.phonesettings.myassistant.conditions;

import com.example.myassistant.R;
import com.phonesettings.myassistant.db.ConditionManager;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.TextView;

public class ConditionsContactActivity extends Activity {
    public static final int PICK_CONTACT = 1;
    public static final int UPDATE_CONTACT = 2;
    private String phoneNumber = "";
    private String name = "";
    private String contactId = "";

    TextView contactName;
    TextView contactPhone;
    ConditionManager conditionsManager;
    private boolean isUpdate;
    private String title;
    private long sitId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conditions_contact);

        conditionsManager = new ConditionManager(ConditionsContactActivity.this);
        contactName = (TextView) findViewById(R.id.contact_name);
        contactPhone = (TextView) findViewById(R.id.contact_number);
        Intent intent = getIntent();
        sitId = intent.getLongExtra("situationId", -1);
        title = getResources().getResourceEntryName(R.string.contact);
        isUpdate = conditionsManager.hasThisCondition(title, sitId);

        if(isUpdate){
            String note = conditionsManager.getNote(title, sitId);
            String contact_name="";
            String phone_numbers="";
            ContentResolver cr = getContentResolver();
            Cursor cCur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, ContactsContract.Contacts._ID+" = ? ", new String[]{note}, null);
            if(cCur.moveToFirst()){
                contact_name=cCur.getString(cCur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            }
            cCur.close();

            phone_numbers = getContactNumber(note);
            contactName.setText(contact_name);
            contactPhone.setText(phone_numbers);
        }else{
            Intent contactIntent = new Intent(Intent.ACTION_PICK,
                    ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(contactIntent, PICK_CONTACT);
        }
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        switch (reqCode) {
            case UPDATE_CONTACT:
                if(resultCode == Activity.RESULT_OK){
                    phoneNumber = "";

                }
            case (PICK_CONTACT):
                if (resultCode == Activity.RESULT_OK) {
                    Uri contactData = data.getData();
                    Cursor c = managedQuery(contactData, null, null, null, null);
                    if (c.moveToFirst()) {
                        int idIndex = c.getColumnIndex(ContactsContract.Contacts._ID);
                        int nameIndex = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                        int phoneIndex = c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
                        contactId = c.getString(idIndex);
                        name = c.getString(nameIndex);
                        if (Integer.parseInt(c.getString(phoneIndex))>0) {
                            // You know have the number so now query it like this
                            phoneNumber = getContactNumber(contactId);
                        }

                        contactName.setText(name);
                        contactPhone.setText(phoneNumber);
                    }
                }
            break;
            default:
                name="";
                contactName.setText(name);
                contactPhone.setText(R.string.no_contact_choosen);
        }
    }

    public void onEditContact(View v){
        Intent contactIntent = new Intent(Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(contactIntent, UPDATE_CONTACT);
    }

    public String getContactNumber(String id){
        String number = "";
        Cursor phones = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = "+ id,
                null, null);
        while (phones.moveToNext()) {
            number += phones.getString(
                    phones.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.NUMBER))+" ";
        }
        phones.close();
        return number;
    }

    @Override
    protected void onDestroy() {
        conditionsManager.stop();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if(!name.equals("")){
            String note = contactId;
            String desc = name + " " + phoneNumber;
            if (!isUpdate) {
                conditionsManager.addCondition(title, sitId, desc, note);
            } else {
                conditionsManager.updateCondition(title, sitId, desc, note);
            }
        }

        super.onBackPressed();
    }
}
