package com.csc301.profilemicroservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;

@Repository
public class ProfileDriverImpl implements ProfileDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitProfileDb() {
		String queryStr;

		try(Session session = ProfileMicroserviceApplication.driver.session()) {
			try(Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.userName)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.password)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT nProfile.userName IS UNIQUE";
				trans.run(queryStr);

				trans.success();
			}
			session.close();
		}
	}
	
	@Override
	public DbQueryStatus createUserProfile(String userName, String fullName, String password) {
		
		StatementResult exists = null;
		DbQueryStatus dbQueryStatus = null;
		
		try (Session session = driver.session()) {			
			try (Transaction trans = session.beginTransaction()) {
				String existsQuery = "MATCH (p:profile {userName: \"" + userName + "\"}) RETURN p";
				exists = trans.run(existsQuery);
				
				if (exists.hasNext()) {
					dbQueryStatus = new DbQueryStatus("User Profile already exists", DbQueryExecResult.QUERY_ERROR_GENERIC);
					trans.failure();
					return dbQueryStatus;
				}
				
				String query = "MERGE (a:profile {userName: \"" + userName + "\", fullName: \"" + fullName + "\", password: \"" + password + "\" })\n" +
						"MERGE (b:playlist {plName: \"" + userName + "-favourites\" })\n" +
						"CREATE (a)-[:created]->(b)\n" +
								"RETURN a,b ";
				trans.run(query);
				dbQueryStatus = new DbQueryStatus("Profile created", DbQueryExecResult.QUERY_OK);
				trans.success();
			}
			session.close();
		}
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus followFriend(String userName, String frndUserName) {
		
		StatementResult exists = null;
		DbQueryStatus dbQueryStatus = null;
		
		try (Session session = driver.session()) {			
			try (Transaction trans = session.beginTransaction()) {
				String checkUser = "MATCH (p:profile {userName: \"" + userName + "\"}) RETURN p";
				 String checkfriend = "MATCH (p:profile {userName: \"" + frndUserName + "\"}) RETURN p";
				exists = trans.run(checkUser);
				List<Record> recordUser = exists.list();
				exists = trans.run(checkfriend);
				List<Record> recordFriend = exists.list();
				
				if (recordUser.isEmpty() || recordFriend.isEmpty()) {
                    dbQueryStatus = new DbQueryStatus("User/Friend does not exist", DbQueryExecResult.QUERY_ERROR_GENERIC);
                    trans.failure();
                    return dbQueryStatus;
                }
				
				String existsQuery = "MATCH (a:profile), (b:profile) WHERE a.userName = \"" + userName +  "\" AND b.userName = \"" + frndUserName + "\" \n" +
						"MATCH (a)-[f:follows]->(b) \n" + "RETURN f";
				
				exists = trans.run(existsQuery);
				
				if (exists.hasNext()) {
					dbQueryStatus = new DbQueryStatus(userName + " is already following " + frndUserName, DbQueryExecResult.QUERY_ERROR_GENERIC);
					trans.failure();
					return dbQueryStatus;
				}
				
				String query = "MATCH (a:profile), (b:profile) WHERE a.userName = \"" + userName +  "\" AND b.userName = \"" + frndUserName + "\" \n" +
						"CREATE (a)-[:follows]->(b) \n" + "RETURN a,b";
				
				trans.run(query);
				dbQueryStatus = new DbQueryStatus(userName + " is now following " + frndUserName, DbQueryExecResult.QUERY_OK);
				trans.success();
			}
			session.close();
		}
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus unfollowFriend(String userName, String frndUserName) {
		
		StatementResult exists = null;
		DbQueryStatus dbQueryStatus = null;
		
		try (Session session = driver.session()) {			
			try (Transaction trans = session.beginTransaction()) {
				String existsQuery = "MATCH (a:profile), (b:profile) WHERE a.userName = \"" + userName +  "\" AND b.userName = \"" + frndUserName + "\" \n" +
						"MATCH (a)-[f:follows]->(b) \n" + "RETURN f";
				exists = trans.run(existsQuery);
				if (exists.hasNext()) {
					
					String deleteQuery = "MATCH (a:profile), (b:profile) WHERE a.userName = \"" + userName +  "\" AND b.userName = \"" + frndUserName + "\" \n" +
							"MATCH (a)-[f:follows]->(b) \n" + "DELETE f";
					
					trans.run(deleteQuery);
					dbQueryStatus = new DbQueryStatus("Friend unfollowed", DbQueryExecResult.QUERY_OK);
					trans.success();
				} else {
					dbQueryStatus = new DbQueryStatus("Not followinig friend", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
					trans.failure();
				}
			}
			session.close();
		}
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus getAllSongFriendsLike(String userName) {
		
		StatementResult exists = null;
		DbQueryStatus dbQueryStatus = null;
		StatementResult allFriends = null;
		
		try (Session session = driver.session()) {			
			try (Transaction trans = session.beginTransaction()) {
				String existsQuery = "MATCH (p:profile {userName: \"" + userName + "\"}) RETURN p";
				exists = trans.run(existsQuery);
				if(!exists.hasNext()) {
                    trans.failure();
                    session.close();
                    return new DbQueryStatus("User does not exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
                }
				
				String allFriendsQuery = "MATCH (p:profile),(nProfile:profile) WHERE p.userName = \"" + userName + "\" \n" +
						"AND (p)-[:follows]->(nProfile) \n" +
						"RETURN nProfile";
				
				allFriends = trans.run(allFriendsQuery);
				Map<String, List<String>> allSongsFriendsLike = new HashMap<String, List<String>>();
				List<Record> allFriendsRecords = allFriends.list();
				List<String> allFriendsNames = new ArrayList<String>();
				StatementResult songsResult;
				for (Record record : allFriendsRecords) {
					allFriendsNames.add(record.get(0).get("userName").toString());	
				}
				
				for (String name : allFriendsNames) {
					String playlistName = name.substring(1, name.length()-1) + "-favourites";
					String getSongsQuery = "MATCH (p:profile {userName: " + name + " }), (pl:playlist {plName: \"" + playlistName + "\" }) \n" +
							"MATCH (pl)-[:includes]-(s:song) \n" +
							"RETURN s";
					songsResult = trans.run(getSongsQuery);
					List<Record> songsResultRecords = songsResult.list();
					List<String> songList = new ArrayList<String>();
					
					for (Record songsRecord : songsResultRecords) {					    
						songList.add(songsRecord.get(0).get("songId").toString().replace("\"", ""));						
					}
					allSongsFriendsLike.put(name.replace("\"", ""), songList);
				}
				dbQueryStatus = new DbQueryStatus("Retrieved playlists of friends", DbQueryExecResult.QUERY_OK);
				dbQueryStatus.setData(allSongsFriendsLike);
			}
			session.close();
		}
		return dbQueryStatus;
	}
}
