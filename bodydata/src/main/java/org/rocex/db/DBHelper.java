package org.rocex.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.rocex.model.ModelStatus;
import org.rocex.model.SuperModel;

import java.util.ArrayList;
import java.util.List;

/***************************************************************************
 * Created by rocexwang on 2017-06-14 11:23:26
 ***************************************************************************/
public abstract class DBHelper<T extends SuperModel> extends SQLiteOpenHelper
{
    private static final String TAG = DBHelper.class.getName();
    
    public DBHelper(Context context, String strDatabaseName, SQLiteDatabase.CursorFactory cursorFactory, int iDatabaseVersion)
    {
        this(context, strDatabaseName, cursorFactory, iDatabaseVersion, null);
    }
    
    public DBHelper(Context context, String strDatabaseName, SQLiteDatabase.CursorFactory cursorFactory, int iDatabaseVersion,
                    DatabaseErrorHandler errorHandler)
    {
        super(context, strDatabaseName, cursorFactory, iDatabaseVersion, errorHandler);
    }
    
    public static void close(Cursor cursor)
    {
        if(cursor == null || cursor.isClosed())
        {
            return;
        }
    
        cursor.close();
    }
    
    public static void close(SQLiteDatabase db)
    {
        if(db == null || !db.isOpen())
        {
            return;
        }
    
        db.close();
    }
    
    public ContentValues convertToContentValues(T superModel, String... strFieldNames)
    {
        if(strFieldNames == null || strFieldNames.length == 0)
        {
            strFieldNames = superModel.getPropNames();
        }
        
        ContentValues contentValues = new ContentValues();
    
        for(String strFieldName : strFieldNames)
        {
            Object objPropValue = superModel.getPropValue(strFieldName);
    
            if(objPropValue == null)
            {
                contentValues.putNull(strFieldName);
    
                continue;
            }
    
            Class classPropType = superModel.getPropType(strFieldName);
    
            if(classPropType.equals(Boolean.class))
            {
                contentValues.put(strFieldName, (Boolean) objPropValue);
            }
            else if(classPropType.equals(Byte.class))
            {
                contentValues.put(strFieldName, (Byte) objPropValue);
            }
            else if(classPropType.equals(byte[].class))
            {
                contentValues.put(strFieldName, (byte[]) objPropValue);
            }
            else if(classPropType.equals(Double.class))
            {
                contentValues.put(strFieldName, (Double) objPropValue);
            }
            else if(classPropType.equals(Float.class))
            {
                contentValues.put(strFieldName, (Float) objPropValue);
            }
            else if(classPropType.equals(Integer.class))
            {
                contentValues.put(strFieldName, (Integer) objPropValue);
            }
            else if(classPropType.equals(Long.class))
            {
                contentValues.put(strFieldName, (Long) objPropValue);
            }
            else if(classPropType.equals(Short.class))
            {
                contentValues.put(strFieldName, (Short) objPropValue);
            }
            else if(classPropType.equals(String.class))
            {
                contentValues.put(strFieldName, (String) objPropValue);
            }
        }
        
        return contentValues;
    }
    
    public T convertToModel(Class<T> clazzModel, ContentValues contentValues, String... strFieldNames)
    {
        T superModel = null;
        
        try
        {
            superModel = clazzModel.newInstance();
        }
        catch(Exception ex)
        {
            Log.e(TAG, "convertToModel: class[" + clazzModel + "]", ex);
    
            return null;
        }
        
        if(strFieldNames == null || strFieldNames.length == 0)
        {
            strFieldNames = superModel.getPropNames();
        }
        
        for(String strFieldName : strFieldNames)
        {
            superModel.setPropValue(strFieldName, contentValues.get(strFieldName));
        }
        
        return superModel;
    }
    
    public int delete(T... superModels)
    {
        if(superModels == null)
        {
            return 0;
        }
        
        int iCount = 0;
    
        SQLiteDatabase db = getWritableDatabase();
        
        try
        {
            for(T superModel : superModels)
            {
                iCount += db.delete(superModel.getTableName(), SuperModel.ID + "=?", new String[]{String.valueOf(superModel.getId())});
            }
        }
        finally
        {
            //close();
        }
        
        return iCount;
    }
    
    public List<Long> insert(T superModels[], String... strPropNames)
    {
        List<Long> listId = new ArrayList<Long>();
        
        if(superModels == null)
        {
            return listId;
        }
    
        SQLiteDatabase db = getWritableDatabase();
        
        try
        {
            for(T superModel : superModels)
            {
                superModel.setStatus(ModelStatus.NEW);
    
                initAudit(superModel);
                
                ContentValues contentValues = convertToContentValues(superModel, strPropNames);
    
                listId.add(db.insert(superModel.getTableName(), null, contentValues));
            }
        }
        finally
        {
            //close();
        }
    
        return listId;
    }
    
