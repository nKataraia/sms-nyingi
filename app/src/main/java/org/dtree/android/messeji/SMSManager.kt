package org.dtree.android.messeji

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import tz.co.gluhen.common.DB
import tz.co.gluhen.common.HelperInterfaces.Changer
import tz.co.gluhen.common.event.AppEvent
import tz.co.gluhen.common.event.EventManager

class SMSManager(val db:DB){

    fun fetchSMS(id:Long):SMS?{
        val query="select * from sms where id=?"
        return db.fetchSingle(query, { SMS(it) },id)
    }

    fun saveMessage(jsonSMS:String):Long{
        Log.e("messeji",jsonSMS)
        val gson= Gson()
        val type=object: TypeToken<Map<String, String>>(){}.type
        val sms=SMS(gson.fromJson<Map<String,String>>(jsonSMS,type))
        return saveMessage(sms)
    }
    fun saveMessage(m: SMS):Long{
        val sql="insert into sms(sender,senderPhone,receiverPhone,topic,text,dateSent,status) values(?,?,?,?,?,?,?)"
        return db.insert(sql,m.sender,m.receiverPhone,m.receiverPhone,m.topic,m.text,m.dateSent,m.status)
    }

    fun updateSMSStatus(id:Long,state:String,evnt: AppEvent){
        db.update("update sms set status=? where id=?",state,id)
        EventManager.getInstance().fireEvent(evnt,id)
    }

    fun fetchNextSMSToSend(id:Long):SMS?{
        val query="select * from sms where status=?  and id>=? limit 1 "
        return db.fetchSingle(query, { SMS(it) },SMS.NEW,id)
    }
    fun fetchSMSToSend(batch:Int):List<SMS>{
        val query="select * from sms where status=? or status=? limit ?"
        return db.fetch(query, { SMS(it) },SMS.NEW,SMS.NEW,batch)
    }

}
