package socialMedia;

import java.io.Serializable;
import java.util.*;

public class Post implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String author;
	private int postId;
	private String title;
	private String text;
	private ArrayList<String> goodQuotes = null;
	private ArrayList<String> badQuotes = null;
	private ArrayList<Comment> comments = null;
	private int goodQuotesSize;
	private int badQuotesSize;
	
	public Post() {
		
	}
	
	public Post(String title, String text, String author, int postId) {
		if(title == null || text == null || author == null) {
			throw new NullPointerException();
		}
		this.title = title;
		this.text = text;
		this.author = author;
		goodQuotes = new ArrayList<String>();
		badQuotes = new ArrayList<String>();
		this.postId = postId;
		this.comments = new ArrayList<>();
	}
	
	public String getAuthor() {
		return author;
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
	
	public ArrayList<String> getGoodQuotes(){
		return new ArrayList<String>(goodQuotes);
	}
	
	public int getGoodQuotesSize() {
		return goodQuotes.size();
	}
	
	public ArrayList<String> getBadQuotes(){
		return new ArrayList<String>(badQuotes);
	}
	
	public int getBadQuotesSize() {
		return badQuotes.size();
	}
	
	public boolean checkGoodQuotes(String username) {
		return goodQuotes.contains(username);
	}
	
	public boolean checkBadQuotes(String username) {
		return badQuotes.contains(username);
	}
	
	public boolean addGoodQuotes(String usename) {
		return goodQuotes.add(usename);
	}
	
	public boolean addBadQuotes(String username) {
		return badQuotes.add(username);
	}
	
	public boolean removeGoodQuotes(String username) {
		return goodQuotes.remove(username);
	}
	
	public boolean removeBadQuotes(String username) {
		return badQuotes.remove(username);
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
	
	public boolean addComment(Comment c) {
		return comments.add(c);
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
	
	@Override
	public String toString() {
		String s = "";
		s = "< Titolo: " + title + "\n< Contenuto: " + text + "\n< Voti: " + goodQuotesSize + " positivi, " + badQuotesSize + " negativi\n";
		if(comments.size() == 0) {
			s = s + "<    Commenti: 0";
		} else {
			s = s + "<    Commenti:\n";
			for(Comment c : comments) {
				s = s + c + "\n";
			}
			s = s + "<-------------------------------------------";
		}
		return s;
	}
}