package com.example.myapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.navigation.fragment.NavHostFragment;

import com.blikoon.qrcodescanner.QrCodeActivity;
import com.example.myapplication.Infraestructura.DeviceIdManager;
import com.example.myapplication.Infraestructura.MyWebSocketClient;
import com.example.myapplication.databinding.FragmentSecondBinding;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SecondFragment extends Fragment {

    private static final int REQUEST_CODE_QR_SCAN = 100;
    private FragmentSecondBinding binding;
    private  ConstraintLayout layou;
    private Handler reconnectHandler = new Handler();
    private int retryDelay = 3000;
    private MyWebSocketClient socketClient;
    private Handler reconnectHandle = new Handler(Looper.getMainLooper());
    private int reconnectAttempts = 0;
    private final int MAX_RECONNECT_ATTEMPTS = 10;
    private final int RECONNECT_DELAY_MS = 5000; // 5 segundos
    private boolean usuarioSalioManualmente = false;
    private  boolean Conectaado = true;
    private DecoratedBarcodeView barcodeView;
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private MediaPlayer mediaPlayer;
    private  MediaPlayer fallidoPlayer;
    private int[] sounds = {R.raw.store};
    private int[] failde = {R.raw.failed};
    private int sound;
    private  String codigoPendiente;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {binding = FragmentSecondBinding.inflate(inflater, container, false);return binding.getRoot();

    }
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //  final MediaPlayer mediaPlayer = MediaPlayer.create(getContext(), R.raw.store);
        //final MediaPlayer mediaPlayerfile = MediaPlayer.create(getContext(), R.raw.failed);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        String ip =  prefs.getString("ip", "Default");  // O usa la IP de tu dispositivo
        String port = prefs.getString("puerto","Default");


        String uniqueId = DeviceIdManager.getUniqueID(getContext());
        barcodeView = view.findViewById(R.id.barcode_scanner);
        mediaPlayer = MediaPlayer.create(getContext(), sounds[0]);
        fallidoPlayer = MediaPlayer.create(getContext(),failde[0]);
        //mediaPlayer.setOnCompletionListener();


        socketClient = new MyWebSocketClient(ip, port, uniqueId, new MyWebSocketClient.WebSocketMessageListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onMessageReceived(final String message) {
                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        String lectura = message;
                        if (lectura.startsWith("\"") && lectura.endsWith("\"")) {
                            lectura = lectura.substring(1, lectura.length() - 1);
                        }
                        lectura = lectura.replace("\\", "");
                        try {
                            JSONObject jo = new JSONObject(lectura);
                            if (jo.getString("Type").equals("Error")) {
                                binding.PanelMensaje.setBackgroundColor(ContextCompat.getColor(activity, R.color.red));
                                fallidoPlayer.start();


                            } else {
                                binding.PanelMensaje.setBackgroundColor(ContextCompat.getColor(activity, R.color.grreen));
                                mediaPlayer.start();
                                reconnectHandle.postDelayed(()->{
                                    mediaPlayer.stop();
                                },retryDelay);

                            }
                            String[] parts = jo.getString("Codigo").split("-");
                            System.out.println(jo.getString("Codigo"));
                            String part1 = parts.length > 0 ? parts[0] : jo.getString("Codigo");
                            String part2 = parts.length > 1 ? parts[1] : "";
                            binding.mensajeCodigo.setText(decodeUnicode(part1));
                            binding.tiempocodigo.setText(decodeUnicode(part2));
                            reconnectHandle.postDelayed(() -> {
                                mediaPlayer.pause();
                                mediaPlayer.seekTo(0);
                            }, retryDelay);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
            @Override
            public void onConnected() {
                requireActivity().runOnUiThread(() -> {
                    Log.d("Conectado", "✅ Conectado al servidor");
                    binding.socketconectado.setVisibility(View.VISIBLE);
                    binding.layoutConexion.setVisibility(View.GONE);
                    binding.mensajeCodigo.setText("");
                    onSocketConnected();
                });
            }
            @Override
            public void onDisconnected() {
                Log.d("Desonectado","⚠️ Desconectado del servidor");
                attemptReconnection();
            }
            @Override
            public void onError(String error) {
                if (error.contains("403")) {
                    requireActivity().runOnUiThread(()->{
                        Toast.makeText(getContext(),"Conexión Rechazado",Toast.LENGTH_LONG).show();
                        reconnectHandle.postDelayed(()->{

                            socketClient.stop();
                            NavHostFragment.findNavController(SecondFragment.this)
                                    .navigate(R.id.action_SecondFragment_to_FirstFragment);
                            onDestroyView();
                        },RECONNECT_DELAY_MS);
                    });
                    Log.e("WebSocket", "❌ Acceso prohibido al WebSocket (403). Verifica autenticación o permisos.");
                    // Mostrar mensaje al usuario o abortar reconexión
                } else {
                    Log.w("WebSocket", "⚠️ Error de conexión: " + error);
                    attemptReconnection();
                }
            }
        });

        socketClient.start();
        barcodeView.setVisibility(View.GONE); // Ocultar al inicio
        barcodeView.pause();
        //binding.layoutConexion.setVisibility(View.VISIBLE);

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            barcodeView.resume();
        }

        binding.buttonSecondy.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            } else {
                binding.layoutConexion.setVisibility(View.GONE);
                barcodeView.setVisibility(View.VISIBLE);
                barcodeView.resume();
            }
        });


        binding.BotonSalirConxion.setOnClickListener(new  View.OnClickListener(){
            @Override
            public void onClick(View v) {
                socketClient.stop();
                NavHostFragment.findNavController(SecondFragment.this)
                        .navigate(R.id.action_SecondFragment_to_FirstFragment);
                // onDestroyView();
            }
        });
        binding.salir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                socketClient.stop();
                NavHostFragment.findNavController(SecondFragment.this)
                        .navigate(R.id.action_SecondFragment_to_FirstFragment);
                onDestroyView();
            }
        });
        binding.ingresoCodigo.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    Toast.makeText(getContext(), "Enter"+binding.ingresoCodigo.getText(), Toast.LENGTH_SHORT).show();
                    String message = binding.ingresoCodigo.getText().toString().trim();

                    if (!message.isEmpty()) {
                        binding.codigos.setText(message);
                        socketClient.sendMessage(message);
                        binding.ingresoCodigo.setText("");
                    }
                    return true;
                }
                return false;
            }
        });
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                //barcodeView.pause(); // pausa luego de escanear
                //Toast.makeText(getContext(), "Código: " + result.getText(), Toast.LENGTH_SHORT).show();

                binding.barcodeScanner.setVisibility(View.GONE);
                if (Conectaado) {
                    binding.codigos.setText(result.getText());
                    // Si ya conectado, enviamos inmediatamente
                    socketClient.sendMessage(result.getText());
                    return;
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {}
        });
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(getContext())
                        .setTitle("Bienvenido")
                        .setMessage("Conexión Completa.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                barcodeView.pause();
                                //Toast.makeText(SecondFragment.this, "OK presionado", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .show();
            }
        });

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CODE_QR_SCAN);
        } else {
            //startScanner();
        }


    }


    // Método para decodificar \u00ED a í
    public static String decodeUnicode(String input) {
        StringBuilder sb = new StringBuilder();
        Pattern pattern = Pattern.compile("\\\\u([0-9A-Fa-f]{4})");
        Matcher matcher = pattern.matcher(input);
        int last = 0;
        while (matcher.find()) {
            sb.append(input, last, matcher.start());
            int codePoint = Integer.parseInt(matcher.group(1), 16);
            sb.append((char) codePoint);
            last = matcher.end();
        }
        sb.append(input.substring(last));
        return sb.toString();
    }
    public void onSocketConnected() {
        Conectaado = true;
        // Si hay código pendiente, se envía ahora
        if (codigoPendiente != null) {
            socketClient.sendMessage(codigoPendiente);
            Toast.makeText(getContext(),codigoPendiente,Toast.LENGTH_SHORT).show();
            codigoPendiente = null;  // Limpiar la variable
        }
    }
    public void callParentMethod(){
        getActivity().onBackPressed();
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
    private void attemptReconnection() {
        if (usuarioSalioManualmente|| !isAdded() || binding == null) {
            Log.d("WebSocket", "🚫 Reconexión cancelada por el usuario.");
            socketClient.stop();
            return;
        }
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            socketClient.stop();
            Log.d("WebSocket", "❌ Reintentos agotados");
            NavHostFragment.findNavController(SecondFragment.this)
                    .navigate(R.id.action_SecondFragment_to_FirstFragment);
            return;
        }

        reconnectHandler.postDelayed(() -> {
            Log.d("WebSocket", "🔄 Reintentando conexión... intento #" + (reconnectAttempts + 1));
            binding.layoutConexion.setVisibility(View.VISIBLE);
            binding.socketconectado.setVisibility(View.GONE);
            binding.mensajeCodigo.setText("Esperando Conexión del Servidor");
            reconnectAttempts++;
            socketClient.start();
        }, RECONNECT_DELAY_MS);
    }



}