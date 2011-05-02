package org.sagebionetworks.repo.model.jdo.persistence;

import java.util.HashSet;
import java.util.Set;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;




@PersistenceCapable(detachable = "false")
public class JDOResourceAccess {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Long id;
	
	@Persistent
	private JDOUserGroup owner;
	
	@Persistent
	private String resourceType;
	
	@Persistent
	private Long resourceId;
		
	// e.g. read, change, share
	@Persistent
	private Set<String> accessType = new HashSet<String>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public JDOUserGroup getOwner() {
		return owner;
	}

	public void setOwner(JDOUserGroup owner) {
		this.owner = owner;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public Long getResourceId() {
		return resourceId;
	}

	public void setResourceId(Long resourceId) {
		this.resourceId = resourceId;
	}

	/**
	 * @return the accessType
	 */
	public Set<String> getAccessType() {
		return accessType;
	}

	/**
	 * @param accessType the accessType to set
	 */
	public void setAccessType(Set<String> accessType) {
		this.accessType = accessType;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof JDOResourceAccess))
			return false;
		JDOResourceAccess other = (JDOResourceAccess) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	
	public String toString() {
		String typeAbbr = this.getResourceType();
		int i = typeAbbr.lastIndexOf(".");
		if (i>0 && i<typeAbbr.length()-1) typeAbbr = typeAbbr.substring(i+1);
		return "type="+this.getResourceType()+", rid="+this.getResourceId();
		}
}