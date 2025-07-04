package com.tickets.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.navigation.fragment.NavHostFragment;

import com.blikoon.qrcodescanner.QrCodeActivity;
import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentFirstBinding;

import org.json.JSONException;
import org.json.JSONObject;

public class FirstFragment extends Fragment {
    private static final int REQUEST_CODE_QR_SCAN = 100;
    private FragmentFirstBinding binding;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        String port = prefs.getString("puerto","") ;
        String ip = prefs.getString("ip","") ;
        binding.ingresopuerto.setText(port);
        binding.TextIpsocket.setText(ip);
        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(binding.ingresopuerto.getText().toString().isEmpty()){
                    Toast.makeText(getContext(),"Complete todos los campos",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(binding.TextIpsocket.getText().toString().isEmpty()) {
                    Toast.makeText(getContext(),"Complete todos los campos",Toast.LENGTH_SHORT).show();
                    return ;
                }
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor edit = prefs.edit();
                edit.putString("ip", binding.TextIpsocket.getText().toString());
                edit.putString("puerto", binding.ingresopuerto.getText().toString());
                edit.apply();

                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });
        binding.btnScanIp.setOnClickListener(new View.OnClickListener(){
            @Override
            public void  onClick(View view){
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Solicitar permiso
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_QR_SCAN);
                } else {
                    // Permiso ya concedido, iniciar actividad
                    Intent i = new Intent(getActivity(), QrCodeActivity.class);
                    startActivityForResult(i, REQUEST_CODE_QR_SCAN);
                }
            }
        });
        binding.ticketsOline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity().getApplicationContext(), "Tree",Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_fragment_three);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_QR_SCAN) {
            if (resultCode == Activity.RESULT_OK ) {
                if (requestCode == REQUEST_CODE_QR_SCAN) {
                    if (data != null) {
                        String lectura = data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult");

                        try {
                            if (lectura.startsWith("\"") && lectura.endsWith("\"")) {
                                lectura = lectura.substring(1, lectura.length() - 1);
                            }


                            lectura = lectura.replace("\\", "");

                            JSONObject jo = new JSONObject(lectura);
                            binding.TextIpsocket.setText(jo.getString("ip"));
                            binding.ingresopuerto.setText(jo.getString("puerto"));
                            Toast.makeText(getContext(), "Leído: " , Toast.LENGTH_SHORT).show();

                                return;
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.d("error",e.getMessage());
                        }
                    }
                }
                String qrResult = data.getStringExtra("com.blikoon.qrcodescanner.error_decoding_image");

                //  String qrResult = data.getStringExtra("qr_result"); // Ajusta el key según el que uses
                if (qrResult != null) {
                    // Procesar el resultado del QR
                    // Gson gson = new Gson();
                    // DatosSocket datosRecibidos = gson.fromJson(qrResult, DatosSocket.class);
                    //binding.TextIpsocket.setText(datosRecibidos.getCampo1());
                    Toast.makeText(getContext(), "Resultado: " + qrResult, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "No se pudo obtener el resultado del escaneo" + qrResult, Toast.LENGTH_SHORT).show();
                }
            } else {
                // Usuario canceló el escaneo (por ejemplo, presionó atrás)
                Toast.makeText(getContext(), "Escaneo cancelado", Toast.LENGTH_SHORT).show();
            }
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_QR_SCAN) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, iniciar actividad
                Intent i = new Intent(getActivity(), QrCodeActivity.class);
                startActivityForResult(i, REQUEST_CODE_QR_SCAN);
            } else {
                Toast.makeText(getContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}