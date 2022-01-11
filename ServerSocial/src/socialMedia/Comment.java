package socialMedia;

public class Comment {
	private int author;
	private int commentId;
	private String text;
	
	public Comment(int commentId, String text) {
		this.commentId = commentId;
		this.text = text;
	}
	
	public int getCommentId() {
		return commentId;
	}
	
	public String getText() {
		return text;
	}
}
