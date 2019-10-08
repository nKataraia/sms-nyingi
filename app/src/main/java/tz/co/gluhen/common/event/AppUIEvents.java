package tz.co.gluhen.common.event;


import android.util.Log;
import android.util.SparseArray;

import java.util.HashMap;
import java.util.Map;

import tz.co.gluhen.common.HelperInterfaces.Consumer;

@SuppressWarnings("WeakerAccess")
public class AppUIEvents {
    public String TAG="EventManager";
    private static AppUIEvents instance;
    public synchronized static AppUIEvents getInstance(){
        if(instance==null){instance=new AppUIEvents();}
        return instance;
    }

   private AppUIEvents(){ }
   private int listenerId=0;
   private SparseArray<EventHolder> eventListeners =new SparseArray<>();
   private Map<AppEvent, Integer> listenersCount=new HashMap<>();


   public synchronized <T> int subscribe(AppEvent eventType, Consumer<T> eventListener){
       eventListeners.put(++listenerId, new EventHolder<>(listenerId, eventType, eventListener));
            Integer s=listenersCount.get(eventType);
                    s=s==null?0:s;
             listenersCount.put(eventType,++s);
      return listenerId;
   }
   public synchronized void unsubscribe(int eventListenerId){
       EventHolder e= eventListeners.get(eventListenerId);
                      eventListeners.remove(eventListenerId);
       if(e!=null){Integer s=listenersCount.get(e.eventType);s=s==null?0:s; s--;
           if(s<=0){listenersCount.remove(e.eventType);}
           else{listenersCount.put(e.eventType,s);}
       }
   }

    public synchronized <T> void fireEvent(AppEvent eventType, T t){
       if(listenersCount.get(eventType)==null){
          System.err.println("not implemented listener for"+eventType.name());
         return;}
       for(int i = 0, l = eventListeners.size(); i<l; i++){
          EventHolder eh= eventListeners.valueAt(i);
           if(eh.eventType!=eventType){continue;}
           Consumer el=eh.getListener();
                try{el.take(t);}
                catch (Exception e){Log.e(TAG,e.toString(),e);}
        }
    }

  private static class EventHolder<T>{
       int id;
       AppEvent eventType;
       Consumer<T> listener;
    EventHolder(int id, AppEvent eventType, Consumer<T> eventListener) {
          this.id=id;
          this.eventType=eventType;
          this.listener=eventListener;
      }
    Consumer<T> getListener(){return listener;}
  }
}
