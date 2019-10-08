package org.dtree.android.messeji

import android.app.PendingIntent
import android.content.Intent
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import tz.co.gluhen.common.AppService
import tz.co.gluhen.common.DB
import tz.co.gluhen.common.HelperInterfaces.*
import tz.co.gluhen.common.event.AppEvent
import tz.co.gluhen.common.io.FakeServer


class MessageSender : AppService(){
    val TAG="MessageSender"
    override fun onBind(p0: Intent?): IBinder?{return null}

    private val fakeServer=FakeServer()
    lateinit var smsManager:SMSManager;
    private var anza=0
    private var requestCode=0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(++anza>1){return START_STICKY;}
           smsManager= SMSManager(DB.getInstance(this.application))
        val port=intent?.getIntExtra("port",8090)?:8090
        Log.e(TAG,"starting service .... $anza")
        subscribe(AppEvent.MESSAGE_FROM_BROWSER,this::sendSMSFromBrowser)
        fakeServer.start(port,this)
        return START_STICKY
    }

    private fun sendSMSFromBrowser(jsonSMS:String) {
        Log.e("messeji",jsonSMS)
        val gson=Gson()
            val type=object:TypeToken<Map<String,String>>(){}.type
            val sms=SMS(gson.fromJson<Map<String,String>>(jsonSMS,type))
            val id=smsManager.saveMessage(sms)
                   sendSMS(id)
    }
    override fun onDestroy() {
        val intent=Intent(this,SMSReceiver::class.java)
        sendBroadcast(intent)
        fakeServer.stop()
        super.onDestroy()
    }

    private fun onMessageReceived(jsonSMS:String) {
        val type=object: TypeToken<Map<String, String>>(){}.type
        val sms=SMS(Gson().fromJson<Map<String,String>>(jsonSMS,type))

    }

    private fun sendSingle(sms:SMS){
        val smsManager = SmsManager.getDefault()
        val sendIntent=getSendingIntent(sms)
        val deliveryIntent=getDeliveryIntent(sms)
        smsManager.sendTextMessage(sms.receiverPhone, null, sms.text, sendIntent, deliveryIntent)
    }
    private fun sendMultipart(sms:SMS){
        val smsManager = SmsManager.getDefault()
        val parts = smsManager.divideMessage(sms.text)
        val sendingIntents= arrayListOf<PendingIntent>()
        sendingIntents.addAll(parts.map{getSendingIntent(sms)})

        val deliveryIntents= arrayListOf<PendingIntent>()
        deliveryIntents.addAll(parts.map{getDeliveryIntent(sms)})
        smsManager.sendMultipartTextMessage( sms.receiverPhone,null, parts, sendingIntents, deliveryIntents)
    }

    private fun sendSMS(smsId:Long):String {
        val sms=smsManager.fetchSMS(smsId)?:return SMS.FAILED
        Log.e("sending","sending ${sms.text}")
        try {if(requestCode>2000000){requestCode=0;}
                requestCode++
            if(sms.text.length>160) {sendMultipart(sms)}
            else{sendSingle(sms) }
            smsManager.updateSMSStatus(sms.id.toLong(),SMS.PENDING,AppEvent.MESSAGE_DELIVERY_PENDING)
            return SMS.PENDING
        } catch (ex: Exception) {
            Log.e("sending","error while sending ${sms.text}",ex)
            super.fireEvent(AppEvent.MESSAGE_SENDING_FAILED,sms)
            Toast.makeText(applicationContext, ex.message.toString(),
                Toast.LENGTH_LONG).show()
            ex.printStackTrace()
        }
        return SMS.FAILED
    }

     private fun getSendingIntent(sms:SMS):PendingIntent{
         val sentIntent = Intent(SMS_SENT_ACTION)
         sentIntent.putExtra(RECEIVER_NUMBER, sms.receiverPhone)
         sentIntent.putExtra(SENT_MESSAGE, sms.id)
         return PendingIntent.getBroadcast(this, requestCode, sentIntent,PendingIntent.FLAG_ONE_SHOT)
     }

     private fun getDeliveryIntent(sms:SMS):PendingIntent{
         val deliveredIntent =Intent(SMS_DELIVERED_ACTION)
         deliveredIntent.putExtra(RECEIVER_NUMBER, sms.receiverPhone)
         deliveredIntent.putExtra(SENT_MESSAGE, sms.id)
         return PendingIntent.getBroadcast(this, requestCode, deliveredIntent, PendingIntent.FLAG_ONE_SHOT)
     }

    companion object{
        const val  SMS_SENT_ACTION = "org.dtree.android.messeji.SMS_SENT"
        const val SMS_DELIVERED_ACTION = "org.dtree.android.messeji.SMS_DELIVERED"
        const val RECEIVER_NUMBER = "receiverNumber"
        const val SENT_MESSAGE = "sentMessage"
    }
}

