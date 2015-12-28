package com.frostwire.android.gui.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.frostwire.android.R;
import com.frostwire.android.gui.MainApplication;
import com.frostwire.android.gui.activities.internal.JsonReader;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class SplashActivity extends Activity {

    public ProgressDialog pDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new MyJsonTask().execute("http://5play.me/5play/torrent_config.json");

    }


    public class MyJsonTask extends AsyncTask<String, JSONObject, Void>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(SplashActivity.this);
            pDialog.setMessage("Loading...");
            pDialog.show();
        }

        @Override
        protected Void doInBackground(String... params) {
            String url = params[0];
            JSONObject object;
            try {
                object = JsonReader.readJsonFromUrl(url);
                publishProgress(object);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(JSONObject... values) {
            pDialog.dismiss();
            JSONObject jsonObject = values[0];
            try {
                String checkEnabled = jsonObject.getString("enable");
                if(checkEnabled.equals("true")){
                    Intent intent = new Intent(SplashActivity.this, MainActivity2.class);
                    startActivity(intent);
                    finish();
                } else {
                    Intent intent = new Intent(SplashActivity.this, MainActivity2.class);
                    startActivity(intent);
                    finish();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            super.onProgressUpdate(values);
        }
    }

}
