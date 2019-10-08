package tz.co.gluhen.common.io;

import android.util.Base64;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import tz.co.gluhen.common.HelperInterfaces.*;
import tz.co.gluhen.common.Utils;


@SuppressWarnings({"WeakerAccess", "unused"})
public class IO {
    private static final String TAG="IO";
    private static final String charEncoding="UTF-8";

    ProgressObserver observer;
    public IO(ProgressObserver observer){this.observer=observer;}
    public  void text(InputStream in, String splitter, Consumer<String> tokenConsumer){
        if(in==null||tokenConsumer==null||splitter==null){return;}
        try(BufferedReader r=new BufferedReader(new InputStreamReader(in))){
            char[] ss=splitter.toCharArray();
            StringBuilder sb= new StringBuilder();
            for(int i=r.read(),j=0;observer.update()&&i!=-1;i=r.read()){
                    sb.append((char)i);
                    j=ss[j]==i?j+1:0;
                    if(j==ss.length){
                        tokenConsumer.take(sb.substring(0,sb.length()-ss.length));
                        sb.setLength(0);j=0;
                    }
            }
        }catch (IOException e){Utils.logE(TAG,e);}
    }

    public  String readTextUntil(InputStream in,String ending){
        try(BufferedReader r=new BufferedReader(new InputStreamReader(in))){
            char[] rs=new char[128];
            StringBuilder sb= new StringBuilder();
            for(int i=r.read(rs);i>0&&observer.update(i*8);i=r.read(rs)){
                sb.append(rs,0,i);
                if(sb.indexOf(ending)>=0){break;}}
            return sb.toString();
        }catch (IOException e){Utils.logE(TAG,e);return "";}
    }


    public  String text(InputStream in){
        try(BufferedReader r=new BufferedReader(new InputStreamReader(in))){
            char[] rs=new char[128];
            StringBuilder sb= new StringBuilder();
            for(int i=r.read(rs);i>0&&observer.update(i*8);i=r.read(rs)){sb.append(rs,0,i);}
            return sb.toString();
        }catch (IOException e){Utils.logE(TAG,e); return "";}
    }

    public  String text(File fileName){
        try(FileInputStream in=new FileInputStream(fileName)){ return text(in);}
        catch (IOException e){Utils.logE(TAG,e); return "";}
    }

    public  void text(String textString,OutputStream out){
        try{ out.write(textString.getBytes()); out.flush();}
        catch(IOException e){Utils.logE("kosa",e);}
    }

    public  void file(InputStream in,File file){
        try(FileOutputStream out=new FileOutputStream(file)){ copy(in,out);}
        catch (IOException e) { Utils.logE(TAG,e);}
    }
    public  void file(OutputStream out,File file){
        try(FileInputStream in=new FileInputStream(file)){ copy(in,out);}
        catch (IOException e) { Utils.logE(TAG,e);}
    }
    public  void fileAppend(InputStream in,File file){
        try(FileOutputStream out=new FileOutputStream(file,true)){ copy(in,out);}
        catch (IOException e) { Utils.logE(TAG,e);}
    }

    public  void fileAppend(String s,File file){
        try(FileOutputStream out=new FileOutputStream(file,true)){ text(s,out);}
        catch (IOException e) { Utils.logE(TAG,e);}
    }

    private  String urlEncode(String input){
        try { return URLEncoder.encode(input,charEncoding); }
        catch (UnsupportedEncodingException e) {Utils.logE("kosa",e);}
        return "";
    }


    public void bytes(OutputStream out,byte[] data){
       copy(new ByteArrayInputStream(data),out);
    }
    public  byte[] readBytes(InputStream inputStream){
        ByteArrayOutputStream out=new ByteArrayOutputStream();
                             copy(inputStream,out);
        return out.toByteArray();
    }
    public  byte[] readBytes(File file){
        ByteArrayOutputStream out=new ByteArrayOutputStream();
            file(out,file); return out.toByteArray();
    }

    public  void copy(InputStream in, OutputStream out){
        try(BufferedInputStream bIn=new BufferedInputStream(in);
            BufferedOutputStream bOut=new BufferedOutputStream(out)){
            for(int i=bIn.read();i!=-1&&observer.update();i=bIn.read()){
                bOut.write(i);}
            bOut.flush();
        }
        catch (IOException e){Utils.logE(TAG,e);}
    }


    public  byte[] readPemKey(File textFile){
        String s= text(textFile)
                .replaceAll("--+?BEGIN.+?KEY.+?\n","")
                .replaceAll("-+END.+?KEY-+","");
        return Base64.decode(s,Base64.DEFAULT);
    }


   public  String trace(Exception e){
       ByteArrayOutputStream out=new ByteArrayOutputStream();
       PrintStream ps=new PrintStream(out);
       e.printStackTrace(ps);
       return new String(out.toByteArray());
   }

   public String readRequestHeaders(BufferedInputStream bin){
       StringBuilder sb=new StringBuilder();
       try{ for(int i=bin.read();i!=-1;i=bin.read()){
               sb.append((char)i);
               if((char)i=='\n'&&sb.indexOf("\r\n\r\n")>0){break;}
           }
       }catch (IOException e){ Log.e(TAG,TAG,e);}
        return sb.toString();
   }
    public byte[] readPostRequestBody(BufferedInputStream bin,int len){
        try(ByteArrayOutputStream out=new ByteArrayOutputStream()){
            observer.reset();
            observer.setBytesToFetchLength(len);
            for(int c=0;observer.update()&&c<len;c++){
                int b=bin.read();
                if(b==-1){break;}
                out.write(b); }
            return out.toByteArray();}
        catch (IOException e){ Log.e(TAG,TAG,e);}
        return new byte[0];
    }

}
