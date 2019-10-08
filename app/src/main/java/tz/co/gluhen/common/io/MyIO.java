package tz.co.gluhen.common.io;

import android.util.Base64;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import tz.co.gluhen.common.HelperInterfaces.Consumer;
import tz.co.gluhen.common.Utils;


@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class MyIO {
    private static final String TAG="MyIO";
    private static final String charEncoding="UTF-8";

    public static void text(InputStream in, String splitter, Consumer<String> tokenConsumer){
        if(in==null||tokenConsumer==null||splitter==null){return;}
        try(BufferedReader r=new BufferedReader(new InputStreamReader(in))){
            char[] ss=splitter.toCharArray();
            StringBuilder sb= new StringBuilder();
            for(int i=r.read(),j=0;i!=-1;i=r.read()){
                    sb.append((char)i);
                    j=ss[j]==i?j+1:0;
                    if(j==ss.length){
                        tokenConsumer.take(sb.substring(0,sb.length()-ss.length));
                        sb.setLength(0);j=0;
                    }
            }
        }catch (IOException e){Utils.logE(TAG,e);}
    }

    public static String readTextUntil(InputStream in,String ending){
        try(BufferedReader r=new BufferedReader(new InputStreamReader(in))){
            char[] rs=new char[1024];
            StringBuilder sb= new StringBuilder();
            for(int i=r.read(rs);i>0;i=r.read(rs)){
                sb.append(rs,0,i);
                if(sb.indexOf(ending)>=0){break;}}
            return sb.toString();
        }catch (IOException e){Utils.logE(TAG,e);return "";}
    }


    public static String text(InputStream in){
        try(BufferedReader r=new BufferedReader(new InputStreamReader(in))){
            char[] rs=new char[1024];
            StringBuilder sb= new StringBuilder();
            for(int i=r.read(rs);i>0;i=r.read(rs)){sb.append(rs,0,i);}
            return sb.toString();
        }catch (IOException e){Utils.logE(TAG,e); return "";}
    }

    public static String text(File fileName){
        try(FileInputStream in=new FileInputStream(fileName)){ return text(in);}
        catch (IOException e){Utils.logE(TAG,e); return "";}
    }

    public static void text(String textString,OutputStream out){
        try{ out.write(textString.getBytes()); out.flush();}
        catch(IOException e){Utils.logE("kosa",e);}
    }

    public static void file(InputStream in,File file){
        try(FileOutputStream out=new FileOutputStream(file)){ copy(in,out);}
        catch (IOException e) { Utils.logE(TAG,e);}
    }
    public static void file(OutputStream out,File file){
        try(FileInputStream in=new FileInputStream(file)){ copy(in,out);}
        catch (IOException e) { Utils.logE(TAG,e);}
    }
    public static void fileAppend(InputStream in,File file){
        try(FileOutputStream out=new FileOutputStream(file,true)){ copy(in,out);}
        catch (IOException e) { Utils.logE(TAG,e);}
    }

    public static void fileAppend(String s,File file){
        try(FileOutputStream out=new FileOutputStream(file,true)){ text(s,out);}
        catch (IOException e) { Utils.logE(TAG,e);}
    }

    private static String urlEncode(String input){
        try { return URLEncoder.encode(input,charEncoding); }
        catch (UnsupportedEncodingException e) {Utils.logE("kosa",e);}
        return "";
    }

    public static byte[] readBytes(InputStream inputStream){
        try {
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            byte[]  block=new byte[2048];
            for(int l=inputStream.read(block);l!=-1;l=inputStream.read(block)){ out.write(block,0,l);}
            byte[] data=out.toByteArray();
            out.close(); inputStream.close();
            return data;
        }catch (IOException e){Utils.logE(TAG,e);return new byte[0];}
    }
    public static byte[] readBytes(File file){
        try(FileInputStream in=new FileInputStream(file)){ return readBytes(in); }
        catch (IOException e){Utils.logE(TAG,e);return new byte[0];}
    }


    public static void copy(InputStream in, OutputStream out){
        try(BufferedInputStream bIn=new BufferedInputStream(in);
            BufferedOutputStream bOut=new BufferedOutputStream(out)){
            byte[] b=new byte[4048];
            for(int i=bIn.read(b);i!=-1;i=bIn.read(b)){
                bOut.write(b,0,i);
               } bOut.flush();
        }
        catch (IOException e){Utils.logE(TAG,e);}
    }

    public static void download(String url,File saveToFile){
        try{ HttpURLConnection con = (HttpURLConnection)new URL(url).openConnection();
            //con.setRequestMethod("GET");
            file(con.getInputStream(),saveToFile);
        }catch (IOException e){Utils.logE(TAG,e);}
    }

    public static String get(String url){return get(url, null);}
    public static String get(String url,Consumer<HttpURLConnection> setHeaders){
        try{ HttpURLConnection con = (HttpURLConnection)new URL(url).openConnection();
            con.setRequestMethod("GET");
            if(setHeaders!=null){setHeaders.take(con);}
            return MyIO.text(con.getInputStream());
        }catch (IOException e){Utils.logE(TAG,e);}
        return "";
    }


    public static byte[] readPemKey(File textFile){
        String s=MyIO.text(textFile)
                .replaceAll("--+?BEGIN.+?KEY.+?\n","")
                .replaceAll("-+END.+?KEY-+","");
        return Base64.decode(s,Base64.DEFAULT);
    }


   public static String trace(Exception e){
       ByteArrayOutputStream out=new ByteArrayOutputStream();
       PrintStream ps=new PrintStream(out);
       e.printStackTrace(ps);
       return new String(out.toByteArray());
   }

    public static Pipe pipe(){ return new Pipe();}

    public interface InputStreamFunction<T>{   T take(InputStream con);}

}
