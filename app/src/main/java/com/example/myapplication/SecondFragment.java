package com.example.myapplication;

import android.Manifest;
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
import com.example.myapplication.Infraestructura.MyWebSocketClient;
import com.example.myapplication.databinding.FragmentSecondBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SecondFragment extends Fragment {

    private static final int REQUEST_CODE_QR_SCAN = 100;
    private FragmentSecondBinding binding;
    private  ConstraintLayout layou;
    private Handler reconnectHandler = new Handler();
    private int retryDelay = 5000;
    private MyWebSocketClient socketClient;
    private Handler reconnectHandle = new Handler(Looper.getMainLooper());
    private int reconnectAttempts = 0;
    private final int MAX_RECONNECT_ATTEMPTS = 10;
    private final int RECONNECT_DELAY_MS = 5000; // 5 segundos
    private boolean usuarioSalioManualmente = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {binding = FragmentSecondBinding.inflate(inflater, container, false);return binding.getRoot();}

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

      //  final MediaPlayer mediaPlayer = MediaPlayer.create(getContext(), R.raw.store);
        //final MediaPlayer mediaPlayerfile = MediaPlayer.create(getContext(), R.raw.failed);


        // return binding.getRoot();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        String ip =  prefs.getString("ip", "Default");  // O usa la IP de tu dispositivo
        String port = prefs.getString("puerto","Default");
        String clientId = getDeviceId();
        Log.d("Puertos",port);
        socketClient = new MyWebSocketClient(ip, port, clientId, new MyWebSocketClient.WebSocketMessageListener() {
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
                            //mediaPlayerfile.start();
                        } else {
                            binding.PanelMensaje.setBackgroundColor(ContextCompat.getColor(activity, R.color.grreen));
                            //mediaPlayer.start();
                        }

                        String[] parts = jo.getString("Codigo").split("-");
                        System.out.println(jo.getString("Codigo"));
                        String part1 = parts.length > 0 ? parts[0] : jo.getString("Codigo");
                        String part2 = parts.length > 1 ? parts[1] : "";
                        binding.mensajeCodigo.setText(decodeUnicode(part1));
                        binding.tiempocodigo.setText(decodeUnicode(part2));

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
                });
            }
            @Override
            public void onDisconnected() {
                Log.d("Desonectado","⚠️ Desconectado del servidor");
                attemptReconnection();
            }
            @Override
            public void onError(String error) {
                Log.d("Desonectado","⚠️ Desconectado del servidor error  "+error);
                attemptReconnection();
            }
        });

        socketClient.start();
        binding.buttonSecondy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Solicitar permiso
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_QR_SCAN);
                } else {
                    // Permiso ya concedido, iniciar actividad
                    Intent i = new Intent(getActivity(), QrCodeActivity.class);
                    startActivityForResult(i, REQUEST_CODE_QR_SCAN);
                }
                /*NavHostFragment.findNavController(SecondFragment.this)
                        .navigate(R.id.action_SecondFragment_to_FirstFragment);*/
            }
        });
        binding.BotonSalirConxion.setOnClickListener(new  View.OnClickListener(){
              @Override
              public void onClick(View v) {
                  socketClient.stop();
                  NavHostFragment.findNavController(SecondFragment.this)
                          .navigate(R.id.action_SecondFragment_to_FirstFragment);
                  onDestroyView();
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
                   String envios = "{ \"Type\": \"\", \"Codigo\": \"" + message + "\" }";
                    if (!message.isEmpty()) {
                        binding.codigos.setText(message);
                        socketClient.sendMessage(envios);
                        binding.ingresoCodigo.setText("");
                    }
                    return true; // Evento manejado
                }
                return false; // Evento no manejado
            }
        });
           /*  sendButton.setOnClickListener(v -> {
            String message = editText.getText().toString();
            MessageSocket messageSocket = new MessageSocket();
            messageSocket.setType("chat");
            messageSocket.setContent(message);

            // Enviar el mensaje al servidor
            socketClient.sendMessageToServer(messageSocket);
        });*/
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_QR_SCAN) {
            if (resultCode == Activity.RESULT_OK ) {
                if (requestCode == REQUEST_CODE_QR_SCAN) {
                    if (data != null) {
                        try {
                        String lectura = data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult");
                            socketClient.sendMessage(lectura);
                            //binding.TextIpsocket.setText(jo
                            return;
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.d("error",e.getMessage());
                        }
                        // Toast.makeText(getContext(), "Leído: " + lectura, Toast.LENGTH_SHORT).show();
                        //return;
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
            return;
        }
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
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
    public static String getDeviceId()
    {
        String serial = null;

        String m_szDevIDShort = "35" +
                Build.BOARD.length() % 10 + Build.BRAND.length() % 10 +
                Build.CPU_ABI.length() % 10 + Build.DEVICE.length() % 10 +
                Build.DISPLAY.length() % 10 + Build.HOST.length() % 10 +
                Build.ID.length() % 10 + Build.MANUFACTURER.length() % 10 +
                Build.MODEL.length() % 10 + Build.PRODUCT.length() % 10 +
                Build.TAGS.length() % 10 + Build.TYPE.length() % 10 +
                Build.USER.length() % 10; //13 bits

        try
        {

            //API>=9 use serial number
            Log.d("UID",new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString());
            return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
        }
        catch (Exception exception)
        {
            //serial needs an initialization
            serial = "serial"; // Any initialization
        }

        //15-digit number pieced together using hardware information
        return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (socketClient != null) {
            socketClient.stop();
        }
    }

}