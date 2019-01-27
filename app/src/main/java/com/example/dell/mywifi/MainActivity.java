package com.example.dell.mywifi;

import java.util.Base64.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.NetworkOnMainThreadException;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {

    Button send;
    Button btServer;
    Button btClient;
    EditText writeMsg;
    TextView status;
    TextView IPadd;
    TextView myMessa;

    SendReceive sendReceive;

     final int STATE_LISTENING = 1;
     final int STATE_CONNECTING = 2;
     final int STATE_CONNECTED = 3;
     final int STATE_CONNECTION_FAILED = 4;
     final int STATE_MESSAGE_RECEIVED = 5;

    LinearLayout upper, lower;
    String txtNickName;

    KeyGenerator keygen;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }       //https://stackoverflow.com/questions/43511365/how-to-socket-thread-in-android-api-25
        findViewByID();
        implementListeners();


    }

    private void implementListeners() {
        btClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClientClass clientClass = new ClientClass();
                clientClass.start();
                btServer.setEnabled(false);
            }
        });

        btServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ServerClass serverClass = new ServerClass();
                serverClass.start();
                IPadd.setText(serverClass.getIpAddress() +" and port: " + serverClass.getPort());
                btClient.setEnabled(false);
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String string = String.valueOf(txtNickName + ": " + writeMsg.getText());
//                byte[]encodeValue = Base64.encode(string.getBytes(), Base64.DEFAULT);
//                sendReceive.write(encodeValue);
                sendReceive.write(string.getBytes());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        txtNickName = sharedPref.getString("NickName", "User");
        Toast.makeText(this, "Witaj " + txtNickName + "!", Toast.LENGTH_SHORT).show();
    }

    private  class ServerClass extends Thread{
        private ServerSocket serverSocket;
        Socket socket;
        static final int socketServerPORT = 8080;

        public ServerClass(){
        }

        @Override
        public void run()
        {
            try {
                serverSocket = new ServerSocket(socketServerPORT);
                while (socket == null) {
                    socket = serverSocket.accept();
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTING;
                    handler.sendMessage(message);
                }
                if (socket != null) {
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTED;
                    handler.sendMessage(message);
                    sendReceive = new SendReceive(socket);
                    sendReceive.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public String getIpAddress() {
            String ip = "";
            try {
                Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                        .getNetworkInterfaces();
                while (enumNetworkInterfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = enumNetworkInterfaces
                            .nextElement();
                    Enumeration<InetAddress> enumInetAddress = networkInterface
                            .getInetAddresses();
                    while (enumInetAddress.hasMoreElements()) {
                        InetAddress inetAddress = enumInetAddress
                                .nextElement();

                        if (inetAddress.isSiteLocalAddress()) {
                            ip += "Server running at : "
                                    + inetAddress.getHostAddress();
                        }
                    }
                }

            } catch (SocketException e) {
                e.printStackTrace();
                ip += "Something Wrong! " + e.toString() + "\n";
            }
            return ip;
        }
        public int getPort() {
            return socketServerPORT;
        }
    }

    private class ClientClass extends Thread{
        String dstAddress;
        int dstPort;

        public ClientClass() {
            dstAddress = "192.168.1.23";//moje
            dstPort = 8080;//moje
        }

        public void run(){
            Socket socket;
            try {
                socket = new Socket(dstAddress,dstPort);
                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);
                sendReceive = new SendReceive(socket);
                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
//        finally {
//                if (socket != null) {
//                    try {
//                        socket.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
        }
    }

    private class SendReceive extends Thread{
        private final Socket socket2;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        Message msg;

        private SendReceive(Socket socket){
            this.socket2=socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                tempIn = socket.getInputStream();
                tempOut = socket.getOutputStream();


            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = tempIn;
            outputStream = tempOut;
        }
        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;

            while(true){
                try {
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes){
            try {
                outputStream.write(bytes);
                byte[] readBuff = (byte[]) msg.obj;
                String tempMsg = new String(readBuff, 0 ,msg.arg1);
                myMessa.setText(tempMsg);
            } catch (IOException e) {
                e.printStackTrace();
            }
            catch (NullPointerException e1) {
                e1.printStackTrace();
            }

        }
    }



    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case STATE_LISTENING:
                    status.setText("Listening");
                    break;
                case STATE_CONNECTING:
                    status.setText("Connecting  ");
                    break;
                case STATE_CONNECTED:
                    status.setText("Connected");
                    btClient.setEnabled(false);
                    btServer.setEnabled(false);
//                    IPadd.setVisibility(View.GONE);
                    break;
                case STATE_CONNECTION_FAILED:
                    status.setText("Connecting Failed");
                    break;
                case STATE_MESSAGE_RECEIVED:
                    byte[] readBuff = (byte[]) msg.obj;//
//                    byte[] decodeValue = Base64.decode((byte[]) msg.obj, Base64.DEFAULT);
                    String tempMsg = new String(readBuff,0,msg.arg1);
//                    String tempMsg = new String(decodeValue);
//                    String tempMsg = new String(decodeValue,0,msg.arg1);
                    myMessa.setText(tempMsg);
                    break;
            }
            return true;
        }
    });




    public void findViewByID(){
        status = findViewById(R.id.tvStatus);
        btServer = findViewById(R.id.btServer);
        btClient = findViewById(R.id.btClient);
        IPadd = findViewById(R.id.TojestmojeID);
        send = findViewById(R.id.btnSend);
        writeMsg = findViewById(R.id.etMessage);
        myMessa = findViewById(R.id.myMessa);

        upper = findViewById(R.id.upperLayout);
        lower = findViewById(R.id.lowerLayout);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.item2:
                openActivityMessage();
                break;
            case  R.id.item1:
                Toast.makeText(this, "This window already opened", Toast.LENGTH_SHORT).show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void openActivityMessage() {
        Intent intent = new Intent(MainActivity.this, SecondActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
//            intent.putExtra("ip", dstAddress);
//            intent.putExtra("port", ip);
        startActivity(intent);
        finish();
    }
}

