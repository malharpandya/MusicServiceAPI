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

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nPlaylist:playlist) ASSERT exists(nPlaylist.plName)";
				trans.run(queryStr);
				trans.success();
			}
			session.close();
		}
	}

	@Override
	public DbQueryStatus likeSong(String userName, String songId) {

		return null;
	}

	@Override
	public DbQueryStatus unlikeSong(String userName, String songId) {
		
		return null;
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
					
					if (record.get(0).toString().equals("FALSE")) {
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
