package com.tickets.myapplication;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.myapplication.R;
import com.tickets.myapplication.Aplication.Iticktes;
import com.tickets.myapplication.Services.ApiClient;
import com.example.myapplication.databinding.FragmentThreeBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;

public class fragment_three extends Fragment {
    private FragmentThreeBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentThreeBinding.inflate(inflater, container, false);
        return binding.getRoot();



    }
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Iticktes iticktes = ApiClient.getClient().create(Iticktes.class);
        Toolbar toolbar = view.findViewById(R.id.tolbaethree);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(toolbar);
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.modal_bottom_sheet, null);
        bottomSheetDialog.setContentView(layout);
        bottomSheetDialog.setCancelable(true);
        bottomSheetDialog.setCanceledOnTouchOutside(true);
        TextView text = layout.findViewById(R.id.cedula);

        // Displaying the bottom sheet dialog

        //BoletoService service = new BoletoService();
        binding.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Call<ResponseBody> call = iticktes.getBoletoPorId("1338582");
                call.enqueue(new retrofit2.Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            String code = null;
                            try {
                                code = response.body().string();
                                JSONObject jo = new JSONObject(code);
                                bottomSheetDialog.show();
                                Log.d("Respons",jo.getString("estado").toString());
                                JSONObject boleto =  jo.getJSONObject("boleto");
                                text.setText(boleto.getString("cedula_boleto"));
                                String infoConciertoStr = boleto.getString("info_concierto");
                                JSONArray infoConciertoArray = new JSONArray(infoConciertoStr);

                                for (int i = 0; i < infoConciertoArray.length(); i++) {
                                    JSONObject item = infoConciertoArray.getJSONObject(i);
                                    String nombreConcierto = item.getString("nombreConcierto");
                                    int cantidad = item.getInt("cantidad");
                                    System.out.println("Concierto: " + nombreConcierto + " - Cantidad: " + cantidad);
                                }

                                Log.d("",boleto.getString("idtickte"));
                            } catch (IOException e) {
                                e.printStackTrace();
                                Log.d("nobody",e.getMessage());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Log.d("API", "Nombre: " + code);
                        } else {
                            Log.e("API", "Código de error: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.e("API", "Error de red: " + t.getMessage());
                    }
                });
            }
        });

    }


}