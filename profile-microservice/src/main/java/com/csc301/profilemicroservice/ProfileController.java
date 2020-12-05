package com.csc301.profilemicroservice;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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
			DbQueryStatus dbQueryStatus = profileDriver.createUserProfile(userName, fullName, password);
			response.put("message", dbQueryStatus.getMessage());
			response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		}

		return response;
	}

	@RequestMapping(value = "/followFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> followFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));

		if(userName == null || friendUserName == null) {
			response.put("message", "Incomplete paramaters provided");
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
		} else if (userName.equals(friendUserName)) {
			response.put("message", "Cannot follow yourself");
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
		} else {
			DbQueryStatus dbQueryStatus = profileDriver.followFriend(userName, friendUserName);
			response.put("message", dbQueryStatus.getMessage());
			response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
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
			DbQueryStatus dbQueryStatus = profileDriver.getAllSongFriendsLike(userName);
			
			if(!dbQueryStatus.getdbQueryExecResult().equals(DbQueryExecResult.QUERY_ERROR_NOT_FOUND)) {
				@SuppressWarnings("unchecked")
				Map<String, List<String>> friendsSongs = (Map<String, List<String>>) dbQueryStatus.getData();
				for (String name : friendsSongs.keySet()) {
					List<String> songName = new ArrayList<String>();
					
					for (String songId : friendsSongs.get(name)) {
						Request sentRequest = new Request.Builder().url("http://localhost:3001/getSongTitleById/"+songId).build();
				        
				        try (Response getResponse = this.client.newCall(sentRequest).execute()){
				        	
				        	String requestBody = getResponse.body().string();
				        	JSONObject requestJson = new JSONObject(requestBody);
							 if (! requestJson.get("status").equals("OK")) {
								 response.put("message", "internal server error");
								 response.put("status", HttpStatus.INTERNAL_SERVER_ERROR);
								 return response;
							 }
							 songName.add((String) requestJson.get("data"));
						} catch (Exception e) {
							response.put("message", "error communicating with song microservice");
							response.put("status", HttpStatus.INTERNAL_SERVER_ERROR);
							return response;
						}
					}
					friendsSongs.put(name, songName);
				}
				response.put("message", dbQueryStatus.getMessage());
				response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), friendsSongs);
			}
			else {
				response.put("message", dbQueryStatus.getMessage());
				response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
			}
		}
		return response;
	}


	@RequestMapping(value = "/unfollowFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> unfollowFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));


		if(userName == null || friendUserName == null) {
			response.put("message", "Incomplete paramaters provided");
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
		}
		else {
			DbQueryStatus dbQueryStatus = profileDriver.unfollowFriend(userName, friendUserName);
			response.put("message", dbQueryStatus.getMessage());
			response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
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
			return response;
		} 
		else {
			
			Request getRequest = new Request.Builder().url("http://localhost:3001/getSongById/"+songId).build();
			try (Response getResponse = this.client.newCall(getRequest).execute()){
				String requestBody = getResponse.body().string();
	        	JSONObject requestJson = new JSONObject(requestBody);
				 if (! requestJson.get("status").equals("OK")) {
					 response.put("message", "song not found");
					 response.put("status", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
					 return response;
				 }
			} catch (Exception e) {
				response.put("message", "error communicating with song microservice");
				response.put("status", HttpStatus.INTERNAL_SERVER_ERROR);
				return response;
			}
			
			DbQueryStatus dbQueryStatus = playlistDriver.likeSong(userName, songId);
			if(!dbQueryStatus.getMessage().equals("song already in playlist")) {
				
				Request sendRequest = new Request.Builder().url("http://localhost:3001/updateSongFavouritesCount/"+songId+"?shouldDecrement=false").put(new FormBody.Builder().build()).build();
				try (Response sentReq = this.client.newCall(sendRequest).execute()){
			        
			        String reqBody = sentReq.body().string();
			        JSONObject reqJson = new JSONObject(reqBody);
			        if(!reqJson.get("status").equals("OK")) {
		                response.put("message", "couldn't update song count");
		                response.put("status", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		                return response;
		            }
				} catch (Exception e) {
					response.put("message", "error communicating with song microservice");
					response.put("status", HttpStatus.INTERNAL_SERVER_ERROR);
					return response;
				}
			}
			response.put("message", dbQueryStatus.getMessage());
			response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
			return response;
		}
	}

	@RequestMapping(value = "/unlikeSong/{userName}/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> unlikeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));

		if(songId == null || userName == null) {
			response.put("message", "Incomplete paramaters provided");
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
			return response;
		} 
		else {
			Request getRequest = new Request.Builder().url("http://localhost:3001/getSongById/"+songId).build();
			try (Response getResponse = this.client.newCall(getRequest).execute()){
				String requestBody = getResponse.body().string();
	        	JSONObject requestJson = new JSONObject(requestBody);
				 if (! requestJson.get("status").equals("OK")) {
					 response.put("message", "song not found");
					 response.put("status", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
					 return response;
				 }
			} catch (Exception e) {
				response.put("message", "error communicating with song microservice");
				response.put("status", HttpStatus.INTERNAL_SERVER_ERROR);
				return response;
			}
			
			DbQueryStatus dbQueryStatus = playlistDriver.unlikeSong(userName, songId);
			if(dbQueryStatus.getdbQueryExecResult().equals(DbQueryExecResult.QUERY_OK)) {
				Request sendRequest = new Request.Builder().url("http://localhost:3001/updateSongFavouritesCount/"+songId+"?shouldDecrement=true").put(new FormBody.Builder().build()).build();
				try (Response sentReq = this.client.newCall(sendRequest).execute()){
			        
			        String reqBody = sentReq.body().string();
			        JSONObject reqJson = new JSONObject(reqBody);
			        if(!reqJson.get("status").equals("OK")) {
		                response.put("message", "couldn't update song count");
		                response.put("status", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		                return response;
		            }
				} catch (Exception e) {
					response.put("message", "error communicating with song microservice");
					response.put("status", HttpStatus.INTERNAL_SERVER_ERROR);
					return response;
				}
			}
			
			response.put("message", dbQueryStatus.getMessage());
			response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
			return response;
		}
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