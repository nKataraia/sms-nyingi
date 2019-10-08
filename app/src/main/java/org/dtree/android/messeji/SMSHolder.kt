package org.dtree.android.messeji

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import tz.co.gluhen.common.ItemList


class SMSHolder(private val recyclerView: RecyclerView){


    private val  TAG="ItemList"
    lateinit var adapter:ItemList<SMS>

    fun createHeader(){}
  fun createMessageView(message: SMS):RecyclerView.ViewHolder{
      if(message.id==-1){
          val view=LayoutInflater.from(recyclerView.context).inflate(R.layout.message,recyclerView,false)
          return object:RecyclerView.ViewHolder(view){ }
      }
     val view=LayoutInflater.from(recyclerView.context).inflate(R.layout.message_status,recyclerView,false)
     val mh= MessageView(view)
         view.setOnClickListener{adapter.itemClicked(mh)}
      return mh
  }

    fun setData(sn:Int,message:SMS,view:RecyclerView.ViewHolder){
        (view as MessageView).setData(sn,message,adapter)
    }

class MessageView(private val view: View):RecyclerView.ViewHolder(view){
    lateinit var sms:SMS
    private val sn:TextView=view.findViewById(R.id.sn)
    private val phone: TextView =view.findViewById(R.id.phone)
    val status: TextView =view.findViewById(R.id.status)
    private val action:TextView=view.findViewById(R.id.action)

    fun setData(sn:Int,message:SMS,adapter:ItemList<SMS>){
        val c=view.context;
        this.sms=message
        this.sn.text=(sn+1).toString()
        this.phone.text=message.receiverPhone
        this.status.text=message.status
        if(message.status==SMS.FAILED){
            this.action.setText(c.getString(R.string.resend))
            this.action.visibility=View.VISIBLE
        }
        else{this.action.visibility=View.GONE;}
        view.setOnClickListener{adapter.itemClicked(this)}
        view.setOnLongClickListener{adapter.itemLongPressed(this);true}
        //view.setOnTouchListener{v,m->adapter.itemLongPressed(this);true}
    }
    fun select(){
        val v=view as CardView
            v.setCardBackgroundColor(0xffffeedd.toInt())
    }
    fun unSelect(){
        val v=view as CardView
        v.setCardBackgroundColor(0xffffffff.toInt())
    }
}
}

