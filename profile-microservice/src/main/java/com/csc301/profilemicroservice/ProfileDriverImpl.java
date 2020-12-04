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
		DbQueryStatus profileObj = new DbQueryStatus("", DbQueryExecResult.QUERY_OK);
		try(Session session = ProfileMicroserviceApplication.driver.session()) {
			String query = "MATCH (p:profile) WHERE p.userName = $userName RETURN p.userName";
			StatementResult result = session.run(query, parameters("userName", userName));
			
			if(result.hasNext()) {
				profileObj.setMessage("Profile already exists");
				profileObj.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
			}
			else {
				Map<String,Object> params = new HashMap<>();
				params.put("userName", userName);
				params.put("fullName", fullName);
				params.put("password", password);
				params.put("plName", userName+"-favourites");
				query = "CREATE (nProfile:profile {userName: $userName, fullName: $fullName, password: $password})" + "-[:created]-> (nPlaylist:playlist {plName: $plName})";
				session.run(query, params);
				profileObj.setMessage("OK");
				profileObj.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
			}		
		}
		return profileObj;
	}

	@Override
	public DbQueryStatus followFriend(String userName, String frndUserName) {
		DbQueryStatus friend = new DbQueryStatus("". DbQueryExecResult.QUERY_OK);
		try(Session session = ProfileMicroserviceApplication.driver.session()) {
			Map<String,Object> params = new HashMap<>();
			params.put("userName", userName);
			params.put("frndUserName", frndUserName);
			String query = "MATCH (uProfile:profile),(fProfile:profile) " + "WHERE uProfile.userName = $userName AND fProfile.userName = $frndUserName" + " RETURN uProfile.userName";
			StatementResult result = session.run(query, params);
			
			if(result.hasNext()) { 
				result = session.run("MATCH (uProfile:profile),(fProfile:profile) " + "WHERE uProfile.userName = $userName AND fProfile.userName = $frndUserName" + " RETURN EXISTS ((uProfile)-[:follows]->(fProfile))", params);
				
				if(result.hasNext()) {
					Record record = result.next();
					if(record.get(0).toString().equals("FALSE")){ 
						session.run("MATCH (uProfile:profile),(fProfile:profile) " + "WHERE uProfile.userName = $userName AND fProfile.userName = $frndUserName" + " MERGE (uProfile)-[:follows]->(fProfile)", params);
						friend.setMessage("OK");
						friend.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
					}
					else {
						friend.setMessage("Already following.");
						friend.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
					}
				}
			}
			else {
				friend.setMessage("Relationship does not exist.");
				friend.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}	
		}
		return friend;
	}

	@Override
	public DbQueryStatus unfollowFriend(String userName, String frndUserName) {
		DbQueryStatus unfriend = new DbQueryStatus("". DbQueryExecResult.QUERY_OK);
		try(Session session = ProfileMicroserviceApplication.driver.session()) {
			Map<String,Object> params = new HashMap<>();
			params.put("userName", userName);
			params.put("frndUserName", frndUserName);
			String query = "MATCH (uProfile:profile),(fProfile:profile) " + "WHERE uProfile.userName = $userName AND fProfile.userName = $frndUserName" + " RETURN uProfile.userName";
			
			StatementResult result = session.run(query, params);
			
			if(result.hasNext()) { 
				result = session.run("MATCH (uProfile:profile)-[f:follows]->(fProfile:profile) " + "WHERE uProfile.userName = $userName AND fProfile.userName = $frndUserName" + " RETURN EXISTS ((uProfile)-[:follows]->(fProfile))", params);
				
				if(result.hasNext()) {
					Record record = result.next();
					if(record.get(0).toString().equals("FALSE")){ 
						unfriend.setMessage("Already Unfollowed");
						unfriend.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
					}
					else {
						session.run("MATCH (uProfile:profile)-[f:follows]->(fProfile:profile) " + "WHERE uProfile.userName = $userName AND fProfile.userName = $frndUserName" + " DELETE f", params);
						unfriend.setMessage("OK");
						unfriend.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
					}
				}
			}
			else {
				unfriend.setMessage("Relationship does not exist.");
				unfriend.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}	
		}
		return unfriend;
	}

	@Override
	public DbQueryStatus getAllSongFriendsLike(String userName) {
		DbQueryStatus songs = new DbQueryStatus("". DbQueryExecResult.QUERY_OK);
		
		try(Session session = ProfileMicroserviceApplication.driver.session()){
			String query =  "MATCH (p:profile) WHERE p.userName = $userName RETURN p.userName";
			StatementResult result = session.run(query, parameters("userName", userName));

			if(result.hasNext()) {
				query = "MATCH (p:profile)-[f:follows]->(friends:profile) WHERE p.userName = $userName RETURN friends.userName";
				result = session.run(query,parameters("userName", userName));
				
				if(result.hasNext()) {
					while (result.hasNext()) {
						Record friend = result.next();
						String fUserName = friend.get(0).toString().replace("\"", "");
						
						StatementResult playlistSongs = session.run("MATCH (p:profile)-[:created]->(pl:playlist)-[:includes]->(s:song)" + " WHERE p.userName = $userName RETURN s.songId", parameters( "userName", fUserName));
					;
						if(playlistSongs.hasNext()){
							ArrayList<String> songsList = new ArrayList<String>();
							while (playlistSongs.hasNext()) {
								Record songId = playlistSongs.next();
								songsList.add(songId.get(0).toString());
								
							}
							friendsSongs.put(fUserName, songsList);
							
							
						}
						else {
							friendsSongs.put(fUserName, null);
						}
						
					}
					songsQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
					songsQuery.setMessage("OK");
					songsQuery.setData(friendsSongs);
				}
				else {
					songs.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
					songs.setMessage("User does not follow anyone");
				}
			}
			else {
				songs.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				songs.setMessage("User does not exist");
			}	
		}
		return songs;
	}
}
