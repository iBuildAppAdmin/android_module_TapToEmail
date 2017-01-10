/****************************************************************************
*                                                                           *
*  Copyright (C) 2014-2015 iBuildApp, Inc. ( http://ibuildapp.com )         *
*                                                                           *
*  This file is part of iBuildApp.                                          *
*                                                                           *
*  This Source Code Form is subject to the terms of the iBuildApp License.  *
*  You can obtain one at http://ibuildapp.com/license/                      *
*                                                                           *
****************************************************************************/
package com.ibuildapp.romanblack.EmailPlugin;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Xml;
import android.widget.Toast;

import com.appbuilder.sdk.android.StartUpActivity;
import com.appbuilder.sdk.android.Utils;
import com.appbuilder.sdk.android.Widget;

import java.util.List;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Main module class. Module entry point.
 * Represents tap to email widget.
 */
@StartUpActivity(moduleName = "Email")
public class EmailPlugin extends Activity {

    private final int INITIALIZATION_FAILED = 0;
    private final int NEED_INTERNET_CONNECTION = 1;
    private final int START_SENDING_EMAIL = 2;

    private final String TAG_MAILTO = "mailto";
    private final String TAG_SUBJECT = "subject";
    private final String TAG_MESSAGE = "message";
    private final String EXTRA_WIDGET = "Widget";
    private String emailAddress = "";
    private String subject = "";
    private String message = "";
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case INITIALIZATION_FAILED: {
                    Toast.makeText(EmailPlugin.this,
                            R.string.common_alert_cannot_initialize_plugin, Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
                case NEED_INTERNET_CONNECTION: {
                    Toast.makeText(EmailPlugin.this,
                            R.string.taptoemail_need_internet_connection, Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
                case START_SENDING_EMAIL: {
                    startSendingEmail();
                }
                break;
            }
        }
    };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent currentIntent = getIntent();
        final Widget widget = (Widget) currentIntent.getSerializableExtra(EXTRA_WIDGET);

        if (widget == null) {
            handler.sendEmptyMessage(INITIALIZATION_FAILED);
            return;
        }

        new Thread() {
            @Override
            public void run() {
                try {
                    String pluginXmlData = widget.getPluginXmlData().length() == 0
                            ? Utils.readXmlFromFile(widget.getPathToXmlFile())
                            : widget.getPluginXmlData();

                    Xml.parse(pluginXmlData, new SAXHandler());

                    handler.sendEmptyMessage(START_SENDING_EMAIL);
                } catch (Exception ex) {
                    handler.sendEmptyMessage(INITIALIZATION_FAILED);
                }
            }
        }.start();
    }

    /**
     * Starts the standart email client to send email.
     */
    private void startSendingEmail() {
        Intent it = new Intent(Intent.ACTION_SEND);
        it.setType("plain/text");
        it.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{emailAddress});
        //it.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        it.putExtra(android.content.Intent.EXTRA_TEXT, Html.fromHtml(message));
        startActivity(Intent.createChooser(it, getString(R.string.choose_email_client)));

        finish();
    }

    /**
     * This class using to handle configuration XML tags and prepare module data.
     */
    private class SAXHandler extends DefaultHandler {

        private StringBuilder sb = new StringBuilder();

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            super.characters(ch, start, length);

            sb.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(uri, localName, qName);

            if (localName.equalsIgnoreCase(TAG_MAILTO)) {
                EmailPlugin.this.emailAddress = sb.toString().trim();
            } else if (localName.equalsIgnoreCase(TAG_MESSAGE)) {
                EmailPlugin.this.message = sb.toString().trim();
            } else if (localName.equalsIgnoreCase(TAG_SUBJECT)) {
                EmailPlugin.this.subject = sb.toString().trim();
            }

            sb.setLength(0);
        }
    }
}
