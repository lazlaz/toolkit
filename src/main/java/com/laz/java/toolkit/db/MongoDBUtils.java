package com.laz.java.toolkit.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoDBUtils {
	private static final String ERROR_MASSAGE = "errorMessage";
	private static final String RESULT = "result";
	
	public static String excuteSQL(MongoClient monGoClient, String dbName, String sql){
		try {
			monGoClient.listDatabaseNames();
		} catch (Exception e) {
			return null;
		}
		MongoDatabase db = monGoClient.getDatabase(dbName);
		String mode = sql.replaceAll(" .*", "").toUpperCase();
		Map<String,String> sqlResult = new HashMap<String,String>();
		switch (mode) {
		case "SELETE":
			excuteSeleteSQL(sql.substring(7),sqlResult,db);
			break;
		case "UPDATE":
			excuteUpdateSQL(sql.substring(7),sqlResult,db);
			break;
		case "INSERT":
			if(sql.toUpperCase().contains(" INTO ")){
				excuteInsertSQL(sql.substring(12),sqlResult,db);
				break;
			}
		case "DELETE":
			excuteDeleteSQL(sql.substring(7),sqlResult,db);
			break;
		case "CREATE":
			excuteCreateSQL(sql.substring(7),sqlResult,monGoClient,db);
			break;
		case "DROP":
			excuteDropSQL(sql.substring(5),sqlResult,monGoClient,db);
			break;
		default:
			return null;
		}
		if(sqlResult.get(ERROR_MASSAGE)!=null){
			return null;
		}
		return sqlResult.get(RESULT);
	}

	private static void excuteSeleteSQL(String sql, Map<String, String> sqlResult, MongoDatabase db) {
		String sqlUpper = sql.toUpperCase();
		if(sqlUpper.indexOf(" FROM ")<0){
			sqlResult.put(ERROR_MASSAGE, "SELETE语句缺少FROM关键字!");
			return;
		}
		String columnName = sqlUpper.replaceFirst(" FROM .*", "");
		String tableName = sqlUpper.replaceFirst(".* FROM ", "");
		String orderColumn = null;
		String whereString = null;
		if(columnName==null || columnName.isEmpty()){
			sqlResult.put(ERROR_MASSAGE, "SELETE语句缺少列名!");
			return;
		}
		if(tableName==null || tableName.isEmpty()){
			sqlResult.put(ERROR_MASSAGE, "SELETE语句缺少表名!");
			return;
		}
		if(tableName.contains(" ORDER BY ")){
			orderColumn = tableName.replaceAll(".* ORDER BY ", "");
			tableName = tableName.replaceAll(" ORDER BY .*", "");
		}
		if(tableName.contains(" WHERE ")){
			whereString = tableName.replaceAll(".* WHERE ", "");
			tableName = tableName.replaceAll(" WHERE .*", "");
		}
		int tableStartIndex = sqlUpper.indexOf(" "+tableName)+1;
		int tableNameLength = tableName.length();
		tableName = sql.substring(tableStartIndex, tableStartIndex+tableNameLength);
		MongoCollection<Document> table = db.getCollection(tableName);
		if("*".equals(columnName)){
			JSONArray json = new JSONArray();
			for(Document document : table.find()){
				JSONObject jobj = new JSONObject();
				for(Entry<String, Object> entry : document.entrySet()){
					try {
						jobj.put(entry.getKey(), entry.getValue());
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				if(jobj.length()>0){
					json.put(jobj);
				}
			}
			sqlResult.put(RESULT, json.toString());
		}else{
			int columnNameLength = columnName.length();
			columnName = sql.substring(0, columnNameLength);
			List<String> columnList = new ArrayList<String>();
			if(columnName.contains(",")){
				for(String str : columnName.split(",")){
					columnList.add(str.trim());
				}
			}else{
				columnList.add(columnName.trim());
			}
			JSONArray json = new JSONArray();
			for(Document document : table.find()){
				JSONObject jobj = new JSONObject();
				for(Entry<String, Object> entry : document.entrySet()){
					if(columnList.contains(entry.getKey())){
						try {
							jobj.put(entry.getKey(), entry.getValue());
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
				if(jobj.length()>0){
					json.put(jobj);
				}
			}
			sqlResult.put(RESULT, json.toString());
		}
	}

	private static void excuteUpdateSQL(String sql, Map<String, String> sqlResult, MongoDatabase db) {
		String sqlUpper = sql.toUpperCase();
		if(sqlUpper.indexOf(" WHERE ")<0){
			sqlResult.put(ERROR_MASSAGE, "UPDATE语句缺少WHERE关键字!");
			return;
		}
		if(sqlUpper.indexOf(" SET ")<0){
			sqlResult.put(ERROR_MASSAGE, "UPDATE语句缺少SET关键字!");
			return;
		}
		String tableName = sqlUpper.replaceFirst(" SET .*", "");
		String updateValues = sqlUpper.replaceFirst(".* SET ", "").replaceFirst(" WHERE .*", "");
		String whereStr = sqlUpper.replaceFirst(".* WHERE ", "");
		if(whereStr==null || whereStr.isEmpty()){
			sqlResult.put(ERROR_MASSAGE, "UPDATE语句缺少执行条件!");
			return;
		}
		if(updateValues==null || updateValues.isEmpty() || !updateValues.contains("=")){
			sqlResult.put(ERROR_MASSAGE, "UPDATE语句缺少更新内容!");
			return;
		}
		if(tableName==null || tableName.isEmpty()){
			sqlResult.put(ERROR_MASSAGE, "UPDATE语句缺少表名!");
			return;
		}
		int valuestartIndex = sqlUpper.indexOf(" "+updateValues)+1;
		int valuesLength = updateValues.length();
		updateValues = sql.substring(valuestartIndex,valuesLength);
		Map<String, String> valuesMap = new HashMap<String,String>();
		for(String str : updateValues.split(",")){
			if(!str.contains("=")){
				sqlResult.put(ERROR_MASSAGE, "UPDATE语句"+str+"格式错误!");
				return;
			}
			String key = str.split("=")[0];
			String value = str.split("=")[1];
			valuesMap.put(key.trim(), value.trim());
		}
		
		int tableNameLength = tableName.length();
		tableName = sql.substring(0,tableNameLength);
		if(tableName.contains("(")){
		}
	}

	private static void excuteInsertSQL(String sql, Map<String, String> sqlResult, MongoDatabase db) {
		String sqlUpper = sql.toUpperCase();
		if(sqlUpper.indexOf(" VALUES ")<0){
			sqlResult.put(ERROR_MASSAGE, "INSERT语句缺少VALUES关键字!");
			return;
		}
		String tableName = sqlUpper.replaceFirst(" VALUES .*", "");
		String values = sqlUpper.replaceFirst(".* VALUES ", "");
		if(values==null || values.isEmpty() || !values.contains("(")){
			sqlResult.put(ERROR_MASSAGE, "INSERT语句缺少插入值!");
			return;
		}
		if(tableName==null || tableName.isEmpty()){
			sqlResult.put(ERROR_MASSAGE, "INSERT语句缺少表名!");
			return;
		}
		int valuestartIndex = sqlUpper.indexOf(" "+values)+1;
		int valuesLength = values.length();
		values = sql.substring(valuestartIndex,valuestartIndex+valuesLength).replaceAll("[(]", "").replaceAll("[)]", "");
		List<String> valuesList = new ArrayList<String>();
		for(String str : values.split(",")){
			valuesList.add(str.trim());
		}
		
		int tableNameLength = tableName.length();
		tableName = sql.substring(0,tableNameLength);
		List<String> columnsList = new ArrayList<String>();
		if(tableName.contains("(")){
			String columns = tableName.replaceFirst(tableName.substring(0,tableName.indexOf(" (")), "").replaceAll(" [(]", "").replaceAll("[)]", "");
			tableName = tableName.substring(0,tableName.indexOf(" ("));
			for(String str : columns.split(",")){
				columnsList.add(str.trim());
			}
		}
		
		if(columnsList.size()>0 && columnsList.size()!=valuesList.size()){
			sqlResult.put(ERROR_MASSAGE, "INSERT语句的插入字段与插入值数量不相符!");
			return;
		}
		MongoCollection<Document> table = db.getCollection(tableName);
		if(columnsList.size()==0){
			Document firstRow = table.find().first();
			if(firstRow==null||firstRow.isEmpty()){
				sqlResult.put(ERROR_MASSAGE, "数据库中没有数据时请使用指定列名的INSERT语句进行插入!");
				return;
			}
//			// 插入
//			contections.insertOne(new Document("name", "test123")
//					.append("sex", "男"));
			Document valueInsert = new Document();
			int index = 0;
			for(String key : firstRow.keySet()){
				if(valuesList.size()==index){
					break;
				}
				if("_id".equals(key)){
					continue;
				}
				Object value = valuesList.get(index++);
				if(value.toString().matches("'.+'")||value.toString().matches("\".+\"")){
					value = value.toString().substring(1, value.toString().length()-1);
					valueInsert.put(key, value);
				}else{
					try {
						if(value.toString().contains("[.]")){
							value = Double.parseDouble(value.toString());
						}else{
							value = Long.parseLong(value.toString());
						}
						valueInsert.put(key, value);
					} catch (Exception e) {
						sqlResult.put(ERROR_MASSAGE, "INSERT语句中"+value.toString()+"的格式不正确!");
						return;
					}
				}
			}
			table.insertOne(valueInsert);
		}else{
			Document valueInsert = new Document();
			int index = 0;
			for(String key : columnsList){
				Object value = valuesList.get(index++);
				if(value.toString().matches("'.+'")||value.toString().matches("\".+\"")){
					value = value.toString().substring(1, value.toString().length()-1);
					valueInsert.put(key, value);
				}else{
					try {
						if(value.toString().contains("[.]")){
							value = Double.parseDouble(value.toString());
						}else{
							value = Long.parseLong(value.toString());
						}
						valueInsert.put(key, value);
					} catch (Exception e) {
						sqlResult.put(ERROR_MASSAGE, "INSERT语句中"+value.toString()+"的格式不正确!");
						return;
					}
				}
			}
			table.insertOne(valueInsert);
		}
	}

	private static void excuteDeleteSQL(String sql, Map<String, String> sqlResult, MongoDatabase db) {
		// TODO 自动生成的方法存根
		
	}

	private static void excuteCreateSQL(String sql, Map<String, String> sqlResult, MongoClient monGoClient, MongoDatabase db) {
		// TODO 自动生成的方法存根
		
	}

	private static void excuteDropSQL(String sql, Map<String, String> sqlResult, MongoClient monGoClient, MongoDatabase db) {
		// TODO 自动生成的方法存根
		
	}
}