    public boolean existTable(String strTableName)
    {
        String strSQL = "select name from sqlite_master where type='table' and name='" + strTableName + "'";
    
        Cursor cursor = getReadableDatabase().rawQuery(strSQL, null);
        
        return cursor.getCount() > 0;
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
    }
    
    public List<T> query(Class<T> clazzModel, String strPropNames[], String strOrderBy, String strSelection, String... strSelectionArgs)
    {
        return query(clazzModel, strPropNames, null, null, strOrderBy, null, strSelection, strSelectionArgs);
    }
    
    public List<T> query(Class<T> clazzModel, String strPropNames[], String strGroupBy, String strHaving, String strOrderBy,
                         String strLimit, String strSelection, String... strSelectionArgs)
    {
        List<T> listSuperModel = new ArrayList<>();
        
        T superModel = null;
        
        try
        {
            superModel = clazzModel.newInstance();
        }
        catch(Exception ex)
        {
            Log.e(TAG, "query: class[" + clazzModel + "]", ex);
    
            return listSuperModel;
        }
        
        if(strPropNames == null || strPropNames.length == 0)
        {
            strPropNames = superModel.getPropNames();
        }
        
        try
        {
            SQLiteDatabase db = getReadableDatabase();
            
            Cursor cursor = db
                    .query(superModel.getTableName(), strPropNames, strSelection, strSelectionArgs, strGroupBy, strHaving, strOrderBy,
                            strLimit);
            
            cursor.moveToFirst();
            
            listSuperModel = convertToModel(clazzModel, cursor, superModel.getPropNames());
        }
        finally
        {
            //close();
        }
        
        return listSuperModel;
    }
    
    public List<T> convertToModel(Class<T> clazzModel, Cursor cursor, String... strPropNames)
    {
        List<T> listSuperModel = new ArrayList<T>();
        
        while(!cursor.isAfterLast())
        {
            T superModel = null;
            
            try
            {
                superModel = clazzModel.newInstance();
            }
            catch(Exception ex)
            {
                Log.e(TAG, "convertToModel: ", ex);
                
                continue;
            }
            
            for(String strPropName : strPropNames)
            {
                int iColumnIndex = cursor.getColumnIndex(strPropName);
                
                Class classPropType = superModel.getPropType(strPropName);
    
                if(classPropType.equals(Boolean.class))
                {
                    superModel.setPropValue(strPropName, 1 == cursor.getInt(iColumnIndex));
                }
                else if(classPropType.equals(byte[].class))
                {
                    superModel.setPropValue(strPropName, cursor.getBlob(iColumnIndex));
                }
                else if(classPropType.equals(Double.class))
                {
                    superModel.setPropValue(strPropName, cursor.getDouble(iColumnIndex));
                }
                else if(classPropType.equals(Float.class))
                {
                    superModel.setPropValue(strPropName, cursor.getFloat(iColumnIndex));
                }
                else if(classPropType.equals(Integer.class))
                {
                    superModel.setPropValue(strPropName, cursor.getInt(iColumnIndex));
                }
                else if(classPropType.equals(Long.class))
                {
                    superModel.setPropValue(strPropName, cursor.getLong(iColumnIndex));
                }
                else if(classPropType.equals(Short.class))
                {
                    superModel.setPropValue(strPropName, cursor.getShort(iColumnIndex));
                }
                else if(classPropType.equals(String.class))
                {
                    superModel.setPropValue(strPropName, cursor.getString(iColumnIndex));
                }
            }
            
            listSuperModel.add(superModel);
            
            cursor.moveToNext();
        }
        
        return listSuperModel;
    }
    
    protected void initAudit(T superModel)
    {
        if(superModel == null)
        {
            return;
        }
        
        long lTime = System.currentTimeMillis();
        
        superModel.setTs(lTime);
        
        switch(superModel.getStatus())
        {
            case NEW:
                superModel.setCreate_time(lTime);
                break;
            case MODIFIED:
                break;
            case DELETED:
                break;
        }
    }
    
    public T query(Class<T> clazzModel, long id)
    {
        List<T> listSuperModel = query(clazzModel, null, null, null, null, null, SuperModel.ID + "=?", new String[]{String.valueOf(id)});
        
        return listSuperModel.get(0);
    }
    
    public int update(T superModels[], String... strPropNames)
    {
        if(superModels == null || superModels.length == 0)
        {
            return 0;
        }
        
        int iCount = 0;
        
        SQLiteDatabase db = getWritableDatabase();
        
        try
        {
            for(T superModel : superModels)
            {
                superModel.setStatus(ModelStatus.MODIFIED);
    
                initAudit(superModel);
                
                ContentValues contentValues = convertToContentValues(superModel, strPropNames);
                
                iCount += db.update(superModel.getTableName(), contentValues, SuperModel.ID + "=?",
                        new String[]{String.valueOf(superModel.getId())});
            }
        }
        finally
        {
            //close();
        }
        
        return iCount;
    }
}
