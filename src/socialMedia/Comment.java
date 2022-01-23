package socialMedia;

import java.io.Serializable;

public class Comment implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String authorComment;
	private int commentId;
	private String text;
	
	public Comment() {
		
	}
	
	public Comment(String author, String text, int commentId) {
		this.authorComment = author;
		this.text = text;
		this.commentId = commentId;
	}
	
	public String getAuthorComment() {
		return authorComment;
	}
	
	public int getCommentId() {
		return commentId;
	}
	
	public String getText() {
		return text;
	}
	
	@Override
	public String toString() {
		return "<	 " + authorComment + ": " + text;
	}
}
