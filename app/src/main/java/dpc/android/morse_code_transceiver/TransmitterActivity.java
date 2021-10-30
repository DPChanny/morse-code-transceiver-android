package dpc.android.morse_code_transceiver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.regex.Pattern;

public class TransmitterActivity extends AppCompatActivity{

    private boolean isTransmitting = false;

    private String message;

    private CameraManager cameraManager = null;
    private String cameraId;

    private final MessageHandler handler = new MessageHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transmitter);
        EditText _message = findViewById(R.id.message);
        _message.setFilters(new InputFilter[] {filter});

        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        if (cameraId == null) {
            try {
                for (String id : cameraManager.getCameraIdList()) {
                    CameraCharacteristics c = cameraManager.getCameraCharacteristics(id);
                    Boolean flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
                    if (flashAvailable != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                        cameraId = id; break;
                    }
                }
            } catch (CameraAccessException e) {
                cameraId = null;
                e.printStackTrace();
            }
        }
    }

    public void OnMainMenu(View v){
        finish();
    }

    public void OnStartTransmitting(View v){
        isTransmitting = true;

        EditText _message = findViewById(R.id.message);
        message = _message.getText().toString();

        AddLog("메시지: " + message);

        SetEnabled();

        (new Thread(new Transmitter())).start();
    }

    public void OnCancelTransmitting(View v){
        isTransmitting = false;
        AddLog("송신 취소 중");
    }

    //버튼과 메시지 활성 상태 조정
    private void SetEnabled(){
        Button cancelTransmitting = findViewById(R.id.cancel_transmitting);
        cancelTransmitting.setEnabled(isTransmitting);
        Button startTransmitting = findViewById(R.id.start_transmitting);
        startTransmitting.setEnabled(!isTransmitting);
        EditText message = findViewById(R.id.message);
        message.setEnabled(!isTransmitting);
        Button toMainMenu = findViewById(R.id.to_main_menu);
        toMainMenu.setEnabled(!isTransmitting);
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

    protected InputFilter filter = (source, start, end, dest, dstart, dend) -> {

        Pattern ps = Pattern.compile("^[a-zA-Z0-9]+$");

        if (!ps.matcher(source).matches()) {

            return "";

        }

        return null;

    };

    //송신 쓰레드
    public class Transmitter implements Runnable
    {
        @Override
        public void run(){
            try{
                AddLogMessage("송신 시작");
                SetFlashMessage(true);
                Thread.sleep(Constants.sentence_start_end);
                SetFlashMessage(false);

                Thread.sleep(Constants.term);

                for (int i = 0; i < message.length() && isTransmitting; i++) {
                    AddLogMessage(message.charAt(i) + " 송신 시작");

                    int[] binary = StringBinary.GetBinary(message.charAt(i));

                    if (binary != null) {
                        StringBuilder str = new StringBuilder();

                        for (int value : binary) {
                            AddLogMessage(message.charAt(i) + "(송신중): " + value);
                            Log.i(getApplicationContext().getClass().getName(),  message.charAt(i) + "(송신중): " + value);
                            if(value == 0){
                                SetFlashMessage(true);
                                Thread.sleep(Constants.zero_bit);
                                SetFlashMessage(false);
                            }else{
                                SetFlashMessage(true);
                                Thread.sleep(Constants.one_bit);
                                SetFlashMessage(false);
                            }
                            str.append(value);
                            Thread.sleep(Constants.term);
                        }

                        AddLogMessage(message.charAt(i) + "(송신완료): " + str);
                        Log.i(getApplicationContext().getClass().getName(),  message.charAt(i) + "(송신완료): " + str);
                    } else {
                        AddLogMessage("송신 실패(위치): " + i);
                        Log.i(getApplicationContext().getClass().getName(),  "송신 실패(위치): " + i);
                    }

                    SetFlashMessage(true);
                    Thread.sleep(Constants.word_start_end);
                    SetFlashMessage(false);
                    Thread.sleep(Constants.term);
                }

                AddLogMessage("송신 종료");
                SetFlashMessage(true);
                Thread.sleep(Constants.sentence_start_end);
                SetFlashMessage(false);

                isTransmitting = false;

                SetEnabledMessage();
                
            }catch (Exception e){
                AddLogMessage("송신 실패(위치): " + e.getMessage());
                Log.i(getApplicationContext().getClass().getName(),  "송신 실패(위치): " + e.getMessage());

                isTransmitting = false;

                SetEnabledMessage();
            }
        }

        private void SetFlashMessage(boolean _enabled){
            Message _message = handler.obtainMessage();
            Bundle _bundle = new Bundle();
            _bundle.putString("work", "SetFlash");
            _bundle.putBoolean("value", _enabled);
            _message.setData(_bundle);
            handler.sendMessage(_message);
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
    }

    private void SetFlash(boolean _isEnabled) {
        try{
            cameraManager.setTorchMode(cameraId, _isEnabled);
        }
        catch (Exception ignored){

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private class MessageHandler extends Handler{
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            Bundle bundle = msg.getData();

            if(bundle.getString("work").equals("AddLog")){
                AddLog(bundle.getString("value"));
            }
            if(bundle.getString("work").equals("SetFlash")){
                SetFlash(bundle.getBoolean("value"));
            }
            if(bundle.getString("work").equals("SetEnabled")){
                SetEnabled();
            }
        }
    }

}