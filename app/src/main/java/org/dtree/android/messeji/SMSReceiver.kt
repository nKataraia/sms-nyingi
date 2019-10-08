package org.dtree.android.messeji

import android.annotation.TargetApi
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import tz.co.gluhen.common.DB
import tz.co.gluhen.common.event.AppEvent
import tz.co.gluhen.common.event.EventManager

class SMSReceiver: BroadcastReceiver(){

    lateinit var smsManager: SMSManager
       override fun onReceive(context:Context, intent:Intent) {
           smsManager= SMSManager(DB.getInstance(context))
           val action = intent.action
           val number = intent.getStringExtra(MessageSender.RECEIVER_NUMBER)?:"0"
           val messageID = intent.getStringExtra(MessageSender.SENT_MESSAGE)?.toLong()?:1L

           Log.e("sending","received imefika aisee and hapaaaaaa...$number and $messageID" )
           // This is the result for a send.
           if (MessageSender.SMS_SENT_ACTION == action) {
               //sendNextMessage()
               fireSendingStatusChange(resultCode,messageID)
           }
           else if (MessageSender.SMS_DELIVERED_ACTION == action) {
               val sms: SmsMessage
               val pdu = intent.getByteArrayExtra("pdu")
               val format = intent.getStringExtra("format")
               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && format != null) {
                   @TargetApi(Build.VERSION_CODES.KITKAT)
                   sms = SmsMessage.createFromPdu(pdu, format)}
               else { sms = SmsMessage.createFromPdu(pdu)}

               // getResultCode() is not reliable for delivery results.
               // We need to get the status from the SmsMessage.
               fireDeliveryStatusChange(sms.status,messageID)
           }
       }

        private fun fireSendingStatusChange(resultCode:Int, smsID:Long){
            return when(resultCode) {
                Activity.RESULT_OK->smsManager.updateSMSStatus(smsID,SMS.SENT,AppEvent.MESSAGE_SENT)
                SmsManager.RESULT_ERROR_GENERIC_FAILURE->smsManager.updateSMSStatus(smsID,SMS.FAILED,AppEvent.MESSAGE_SENDING_FAILED)
                SmsManager.RESULT_ERROR_RADIO_OFF->smsManager.updateSMSStatus(smsID,SMS.FAILED,AppEvent.MESSAGE_SENDING_FAILED)
                SmsManager.RESULT_ERROR_NULL_PDU->smsManager.updateSMSStatus(smsID,SMS.FAILED,AppEvent.MESSAGE_SENDING_FAILED)
                SmsManager.RESULT_ERROR_NO_SERVICE->smsManager.updateSMSStatus(smsID,SMS.FAILED,AppEvent.MESSAGE_SENDING_FAILED)
                else ->smsManager.updateSMSStatus(smsID,SMS.FAILED,AppEvent.MESSAGE_SENDING_FAILED)
            }
        }

        private fun fireDeliveryStatusChange(status:Int, smsID:Long){
             when (status) {
                Telephony.Sms.STATUS_COMPLETE->smsManager.updateSMSStatus(smsID,SMS.DERIVERED,AppEvent.MESSAGE_DELIVERED)
                Telephony.Sms.STATUS_FAILED->smsManager.updateSMSStatus(smsID,SMS.FAILED,AppEvent.MESSAGE_DELIVERY_FAILED)
                Telephony.Sms.STATUS_PENDING->smsManager.updateSMSStatus(smsID,SMS.PENDING,AppEvent.MESSAGE_DELIVERY_PENDING)
                Telephony.Sms.STATUS_NONE->smsManager.updateSMSStatus(smsID,SMS.FAILED,AppEvent.MESSAGE_DELIVERY_FAILED)
                else ->smsManager.updateSMSStatus(smsID,SMS.FAILED,AppEvent.MESSAGE_DELIVERY_FAILED)
    }}
}
