package com.neo.bluec;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static android.os.SystemClock.sleep;

public class InfoActivity extends AppCompatActivity {

    // log使用
    String TAG = "InfoActivity";
    // 蓝牙串口UUID
    UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // 选中的蓝牙设备
    BluetoothDevice selectDevice;
    // 蓝牙适配器
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    // 蓝牙Socket
    BluetoothSocket mmSocket;
    // 连接状态
    Boolean connectStatus = false;

    // 显示信息区
    TextView dataArea = null;
    // 滚动区,为了实现自动滚动到底部效果
    ScrollView scrollView = null;
    // 发送信息编辑区
    EditText editText = null;
    // 发送按钮
    Button sendButton = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate()");
        setContentView(R.layout.activity_info);

        dataArea = (TextView) findViewById(R.id.info_textView);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        editText = (EditText) findViewById(R.id.send_editText);
        sendButton = (Button) findViewById(R.id.send_button);


        // 选中的蓝牙设备信息：名称和mac地址
        String[] deviceInfos = getIntent().getStringArrayExtra("deviceInfos");
        // 通过mac地址得到选中的蓝牙设备
        selectDevice = bluetoothAdapter.getRemoteDevice(deviceInfos[1]);
        // 开启连接蓝牙线程
        new ConnectTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,selectDevice);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sendData = editText.getText().toString();
                editText.setText("");
                sendMessage(sendData);
            }
        });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy()");
        // 关闭socket
        try {
            mmSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 发送消息，开启一个写线程
    void sendMessage(String str){
        Log.d(TAG,"sendMessage()");
        new WriteTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,str);
    };

    private class ConnectTask extends AsyncTask<BluetoothDevice, String, Integer> {

        // 等待提示框
        ProgressDialog waitDialog = new ProgressDialog(InfoActivity.this);

        // 一些返回值
        final int SOCKET_INIT_FAILURE = 1;
        final int SOCKET_CLOSE_FAILURE = 2;
        final int SOCKET_CONNECT_FAILURE = 3;
        final int SOCKET_CONNECT_SUCCESS = 4;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            waitDialog.setMessage(getString(R.string.connecting_device));
            waitDialog.setCancelable(false);
            waitDialog.show();

        }

        @Override
        protected Integer doInBackground(BluetoothDevice... device) {
            // 通过给定的BluetoothDevice来 获取一个BluetoothSocket
            try {
                mmSocket = device[0].createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                return SOCKET_INIT_FAILURE;
            }


            try {
                //通过mmSocket连接设备  这会产生阻塞阻止，直到成功或抛出异常
                mmSocket.connect();
            } catch (IOException connectException) {
                // 无法连接; 关闭socket并离开
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    return SOCKET_CLOSE_FAILURE;
                }
                return SOCKET_CONNECT_FAILURE;
            }
            connectStatus = true;  // 将连接状态设为true
            return SOCKET_CONNECT_SUCCESS;
        }

        @Override
        protected void onPostExecute(Integer result) {
            waitDialog.dismiss();
            String showMessage = "";
            switch (result){
                case SOCKET_INIT_FAILURE:
                    showMessage = getString(R.string.socket_init_failure);
                    break;
                case SOCKET_CLOSE_FAILURE:
                    showMessage = getString(R.string.socket_close_failure);
                    break;
                case SOCKET_CONNECT_FAILURE:
                    showMessage = getString(R.string.socket_connect_failure);
                    break;
                case SOCKET_CONNECT_SUCCESS:
                    showMessage = getString(R.string.socket_connect_success);
                    // 连接成功，开启读线程
                    new ReadTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    break;
            }
            Toast.makeText(InfoActivity.this, showMessage, Toast.LENGTH_SHORT).show();
            //连接失败，返回上个activity，重新连接
            if(result != SOCKET_CONNECT_SUCCESS){
                finish();
            }
        }
    }


    // 线程读数据
    private class ReadTask extends AsyncTask<String, String, Integer>{

        private InputStream mmInStream = null;

        @Override
        protected Integer doInBackground(String... params) {
            //获取socket输入流
            try {
                mmInStream = mmSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }


            // 正式读数据
            while(true){
                byte[] buffer = new byte[1024];
                int bytes = 0;

                try {
                    bytes = mmInStream.read(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // 如果读到内容，则通知更新界面
                if(bytes > 0){
                    Log.d(TAG,"bytes:" + bytes);
                    publishProgress(new String(buffer));
                }

                // 睡1秒，也就是1秒读一次
                sleep(1000);
            }

        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            Log.d(TAG,values[0]);
            // 追加数据
            dataArea.append(values[0] + "\n\n");
            // 自动滚动到底部
            scrollView.fullScroll(ScrollView.FOCUS_DOWN);

        }
    }



    // 线程写数据
    private class WriteTask extends AsyncTask<String, String, Integer>{

        private OutputStream mmOutStream = null;


        @Override
        protected Integer doInBackground(String... params) {

            //获取socket输出流,发送数据
            try {
                mmOutStream = mmSocket.getOutputStream();
                mmOutStream.write(params[0].getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }

            return 0;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            // 提示已发送
            Toast.makeText(InfoActivity.this, R.string.sended, Toast.LENGTH_SHORT).show();
        }
    }

}
