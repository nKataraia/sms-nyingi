package tz.co.gluhen.common.io;


import java.util.HashMap;
import java.util.Map;

import tz.co.gluhen.common.HelperInterfaces.BiConsumer;

@SuppressWarnings({"unused", "WeakerAccess", "UnusedReturnValue"})
public class ProgressObserver{

    private int observerId=0;
    private boolean isCancelled=false;
    private int bytesRead=0;
    //private SparseArray<BiConsumer<Integer,Integer>> progressObservers=new SparseArray<>();
    private Map<Integer,BiConsumer<Integer,Integer>> progressObservers=new HashMap<>();
    public void cancel(){isCancelled=true;}
    private void onProgress(int bytesCount){
            for(BiConsumer<Integer,Integer> c:progressObservers.values()){
                 c.take(bytesCount,bytesToFetch);
            }
//        for(int i=0,l=progressObservers.size();i<l;i++){
//            progressObservers.valueAt(i).take(bytesCount,bytesToFetch);
//        }
    }
    private int updateIfReached=1024,chunk=0;
    public synchronized void reset(){
        isCancelled=false;
        bytesRead=0; chunk=0;
    }

     synchronized boolean update(){return update(1);}
     synchronized boolean update(int readLength){
        if(isCancelled||progressObservers.isEmpty()){return !isCancelled;}
         bytesRead+=readLength;
         chunk+=readLength;
        if(chunk>updateIfReached){ onProgress(bytesRead);chunk=0;}
        return !isCancelled;
    }
    public int addProgressObserver(BiConsumer<Integer,Integer> observer){
        progressObservers.put(++observerId,observer);
        return observerId;
    }
    public void removeProgressObserver(int observerId){progressObservers.remove(observerId);}
    int bytesToFetch=0;
    public void setBytesToFetchLength(int bytesToFetch){
        int MB=1048576,GB=1073741824,KB=1024;
        this.bytesToFetch=bytesToFetch;
        if(bytesToFetch>GB){updateIfReached=MB;}
        else if(bytesToFetch>MB){updateIfReached=KB;}
        else if(bytesToFetch>KB){updateIfReached=128;}
    }
    public void done(){ onProgress(-1); System.out.println("donee");}
}
