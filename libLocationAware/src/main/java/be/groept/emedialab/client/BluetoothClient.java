package be.groept.emedialab.client;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import be.groept.emedialab.communications.InputThread;
import be.groept.emedialab.communications.OutputThread;
import be.groept.emedialab.fragments.ClientFragment;
import be.groept.emedialab.server.SocketInputOutputTrio;
import be.groept.emedialab.util.ConnectionException;
import be.groept.emedialab.util.GlobalResources;

import java.io.IOException;
import java.util.UUID;

/**
 * BluetoothClient is an implementation for connections using the Bluetooth connection. Android
 * Bluetooth-sockets are used, which are nearly identical in use to the Java Network Sockets. The
 * BluetoothSocket and Socket classes do not have a common super class. But they do use the same
 * Input and OutputStreams.
 */

/**
 * a helper class AsyncTask is used to perform background threads
 */
public class BluetoothClient extends AsyncTask<Void, String, Void> {
    /**
     * doInBackground() has void parameters and will inform about its progress by string values
     * it will return a void value as its result
     */
    private static final String TAG = "BluetoothClient";
    private BluetoothSocket socket;
    private BluetoothDevice device;
    private Context context;
    private final Object mutex;

    private boolean isConnected;

    public BluetoothClient(Object mutex, Context context, BluetoothDevice device) {
        this.mutex = mutex;
        this.context = context;
        this.device = device;
    }

    /**
     * Will cancel an in-progress connection, and close the socket
     */
    /*public void cancel() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    /*public void update(String string){
        this.publishProgress(string);
    }*/

    @Override
    /**
     * a dedicated background method is used for handling multiple threads.
     * this method defines the event handling operation itself
     * defines the operation of the background threads.
     */
    public Void doInBackground(Void... voids){
        try{
            isConnected = start();
            Log.i(TAG, "[ BUSY ] Preparing to release lock.");
            //The notify must be done _AFTER_ 'isConnected' is assigned.
            synchronized (mutex){// serializes access to the block of this method by multiple threads
                mutex.notify();//wakes up the thread next in the queue
                Log.i(TAG, "[ OK ] Released lock");
            }
        } catch (ConnectionException e){
            e.printStackTrace();
            //Toast.makeText(context, "Client End: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return null;
    }
    /**
     * @return True when connection successful, false when failed.
     * @throws ConnectionException
     */
    public boolean start() throws ConnectionException {//invoking the thread's start()
        try {
            Log.i(TAG, "[ - ] Bluetooth connecting...");

            // Creating a socket by using the same UUID defined by the server.
            socket = device.createRfcommSocketToServiceRecord(UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66"));
            socket.connect();//connecting to a partner device.

            if (socket.isConnected()) {
                Log.i(TAG, "Connection Successful.");

                Log.i(TAG, "Creating ClientOutputThread");
                //obtain access to stream going out of the socket
                OutputThread clientOutputThread = new ClientOutputThread(socket.getOutputStream());
                Log.i(TAG, "Creating ClientInputThread");
                //obtain access to stream coming into the socket
                InputThread inputThread = new InputThread(socket.getInputStream(), "");
                Log.i(TAG, "Starting input and output threads.");
                clientOutputThread.start();//start method is called to launch the thread
                inputThread.start();

                GlobalResources.getInstance().addConnectedDevice("", new SocketInputOutputTrio(socket, null, clientOutputThread));

                isConnected = true;
            } else {
                Log.e(TAG, "Connection failed.");
                isConnected = false;
            }
            return isConnected;
        } catch (Exception e) {
            throw new ConnectionException(e.getMessage(), e);
        }
    }
    // disconnecting with a partner device
    public void quit(){
        GlobalResources.getInstance().removeConnectedDevice("");
        try {
            if(socket != null) {
                this.socket.close();// closing a communication socket.
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    /**
     * After the doBackground method returned the last method is executed by the thread to take the result.
     */
    public void onPostExecute(Void aVoid){
        super.onPostExecute(aVoid);
        ClientFragment.connectionDone(isConnected, context);
    }

    public boolean isConnected(){
        return isConnected;
    }
}