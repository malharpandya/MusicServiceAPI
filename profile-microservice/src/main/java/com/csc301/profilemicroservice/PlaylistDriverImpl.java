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
		
		StatementResult result = null;
		StatementResult exists = null;	
		DbQueryStatus dbQueryStatus = null;
		
		try (Session session = driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				String userExistsQuery = "MATCH (pl:playlist {plName: \"" + userName + "-favourites\" }) RETURN pl";
				result = trans.run(userExistsQuery);
				
				if (!result.hasNext()) {
					dbQueryStatus = new DbQueryStatus("User not found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
					trans.failure();
				} else {
					String message = "added song to playlist";
					
					String existsQuery = "MATCH (pl:playlist {plName: \"" + userName + "-favourites\" }), (s:song {songId: \"" + songId + "\" }) \n" +
							"MATCH (pl)-[r:includes]-(s) \n" +
							"RETURN r";
					
					
					exists = trans.run(existsQuery);
					
					if(exists.hasNext()) {
					    message = "song already in playlist";
					    dbQueryStatus = new DbQueryStatus(message, DbQueryExecResult.QUERY_OK);
	                    trans.success();
	                    return dbQueryStatus;
					}
					
					String query = "MATCH (pl:playlist {plName: \"" + userName + "-favourites\" }) \n" +
							"MERGE (s:song {songId: \"" + songId + "\" }) \n" + 
							"MERGE (pl)-[r:includes]->(s)\n" +
							"RETURN r";
					
					trans.run(query);
					
					dbQueryStatus = new DbQueryStatus(message, DbQueryExecResult.QUERY_OK);
					trans.success();
				}
			}
			
			session.close();
		}
		
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus unlikeSong(String userName, String songId) {
		
		StatementResult exists = null;
		
		DbQueryStatus dbQueryStatus = null;
		
		try (Session session = driver.session()) {			
			try (Transaction trans = session.beginTransaction()) {				
				String existsQuery = "MATCH (p:profile {userName: \"" + userName + "\"}) RETURN p";
				
				exists = trans.run(existsQuery);
				if (exists.hasNext()) {
					String checkSong = "MATCH (pl:playlist {plName: \"" + userName + "-favourites\" }), (pl)-[r:includes]->(:song {songId: \"" + songId + "\" })\n" + "RETURN r";
					String query = "MATCH (pl:playlist {plName: \"" + userName + "-favourites\" }), (pl)-[r:includes]->(:song {songId: \"" + songId + "\" })\n" +
							"DELETE r";
					
					exists = trans.run(checkSong);
					
					if(!exists.hasNext()) {
					    dbQueryStatus = new DbQueryStatus("Song is not in playlist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
	                    trans.failure();
	                    return dbQueryStatus;
					}
					
					exists = trans.run(query);
					
					dbQueryStatus = new DbQueryStatus("Removed song from playlist", DbQueryExecResult.QUERY_OK);
					trans.success();
				} else {
					dbQueryStatus = new DbQueryStatus("no such user found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
					trans.failure();
				}
			}
			session.close();
		}
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus deleteSongFromDb(String songId) {
		
		
		DbQueryStatus dbQueryStatus = null;
		
		
		try (Session session = driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				
				String existsQuery = "MATCH (s:song {songId: \"" + songId + "\"}) RETURN s";
				trans.run(existsQuery);
				String query = "MATCH (s:song {songId: \"" + songId + "\"}) DETACH DELETE s";
				trans.run(query);

				dbQueryStatus = new DbQueryStatus("Deleted song from database", DbQueryExecResult.QUERY_OK);
				trans.success();
			}
			session.close();
		}
		return dbQueryStatus;
	}
}
