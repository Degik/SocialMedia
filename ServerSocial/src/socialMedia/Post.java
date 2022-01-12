package socialMedia;

import java.util.*;

public class Post {
	private int author;
	private int postId;
	private String title;
	private String text;
	private int quotes;
	private Set<Comment> comments = null;
	
	private static int nextId = 0;
	
	public Post(String title, String text) {
		if(title == null || text == null) {
			throw new NullPointerException();
		}
		this.title = title;
		this.text = text;
		quotes = 0;
		this.postId = nextId;
		nextId++;
		this.comments = new HashSet<>();
	}
	
	public int getPostId() {
		return postId;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getText() {
		return text;
	}
	
	public int getQuotes() {
		return quotes;
	}
	
	public Set<Comment> getComments(){
		return new HashSet<>(comments);
	}
	
	public boolean getComment(int commentId) {
		for(Comment c : comments) {
			if(c.getCommentId() == commentId) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == null) return false;
		if(this == o) return true;
		if(!(o instanceof Post)) return false;
		Post ps = (Post) o;
		return postId == ps.postId;
	}
	
	@Override
	public int hashCode() {
		return postId;
	}
}