package org.dtree.android.messeji

import tz.co.gluhen.common.DB
import tz.co.gluhen.common.HelperInterfaces.Changer
import tz.co.gluhen.common.event.AppEvent
import tz.co.gluhen.common.event.EventManager

class SMSManager(val db:DB){

    fun fetchSMS(id:Long):SMS?{
        val query="select * from sms where id=?"
        return db.fetchSingle(query, Changer { SMS(it) },id)
    }

    fun saveMessage(m: SMS):Long{
        val sql="insert into sms(sender,senderPhone,receiverPhone,topic,text,dateSent,status) values(?,?,?,?,?,?,?)"
        return db.insert(sql,m.sender,m.receiverPhone,m.receiverPhone,m.topic,m.text,m.dateSent,m.status)
    }

    fun updateSMSStatus(id:Long,state:String,evnt: AppEvent){
        db.update("update sms set status=? where id=?",state,id)
        EventManager.getInstance().fireEvent(evnt,id)
    }

}
