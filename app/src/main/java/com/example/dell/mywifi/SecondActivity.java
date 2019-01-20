package com.example.dell.mywifi;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class SecondActivity extends AppCompatActivity {
Button btn_language, btn_OnOff, btn_discover, btn_changeNick;
EditText text_newNick;
String language;
String txtNickName;
TextView WelcomeMessage;

SharedPreferences sharedPref;

WifiManager wifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        findViewByID();

        clickMe();
    }



    private void clickMe() {
        btn_OnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (wifiManager.isWifiEnabled()){
                    wifiManager.setWifiEnabled(false);
                    btn_OnOff.setText(R.string.btn_On);
                } else {
                    wifiManager.setWifiEnabled(true);
                    btn_OnOff.setText(R.string.btn_Off);
                }
            }
        });



        btn_language.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChangeLanguageDialog();
            }
        });

        btn_changeNick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeNickName(text_newNick.getText().toString());
                text_newNick.setText("");
            }
        });

        btn_discover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    private void changeNickName(String newNick) {
//        sharedPref = getPreferences(Context.MODE_PRIVATE);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("NickName", newNick);
        editor.commit();
        Toast.makeText(this, R.string.ToastChangedNick, Toast.LENGTH_SHORT).show();
        WelcomeMessage.setText(getString(R.string.textWelcome) + " " + newNick + "!");

        int test = 5;


    }


    @Override
    protected void onResume() {
        super.onResume();
        readNickAndSetTextV();
    }

    private void readNickAndSetTextV(){
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        txtNickName = sharedPref.getString("NickName", "User");
        WelcomeMessage.setText(getString(R.string.textWelcome) + " " + txtNickName + "!");
    }

    private void findViewByID() {
        btn_language = findViewById(R.id.btn_language);
        btn_OnOff = findViewById(R.id.btn_OnOff);
        btn_discover = findViewById(R.id.btn_Discover);
        btn_changeNick = findViewById(R.id.btn_changeNick);
        WelcomeMessage = findViewById(R.id.WelcomeMessage);
        WelcomeMessage.setText(txtNickName);

        text_newNick = findViewById(R.id.text_newNick);

        if (wifiManager.isWifiEnabled()){
            btn_OnOff.setText(R.string.btn_Off);
        } else {
            btn_OnOff.setText(R.string.btn_On);
        }
    }


    private void showChangeLanguageDialog() {
        final String[] listItems = {"English", "Polski"};
        AlertDialog.Builder builder = new AlertDialog.Builder(SecondActivity.this) //nowe okno AlertDialog
                .setTitle(R.string.AlertDialog_Builder_title); //tytuł okienka z wyborem języków //Available Languages
        builder.setSingleChoiceItems(listItems, -1, new DialogInterface.OnClickListener() { //-1 oznacza, że żadna z opcji nie jest "checked", nie jest zaznaczona
            @Override //Dialog Interface is..Interface used to allow the creator of a dialog to run some code when an item on the dialog is clicked.
            public void onClick(DialogInterface dialog, int which) {
                if(which == 0){
                    language = "en";
                    setLocale("en");
                    recreate();// wywołuje onDestroy() oraz onCreate() na tej Activity. Tworzy się nowa instancja Activity
                    readNickAndSetTextV();
                } else {
                    language = "pl";
                    setLocale("pl");
                    recreate();
                    readNickAndSetTextV();
                }
            }
        })
                .setIcon(R.mipmap.language) //ustawienie ikonki języków
                .show();    //wyświetla AlertDialog
    }
//
    private void setLocale(String lang) {
        Locale locale = new Locale(lang); //obiekt reprezentujący ustawienia geograficzne, polityczne itp.
        Locale.setDefault(locale);  //ustawienia lokalne dla JVM względem obiektu locale
        Configuration config = new Configuration();
        config.setLocale(locale); // //zmiana ustawień regionalnych
        getResources().updateConfiguration(config, getResources().getDisplayMetrics()); //uaktualnianie danych :)
        //getResources()..Umożliwia dostęp do zasobów i klas związanych z aplikacją, a także do wywołań dla
        // operacji na poziomie aplikacji, takich jak uruchamianie działań, nadawanie i otrzymywanie zamiarów itp.
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
            case R.id.item1:
                openActivityMessage();
                break;
            case  R.id.item2:
                Toast.makeText(this, "This window already opened", Toast.LENGTH_SHORT).show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }
    private void openActivityMessage() {
        Intent intent = new Intent(SecondActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
//            intent.putExtra("ip", dstAddress);
//            intent.putExtra("port", ip);
        startActivity(intent);
        finish();
    }
}
