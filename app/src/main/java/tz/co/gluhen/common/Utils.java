package tz.co.gluhen.common;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static tz.co.gluhen.common.HelperInterfaces.*;

import javax.xml.parsers.DocumentBuilderFactory;

import tz.co.gluhen.common.io.MyIO;

@SuppressWarnings("unused")
public class Utils {

    public static String join(Collection strings, String joiner){
        if(strings==null||strings.isEmpty())return "";
        Iterator it=strings.iterator();
        StringBuilder sb=new StringBuilder().append(it.next());
        while(it.hasNext()){sb.append(joiner).append(it.next());}
        return sb.toString();
    }
    public static String join(String[] strings, String joiner){return join(Arrays.asList(strings),joiner);}

    public static void splitForEach(String string, String splitter, Consumer<String> action){
        for(String s:string.split(splitter)){ if(s.isEmpty()){continue;} action.take(s);}
    }

    public static Map<String,String> xmlToMap(String xml){
        Map<String,String> data=new HashMap<>();
        try {
            Document doc= DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)));
            doc.getDocumentElement().normalize();
            NodeList nodes=doc.getDocumentElement().getChildNodes();
            for(int i=0,l=nodes.getLength();i<l;i++){
                Node node=nodes.item(i);
                data.put(node.getNodeName(),node.getTextContent());
            }
        } catch (Exception e) { Log.e("makosa","kosa",e);}
        return data;
    }
    public static String mapToXML(String rootTag,Map<String,Object> data){
        StringBuilder sb=new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<")
                .append(rootTag).append(">");
        for(String k:data.keySet()){ sb.append("<").append(k).append(">").append(data.get(k)).append("</").append(k).append(">");}
        sb.append("</").append(rootTag).append(">");
        return sb.toString();
    }

    public static String between(String string,String after,String before){
         int s=string.indexOf(after)+after.length();
         int e=string.indexOf(before,s);
         return s>0&&e>=s?string.substring(s,e):"";
    }

    public static String after(String string,String after){
        int s=string.indexOf(after)+after.length();
        return s>0?string.substring(s):"";
    }
    public static void logD(String format,Object ... data){
        Log.e("inaitwa logd","imeitwa log d na "+format);
         for(Object o:data){
            format= format.replaceFirst("\\?",String.valueOf(o));
         }
         Log.i(data.length>0?String.valueOf(data[0]):"Default",format);
    }
    public static void logE(String tag,Exception e){System.err.println(tag+tag+ MyIO.trace(e));}

    public static byte[] bytes(Object data){
        if(data instanceof Date){ return  DB.dateToString((Date)data).getBytes();}
        else if(data instanceof byte[]){return (byte[])data;}
        else if(data instanceof Collection){return Utils.join((Collection)data,",").getBytes();}
        else return String.valueOf(data).getBytes();
    }

    public static List<Map<String,String>> csvToMapList(String csvString){
        String[] lines=csvString.split("\n");
        String[] fields=lines[0].split(",");lines[0]="";
        List<Map<String,String>> data= new ArrayList<>();

        Pattern pattern = Pattern.compile("(\"[^\"]+\")|(,?([^,]+),?)");
        Pattern rm=Pattern.compile("(^,+)|(,+$)");
        for(String s:lines){
            Matcher m=pattern.matcher(s);
            Map<String,String> line= new HashMap<>();
            for(int i=0,l=fields.length;i<l&&m.find();i++){
                String key=fields[i];
                String value=rm.matcher(m.group()).replaceAll("");
               line.put(key,value);
            }
            data.add(line);
        }
        return data;
    }
    public static String parsePhoneNumber(String phone){
        phone=phone!=null?phone.replaceAll("[^\\d+]+",""):"";
        return phone.length()>=9?"0"+phone.substring(phone.length()-9):"";
    }

    public static boolean fuzzyMatch(char[] chars,String string){
        return fuzzyMatch(new String(chars),string);
    }
    public static boolean fuzzyMatch(CharSequence chars,String string){
        return fuzzyMatch(chars.toString(),string);
    }
    public static boolean fuzzyMatch(String chars,String string){
        if(chars==null||string==null||chars.isEmpty()||string.isEmpty()){return false;}
        int i=0;
        for(int j=0,l=string.length(),s=chars.length();i<s&&j<l;j++){
            i=Character.toLowerCase(string.charAt(j))
                    ==Character.toLowerCase(chars.charAt(i))?i+1:i;
        }
        return i==chars.length();
    }
}
