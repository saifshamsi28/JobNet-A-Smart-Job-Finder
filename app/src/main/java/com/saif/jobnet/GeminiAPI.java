package com.saif.jobnet;

import android.util.Log;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;

//public class GeminiAPI {
//    private static final String API_KEY = "AIzaSyA6xrcZjb2r_seYAHNwfxBtUK3v4uoj5PQ"; // Replace with your Gemini API Key
//    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";
//    private static final OkHttpClient client = new OkHttpClient();
//
//    public interface GeminiCallback {
//        void onSuccess(String formattedText);
//        void onFailure(String error);
//    }
//
//    public static void formatJobDescription(String jobDescription, GeminiCallback callback) {
//        try {
//            JSONObject jsonBody = new JSONObject();
//            jsonBody.put("prompt", "Format this job description with bullet points and headings so that i can direct paste that in textview in android app:\n" + jobDescription);
//            jsonBody.put("temperature", 0.7); // Adjust for variation
//
//            RequestBody body = RequestBody.create(
//                MediaType.parse("application/json"), jsonBody.toString()
//            );
//
//            Request request = new Request.Builder()
//                .url(API_URL + "?key=" + API_KEY)
//                .post(body)
//                .build();
//
//            client.newCall(request).enqueue(new Callback() {
//                @Override
//                public void onFailure(Call call, IOException e) {
//                    callback.onFailure(e.getMessage());
//                }
//
//                @Override
//                public void onResponse(Call call, Response response) throws IOException {
//                    if (!response.isSuccessful()) {
//                        callback.onFailure("Error: " + response.message());
//                        Log.d("GeminiApi","Error: "+response.message());
//                        return;
//                    }
//                    String responseBody = response.body().string();
//                    Log.d("GeminiApi", "Response: " + responseBody); // Debugging
//
//                    try {
//                        JSONObject jsonResponse = new JSONObject(responseBody);
//                        JSONArray candidates = jsonResponse.getJSONArray("candidates");
//                        JSONObject firstCandidate = candidates.getJSONObject(0);
//                        JSONArray parts = firstCandidate.getJSONObject("content").getJSONArray("parts");
//                        String formattedText = parts.getJSONObject(0).getString("text");
//
//                        Log.d("GeminiApi", "Formatted Text: " + formattedText);
//                        callback.onSuccess(formattedText);
//                    } catch (Exception e) {
//                        callback.onFailure("Parsing error: " + e.getMessage());
//                    }
//                }
//            });
//        } catch (Exception e) {
//            callback.onFailure("Request error: " + e.getMessage());
//        }
//    }
//}

import android.util.Log;

import androidx.annotation.NonNull;

import okhttp3.*;
        import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;

public class GeminiAPI {
    private static final String API_KEY = "AIzaSyA6xrcZjb2r_seYAHNwfxBtUK3v4uoj5PQ"; // Replace with your Gemini API Key
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";
    private static final OkHttpClient client = new OkHttpClient();

    public interface GeminiCallback {
        void onSuccess(String formattedText);
        void onFailure(String error);
    }

    public static void formatJobDescription(String jobDescription, GeminiCallback callback) {
        try {
            // Fix: Correct JSON Body Format
            JSONObject jsonBody = new JSONObject();
            JSONArray contentsArray = new JSONArray();
            JSONObject contentObject = new JSONObject();
            contentObject.put("parts", new JSONArray().put(new JSONObject().put("text",
                    "Format this job description with bullet points and headings so that i can direct set that text to textview to look beautiful:\n" + jobDescription)));
            contentsArray.put(contentObject);
            jsonBody.put("contents", contentsArray);

            RequestBody body = RequestBody.create(MediaType.parse("application/json"), jsonBody.toString());

            Request request = new Request.Builder()
                    .url(API_URL + "?key=" + API_KEY)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    callback.onFailure(e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.onFailure("Error: " + response.message());
                        Log.d("GeminiApi", "Error: " + response.message());
                        return;
                    }

                    String responseBody = response.body().string();
                    Log.d("GeminiApi", "Response: " + responseBody); // Debugging

                    try {
                        // Fix: Correct Response Parsing
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        JSONArray candidates = jsonResponse.getJSONArray("candidates");
                        JSONObject firstCandidate = candidates.getJSONObject(0);
                        JSONArray parts = firstCandidate.getJSONObject("content").getJSONArray("parts");
                        String formattedText = parts.getJSONObject(0).getString("text");

                        Log.d("GeminiApi", "Formatted Text: " + formattedText);
                        callback.onSuccess(formattedText);
                    } catch (Exception e) {
                        callback.onFailure("Parsing error: " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            callback.onFailure("Request error: " + e.getMessage());
        }
    }
}

