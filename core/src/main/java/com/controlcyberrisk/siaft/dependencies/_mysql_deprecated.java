package com.controlcyberrisk.siaft.dependencies;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class _mysql_deprecated {
	public static Connection connect(String mysql_instance, String mysql_user, String mysql_pass, String mysql_db) {
		// connect to mysql server
				Connection connection = null;
				try {
					connection = DriverManager.getConnection("jdbc:mariadb://" + mysql_instance + "/" + mysql_db + "?user="
							+ mysql_user + "&password=" + mysql_pass);
					System.out.println("connected to database");
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
				return connection;
	}
	public static ResultSet _mysql_select(Connection conn, String select, String from, String where) {
		try {
			Statement q = conn.createStatement();
			System.out.println("SELECT "+select+" FROM "+from+" WHERE "+where+";");
			ResultSet result = q.executeQuery("SELECT "+select+" FROM "+from+" WHERE "+where+";");
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static ResultSet _mysql_insert(Connection conn, String into, String fields, String values) {
		try {
			Statement q = conn.createStatement();
//			System.out.println("INSERT INTO "+into+" ("+fields+") VALUES("+values+");");
			ResultSet result = q.executeQuery("INSERT INTO "+into+" ("+fields+") VALUES("+values+");");
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static ResultSet _mysql_update(Connection conn, String table, String[] fields, String[] values, String condition) {
		System.out.println("lengths= "+Integer.toString(fields.length)+", "+Integer.toString(values.length));
		StringBuilder _setlist = new StringBuilder();
		for(int i=0; i<fields.length-1;i++) {
			values[i] = values[i].replace("\'", "\\'");
			_setlist.append(fields[i])
			.append("='").append(values[i])
			.append("', ");
		}
			_setlist.append(fields[fields.length-1])
			.append("='").append(values[values.length-1].replace("\'", "\'\'"))
			.append("'");
		try {
			Statement q = conn.createStatement();
			System.out.println("UPDATE "+table+" SET "+_setlist.toString()+" WHERE "+condition+";");
			ResultSet result = q.executeQuery("UPDATE "+table+" SET "+_setlist+" WHERE "+condition+";");
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
