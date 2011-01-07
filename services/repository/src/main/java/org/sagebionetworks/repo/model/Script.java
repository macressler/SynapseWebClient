package org.sagebionetworks.repo.model;

import java.net.URI;
import java.util.Date;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;

/**
 * note 'source' may be a reference to a version control repository, like Subversion
 * @author bhoff
 *
 */
@PersistenceCapable(detachable = "true")
public class Script implements Revisable<Script> {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key id;
	
	@Persistent
	private String name;
	
	// http://code.google.com/appengine/docs/java/datastore/relationships.html#Owned_One_to_One_Relationships
	@Persistent(dependent = "true") 
	private Revision<Script> revision;
	
	@Persistent
	private Date publicationDate;
	
	@Persistent
	private Text overview;
	
	@Persistent(serialized="true")
	private URI source;

	public Key getId() {
		return id;
	}

	public void setId(Key id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Revision<Script> getRevision() {
		return revision;
	}

	public void setRevision(Revision<Script> revision) {
		this.revision = revision;
	}

	public Date getPublicationDate() {
		return publicationDate;
	}

	public void setPublicationDate(Date publicationDate) {
		this.publicationDate = publicationDate;
	}

	public Text getOverview() {
		return overview;
	}

	public void setOverview(Text overview) {
		this.overview = overview;
	}

	public URI getSource() {
		return source;
	}

	public void setSource(URI source) {
		this.source = source;
	}

	public boolean isPublished() {return null!=publicationDate;}
}
