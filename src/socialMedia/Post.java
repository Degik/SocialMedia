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
	private ArrayList<String> goodQuotes = null; // Tengo traccia dei voti positivi
	private ArrayList<String> badQuotes = null;	 // Tengo traccia dei voti negativi
	private ArrayList<Comment> comments = null;
	private ArrayList<String> newPeopleComment = null; //Con questa tengo traccia delle nuove persone che commentano
	private ArrayList<String> rewardsList = null; // Per accorciare le operazioni di CalculateSystem creo una lista con dentro i nomi degli utenti che riceveranno una ricompensa
	private int goodQuotesSize;
	private int badQuotesSize;
	private int newPeopleLikes;
	private int newPeopleCommenting;
	private int numberIter;
	
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
		this.newPeopleComment = new ArrayList<>();
		newPeopleLikes = 0;
		newPeopleCommenting = 0;
		numberIter = 1;
		rewardsList = new ArrayList<>();
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
	
	public ArrayList<Comment> getComments(){
		return new ArrayList<>(comments);
	}
	
	public int getNewPeopleLikes() {
		return newPeopleLikes;
	}
	
	public void addNewPeopleLikes() {
		newPeopleLikes++;
	}
	
	public void removeNewPeopleLikes() {
		newPeopleLikes--;
	}
	
	public void resetNewPeopleLikes() {
		newPeopleLikes = 0;
	}
	
	public int getNewPeopleCommenting() {
		return newPeopleCommenting;
	}
	
	public void addNewPeopleCommenting() {
		newPeopleCommenting++;
	}
	
	public void resetNewPeopleCommenting() {
		newPeopleCommenting = 0;
	}
	
	public int getNumberIter() {
		return numberIter;
	}
	
	public void addNumberIter() {
		numberIter++;
		
	}
	
	public ArrayList<String> getRewardsList(){
		return new ArrayList<>(rewardsList);
	}
	
	public boolean addRewardsList(String username){
		if(rewardsList.contains(username)) { // Evito duplicati
			return false;
		}
		return rewardsList.add(username);
	}
	
	public boolean removeRewardsList(String username) {
		return rewardsList.remove(username);
	}
	
	public void resetRewardsList() {
		rewardsList = new ArrayList<>();
	}
	
	public boolean getComment(int commentId) {
		for(Comment c : comments) {
			if(c.getCommentId() == commentId) {
				return true;
			}
		}
		return false;
	}
	
	public boolean checkComment(String username) {
		for(String c : newPeopleComment) {
			if(c.equals(username)) {
				return true;
			}
		}
		return false;
	}
	
	public ArrayList<String> getNewPeopleComment() {
		return new ArrayList<>(newPeopleComment);
	}
	
	public boolean addNewPeopleComment(String username) {
		return newPeopleComment.add(username);
	}
	
	public void resetNewPeopleComment() {
		newPeopleComment = new ArrayList<>();
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