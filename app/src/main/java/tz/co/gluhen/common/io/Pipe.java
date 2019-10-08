package tz.co.gluhen.common.io;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import tz.co.gluhen.common.HelperInterfaces.*;

public class Pipe implements Closeable {

    private InputStream openedStream;
    private byte[] readBytes =new byte[0];
    private Exception exception;
    private Map<String,String> headers=new HashMap<>();


    public Pipe post(String url, String data){
        post(url,data.getBytes(),null);
        return this;
    }
    public Pipe post(String url, byte[] data, Consumer<HttpURLConnection> setHeaders){
        try{
            HttpURLConnection con = (HttpURLConnection)new URL(url).openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-length",String.valueOf(data.length));
            if(setHeaders!=null){setHeaders.take(con);}
            con.setDoOutput(true);
            try(BufferedOutputStream out=new BufferedOutputStream(con.getOutputStream())){ out.write(data);}
            catch (IOException e){e.printStackTrace();}
            openedStream=con.getInputStream();
        }catch (Exception e){exception=e;}
        return this;
    }

    public Pipe get(String url, Consumer<HttpURLConnection> setHeaders){
        try{
            HttpURLConnection con = (HttpURLConnection)new URL(url).openConnection();
            con.setRequestMethod("GET");
            if(setHeaders!=null){setHeaders.take(con);}
            openedStream=con.getInputStream();
        }catch (Exception e){exception=e;}
        return this;
    }

    public Pipe headers(Consumer<Map<String,String>> headerConsumer){
        if(exception != null){exception.printStackTrace();}
        headerConsumer.take(headers);
        return this;
    }
    public Pipe streamText(String token,Consumer<String> stringConsumer){
       MyIO.text(openedStream,token,stringConsumer);
       return this;
    }
    public Pipe text(Consumer<String> stringConsumer){
        if(exception != null){exception.printStackTrace();}
        else if(openedStream==null){stringConsumer.take(new String(readBytes));}
        else {String s=MyIO.text(openedStream); openedStream=null;stringConsumer.take(s);}
        return this;
    }
    public Pipe bytes(Consumer<byte[]> byteConsumer){
        if(exception!=null){exception.printStackTrace();byteConsumer.take(new byte[0]);}
        if(openedStream==null){byteConsumer.take(readBytes);}
        else {
            readBytes =MyIO.readBytes(openedStream); openedStream=null;
         byteConsumer.take(readBytes);}
        return this;
    }
    public void to(File file){ MyIO.file(openedStream,file);}
    public void to(OutputStream out){ MyIO.copy(openedStream,out);}

    @Override
    public void close() throws IOException {

    }
}
