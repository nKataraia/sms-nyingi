package org.dtree.android.messeji

import android.Manifest.permission.*
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import tz.co.gluhen.common.App
import tz.co.gluhen.common.DB
import tz.co.gluhen.common.HelperInterfaces.Changer
import tz.co.gluhen.common.event.AppEvent
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.*


class MainActivity : App() {
    lateinit var serviceIntent:Intent
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)
        val permissions= arrayOf(INTERNET,ACCESS_WIFI_STATE,RECEIVE_SMS,SEND_SMS,READ_SMS,
            READ_PHONE_STATE,WRITE_EXTERNAL_STORAGE)
        serviceIntent=Intent(this,MessageSender::class.java)

        if (!hasPermissions(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_ALL)
        }else{ startService(serviceIntent)}
        showURL()
        showSimInfo()
        setIDBeforeSending()
    }

    private var idBeforeSending=0L
    private fun setIDBeforeSending(){
        val query="select max(id) from sms"
        idBeforeSending= DB.getInstance(this).fetchSingle(query, Changer{it.getLong(0)})
   }

    override fun onStart() {
        subscribe(AppEvent.MESSAGE_FROM_BROWSER,this::startViewingProgress)
        super.onStart()
    }


    private fun startViewingProgress(sms:String){
        val intent=Intent(this,ActivityShowProgress::class.java)
        intent.putExtra(GREATER_THAN_SMS_ID,idBeforeSending.toString())
        startActivity(intent)
        this.finish()
    }

    private fun showURL(){
        Thread {
             val interfaces=NetworkInterface.getNetworkInterfaces();
             while(interfaces.hasMoreElements()){
                 val i=interfaces.nextElement()
                 if(i.name== TETHERING_WIFI||i.name==WIRELESS){
                     val ips=i?.interfaceAddresses?:continue
                     val ip= ips.firstOrNull { ip -> ip.address is Inet4Address }?.address?.hostName?:continue
                     val url=String.format(getString(R.string.url),ip)
                     this.runOnUiThread{findViewById<TextView>(R.id.url).text=url}
                 }
             }
        }.start()
    }
    private fun showSimInfo(){
        val defaultSim=SmsManager.getDefault().subscriptionId
        if(defaultSim<0){return;}

        val urlTelephone= Uri.parse("content://telephony/siminfo/");
        val  cursor = this.contentResolver.query(urlTelephone, arrayOf("sim_id","carrier_name"), "_id=?",
            arrayOf(defaultSim.toString()), null)
            cursor?:return
            if(cursor.moveToNext()){
                val simId=cursor.getInt(cursor.getColumnIndex("sim_id"))+1
                val network=cursor.getString(cursor.getColumnIndex("carrier_name")).toLowerCase(Locale.ENGLISH)
                val sender="sim $simId ($network)"
                findViewById<TextView>(R.id.sendingSim).text=sender
            }
            cursor.close()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode==PERMISSION_ALL
             && grantResults.isNotEmpty()
             &&grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startService(serviceIntent)
        }
        else{ Toast.makeText(this,"Permissions Denied sending of SMS will be aborted",Toast.LENGTH_SHORT).show()}
    }


    private fun hasPermissions( permissions: Array<String>): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }
    companion object{const val PERMISSION_ALL=2
            const val TETHERING_WIFI="ap0"
            const val WIRELESS="wlan0"
            const val TAG="MainActivity"
            const val GREATER_THAN_SMS_ID="greaterthanSMSID"
    }
}
