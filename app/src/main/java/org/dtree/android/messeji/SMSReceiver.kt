package org.dtree.android.messeji

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import tz.co.gluhen.common.DB
import tz.co.gluhen.common.event.AppEvent

class SMSReceiver: BroadcastReceiver(){

    private lateinit var smsManager: SMSManager
       override fun onReceive(context:Context, intent:Intent) {
           smsManager= SMSManager(DB.getInstance(context))
           val action = intent.action
           val number = intent.getStringExtra(MessageSender.RECEIVER_NUMBER)?:"0"
           val messageID = intent.getStringExtra(MessageSender.SENT_MESSAGE)?.toLong()?:1L

           Log.e("sending","received imefika aisee and hapaaaaaa...$number and $messageID" )
           // This is the result for a send.
           when (action) {
               MessageSender.SMS_SENT_ACTION ->fireSendingStatusChange(resultCode,messageID)
               MessageSender.SMS_DELIVERED_ACTION -> {
                   val sms: SmsMessage
                   val pdu = intent.getByteArrayExtra("pdu")
                   val format = intent.getStringExtra("format")
                   sms = format?.let{ SmsMessage.createFromPdu(pdu,it) }?:return
                   // getResultCode() is not reliable for delivery results.
                   // We need to get the status from the SmsMessage.
                   fireDeliveryStatusChange(sms.status,messageID)
               }
               else -> Log.e("weka kuwa","nyingie kabisa $action,${MessageSender.SMS_SENT_ACTION} $number and $messageID for $action")
           }
       }

        private fun fireSendingStatusChange(resultCode:Int, smsID:Long){
            val isSent=resultCode==Activity.RESULT_OK
            Log.e("weka kuwa","imesend status change is $smsID and for $resultCode na pia je imetuma=${resultCode==Activity.RESULT_OK}")
            return if(isSent)smsManager.updateSMSStatus(smsID,SMS.SENT,AppEvent.MESSAGE_SENT)
                else smsManager.updateSMSStatus(smsID,SMS.FAILED,AppEvent.MESSAGE_SENDING_FAILED)
        }

        private fun fireDeliveryStatusChange(status:Int, smsID:Long){
            Log.e("weka kuwa","nyingie kabisa  $smsID and for $status")
             when (status) {
                Telephony.Sms.STATUS_COMPLETE->smsManager.updateSMSStatus(smsID,SMS.DERIVERED,AppEvent.MESSAGE_DELIVERED)
                Telephony.Sms.STATUS_FAILED->smsManager.updateSMSStatus(smsID,SMS.FAILED,AppEvent.MESSAGE_DELIVERY_FAILED)
//                Telephony.Sms.STATUS_PENDING->smsManager.updateSMSStatus(smsID,SMS.PENDING,AppEvent.MESSAGE_DELIVERY_PENDING)
                Telephony.Sms.STATUS_NONE->smsManager.updateSMSStatus(smsID,SMS.FAILED,AppEvent.MESSAGE_DELIVERY_FAILED)
                else ->smsManager.updateSMSStatus(smsID,SMS.FAILED,AppEvent.MESSAGE_DELIVERY_FAILED)
    }}
}
