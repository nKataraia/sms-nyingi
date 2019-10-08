package tz.co.gluhen.common.event;


import android.database.Cursor;
import android.util.Log;
import com.google.gson.Gson;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import tz.co.gluhen.common.DB;




public class EventScheduler{
private static EventScheduler instance;

private static final String TAG="EventScheduler";
synchronized static EventScheduler getInstance(EventManager eventManager, DB db){
    if(instance==null){instance=new EventScheduler(eventManager,db);}
    return instance;
}

private EventManager eventManager;
private DB db;
private EventScheduler(EventManager eventManager,DB db){
    this.eventManager=eventManager;
    this.db=db;
   fireEventPassed();
   scheduleFutureEventFromDB();
}

private boolean isFiringPastEvents=false;
private synchronized void fireEventPassed(){
    if(isFiringPastEvents){return;}
   new Thread(()->{
       isFiringPastEvents =true;
     while(isFiringPastEvents){
        try{
            String sql="select * from FUTURE_EVENT where STATE=? and EVENT_DATE<now() order by EVENT_DATE asc limit 100";
           List<FutureEvent> futureEventList=db.fetch(sql,FutureEvent::new,EVENT_WAITING);
                   for(FutureEvent f:futureEventList){
                       eventManager.fireEvent(f.eventType,f.eventData);
                       setTaskDone(f);
                   }
           isFiringPastEvents=!futureEventList.isEmpty();
           Thread.sleep(5000);
        }
        catch (Exception e){Log.e(TAG,e.toString(),e); isFiringPastEvents=false;}
     }
   }).start();
}

private TreeMap<Long,FutureEvent> eventList=new TreeMap<>();
private void scheduleFutureEventFromDB(){
    String sql="select * from FUTURE_EVENT where STATE=? and EVENT_DATE>now() order by EVENT_DATE asc limit 100";
    List<FutureEvent> futureEvents=db.fetch(sql,FutureEvent::new,EVENT_WAITING);
    for(FutureEvent fe:futureEvents){ eventList.put(fe.eventDate.getTime(),fe);}
    synchronized (lock){lock.notify();}
}
private void setTaskDone(FutureEvent event){
   Log.e(TAG,String.format("update FUTURE_EVENT set STATE=%s where ID=%s\n",EVENT_DONE,event.id));
    String sql="update FUTURE_EVENT set STATE=? where ID=?";
    db.update(sql, EVENT_DONE, event.id);
}
private int eventSubscription;
private Thread thread;
synchronized void start(){
 if(keepRunning){return;}
 keepRunning=true;
   thread=new Thread(this::runner);
   thread.start();
   eventSubscription=eventManager.subscribe(AppEvent.FUTURE_EVENT_ADDED,this::schedule);
}

public synchronized void stop(){
   keepRunning=false;
   thread.interrupt();
   eventManager.unsubscribe(eventSubscription);
}

private volatile boolean keepRunning=false;
private static final Object lock=new Object();
private  void runner(){
    while(keepRunning){
        try{
            synchronized (lock){
                Log.i(TAG,"waiting begin for"+getWaitTime());
                try { while(getWaitTime()>0){ lock.wait(getWaitTime());
                        Log.i(TAG,"waiting for"+getWaitTime());}
                }
                catch (InterruptedException e) { Log.e(TAG,e.toString(),e);continue;}

                Map.Entry<Long,FutureEvent> entry=eventList.pollFirstEntry();
                if(entry==null){continue;}

                FutureEvent event=entry.getValue();
                EventManager.getInstance().fireEvent(event.eventType,event.eventData);
                setTaskDone(event);
            }
        }catch (Exception e){Log.e(TAG,"error {}",e);}
    }
}
private long getWaitTime(){
   if(eventList.isEmpty()){
      Log.i(TAG,"eventlist is empty waiting indefinitely");
       return Integer.MAX_VALUE;}
   Map.Entry<Long,FutureEvent> e=eventList.firstEntry();
    return e!=null?e.getValue().eventDate.getTime()-System.currentTimeMillis():Integer.MAX_VALUE;
}

private void schedule(long id){
   FutureEvent event=db.fetchSingle("select * from FUTURE_EVENT where ID=?",FutureEvent::new,id);
   if(event==null){
       Log.i(TAG,"cannot schedule anything event is null"+id);
       return;
   }
    Log.i(TAG,String.format("scheduling for event %s,\n at  %s\n",new Gson().toJson(event),event.eventDate));
   eventList.put(event.id,event);
   synchronized (lock){lock.notify();}
}

private static class FutureEvent{
    final long id;
    final Date eventDate,date;
    final String eventData;
    final AppEvent eventType;
    FutureEvent(Cursor cs){
        id=cs.getLong(cs.getColumnIndex("ID"));
        eventData=cs.getString(cs.getColumnIndex("DATA"));
        eventDate= DB.stringToDate(cs.getString(cs.getColumnIndex("EVENT_DATE")));
        date= DB.stringToDate(cs.getString(cs.getColumnIndex("CREATED_DATE")));
        eventType= AppEvent.valueOf(cs.getString(cs.getColumnIndex("EVENT")));
    }
}
private static final int EVENT_WAITING=1,EVENT_DONE=3;
}
