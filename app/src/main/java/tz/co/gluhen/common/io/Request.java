package tz.co.gluhen.common.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tz.co.gluhen.common.HelperInterfaces.Consumer;
import tz.co.gluhen.common.HelperInterfaces.BiConsumer;
import tz.co.gluhen.common.Utils;

@SuppressWarnings({"UnusedReturnValue",  "unused","WeakerAccess"})
public class Request implements Closeable {

    public static final String TAG="Request";
    private Header headers;
    private HttpURLConnection connection;
    private BufferedOutputStream outputStream;
    private  Exception exception;
    IO io;

    private Request(ProgressObserver observer){this.io=new IO(observer);}
    public static Header requestHeaders(ProgressObserver observer){ return new Header(new Request(observer));}
    public Request post(String url, int contentLength){
          try {
              connection = (HttpURLConnection) new URL(url).openConnection();
              connection.setRequestMethod("POST");
               headers.setReqHeaders(connection);
              connection.setRequestProperty("Content-length", String.valueOf(contentLength));
              connection.setDoOutput(true);
              outputStream = new BufferedOutputStream(connection.getOutputStream());
          }catch (IOException e){ exception=e;Utils.logE(TAG,e);}
        return this;
    }

    public Request get(String url){
        try {
            connection= (HttpURLConnection)new URL(url).openConnection();
            connection.setRequestMethod("GET");
            headers.setReqHeaders(connection);
            }catch (IOException e){ exception=e;Utils.logE(TAG,e);}
         return this;
    }

    public Request headers(Consumer<Map<String,String>> headerConsumer){
        if(headers.rsHeader!=null&&headers.rsHeader.isEmpty()){
            headerConsumer.take(headers.rsHeader);
        }
        return this;
    }

    public Request response(BiConsumer<IO,BufferedInputStream> streamConsumer){
        if(connection==null){return this;}
        try(BufferedInputStream in=new BufferedInputStream(connection.getInputStream())){
            headers.setRespHeaders(connection);
            streamConsumer.take(io,in);}
        catch (IOException e) { exception=e;Utils.logE(TAG,e);}
        return this;
    }
    public Request response(Consumer<BufferedInputStream> streamConsumer){
        if(connection==null){return this;}
        try(BufferedInputStream in=new BufferedInputStream(connection.getInputStream())){
            headers.setRespHeaders(connection);
            streamConsumer.take(in);}
         catch (IOException e) { exception=e;Utils.logE(TAG,e);}
        return this;
    }
    public BufferedInputStream response(){
        if(connection==null){return null;}
        try(BufferedInputStream in=new BufferedInputStream(connection.getInputStream())){
            headers.setRespHeaders(connection);
            return new BufferedInputStream(connection.getInputStream());
        }
        catch (IOException e) { exception=e;Utils.logE(TAG,e);}
        return null;
    }
    public Exception error(){return exception;}
    public int responseCode(){
        try { return connection.getResponseCode();}
        catch (IOException e) { exception=e; }
        return -1;
    }
    public boolean isError(){return exception!=null;}

    @Override
    public void close() throws IOException {
            outputStream.flush();
            outputStream.close();
            connection.disconnect();
    }

    public Request writeBody(byte[] b) { io.copy(new ByteArrayInputStream(b),outputStream);return this;}
    public Request writeBody(String b) { io.text(b,outputStream);return this;}
    public Request writeBody(Consumer<BufferedOutputStream> outputStreamConsumer) {
        outputStreamConsumer.take(outputStream);
        return this;
    }

    public static  class Header {
        Request p;
        Header(Request p) { this.p = p; }
        Map<String, String> rqHeader = new HashMap<>();
        Map<String, String> rsHeader = new HashMap<>();

        public Header set(Map<String, Object> headers){
            for(String k:headers.keySet()){setHeader(k,headers.get(k));}
            return this;}

        public Header setHeader(String name, Object o) {
            rqHeader.put(name, o instanceof Collection ? Utils.join((Collection) o, ";") : String.valueOf(o));
            return this;
        }
        public Request openPost(String url, int contentLength) {p.headers=this;return p.post(url,contentLength);}

        public Request sendPost(String url, String body){return sendPost(url,body.getBytes());}
        public Request sendPost(String url, byte[] body){
            p.headers=this;
            return p.post(url,body.length).writeBody(o->p.io.copy(new ByteArrayInputStream(body),o));}

        public Request sendGet(String url) {p.headers=this;return p.get(url);}
        public Request openGet(String url) {p.headers=this;return p.get(url);}
        private void setRespHeaders(HttpURLConnection con) {
            Map<String, List<String>> h = con.getHeaderFields();
            for (String k : h.keySet()) {
                rsHeader.put(k, Utils.join(h.get(k), ";"));
            }
        }
        private void setReqHeaders(HttpURLConnection con) {
            for (String k : rqHeader.keySet()) {
                con.setRequestProperty(k, rqHeader.get(k));
            }
        }
    }
}
