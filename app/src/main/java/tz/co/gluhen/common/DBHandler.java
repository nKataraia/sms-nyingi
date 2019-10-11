package tz.co.gluhen.common;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.dtree.android.messeji.R;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import tz.co.gluhen.common.io.IO;
import tz.co.gluhen.common.io.Post;
import tz.co.gluhen.common.io.ProgressObserver;

public abstract class DBHandler {
    private SQLiteDatabase sqlDB;
    private ProgressObserver observer=new ProgressObserver();
    protected DBHandler(Context c) {
         GsonBuilder gb=new GsonBuilder();
                     gb.setDateFormat(DATE_PATTERN);
                gson=gb.create();
                initialize(c);
    }
    private static final String DATE_PATTERN="yyyy-MM-dd HH:mm:ss";
    private static final String TAG="DB.java";
    private final Gson gson;

    private void initialize(final Context c){
          final File p= new File(c.getFilesDir(),getDatabaseName());
              if(p.isFile()&&p.length()>10){
                  sqlDB=SQLiteDatabase.openDatabase(p.getAbsolutePath(),null,SQLiteDatabase.OPEN_READWRITE, dbObj -> {
                    sqlDB=SQLiteDatabase.openOrCreateDatabase(p,null);
                       createTables(c);});
                 if(isOldVersion()){ createTables(c);}
              }
              else{ sqlDB=SQLiteDatabase.openOrCreateDatabase(p,null);
                    createTables(c);
                  }
    }

    protected abstract int getDatabaseVersion();
    protected abstract String getDatabaseName();
    private boolean isOldVersion(){
            Long dbVersion = fetchSingle("select dbVersion from db order by dbVersion desc limit 1", cs -> cs.getLong(1));
            return sqlDB == null
                    || dbVersion == null
                    || dbVersion < getDatabaseVersion();
    }

    private void createTables(Context c){
           String data=new IO(observer).text(c.getResources().openRawResource(R.raw.tables));
           queries(data);

    }

    private void runSQL(String sql){
       Log.e(TAG,sql);
       sqlDB.execSQL(sql);
    }
    private void queries(String sql){
        try {
            sqlDB.beginTransaction();
            Utils.splitForEach(sql, ";", this::runSQL);
            sqlDB.setTransactionSuccessful();
            sqlDB.endTransaction();
            if(isOldVersion()){
                throw new IllegalStateException(" make sure you create table db and insert dbVersion and codeVersion");
            }
        }catch (SQLException e){Log.e(TAG,e.getMessage(),e);}
    }

    @SuppressWarnings("unused")
    private void syncTable(String tableName,long userId){

        String query="select max(date),max(rowId) from syncEvents where tableName=?";
        String outData=fetchSingleJson(query,tableName);

        sqlDB.beginTransaction();
        Post.requestHeaders(observer)
            .sendPost("","")
            .response((io,v)->{});

        sqlDB.setTransactionSuccessful();
        sqlDB.endTransaction();

    }

    private String[] bindSelect(Object[] args){
       String[] s=new String[args.length];
       for(int i=0;i<args.length;i++) {
           s[i] = (args[i] instanceof Date)
                   ?dateToString((Date)args[i])
                   :String.valueOf(args[i]);}
       return s;
   }


    @SuppressWarnings("WeakerAccess")
    public Map<String,String> fetchSingleMap(String sql, Object ... args){
       Map<String,String> map=new HashMap<>();
        String[] s=bindSelect(args);
        Cursor cs=sqlDB.rawQuery(sql,s);
        if(cs.moveToNext()){
            for(int i=1,n=cs.getColumnCount();i<n;i++){
                map.put(cs.getColumnName(i),cs.getString(i));}
        }
        cs.close();
        return map;
    }
    @SuppressWarnings("WeakerAccess")
    public String fetchSingleJson(String sql,Object ... args){return gson.toJson(fetchSingleMap(sql,args));}
    public <T> T fetchSingle(String sql, HelperInterfaces.Changer<Cursor,T> maker, Object...args){
          String[] s = bindSelect(args);
      try(Cursor cs = sqlDB.rawQuery(sql,s)){
            if (cs.moveToNext()) { return maker.change(cs);}}
      catch (Exception e){Log.e(TAG,TAG,e);}
        return null;
    }


    @SuppressWarnings("unused")
    public  void forEach(HelperInterfaces.Consumer<Cursor> action, String sql, Object...args){
        String[] s=bindSelect(args);
        try(Cursor cs=sqlDB.rawQuery(sql,s)) {
            while (cs.moveToNext()) {
                action.take(cs);
            }}
        catch (Exception e){Log.e(TAG,TAG,e);}
    }
    @SuppressWarnings("unused")
    public <T> void forEach(HelperInterfaces.Consumer<T> action, String sql, HelperInterfaces.Changer<Cursor,T> maker, Object...args){
                  List<T> objects=fetch(sql,maker,args);
                  for(T o:objects) {action.take(o);}
    }
    public <T> List<T> fetch(String sql, HelperInterfaces.Changer<Cursor,T> maker, Object...args){
        String[] s=bindSelect(args);
        List<T> list=new ArrayList<>();
        try(Cursor cs=sqlDB.rawQuery(sql,s)){
            while(cs.moveToNext()){
                  list.add(maker.change(cs));}}
        catch (Exception e){Log.e(TAG,TAG,e);}
        return list;
    }

    private long save(int type,String sql,Object ...args){

        try(SQLiteStatement st=sqlDB.compileStatement(sql)){
            for(int i=1;i<args.length+1;i++){
                Object a=args[i];
                if(a instanceof Date){ st.bindString(i,dateToString((Date)a));}
                else st.bindString(i,String.valueOf(a));
            }
            return type==1?st.executeInsert():st.executeUpdateDelete();
        }catch (SQLException e){ Log.e(TAG,"",e);}
        return 0;
    }

    public long insert(String sql,Object ...args){ return (int)save(1,sql,args);}
    public int update(String sql,Object ...args){ return (int)save(2,sql,args);}

    public static String dateToString(Date date){ return new SimpleDateFormat(DATE_PATTERN, Locale.ENGLISH).format(date);}
    public static Date stringToDate(String date){
        try { return new SimpleDateFormat(DATE_PATTERN, Locale.ENGLISH).parse(date);}
        catch (ParseException e) { return DATE_ZERO; }
    }

    private static final Date DATE_ZERO=new Date(-62167402799000L);
}
