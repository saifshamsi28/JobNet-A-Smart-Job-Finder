package com.saif.jobnet.Api;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SkillFetcher {

    private static final String CLIENT_ID = "7uu7wk790cm2yqa7";
    private static final String CLIENT_SECRET = "xt4tH75n";
    private static final String TOKEN_URL = "https://auth.emsicloud.com/connect/token";
    private static final String SKILLS_URL = "https://emsiservices.com/skills/versions/latest/skills?limit=500";

    public interface SkillCallback {
        void onSkillsFetched(List<String> skills);
        void onError(String error);
    }

    public static void fetchSkills(SkillCallback callback) {
        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .add("grant_type", "client_credentials")
                .add("scope", "emsi_open")
                .build();

        Request tokenRequest = new Request.Builder()
                .url(TOKEN_URL)
                .post(formBody)
                .build();

        client.newCall(tokenRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Token fetch failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("Token response error: " + response.code());
                    return;
                }

                String json = response.body().string();
                String token = json.split("\"access_token\":\"")[1].split("\"")[0];

                Request skillRequest = new Request.Builder()
                        .url(SKILLS_URL)
                        .get()
                        .addHeader("Authorization", "Bearer " + token)
                        .build();

                client.newCall(skillRequest).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        callback.onError("Skill fetch failed: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            callback.onError("Skill response error: " + response.code());
                            return;
                        }

                        String responseBody = response.body().string();
                        try {
                            JSONObject jsonObject = new JSONObject(responseBody);
                            JSONArray dataArray = jsonObject.getJSONArray("data");

                            List<String> skills = new ArrayList<>();
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject skillObj = dataArray.getJSONObject(i);
                                System.out.println("skill: " + skillObj);
                                skills.add(skillObj.getString("name"));
                            }

                            callback.onSkillsFetched(skills);
                        } catch (Exception e) {
                            callback.onError("JSON parsing failed: " + e.getMessage());
                        }
                    }
                });
            }
        });
    }
}
