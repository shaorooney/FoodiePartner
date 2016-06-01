package db;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;

import java.text.ParseException;

public class MongoDBImport {
	public static void main(String[] args) throws ParseException {
   	 MongoClient mongoClient = new MongoClient(); //open connection
   	 MongoDatabase db = mongoClient.getDatabase(DBUtil.DB_NAME); //use laiproject database
   	 db.getCollection("users").insertOne(
   			 new Document() //insert a document into mongodb
   					 .append("first_name", "John")
   					 .append("last_name", "Smith")
   					 .append("password", "3229c1097c00d497a0fd282d586be050")
   					 .append("user_id", "1111"));
   	 mongoClient.close();
    }
}