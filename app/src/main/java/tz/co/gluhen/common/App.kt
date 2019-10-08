package tz.co.gluhen.common

import androidx.appcompat.app.AppCompatActivity
import tz.co.gluhen.common.event.EventManager
import tz.co.gluhen.common.event.AppEvent


abstract  class App : AppCompatActivity() {
    private val eventManager:EventManager= EventManager.getInstance()

    private val events= mutableListOf<Int>()
   fun <T> subscribe( app: AppEvent, eventConsumer:(T)->Unit){
     events.add(eventManager.subscribe(app,eventConsumer,this))
    }
    fun <T> fireEvent(event:AppEvent,data:T){ eventManager.fireEvent<T>(event,data) }

    override fun onStop() {
        events.forEach{eventManager.unsubscribe(it)}
        events.clear()
        super.onStop()
    }

    override fun onPause() {
        events.forEach{eventManager.unsubscribe(it)}
        events.clear()
        super.onPause()
    }

    override fun onDestroy() {
        events.forEach{eventManager.unsubscribe(it)}
        events.clear()
        super.onDestroy()
    }
}
