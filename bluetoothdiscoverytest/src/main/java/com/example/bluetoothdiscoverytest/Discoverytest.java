package com.example.bluetoothdiscoverytest;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Set;


public class Discoverytest extends AppCompatActivity {

    private BluetoothAdapter BT;
    int BLUETOOTH_REQUEST=1;
    TextView txt;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discoverytest);
        txt = (TextView) findViewById(R.id.txt);
        BT = BluetoothAdapter.getDefaultAdapter();
        txt.append("\n Adapter: " + BT);

        Bluetoothstate();
    }

    public void onActivityResult(int request_code, int result_code, Intent data){
        if(request_code==BLUETOOTH_REQUEST){
            if(result_code==RESULT_OK){
                Toast.makeText(getBaseContext()," your bluetooth is enabled",Toast.LENGTH_LONG).show();
                Bluetoothstate();
            }
            if(result_code==RESULT_CANCELED){
                Toast.makeText(getBaseContext(),"Bluetooth not enabled",Toast.LENGTH_LONG).show();
            }
        }
    }
    private void Bluetoothstate(){

        if (BT == null) {
            Toast.makeText(getBaseContext(), "No Adapter Found", Toast.LENGTH_LONG).show();

        }
        else
        {
            if (BT.isEnabled()) {

                //Toast.makeText(getBaseContext(), "Bluetooth is enabled", Toast.LENGTH_LONG).show();
                txt.append( "\n paired devices are: ");
                Set<BluetoothDevice> devices = BT.getBondedDevices();
                for (BluetoothDevice device: devices) {
                    txt.append("\n Devices: " + device.getName() + " , " + devices);
                }
            }
            else
            {
                Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(i, BLUETOOTH_REQUEST);
            }
        }

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_discoverytest, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
