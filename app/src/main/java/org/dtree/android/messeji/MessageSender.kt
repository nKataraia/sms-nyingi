package org.dtree.android.messeji

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import tz.co.gluhen.common.AppService
import tz.co.gluhen.common.DB
import tz.co.gluhen.common.event.AppEvent
import tz.co.gluhen.common.io.FakeServer


class MessageSender : AppService(){
    val tag="MessageSender"
    override fun onBind(p0: Intent?): IBinder?{return null}

    private val fakeServer=FakeServer()
    private lateinit var smsManager:SMSManager
    private var requestCode=2348

    override fun onCreate() {
        super.onCreate()
        makeItForeground()

        subscribe(AppEvent.MESSAGE_FROM_BROWSER,this::onMessageFromBrowser)
        subscribe(AppEvent.MESSAGE_SENT,this::onMessageSentOrFailed)

        Log.e(tag,"starting service ..... on port 8090")
        fakeServer.start(8090,this)
        val receiver=SMSReceiver()
        registerReceiver(receiver, IntentFilter(SMS_SENT_ACTION))
        registerReceiver(receiver, IntentFilter(SMS_DELIVERED_ACTION))
    }

    private fun makeItForeground(){
        val ongoingNotificationId=2

        val channelName="Meseji Nyingi"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val mChannel = NotificationChannel(channelName, channelName, importance)
            mChannel.description = "For bulk sms by Message Nyingi App"
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(mChannel)

        val pendingIntent: PendingIntent =
            Intent(this, ActivityShowProgress::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }
        val notification: Notification = Notification.Builder(this,channelName)
            .setContentTitle(getText(R.string.message))
            .setContentText(getText(R.string.notificationDescription))
            .setSmallIcon(R.drawable.messeji_icon)
            .setContentIntent(pendingIntent)
            .setTicker(getText(R.string.message))
            .setAutoCancel(true)
            .build()
        startForeground(ongoingNotificationId, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
         smsManager= SMSManager(DB.getInstance(this.application))
        return START_STICKY
    }

    override fun onDestroy() {
        val intent=Intent(this,SMSReceiver::class.java)
        sendBroadcast(intent)
        fakeServer.stop()
        Log.e("MessageSender","Service Got destroyed");
        super.onDestroy()
    }

   private var toSend=0
   var sessionId:Long=Long.MAX_VALUE
    @Synchronized  fun onMessageFromBrowser(message:String){
        val id=smsManager.saveMessage(message)
        toSend++
        Log.e("MessageSender","To send $toSend");
        if(toSend==1){
            sessionId=id
            val sms=smsManager.fetchNextSMSToSend(id)?:return
            sendSMS(sms)
        }
    }
    @Synchronized fun onMessageSentOrFailed(message:Any){
        toSend--
        Log.e("sms-status", "$message  hapa To Send $toSend")
        sendSMS(smsManager.fetchNextSMSToSend(sessionId)?:return)
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

    private fun sendSMS(sms:SMS):String {
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
        }
        return SMS.FAILED
    }

     private fun getSendingIntent(sms:SMS):PendingIntent{
         Log.e("message inayotumwa","message inayotumwa ni ${sms.id}")
         val sentIntent = Intent(SMS_SENT_ACTION)
         sentIntent.putExtra(RECEIVER_NUMBER, sms.receiverPhone)
         sentIntent.putExtra(SENT_MESSAGE, sms.id.toString())
         return PendingIntent.getBroadcast(this, requestCode, sentIntent,PendingIntent.FLAG_ONE_SHOT)
     }

     private fun getDeliveryIntent(sms:SMS):PendingIntent{
         val deliveredIntent =Intent(SMS_DELIVERED_ACTION)
         deliveredIntent.putExtra(RECEIVER_NUMBER, sms.receiverPhone)
         deliveredIntent.putExtra(SENT_MESSAGE, sms.id.toString())
         return PendingIntent.getBroadcast(this, requestCode, deliveredIntent, PendingIntent.FLAG_ONE_SHOT)
     }

    companion object{
        const val  SMS_SENT_ACTION = "org.dtree.android.messeji.SMS_SENT"
        const val SMS_DELIVERED_ACTION = "org.dtree.android.messeji.SMS_DELIVERED"
        const val RECEIVER_NUMBER = "receiverNumber"
        const val SENT_MESSAGE = "sentMessage"
    }
}

