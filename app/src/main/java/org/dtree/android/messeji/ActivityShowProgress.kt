package org.dtree.android.messeji

import android.Manifest.permission.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import tz.co.gluhen.common.App
import tz.co.gluhen.common.DB
import tz.co.gluhen.common.HelperInterfaces.*
import tz.co.gluhen.common.ItemList
import tz.co.gluhen.common.event.AppEvent


class ActivityShowProgress : App() {
     private lateinit var adapter: ItemList<SMS>
    lateinit var smsView:TextView
    lateinit var db:DB
    lateinit var readMore:TextView
    lateinit var waiting:TextView
    lateinit var sent:TextView
    lateinit var delivered:TextView
    lateinit var failed:TextView
    lateinit var total:TextView;
    lateinit var smsManager:SMSManager;
    private var smsIdStarting="1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        smsManager= SMSManager(DB.getInstance(this.application))
        setContentView(R.layout.activity_main)
        smsView=findViewById(R.id.sms)
        smsIdStarting=intent.getStringExtra(MainActivity.GREATER_THAN_SMS_ID)?:"1"

        val permissions= arrayOf(INTERNET,ACCESS_WIFI_STATE,SEND_SMS,WRITE_EXTERNAL_STORAGE,READ_SMS)
        if (!hasPermissions(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_ALL)
            }

        db=DB.getInstance(this)

        val rc:RecyclerView=findViewById(R.id.messages)
        val holder=SMSHolder(rc)
        adapter=ItemList(rc,holder::createMessageView,holder::setData)
        holder.adapter=adapter
        adapter.itemClicked=this::onItemSelected
        readMore=findViewById(R.id.read_more)
        waiting=findViewById(R.id.waiting)
        sent=findViewById(R.id.sent)
        delivered=findViewById(R.id.delivered)
        failed=findViewById(R.id.failed)
        total=findViewById(R.id.total)
        loadMessages()
    }


    private fun setSummary(){
        val query="select sum(case(status) when ? then 1 when ? then 1 else 0 end) as waiting" +
                ",sum(case(status) when ? then 1 else 0 end) as sent" +
                ",sum(case(status) when ? then 1 else 0 end) as delivered" +
                ",sum(case(status) when ? then 1 else 0 end)as failed" +
                ",count(id) as total" +
                " from sms where id>?"
        val data=db.fetchSingleMap(query,SMS.NEW,SMS.PENDING,SMS.SENT,SMS.DERIVERED,SMS.FAILED,smsIdStarting)
//        Log.e("data","data $data")
        waiting.text= getString(R.string.waiting,data["waiting"])
        sent.text= getString(R.string.sent,data["sent"])
        delivered.text= getString(R.string.delivered,data["delivered"])
        failed.text= getString(R.string.failed,data["failed"])
        total.text= getString(R.string.total,data["total"])
    }
    private fun loadMessages(){
        var query="select * from sms where id>?  order by id desc limit 1000"
           val sms=db.fetch<SMS>(query,Changer{SMS(it)},smsIdStarting)
        sms.forEach{ adapter.addItem(it.id,it)}
        setSummary()
    }

    private var selectSMS:SMSHolder.MessageView?=null
    private fun onItemSelected(holder:RecyclerView.ViewHolder){
        holder as SMSHolder.MessageView
        setSelectedSMS(holder.sms)
        selectSMS?.unSelect()
        selectSMS=holder
        selectSMS?.select()
    }

    override fun onStart() {
        subscribe(AppEvent.MESSAGE_DELIVERY_PENDING,this::onMessageStatusChanged)
        subscribe(AppEvent.MESSAGE_DELIVERY_FAILED,this::onMessageStatusChanged)
        subscribe(AppEvent.MESSAGE_DELIVERED,this::onMessageStatusChanged)
        subscribe(AppEvent.MESSAGE_SENDING_FAILED, this::onMessageStatusChanged)
        subscribe(AppEvent.MESSAGE_SENT, this::onMessageStatusChanged)
        super.onStart()
    }

    override fun onDestroy() {
        stopService(intent)
        super.onDestroy()
    }

    private fun setSelectedSMS(sms:SMS){
        var text="to:${sms.receiverPhone}\n${sms.text}"
           if(text.length>160){ text=text.substring(0,160)+"..."
               readMore.visibility= View.VISIBLE }
          else{readMore.visibility=View.GONE}
        smsView.text=text
    }

    private fun onMessageStatusChanged(smsId:Long){
        Log.e("informed change","Informed sms status change for sms $smsId")
        val sms=smsManager.fetchSMS(smsId)?:return
        adapter.addItem(sms.id,sms)
        setSelectedSMS(sms)
        setSummary()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode==PERMISSION_ALL
             && grantResults.isNotEmpty()
             &&grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        }
        else{ Toast.makeText(this,"Permissions Denied sending of SMS will be aborted",Toast.LENGTH_SHORT).show()}
}


    private fun hasPermissions( permissions: Array<String>): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }
    companion object{const val PERMISSION_ALL=2
            const val TAG="MainActivity"
    }
}
