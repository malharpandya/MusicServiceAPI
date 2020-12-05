package com.csc301.songmicroservice;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class SongDalImpl implements SongDal {

	private final MongoTemplate db;

	@Autowired
	public SongDalImpl(MongoTemplate mongoTemplate) {
		this.db = mongoTemplate;
	}

	@Override
	public DbQueryStatus addSong(Song songToAdd) {
		// TODO Auto-generated method stub
		try {
			db.insert(songToAdd);
			DbQueryStatus dbQueryStatus = new DbQueryStatus("added song", DbQueryExecResult.QUERY_OK);
			dbQueryStatus.setData(songToAdd);
			return dbQueryStatus;
		} catch (Exception e) {
			DbQueryStatus dbQueryStatus = new DbQueryStatus("internal error", DbQueryExecResult.QUERY_ERROR_GENERIC);
			return dbQueryStatus;
		}
	}

	@Override
	public DbQueryStatus findSongById(String songId) {
		// TODO Auto-generated method stub
		try {
			ObjectId song_id = new ObjectId(songId);
			Song song = db.findById(song_id, Song.class);
			if (song == null) {
				DbQueryStatus dbQueryStatus = new DbQueryStatus("song not found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return dbQueryStatus;
			} else {
				DbQueryStatus dbQueryStatus = new DbQueryStatus("found song", DbQueryExecResult.QUERY_OK);
				dbQueryStatus.setData(song);
				return dbQueryStatus;
			}
		} catch (Exception e) {
			DbQueryStatus dbQueryStatus = new DbQueryStatus("internal error", DbQueryExecResult.QUERY_ERROR_GENERIC);
			return dbQueryStatus;
		}
	}

	@Override
	public DbQueryStatus getSongTitleById(String songId) {
		// TODO Auto-generated method stub
		try {
			ObjectId song_id = new ObjectId(songId);
			Song song = db.findById(song_id, Song.class);
			if (song == null) {
				DbQueryStatus dbQueryStatus = new DbQueryStatus("song not found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return dbQueryStatus;
			} else {
				DbQueryStatus dbQueryStatus = new DbQueryStatus("found song", DbQueryExecResult.QUERY_OK);
				dbQueryStatus.setData(song.getSongName());
				return dbQueryStatus;
			}
		} catch (Exception e) {
			DbQueryStatus dbQueryStatus = new DbQueryStatus("internal error", DbQueryExecResult.QUERY_ERROR_GENERIC);
			return dbQueryStatus;
		}
	}

	@Override
	public DbQueryStatus deleteSongById(String songId) {
		// TODO Auto-generated method stub
		try {
			ObjectId song_id = new ObjectId(songId);
			Song song = db.findById(song_id, Song.class);
			if (song == null) {
				DbQueryStatus dbQueryStatus = new DbQueryStatus("song not found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return dbQueryStatus;
			} else {
				db.remove(song);
				DbQueryStatus dbQueryStatus = new DbQueryStatus("deleted song", DbQueryExecResult.QUERY_OK);
				return dbQueryStatus;
			}
		} catch (Exception e) {
			DbQueryStatus dbQueryStatus = new DbQueryStatus("internal error", DbQueryExecResult.QUERY_ERROR_GENERIC);
			return dbQueryStatus;
		}
	}

	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
		// TODO Auto-generated method stub
		try {
			ObjectId song_id = new ObjectId(songId);
			Query query = new Query().addCriteria(Criteria.where("_id").is(song_id));
			Song song = db.findById(song_id, Song.class);
			if (song == null) {
				DbQueryStatus dbQueryStatus = new DbQueryStatus("song not found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return dbQueryStatus;
			} else {
				if (song.getSongAmountFavourites() == 0 && shouldDecrement) {
					return new DbQueryStatus("Cannot have negative counter", DbQueryExecResult.QUERY_ERROR_GENERIC);
				}
				if (shouldDecrement) {
					Update update = new Update().set("songAmountFavourites", song.getSongAmountFavourites() - 1);
					db.findAndModify(query, update, Song.class);
				} else {
					Update update = new Update().set("songAmountFavourites", song.getSongAmountFavourites() + 1);
					db.findAndModify(query, update, Song.class);
				}
				DbQueryStatus dbQueryStatus = new DbQueryStatus("updated favourites amount", DbQueryExecResult.QUERY_OK);
				return dbQueryStatus;
				
			}
		} catch (Exception e) {
			DbQueryStatus dbQueryStatus = new DbQueryStatus("internal error", DbQueryExecResult.QUERY_ERROR_GENERIC);
			return dbQueryStatus;
		}
	}
}