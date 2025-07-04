package com.tickets.myapplication.Services;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

public class ApiClient {

    private static final String BASE_URL = "http://177.234.209.101:3545/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
          if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(new OkHttpClient.Builder().build())
                    .build();
        }
        return retrofit;
    }
}
