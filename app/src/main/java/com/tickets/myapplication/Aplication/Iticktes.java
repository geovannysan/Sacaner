package com.tickets.myapplication.Aplication;

import com.tickets.myapplication.Dominio.ApiResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface Iticktes {
        @GET("Boleteria/Boletopdf/{id}")
        Call<ResponseBody> getBoletoPorId(@Path("id") String id);
}
