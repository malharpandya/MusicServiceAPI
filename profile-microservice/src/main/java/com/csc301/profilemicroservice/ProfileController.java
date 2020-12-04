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

		if (userName == null || fullName == null || password == null) {
			response.put("message", "Incomplete paramaters provided");
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
		}
		else {
			DbQueryStatus profile = profileDriver.createUserProfile(userName, fullName, password);
			response.put("message", profile.getMessage());
			response = Utils.setResponseStatus(response, profile.getdbQueryExecResult(), null);
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

		if (username == null || friendUsername == null) {
			response.put("message", "Incomplete paramaters provided");
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
		}
		else {
			DbQueryStatus follow = profileDriver.followProfile(userName, friendUserName);
			response.put("message", follow.getMessage());
			response = Utils.setResponseStatus(response, follow.getdbQueryExecResult(), null);
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
			

			response.put("message", songs.getMessage());
			response = Utils.setResponseStatus(response, songs.getdbQueryExecResult(), songs.getData());
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

		if (username == null || friendUsername == null) {
			response.put("message", "Incomplete paramaters provided");
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
		}
		else {
			DbQueryStatus unfollow = profileDriver.unfollowProfile(userName, friendUserName);
			response.put("message", unfollow.getMessage());
			response = Utils.setResponseStatus(response, unfollow.getdbQueryExecResult(), null);
		}

		return response;
	}

	@RequestMapping(value = "/likeSong/{userName}/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> likeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));

		return null;
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
		else {
			DbQueryStatus delete = playlistDriver.deleteSongFromDb(songId);
			response.put("message", delete.getMessage());
			response = Utils.setResponseStatus(response, delete.getdbQueryExecResult(), null);
		}
	return response;
	}
}