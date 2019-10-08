package tz.co.gluhen.common.io;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import tz.co.gluhen.common.HelperInterfaces.*;
import tz.co.gluhen.common.Utils;

@SuppressWarnings({"unused", "WeakerAccess", "UnusedReturnValue"})
public class PostMultipart implements Closeable{

    public static final String TAG="PostMultipart";
    private PostMultipart(ProgressObserver observer){this.observer=observer;}
    private ProgressObserver observer;
    private final String LF="\r\n";
    private final String boundary = "mpaka-"+System.currentTimeMillis();

    private BufferedOutputStream writer;
    private HttpURLConnection connection;
    private StringBuilder sb=new StringBuilder();

    public static PostMultipart open(String url) throws IOException {return open(url,null);}
    public static PostMultipart open(String url,ProgressObserver observer) throws IOException {
          PostMultipart $this=new PostMultipart(observer);
            $this.connection = (HttpURLConnection)new URL(url).openConnection();
            $this.connection.setRequestMethod("POST");
            $this.connection.setRequestProperty("Content-Type","multipart/form-data; boundary=" + $this.boundary);
            $this.connection.setDoOutput(true);
            $this.writer=new BufferedOutputStream($this.connection.getOutputStream());
            return $this;
    }

    public PostMultipart write(String name, String value) {
        String charset = "UTF-8";
        sb.setLength(0);
        sb.append("--").append(boundary).append(LF);
        sb.append("Content-Disposition: form-data; ");
        sb.append("name=\"").append(name).append("\"").append(LF);
        sb.append("Content-Type: text/plain; charset=").append(charset).append(LF);
        sb.append(LF);
        sb.append(value).append(LF);

        observer.reset();
        observer.setBytesToFetchLength(value.length()+sb.length());
        write(sb);
        observer.done();
        return this;
    }

    public PostMultipart write(String name, byte[] data){
          sb.append("--").append(boundary).append(LF);
          sb.append("Content-Disposition: form-data; ");
          sb.append("name=\"").append(name).append("\"; filename=\"").append(name).append("\"").append(LF);
          sb.append("Content-Type: application/octet-stream").append(LF);
          sb.append("Content-Transfer-Encoding: binary").append(LF);
          sb.append(LF);

        observer.reset();
        observer.setBytesToFetchLength(data.length+sb.length()+LF.length());
          write(sb); write(data);write(LF);
          observer.done();
      return this;
    }

    public PostMultipart write(String name,InputStream inputStream){
        try(BufferedInputStream in=new BufferedInputStream(inputStream)){
                sb.append("--").append(boundary).append(LF);
                sb.append("Content-Disposition: form-data; ");
                sb.append("name=\"").append(name).append("\"; filename=\"").append(name).append("\"");
                sb.append(LF);
                sb.append("Content-Type: application/octet-stream").append(LF);
                sb.append("Content-Transfer-Encoding: binary").append(LF);
                sb.append(LF);
                write(sb);
                byte[] buffer = new byte[1024];  int bytesRead=0;
                while (observer.update(bytesRead)&&(bytesRead = in.read(buffer)) != -1) {
                        writer.write(buffer, 0, bytesRead);
                     }
                write(LF);
            observer.done();
             }
        catch (IOException e){Utils.logE(TAG,e);}
        return this;
    }

    public PostMultipart write(String name, File file) {
        if(file==null){return this;}
        try(BufferedInputStream in=new BufferedInputStream(new FileInputStream(file))){
            sb.append("--").append(boundary).append(LF);
            sb.append("Content-Disposition: form-data; ");
            sb.append("name=\"").append(name).append("\"; filename=\"").append(name).append("\"");
            sb.append(LF);
            sb.append("Content-Type: application/octet-stream").append(LF);
            sb.append("Content-Transfer-Encoding: binary").append(LF);
            sb.append(LF);
            observer.reset();
            observer.setBytesToFetchLength((int)(file.length()+sb.length()+LF.length()));
            write(sb);

            byte[] buffer = new byte[1024];  int bytesRead=0;
            while (observer.update(bytesRead)&&(bytesRead = in.read(buffer)) != -1) {
                writer.write(buffer, 0, bytesRead);
            }
            write(LF);
            observer.done();
        }
        catch (IOException e){Utils.logE(TAG,e);}
        return this;
    }
    public PostMultipart write(String name,Object object){
        if(object instanceof byte[]){ write(name,(byte[])object);}
        else if(object instanceof String){ write(name,(String)object);}
        else if(object instanceof File){ write(name,(File)object);}
        else {write(name,String.valueOf(object));}
        return this;
    }


    public void write(String s){ write(s.getBytes());}
    public void write(StringBuilder sb){
        write(sb.toString().getBytes());
        sb.setLength(0);
    }
    public void write(byte[] data){
        if(!observer.update(data.length)||writer==null){return;}
        try { writer.write(data); writer.flush();}
        catch (IOException e) {Utils.logE(TAG,e);}
    }
    @Override public void close() throws IOException {
        if(connection!=null){connection.disconnect();}
        if(writer!=null){ writer.close(); writer=null;}
    }

   public void response(Consumer<InputStream> streamConsumer){
         try(BufferedInputStream in=new BufferedInputStream(connection.getInputStream())){
                        streamConsumer.take(in);
         }catch (IOException e){Utils.logE(TAG,e);}
    }
    public BufferedInputStream responseStream() throws IOException {
        return new BufferedInputStream(connection.getInputStream());
    }

    public Map<String, List<String>> responseHeaders(){
        return connection.getHeaderFields();
    }

}
