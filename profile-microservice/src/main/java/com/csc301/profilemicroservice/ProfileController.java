package com.csc301.profilemicroservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.csc301.profilemicroservice.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/")
public class ProfileController {
	public static final String KEY_USER_NAME = "userName";
	public static final String KEY_USER_FULLNAME = "fullName";
	public static final String KEY_USER_PASSWORD = "password";

	@Autowired
	private final ProfileDriverImpl profileDriver;

	@Autowired
	private final PlaylistDriverImpl playlistDriver;

	OkHttpClient client = new OkHttpClient();

	public ProfileController(ProfileDriverImpl profileDriver, PlaylistDriverImpl playlistDriver) {
		this.profileDriver = profileDriver;
		this.playlistDriver = playlistDriver;
	}

	@RequestMapping(value = "/profile", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addProfile(@RequestParam Map<String, String> params,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("POST %s", Utils.getUrl(request)));

		String userName = params.get("userName");
		String fullName = params.get("fullName");
		String password = params.get("password");

		if(userName == null || fullName == null || password == null) {
			response.put("message", "Incomplete paramaters provided");
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
		}
		else {
			DbQueryStatus profile = profileDriver.createUserProfile(userName, fullName, password);
			response.put("message", profile.getMessage());
			response = Utils.setResponseStatus(response, profile.getdbQueryExecResult(), dbQueryStatus.getData());
		}

		return response;
	}

	@RequestMapping(value = "/followFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> followFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		
		String username = params.get("userName");
		String friendUsername = params.get("friendUserName");

		if(username == null || friendUsername == null) {
			response.put("message", "Incomplete paramaters provided");
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
		}
		else {
			DbQueryStatus follow = profileDriver.followProfile(userName, friendUserName);
			response.put("message", follow.getMessage());
			response = Utils.setResponseStatus(response, follow.getdbQueryExecResult(), dbQueryStatus.getData());
		}

		return response;
	}

	@RequestMapping(value = "/getAllFriendFavouriteSongTitles/{userName}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getAllFriendFavouriteSongTitles(@PathVariable("userName") String userName,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));

		if(userName == null) {
			response.put("message", "Incomplete paramaters provided");
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
		}
		else {
			DbQueryStatus songs = profileDriver.getAllSongFriendsLike(userName);
			
			if(songs.getData() != null) {
				HashMap<String, ArrayList<String>> friends = (HashMap<String, ArrayList<String>>) songs.getData();
				HashMap<String, ArrayList<String>> friendsSongTitles = new HashMap<String, ArrayList<String>>();
				
				for(Map.Entry<String, ArrayList<String>> friend: friends.entrySet()) {
					   ArrayList<String> songList = friend.getValue();
					   ArrayList<String> songTitles = new ArrayList<String>();
					   
					   for(String friendSongs: songList){
							HttpUrl.Builder urlBuilder = HttpUrl.parse("http://localhost:3001" + "/getSongTitleById").newBuilder();
							urlBuilder.addPathSegment(friendSongs);
							String url = urlBuilder.build().toString();
							Request songCheck = new Request.Builder().url(url).method("GET", null).build();	
							
							Call call = client.newCall(songCheck);
							
							Response responseFromSongMs = null;
							String friendSongBody;
						   
							try{
								responseFromSongMs = call.execute();
								friendSongBody = responseFromSongMs.body().string();
								System.out.println("responseFromSongMs: "+ friendSongBody);
								Map<String, Object> map =  mapper.readValue(friendSongBody, Map.class);
								
								if(map.containsKey("data")) { //see if song was deleted of not in mongo
									songTitles.add(map.get("data").toString());
								}
								else{
									songTitles.add("song deleted");
								}
							}
							catch (IOException e) {
							   e.printStackTrace();
							}	   
					   }
					   friendsSongTitles.put(friend.getKey(), songTitles);
				}
				songs.setData(friendsSongTitles);
				response = Utils.setResponseStatus(response, songs.getdbQueryExecResult(), songs.getData());
			}
			else {
				response.put("message", songs.getMessage());
				response = Utils.setResponseStatus(response, songs.getdbQueryExecResult(), songs.getData());
			}
		}
		return response;
	}


	@RequestMapping(value = "/unfollowFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> unfollowFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));

		String username = params.get("userName");
		String friendUsername = params.get("friendUserName");

		if(username == null || friendUsername == null) {
			response.put("message", "Incomplete paramaters provided");
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
		}
		else {
			DbQueryStatus unfollow = profileDriver.unfollowProfile(userName, friendUserName);
			response.put("message", unfollow.getMessage());
			response = Utils.setResponseStatus(response, unfollow.getdbQueryExecResult(), dbQueryStatus.getData());
		}

		return response;
	}

	@RequestMapping(value = "/likeSong/{userName}/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> likeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));

		if(songId == null || userName == null) {
			response.put("message", "Incomplete paramaters provided");
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
		} 
		else {
			HttpUrl.Builder urlBuilder = HttpUrl.parse("http://localhost:3001" + "/findSongById").newBuilder();
			urlBuilder.addPathSegment(songId);
			String url = urlBuilder.build().toString();
			RequestBody body = RequestBody.create(null, new byte[0]);
			Request songCheck = new Request.Builder().url(url).method("GET", null).build();	
			
			Call call = client.newCall(songCheck);
			Response responseFromSongMs = null;
			String songServiceBody;
			try{
				responseFromSongMs = call.execute();
				songServiceBody = responseFromSongMs.body().string();
				Map<String, Object> map =  mapper.readValue(songServiceBody, Map.class);
				if(map.get("data") == null) {
					response.put("message", "Song doesn't exist in mongo");
					response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_NOT_FOUND,null);
				
				}
				else {
					DbQueryStatus like = playlistDriver.likeSong(userName, songId);
					if(like.getMessage().equals("OK")) {
						urlBuilder = HttpUrl.parse("http://localhost:3001" + "/updateSongFavouritesCount").newBuilder();
						urlBuilder.addPathSegment(songId);
						urlBuilder.addQueryParameter("shouldDecrement", "false");
						url = urlBuilder.build().toString();
							
						Request likeSong = new Request.Builder().url(url).method("PUT", body).build();
						call = client.newCall(likeSong);
						responseFromSongMs = call.execute();
						response.put("message", "OK");
						response = Utils.setResponseStatus(response, like.getdbQueryExecResult(), null);
					}
					else {
						response.put("message", like.getMessage());
						response = Utils.setResponseStatus(response, like.getdbQueryExecResult(), null);
					}	
				}
			} 
			catch (IOException e) {
				e.printStackTrace();
			}	
		}
		return response;
	}

	@RequestMapping(value = "/unlikeSong/{userName}/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> unlikeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));

		return null;
	}

	@RequestMapping(value = "/deleteAllSongsFromDb/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> deleteAllSongsFromDb(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		
		if(songId == null) {
			response.put("message", "Incomplete paramaters provided");
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
		}
		else{
			DbQueryStatus delete = playlistDriver.deleteSongFromDb(songId);
			response.put("message", delete.getMessage());
			response = Utils.setResponseStatus(response, delete.getdbQueryExecResult(), null);
		}
	return response;
	}
}