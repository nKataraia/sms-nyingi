package tz.co.gluhen.common

import android.app.Service
import androidx.appcompat.app.AppCompatActivity
import tz.co.gluhen.common.event.EventManager
import tz.co.gluhen.common.HelperInterfaces.Consumer
import tz.co.gluhen.common.event.AppEvent


abstract  class AppService:Service(){
    private val eventManager:EventManager= EventManager.getInstance()

    private val events= mutableListOf<Int>()
    fun <T> subscribe( app: AppEvent, eventConsumer:(T)->Unit){
        events.add(eventManager.subscribe(app,eventConsumer,this))
    }
    fun <T> fireEvent(event:AppEvent,data:T){ eventManager.fireEvent<T>(event,data) }
    override fun onDestroy() {
        events.forEach{eventManager.unsubscribe(it)}
        events.clear()
        super.onDestroy()
    }


}
