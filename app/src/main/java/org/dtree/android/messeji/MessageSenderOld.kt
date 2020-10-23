package org.dtree.android.messeji

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tz.co.gluhen.common.AppService
import tz.co.gluhen.common.DB
import tz.co.gluhen.common.event.AppEvent
import tz.co.gluhen.common.io.FakeServer
import kotlin.time.Duration


class MessageSenderOld : AppService(){
    val tag="MessageSender"
    override fun onBind(p0: Intent?): IBinder?{return null}

    private val fakeServer=FakeServer()
    private lateinit var smsManager:SMSManager
    private var anza=0
    private var requestCode=2348

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {makeItForeground()}
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun makeItForeground(){
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
            .build()
        startForeground(ongoingNotificationId, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(++anza>1){return START_STICKY;}
           smsManager= SMSManager(DB.getInstance(this.application))
        val port=intent?.getIntExtra("port",8090)?:8090
        Log.e(tag,"starting service ....  $anza. on port $port")
        subscribe(AppEvent.MESSAGE_FROM_BROWSER,this::onMessageFromBrowser)
        subscribe(AppEvent.MESSAGE_SENT,this::onMessageSentOrFailed)
        subscribe(AppEvent.MESSAGE_SENT,this::onMessageSentOrFailed)
        fakeServer.start(port,this)
        val receiver=SMSReceiver()
        registerReceiver(receiver, IntentFilter(SMS_SENT_ACTION))
        registerReceiver(receiver, IntentFilter(SMS_DELIVERED_ACTION))
        return START_STICKY

    }

    override fun onDestroy() {
        val intent=Intent(this,SMSReceiver::class.java)
        sendBroadcast(intent)
        fakeServer.stop()
        super.onDestroy()
    }

    @Synchronized  fun onMessageFromBrowser(message:String){
        val id=smsManager.saveMessage(message)
        toSend++
        sendingLoop()
    }
    @Synchronized fun onMessageSentOrFailed(message:Any){
        Log.e("ujumbesubiriwa", "$message  hapa")
        toSend--
        batch--
    }

    private var toSend=0
    private var batch=0

    @Synchronized  private fun sendingLoop(){
        if(batch>0){return}
        GlobalScope.launch{
            while(toSend>0){
                val smsList=smsManager.fetchSMSToSend(4)
                batch=smsList.size
                for(sms in smsList){sendSMS(sms)}
                delay(60000)
            }
        }
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
         Log.e("message inayotumwa","message inayotumwa ni ${sms.id}")
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

