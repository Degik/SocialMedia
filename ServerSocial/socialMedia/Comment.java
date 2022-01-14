package socialMedia;

public class Comment {
	private String author;
	private int commentId;
	private String text;
	
	public Comment(int commentId, String text) {
		this.commentId = commentId;
		this.text = text;
	}
	
	public String getAuthorComment() {
		return author;
	}
	
	public int getCommentId() {
		return commentId;
	}
	
	public String getText() {
		return text;
	}
}
