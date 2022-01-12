package socialMedia;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


public class User {
	
	/*
	 * Il modo in cui voglio strutturare User
	 * User:
	 * 	username
	 * 	password
	 * 	post
	 * 	followers
	 */
	
	private String username;
	private String password;
	private int userId;
	
	//@JsonProperty("posts")
	private ArrayList<Post> posts = null;
	
	//@JsonProperty("followers")
	private ArrayList<Integer> followers = null;
	
	//@JsonProperty("tags")
	private ArrayList<String> tags = null;
	
	private static int nextId = 0;
	
	public User() { 
		/*
		 * Jackson neccesita' di un costruttore vuoto o del 
		 * @JsonProperty("example") 
		 * per riuscire a costruire il modello
		 */
	}
	
	public User(String username, String password, LinkedList<String> tags) {
		if(username == null || password == null || tags == null) {
			throw new NullPointerException();
		}
		this.username = username;
		this.password = password;
		posts = new ArrayList<Post>();
		followers = new ArrayList<Integer>();
		this.tags = new ArrayList<String>(tags);
		userId = nextId; // userId
		nextId++;		 // aumento con nextId
	}
	
	public User(String username, String password, // username, password, posts, followers, userId
			ArrayList<Post> posts, ArrayList<Integer> followers, ArrayList<String> tags, int userId) {
		//
		if(username == null || password == null) {
			throw new NullPointerException();
		}
		this.username = username;
		this.password = password;
		this.posts = posts;
		this.followers = followers;
		this.tags = tags;
		this.userId = userId;
	}
	
	public User(User u) {
		if(u == null) {
			throw new NullPointerException();
		}
	}
	
	public int getUserId() {
		return userId;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public ArrayList<Post> getPosts() {
		return new ArrayList<>(posts);
	}
	
	public Post getPost(int postId) {
		for(Post p : posts) {
			if(p.getPostId() == postId) {
				return p;
			}
		}
		return null;
	}
	
	public ArrayList<Integer> getFollowers(){
		return new ArrayList<>(followers);
	}
	
	public ArrayList<String> getTags(){
		return new ArrayList<>(tags);
	}
	
	public boolean getFollower(int userId) {
		for(Integer fl : followers) {
			if(fl == userId) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == null) return false;
		if(this == o) return true;
		if(!(o instanceof User)) return false;
		User us = (User) o;
		return userId == us.userId;
	}
	
	@Override
	public int hashCode() {
		return userId;
	}
	
}
