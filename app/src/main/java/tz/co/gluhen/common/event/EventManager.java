package tz.co.gluhen.common.event;


import android.app.Activity;
import android.util.Log;
import android.util.SparseArray;

import androidx.lifecycle.LifecycleObserver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import tz.co.gluhen.common.HelperInterfaces.*;

@SuppressWarnings("WeakerAccess")
public class EventManager {
    public String TAG="EventManager";
    private static EventManager instance;
    public synchronized static EventManager getInstance(){
        if(instance==null){instance=new EventManager();}
        return instance;
    }

   private EventManager(){ }
   private int listenerId=0;
   private SparseArray<EventHolder> eventListeners =new SparseArray<>();
   private Map<AppEvent, Integer> listenersCount=new HashMap<>();


   public synchronized <T> int subscribe(AppEvent eventType, Consumer<T> eventListener,Object runner){
       eventListeners.put(++listenerId, new EventHolder<>(listenerId, eventType, eventListener,runner));
       Integer s=listenersCount.get(eventType);
       s=s==null?0:s;
       listenersCount.put(eventType,++s);
       return listenerId;
   }
   public synchronized <T> int subscribe(AppEvent eventType, Consumer<T> eventListener){
       eventListeners.put(++listenerId, new EventHolder<>(listenerId, eventType, eventListener,null));
            Integer s=listenersCount.get(eventType);
                    s=s==null?0:s;
             listenersCount.put(eventType,++s);
       return listenerId;
   }
   public synchronized void unsubscribe(int eventListenerId){
       EventHolder e= eventListeners.get(eventListenerId);
                      eventListeners.remove(eventListenerId);
                      Log.e("removed","removed is ...."+e.eventType.name());

       Integer s = listenersCount.get(e.eventType);s=s==null?0:s;
       s--;
       if(s<=0){ listenersCount.remove(e.eventType); }
       else{listenersCount.put(e.eventType,s);}

   }

    public synchronized <T> void fireEvent(AppEvent eventType, T t){
       if(listenersCount.get(eventType)==null){
          System.err.println("not implemented listener for"+eventType.name());
         return;}
       for(int i = 0, l = eventListeners.size(); i<l; i++){
          EventHolder eh= eventListeners.valueAt(i);
           if(eh.eventType!=eventType){continue;}
          Runnable r=()->{
                   Consumer el=eh.getListener();
                   try{el.take(t); }
                   catch (Exception e){Log.e(TAG,e.toString(),e);}
               };
           if(eh.runner instanceof Activity){ ((Activity)eh.runner).runOnUiThread(r);}
           else if(eh.runner instanceof ExecutorService){((ExecutorService)eh.runner).submit(r);}
           else {r.run();}
        }
    }

  private static class EventHolder<T>{
       int id;
       AppEvent eventType;
       Consumer<T> listener;
       Object runner;
    EventHolder(int id, AppEvent eventType, Consumer<T> eventListener,Object runner) {
          this.id=id;
          this.eventType=eventType;
          this.listener=eventListener;
          this.runner=runner;
      }
    Consumer<T> getListener(){return listener;}
  }
}
