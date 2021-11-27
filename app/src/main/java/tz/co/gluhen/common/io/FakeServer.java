package tz.co.gluhen.common.io;

import android.content.Context;
import android.util.Log;
import org.dtree.android.messeji.R;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import tz.co.gluhen.common.Utils;
import tz.co.gluhen.common.event.AppEvent;
import tz.co.gluhen.common.event.EventManager;

public class FakeServer {

  private static final String TAG="FakeServer";
 private boolean keepRunning;
  private final ExecutorService  ex= Executors.newCachedThreadPool();
   private ServerSocket sc;
   private Context context;
   private final ProgressObserver observer=new ProgressObserver();
    public synchronized void start(int port, Context c){
        this.context=c;
     ex.submit(()->{
         if(keepRunning){return;}
         keepRunning=true;
         while(keepRunning) {
             try {
                 Thread.sleep(2000);
                 System.out.println("running fake server....");
                 sc = new ServerSocket(port);
                 while (keepRunning) {
                     Socket s = sc.accept();
                     ex.submit(()->processMessage(s));
                 }
             } catch (Exception e) { Log.e(TAG, TAG, e); }
         }
     });
 }
  private final EventManager eventManager=EventManager.getInstance();

 private void processMessage(Socket socket){
     try(BufferedInputStream bin=new BufferedInputStream(socket.getInputStream());
         BufferedOutputStream out=new BufferedOutputStream(socket.getOutputStream())){
          IO io=new IO(observer);
           String head= io.readRequestHeaders(bin).toLowerCase();
           if(head.contains("post ")){handlePost(io,socket,head,bin);}
           else {handleGet(io,socket,head);}

         out.flush();
      }
      catch (Exception e){Log.e(TAG,TAG,e);}
 }

    private void handlePost(IO io,Socket socket,String head,BufferedInputStream bin){
        int len=Integer.parseInt(Utils.between(head,"content-length:","\n").trim());

        String rq=new String(io.readPostRequestBody(bin,len)).replaceAll("&#10;","\n");
        eventManager.fireEvent(AppEvent.MESSAGE_FROM_BROWSER,rq);

        try(BufferedOutputStream out=new BufferedOutputStream(socket.getOutputStream())){
            String mime="text/json";
            byte[] body;body = "{\"uploaded\":200}".getBytes();
        Date date=new Date();
        String resp=String.format(response,date,date,body.length,mime);
        out.write(resp.getBytes());
        out.flush();
        out.write(body);
        out.flush();
        }catch (Exception e){Log.e(TAG,TAG,e);}
    }

    private void handleGet(IO io,Socket socket,String head){
         String url=Utils.between(head,"get "," http").toLowerCase().replaceAll("[^\\w.-]+","");
        try(BufferedOutputStream out=new BufferedOutputStream(socket.getOutputStream())){
          String mime="text/javascript";
            byte[] body;
            switch (url){
             case "":
             case "upload.html": body=io.text(context.getResources().openRawResource(R.raw.upload)).getBytes();mime="text/html";break;
             case "angular.js": body=io.text(context.getResources().openRawResource(R.raw.angular)).getBytes();break;
             case "jszip.js": body=io.text(context.getResources().openRawResource(R.raw.jszip)).getBytes();break;
             case "xlsx.js":body=io.text(context.getResources().openRawResource(R.raw.xlsx)).getBytes();break;
             case "jquery.js":body=io.text(context.getResources().openRawResource(R.raw.jquery)).getBytes();break;
             default :body="not found".getBytes();mime="text/html";break;
         }

         Date date=new Date();
         String resp=String.format(response,date,date,body.length,mime);
         out.write(resp.getBytes());
         out.flush();
         out.write(body);
         out.flush();
     }catch (Exception e){Log.e(TAG,TAG,e);}
 }

 public synchronized void stop(){
     if(sc!=null && !sc.isClosed()){
         try { sc.close();}
         catch (IOException e) {Log.e(TAG,TAG,e);}
     }
       observer.cancel();
       keepRunning=false;
       ex.shutdownNow();
       Log.e("stopping","stopping fake server");
 }

    private static final String response=
            "HTTP/1.1 200 OK\n" +
            "Date: %s\n" +
            "Server: Custom FakeServer" +
            "Last-Modified: %s\n" +
            "Content-Length: %s\n" +
            "Content-Type: %s\r\n\r\n";
}
