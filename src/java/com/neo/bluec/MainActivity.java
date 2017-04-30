package com.neo.bluec;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    String TAG = "MainActivity";

    // 搜索并配对按钮
    Button searchpair_button = null;

    // 界面显示配对设备
    ListView pairedListview;
    // 蓝牙适配器
    BluetoothAdapter bluetoothAdapter;
    // 已配对设备
    Set<BluetoothDevice> pairedDevices;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate()");
        setContentView(R.layout.activity_main);

        searchpair_button = (Button) findViewById(R.id.searchpair_button);
        pairedListview = (ListView) findViewById(R.id.paired_listview);

        // 获取 BluetoothAdapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // 首先判断设备是否支持蓝牙，不支持则退出，支持则打开蓝牙
        if (bluetoothAdapter == null){
            Toast.makeText(this, R.string.nonsupport_bluetooth, Toast.LENGTH_LONG).show();
            finish();
        }else{
            bluetoothAdapter.enable();
            //蓝牙开启需要时间，在蓝牙开启之后才能获取已配对设备，所以先sleep一秒，不然会没有已配对设备信息
            SystemClock.sleep(1000);
        }


        // 点击"搜索配对"按钮，调用系统发现配对蓝牙界面
        searchpair_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
            }
        });

        // 选择对应蓝牙设备，转到InfoActivity建立连接，接收信息
        pairedListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView selectItem = (TextView) view;
                Intent intent = new Intent(MainActivity.this,InfoActivity.class);
                // 其实可以只传mac地址过去
                intent.putExtra("deviceInfos",selectItem.getText().toString().split("\n"));
                startActivity(intent);

            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG,"onStart()");
        // 获取已配对设备,在onStart()里调用，写一次就够了
        showPairedDevices();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy()");
        // 关闭蓝牙
        if(bluetoothAdapter.isEnabled()){
            bluetoothAdapter.disable();
        }
    }

    // 获取已配对设备
    private void showPairedDevices(){
        Log.d(TAG,"showPairedDevices()");

        pairedDevices = bluetoothAdapter.getBondedDevices();

        // 如果有已配对设备，则显示到界面上
        if (pairedDevices.size() > 0) {
            // 已配对设备ArrayAdapter
            ArrayAdapter<String> pairedArrayAdapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1);
            String deviceName;
            for (BluetoothDevice device : pairedDevices) {
                // 记录名字和地址
                deviceName = device.getName();  //可能返回值为Null，做下处理
                if(deviceName == null){
                    deviceName = getString(R.string.Null);
                }
                pairedArrayAdapter.add(deviceName + "\n" + device.getAddress());

            }
            pairedListview.setAdapter(pairedArrayAdapter);
        }
    }

}
