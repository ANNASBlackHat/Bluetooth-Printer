package annasblackhat.dev.com.printbluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import android.content.IntentFilter
import android.os.Handler
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.*


class MainActivity : AppCompatActivity() {

    lateinit var bluetoothAdapter: BluetoothAdapter
    lateinit var bluetoothSocket: BluetoothSocket
    lateinit var bluetoothDevice: BluetoothDevice
    lateinit var mReceiver: BroadcastReceiver

    // needed for communication to bluetooth device / network
    lateinit var mOutputStream: OutputStream
    lateinit var mInputStream: InputStream

    lateinit var workerThread: Thread

    lateinit var readBuffer: ByteArray
    var readBufferPosition: Int = 0
    var stopWorker: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initBroadcastReceiver()
        btnPair.setOnClickListener {
            findBluetooth()
        }

        btnPrint.setOnClickListener { sendData() }

        val filter = IntentFilter()
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)

        registerReceiver(mReceiver, filter)
    }

    private fun sendData() {
        mOutputStream.write(edtInput.text.toString().toByteArray())
        pushInfo("Data sent!")
    }

    private fun initBroadcastReceiver() {
        mReceiver = object : BroadcastReceiver(){
            override fun onReceive(p0: Context?, intent: Intent?) {
                val action = intent?.action
                print("xxx on receive...")
                if(BluetoothDevice.ACTION_FOUND == action){
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice
                    println("xxx found devicex : "+device.name)
                }
                when(action){
                    BluetoothDevice.ACTION_FOUND  -> {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice
                        println("xxx found device : "+device.name)
                    }

                }
            }
        }
    }

    fun pushInfo(info: String){
        txtInfo.text = txtInfo.text.toString()+"\n"+info
    }

    private fun findBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if(bluetoothAdapter == null){
            pushInfo("No Bluetooth adapter found!")
        }
        val pairedDevice = bluetoothAdapter.bondedDevices
        if(pairedDevice.isNotEmpty()){
            for(device in pairedDevice){
                println("xxxx printer name : "+device.name)
                //EP5802AI is the name for your bluetooth printer
                //the printer should be paired in order to scanable
                if(device.name.equals("EP5802AI")){
                    bluetoothDevice = device
                    pushInfo("Bluetooth device found!")
                    openBluetooth()
                }
            }
        }
        bluetoothAdapter.startDiscovery()
    }

    private fun openBluetooth() {
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid)
        bluetoothSocket.connect()
        mOutputStream = bluetoothSocket.outputStream
        mInputStream = bluetoothSocket.inputStream

        pushInfo("Bluetooth opened...")
        beginListenerForData()
    }

    private fun beginListenerForData() {
        val handler = Handler()
        val delimiter: Byte = 10

        stopWorker = false
        readBufferPosition = 0
        readBuffer = ByteArray(1024)

        workerThread = Thread(Runnable {
            while (!Thread.currentThread().isInterrupted && !stopWorker){
                val bytesAvaliable = mInputStream.available()
                if(bytesAvaliable > 0){
                    val packetBytes = ByteArray(bytesAvaliable)
                    mInputStream.read(packetBytes)

                    for(i in 0 until bytesAvaliable-1){
                        val b = packetBytes[i]
                        if(b == delimiter){
                            val encodedBytes = ByteArray(readBufferPosition)
                            System.arraycopy(
                                    readBuffer, 0,
                                    encodedBytes, 0,
                                    encodedBytes.size
                            )

//                            val data = String(encodedBytes, Charset("US-ASCII"))
                            readBufferPosition = 0

//                            handler.post { pushInfo(data) }
                        }else{
                            readBuffer[readBufferPosition++] = b
                        }
                    }
                }
            }
        })

        workerThread.start()
    }
}
