package dpc.android.morse_code_transceiver;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void OnReceiver(View v){
        Intent intent = new Intent(getApplicationContext(), ReceiverActivity.class);
        startActivity(intent);
    }

    public void OnTransmitter(View v){
        Intent intent = new Intent(getApplicationContext(), TransmitterActivity.class);
        startActivity(intent);
    }

}