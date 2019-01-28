package com.example.dell.mywifi;

import java.net.InetAddress;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


public class MainActivity extends AppCompatActivity {

    Button btServer, send;
    Button btClient, resetButton, clientConnect;
    EditText writeMsg, etIpAddress, etPortNumber;
    TextView status, IPadd, myMessa;

    SendReceive sendReceive;

    ListView mojawiadomosc;
    ArrayAdapter<String> BTArrayAdapter;

     final int STATE_LISTENING = 1;
     final int STATE_CONNECTING = 2;
     final int STATE_CONNECTED = 3;
     final int STATE_CONNECTION_FAILED = 4;
     final int STATE_MESSAGE_RECEIVED = 5;
     final int CHANGE_STRING_CONNECTED_WITH = 6;
    String connectedWith = "null";


    LinearLayout upper, lower, ipAndPortLayout;
    String txtNickName;

    String AES = "AES";
    String outputString;
    String key = "toJestKluczZmienGo";

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


    @Override
    protected void onResume() {
        super.onResume();
//        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("MyPREFERENCES", Context.MODE_PRIVATE);
        txtNickName = sharedPref.getString("NickName", "User");
        Toast.makeText(this, "Witaj " + txtNickName + "!", Toast.LENGTH_SHORT).show();
    }
    public void findViewByID(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN); //służy do zakrycia klawiatury
        mojawiadomosc = findViewById(R.id.mojawiadomosc);
        BTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        mojawiadomosc.setAdapter(BTArrayAdapter);

        status = findViewById(R.id.tvStatus);
        btServer = findViewById(R.id.btServer);
        btClient = findViewById(R.id.btClient);
        resetButton = findViewById(R.id.resetButton);
        clientConnect = findViewById(R.id.clientConnect);
        IPadd = findViewById(R.id.TojestmojeID);
        send = findViewById(R.id.btnSend);
        send.setEnabled(false);
        writeMsg = findViewById(R.id.etMessage);

        upper = findViewById(R.id.upperLayout);
        lower = findViewById(R.id.lowerLayout);
        ipAndPortLayout = findViewById(R.id.ipAndPortLayout);
        etIpAddress = findViewById(R.id.etIpAddress);
        etPortNumber = findViewById(R.id.etPortNumber);

    }
    private void implementListeners() {
        btClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etIpAddress.setVisibility(View.VISIBLE);
                etPortNumber.setVisibility(View.VISIBLE);
                clientConnect.setVisibility(View.VISIBLE);
            }
        });

        btServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ServerClass serverClass = new ServerClass();
                serverClass.start();
                IPadd.setVisibility(View.VISIBLE);
                IPadd.setText(serverClass.getIpAddress() +" and port: " + serverClass.getPort());
                btClient.setEnabled(false);
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String string = String.valueOf(txtNickName + ": " + getNowTime() +  "\n"+ writeMsg.getText() );
                try {
                    outputString = encrypt(string, key);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                sendReceive.write(outputString.getBytes());
            }
        });
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
        clientConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ipAddress = etIpAddress.getText().toString();
                int portNumber = Integer.parseInt(etPortNumber.getText().toString());
                if(!etIpAddress.getText().equals("null")){
                    ClientClass clientClass = new ClientClass(ipAddress, portNumber);
                    clientClass.start();
                    btServer.setEnabled(false);
                } else {
                    Toast.makeText(MainActivity.this, "Wrong IPAddress or port number format!", Toast.LENGTH_SHORT).show();
                }

            }
        });




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

                    String string = txtNickName;
                    sendReceive.write(string.getBytes());
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

        public ClientClass(String ipAddress, int portNumer) {
            dstAddress = ipAddress;
            dstPort = portNumer;
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

                String string = txtNickName;
                sendReceive.write(string.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
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
                    if(connectedWith.equals("null")) {
                        handler.obtainMessage(CHANGE_STRING_CONNECTED_WITH, bytes, -1, buffer).sendToTarget();
                    }
                    if(!connectedWith.equals("null")){
                        handler.obtainMessage(STATE_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget();
                    }
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
                    status.setText("Connected with: " + (String)msg.obj);
                    send.setEnabled(true);
                    upper.setVisibility(View.GONE); //for upper layout
                    break;
                case STATE_CONNECTION_FAILED:
                    status.setText("Connecting Failed");
                    break;
                case STATE_MESSAGE_RECEIVED:
                    byte[] readBuff = (byte[]) msg.obj;//
                    String tempMsg = new String(readBuff,0,msg.arg1);
                    try {
                        outputString = decrypt(tempMsg, key);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    BTArrayAdapter.add(outputString);
                    break;
                case CHANGE_STRING_CONNECTED_WITH:
                    byte[] readBuff2 = (byte[]) msg.obj;
                    connectedWith = new String(readBuff2,0,msg.arg1);
                    status.setText("Connected with: " + connectedWith);
            }
            return true;
        }
    });

    private SecretKeySpec generateKey(String password) throws Exception {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256"); //tworzenie instancji SkrótuWiadomości z algorytmemSHA-256bit
        byte[] bytes = password.getBytes("UTF-8");
        digest.update(bytes, 0, bytes.length); //przekazuje do instancji digest tablicze bytes
        byte[] key = digest.digest(); //generuje klucz wiadomości w byte, oblicza skrót
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES"); //tworzy klucz z tablicy key zgodny z algorytmem AES
        return secretKeySpec;
    }
    private String encrypt(String Data, String password) throws Exception{
        SecretKeySpec key = generateKey(password);
        Cipher c = Cipher.getInstance(AES); //c reprezentuje klasę szyfru
        c.init(Cipher.ENCRYPT_MODE, key); //inicjalizuje szyfr z danym kluczem, w trybie zmiany wiadomości na szyfr
        byte[] encVal = c.doFinal(Data.getBytes()); //kończy operację szyfrowania
        String encryptedValue = Base64.encodeToString(encVal, Base64.DEFAULT); //zamiana na String bez przesunięć/paddingu, DEFAULT
        return encryptedValue;
    }
    private String decrypt(String outputString, String password) throws Exception {
        SecretKeySpec key = generateKey(password);
        Cipher c = Cipher.getInstance(AES); //c reprezentuje klasę szyfru
        c.init(Cipher.DECRYPT_MODE, key); //inicjalizuje szyfr z danym kluczem, w trybie zmiany szyfru na wiadomość
        byte[] decodedVal = Base64.decode(outputString, Base64.DEFAULT); //zamiana na byte bez przesunięć/paddingu, DEFAULT
        byte[] decValue = c.doFinal(decodedVal);
        String decryptedValue = new String(decValue);
        return decryptedValue;

    }
    private String getNowTime() {
        DateFormat df = new SimpleDateFormat("H:m:s");
        Date now = Calendar.getInstance().getTime();
        String text = df.format(now);
        return text;
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
        startActivity(intent);
        finish();
    }
}

