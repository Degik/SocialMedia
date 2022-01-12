package socialMedia;

import java.util.*;

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
	private Set<Post> posts = null;
	private Set<Integer> followers = null;
	private LinkedList<String> tags = null;
	
	private static int nextId = 0;
	
	public User(String username, String password, LinkedList<String> tags) {
		if(username == null || password == null || tags == null) {
			throw new NullPointerException();
		}
		this.username = username;
		this.password = password;
		posts = new HashSet<Post>();
		followers = new HashSet<Integer>();
		this.tags = tags;
		userId = nextId; // userId
		nextId++;		 // aumento con nextId
	}
	
	public User(String username, String password, // username, password, posts, followers, userId
			Set<Post> posts, Set<Integer> followers, int userId) {
		//
		if(username == null || password == null) {
			throw new NullPointerException();
		}
		this.username = username;
		this.password = password;
		this.posts = posts;
		this.followers = followers;
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
	
	public Set<Post> getPosts() {
		return new HashSet<>(posts);
	}
	
	public Post getPost(int postId) {
		for(Post p : posts) {
			if(p.getPostId() == postId) {
				return p;
			}
		}
		return null;
	}
	
	public Set<Integer> getFollowers(){
		return new HashSet<>(followers);
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
