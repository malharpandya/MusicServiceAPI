package com.csc301.profilemicroservice;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;

@Repository
public class PlaylistDriverImpl implements PlaylistDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitPlaylistDb() {
		String queryStr;

		try(Session session = ProfileMicroserviceApplication.driver.session()) {
			try(Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nPlaylist:playlist) ASSERT exists(nPlaylist.plName)";
				trans.run(queryStr);
				trans.success();
			}
			session.close();
		}
	}

	@Override
	public DbQueryStatus likeSong(String userName, String songId) {
		DbQueryStatus likesong = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_GENERIC);
		
		try(Session session = ProfileMicroserviceApplication.driver.session()){
			Map<String,Object> params = new HashMap<>();
			params.put("userName", userName);
			params.put("playlistName", userName+"-favourites");
			StatementResult result = session.run( "MATCH (u:profile)-[:created]->(p:playlist)" + " WHERE u.userName = $userName AND p.plName = $playlistName" + " RETURN u.userName", params);
			
			if(result.hasNext()) {
				result = session.run("MERGE (s:song {songId: $songId})", parameters( "songId", songId));
				params.put("songId", songId);

				result = session.run("MATCH (p:playlist),(s:song)" + " WHERE p.plName = $playlistName AND s.songId = $songId" + " RETURN EXISTS ( (p)-[:includes]->(s))", params);
				
				if(result.hasNext()) {
					Record exists = result.next();
					System.out.println(exists.get(0).toString());
					
					if(exists.get(0).toString().equals("FALSE")) {
						session.run("MATCH (p:playlist),(s:song)"
						+ " WHERE p.plName = $playlistName AND s.songId = $songId" + " MERGE (p)-[:includes]->(s)", params);
						likesong.setMessage("You've successfully liked this song");
						likesong.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
					} 
					else {
						likesong.setMessage("OK"); 
						likesong.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
					}				
				}
			} 
			else {
				likesong.setMessage("The playlist does not exist in mongo");
				likesong.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
		}
		return likesong;
	}

	@Override
	public DbQueryStatus unlikeSong(String userName, String songId) {
		DbQueryStatus unlikesong = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_GENERIC);
		try (Session session = ProfileMicroserviceApplication.driver.session()){
			Map<String,Object> params = new HashMap<>();
			params.put("userName", userName);
			params.put("playlistName",userName+"-favourites");
			StatementResult result = session.run("MATCH (u:profile)-[:created]->(p:playlist)" + " WHERE u.userName = $userName AND p.plName = $playlistName" + " RETURN u.userName", params);
			
			if(result.hasNext()) {
				params.put("songId", songId);
				result = session.run("MATCH (p:playlist),(s:song)" + " WHERE p.plName = $playlistName AND s.songId = $songId" + " RETURN EXISTS ( (p)-[:includes]->(s))", params);
				if(result.hasNext()) {
					Record exists = result.next();
					if(exists.get(0).toString().equals("FALSE")) {
						unlikesong.setMessage("You've already unliked this song");
						unlikesong.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
					}
					else {
						session.run("MATCH (p:playlist)-[i:includes]->(s:song) " + "WHERE p.plName = $playlistName AND s.songId = $songId" + " DELETE i", params);
						unlikesong.setMessage("OK");
						unlikesong.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
					}
				}
			} 
			else {
				unlikesong.setMessage("The playlist does not exist in the mongo");
				unlikesong.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
		}
		return unlikesong;
	}

	@Override
	public DbQueryStatus deleteSongFromDb(String songId) {
		DbQueryStatus deleteSong = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_GENERIC);

		try(Session session = ProfileMicroserviceApplication.driver.session()){
			String query = "MATCH (s:song) WHERE s.songId = $songId RETURN s.songId";
			StatementResult result = session.run(query,parameters( "songId", songId));

			if(result.hasNext()) {
				result = session.run("MATCH (s:song) WHERE s.songId = $songId" + " RETURN EXISTS ((:playlist)-[:includes]->(s))", parameters( "songId", songId));

				if(result.hasNext()) {
					Record record = result.next();
					
					if(record.get(0).toString().equals("FALSE")) {
						deleteSong.setMessage("Song does not exist in playlist.");
						deleteSong.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
					} 
					else {
						session.run("MATCH (s:song) WHERE s.songId = $songId DETACH DELETE s", parameters( "songId", songId));
						deleteSong.setMessage("OK");
						deleteSong.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
					}
				}
			}
			else {
				deleteSong.setMessage("Song doesn't exist in neo4j");
				deleteSong.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
		}	
		return deleteSong;
	}
}
