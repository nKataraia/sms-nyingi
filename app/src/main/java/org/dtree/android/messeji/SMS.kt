package org.dtree.android.messeji

import android.database.Cursor
import tz.co.gluhen.common.DB
import tz.co.gluhen.common.Utils
import java.util.*


class SMS(val sender:String, val receiverPhone:String
    ,val topic:String, val text:String, val dateSent:Date,val id:Int=0,var status:String=NEW){

    constructor(map:Map<String,String>):this(map["sender"]?:""
        ,Utils.parsePhoneNumber(map["phone"]),map["topic"]?:""
        ,map["message.xml"]?:map["sms"]?:map["text"]?:"",Date(),map["id"]?.toInt()?:0)

    constructor(cs:Cursor):this(
        cs.getString(cs.getColumnIndex("sender"))?:""
        ,Utils.parsePhoneNumber(cs.getString(cs.getColumnIndex("receiverPhone")))
        ,cs.getString(cs.getColumnIndex("topic"))?:""
        ,cs.getString(cs.getColumnIndex("text"))
        ,DB.stringToDate(cs.getString(cs.getColumnIndex("dateSent")))
        ,cs.getInt(cs.getColumnIndex("id"))
        ,cs.getString(cs.getColumnIndex("status")))

    companion object{
        const val NEW="New"
        const val FAILED="Failed"
        const val PENDING="Pending"
        const val SENT="Sent"
        const val DERIVERED="Delivered"
    }
}
