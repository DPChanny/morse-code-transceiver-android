package dpc.android.morse_code_transceiver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class ReceiverActivity extends AppCompatActivity implements SensorEventListener {

    private boolean isReceiving = false;

    private final MessageHandler handler = new MessageHandler();

    private SensorManager sensorManager;

    private Sensor lightSensor;

    private int light = 0;

    private int lightMiddle = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if( lightSensor == null ){
            Toast.makeText(getApplicationContext(), "조도 센서를 찾지 못하였습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }
        SeekBar lightSensorSeekBar = findViewById(R.id.light_sensor_seek_bar);
        lightSensorSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lightMiddle = progress;
                TextView lightSensorMiddleValue = findViewById(R.id.light_sensor_middle_value);
                lightSensorMiddleValue.setText(Integer.toString(lightMiddle));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        lightSensorSeekBar.setProgress(lightMiddle);
    }

    public void OnMainMenu(View v){
        finish();
    }

    public void OnStartReceiving(View v){
        isReceiving = true;
        
        AddLog("메시지 수신 시작");

        SetEnabled();

        (new Thread(new Receiver())).start();
    }

    public void OnCancelReceiving(View v){
        isReceiving = false;
    }

    private void ResetMessage(){
        TextView message = findViewById(R.id.message);
        message.setText("");
        SetEnabled();
    }
    
    //버튼과 메시지 활성 상태 조정
    private void SetEnabled(){
        Button cancelReceiving = findViewById(R.id.cancel_receiving);
        cancelReceiving.setEnabled(isReceiving);
        Button startReceiving = findViewById(R.id.start_receiving);
        startReceiving.setEnabled(!isReceiving);
        SeekBar lightSensorSeekBar = findViewById(R.id.light_sensor_seek_bar);
        lightSensorSeekBar.setEnabled(!isReceiving);
        Button toMainMenu = findViewById(R.id.to_main_menu);
        toMainMenu.setEnabled(!isReceiving);
    }

    private void AddLog(String _log){
        TextView log = findViewById(R.id.log);
        String text = log.getText().toString() + "\n" +  _log;
        log.setText(text);
        ScrollView scrollview = findViewById(R.id.log_scroll);
        new Handler().postDelayed(() -> {
            scrollview.fullScroll(View.FOCUS_DOWN);
            scrollview.invalidate();
        }, 100);
    }

    private void AddMessage(String _message){
        TextView message = findViewById(R.id.message);
        String text = message.getText().toString() + _message;
        message.setText(text);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_LIGHT){
            light = (int) event.values[0];
        }
        TextView lightSensorValue = findViewById(R.id.light_sensor_value);
        Log.i(getApplicationContext().getClass().getName(), Integer.toString(light));
        lightSensorValue.setText(Integer.toString(light));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    //수신 쓰레드
    public class Receiver implements Runnable
    {
        private boolean lastStatus = false;

        private long lastOnTime = 0;

        private boolean isReceivingSentence = false;

        private int[] currentBits = new int[] {0, 0, 0, 0, 0, 0};

        private int lastIndex = 0;

        @Override
        public void run(){
            while(isReceiving){
                if(light < lightMiddle){
                    if(lastStatus){
                        lastStatus = false;
                        AddLogMessage("꺼짐 감지");
                        long onTime = System.currentTimeMillis() - lastOnTime;
                        AddLogMessage("지연 시간: " + onTime);

                        long[] intervals = new long[]
                                        {Math.abs(Constants.sentence_start_end - onTime),
                                        Math.abs(Constants.word_start_end - onTime),
                                        Math.abs(Constants.zero_bit - onTime),
                                        Math.abs(Constants.one_bit - onTime)};
                        int index = 0;
                        for (int i = 0; i < intervals.length; i++){
                            if(intervals[i] < intervals[index]){
                                index = i;
                            }
                        }
                        if(index == 0){
                            if(!isReceivingSentence){
                                isReceivingSentence = true;
                                ResetMessageMessage();
                                AddLogMessage("문장 수신 시작");
                            }
                            else{
                                isReceivingSentence = false;
                                AddLogMessage("문장 수신 종료");
                            }
                        }
                        if(index == 1){
                            if(isReceivingSentence){
                                AddMessageMessage(StringBinary.GetString(currentBits));
                                AddLogMessage(StringBinary.GetString(currentBits) + " 수신 완료");
                                currentBits = new int[] {0, 0, 0, 0, 0, 0};
                                lastIndex = 0;
                            }
                        }
                        if(index == 2){
                            if(isReceivingSentence){
                                if(lastIndex == 6){

                                    AddLogMessage("수신 에러");
                                    isReceivingSentence = false;
                                    AddLogMessage("문장 수신 종료");
                                    continue;
                                }
                                AddLogMessage("0 수신 완료");
                                lastIndex++;
                            }
                        }
                        if(index == 3){
                            if(isReceivingSentence){
                                if(lastIndex == 6){

                                    AddLogMessage("수신 에러");
                                    isReceivingSentence = false;
                                    AddLogMessage("문장 수신 종료");
                                    continue;
                                }
                                AddLogMessage("1 수신 완료");
                                currentBits[lastIndex++] = 1;
                            }
                        }
                    }
                }else{
                    if(!lastStatus){
                        lastStatus = true;
                        AddLogMessage("켜짐 감지");
                        lastOnTime = System.currentTimeMillis();
                    }
                }
            }

            isReceiving = false;

            SetEnabledMessage();
        }

        private void AddMessageMessage(String _message){
            Message message = handler.obtainMessage();
            Bundle _bundle = new Bundle();
            _bundle.putString("work", "AddMessage");
            _bundle.putString("value", _message);
            message.setData(_bundle);
            handler.sendMessage(message);
        }

        private void AddLogMessage(String _log){
            Message _message = handler.obtainMessage();
            Bundle _bundle = new Bundle();
            _bundle.putString("work", "AddLog");
            _bundle.putString("value", _log);
            _message.setData(_bundle);
            handler.sendMessage(_message);
        }

        private void SetEnabledMessage(){
            Message _message = handler.obtainMessage();
            Bundle _bundle = new Bundle();
            _bundle.putString("work", "SetEnabled");
            _message.setData(_bundle);
            handler.sendMessage(_message);
        }

        private void ResetMessageMessage(){
            Message message = handler.obtainMessage();
            Bundle _bundle = new Bundle();
            _bundle.putString("work", "ResetMessage");
            message.setData(_bundle);
            handler.sendMessage(message);
        }

    }

    private class MessageHandler extends Handler{
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            Bundle bundle = msg.getData();

            if(bundle.getString("work").equals("AddLog")){
                AddLog(bundle.getString("value"));
            }
            if(bundle.getString("work").equals("SetEnabled")){
                SetEnabled();
            }
            if(bundle.getString("work").equals("AddMessage")){
                AddMessage(bundle.getString("value"));
            }
            if(bundle.getString("work").equals("ResetMessage")){
                ResetMessage();
            }
        }
    }
}