package org.sagebionetworks.repo.model.jdo;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.web.NotFoundException;




public class JDOUserGroupDAOImpl extends
		JDOBaseDAOImpl<UserGroup, JDOUserGroup> implements UserGroupDAO {

	public JDOUserGroupDAOImpl(String userId) {
		super(userId);
	}

	public static boolean isPublicGroup(JDOUserGroup g) {
		return g.getIsSystemGroup()
				&& AuthorizationConstants.PUBLIC_GROUP_NAME.equals(g.getName());
	}

	/**
	 * Create a default Public Group. By default, everyone is allowed to create
	 * g users and groups. This is necessary to bootstrap the system, after
	 * which permissions can be locked down.
	 * 
	 * @return
	 */
	public JDOUserGroup createPublicGroup(PersistenceManager pm) {
		JDOUserGroup g = newJDO();
		g.setName(AuthorizationConstants.PUBLIC_GROUP_NAME);
		g.setCreationDate(new Date());
		g.setIsSystemGroup(true);
		g.setIsIndividual(false);
		Set<String> creatableTypes = g.getCreatableTypes();
		creatableTypes.add(JDOUser.class.getName());
		creatableTypes.add(JDOUserGroup.class.getName());
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			pm.makePersistent(g);
			// now give the public access to this group
			addResourceToGroup(g, g.getId(), Arrays.asList(new String[] {
					AuthorizationConstants.READ_ACCESS,
					AuthorizationConstants.CHANGE_ACCESS,
					AuthorizationConstants.SHARE_ACCESS }));
			tx.commit();
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
		}
		return g;
	}

	public static void addResourceToGroup(JDOUserGroup group, Long resource,
			List<String> accessTypes) {
		if (resource == null)
			throw new NullPointerException();
		Set<JDOResourceAccess> ras = group.getResourceAccess();
		for (String accessType : accessTypes) {
			JDOResourceAccess ra = new JDOResourceAccess();
			ra.setResource(resource);
			ra.setAccessType(accessType);
			ras.add(ra);
		}
	}

	/**
	 * This is the externally facing method. 
	 */
	public UserGroup getPublicGroup() throws NotFoundException,
			DatastoreException {
		PersistenceManager pm = PMF.get();
		JDOUserGroup jdo = getPublicGroup(pm);
		UserGroup dto = new UserGroup();
		copyToDto(jdo, dto);
		return dto;		
	}

	public static JDOUserGroup getPublicGroup(PersistenceManager pm) {
		Query query = pm.newQuery(JDOUserGroup.class);
		query.setFilter("isSystemGroup==true && name==\""
				+ AuthorizationConstants.PUBLIC_GROUP_NAME + "\"");
		@SuppressWarnings("unchecked")
		Collection<JDOUserGroup> ans = (Collection<JDOUserGroup>) query
				.execute();
		if (ans.size() > 1)
			throw new IllegalStateException("Expected 0-1 but found "
					+ ans.size());
		if (ans.size() == 0)
			return null;
		return ans.iterator().next();
	}

	/**
	 * There must be one public group. This method returns it if it exists, and
	 * creates one if it doesn't
	 * 
	 * @param pm
	 * @return
	 */
	public JDOUserGroup getOrCreatePublicGroup(PersistenceManager pm) {
		// get the Public group
		JDOUserGroup group = getPublicGroup(pm);
		if (/* public group doesn't exist */null == group) {
			// create a Public group
			group = createPublicGroup(pm);
		}
		return group;
	}

	/**
	 * Create a group for a particular user. Give the user READ and CHANGE
	 * access to their own group.
	 * 
	 * @param pm
	 * @return
	 */
	public JDOUserGroup createIndividualGroup(PersistenceManager pm,
			JDOUser user) {
		JDOUserGroup g = newJDO();
		g.setName(user.getUserId());
		g.setCreationDate(new Date());
		g.setIsSystemGroup(true);
		g.setIsIndividual(true);
		Set<String> creatableTypes = g.getCreatableTypes();
		creatableTypes.add(JDOUser.class.getName());
		creatableTypes.add(JDOUserGroup.class.getName());
		g.getUsers().add(user.getId());
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			pm.makePersistent(g);
			// give the group total access to the created group itself.
			addResourceToGroup(g, g.getId(), Arrays.asList(new String[] {
					AuthorizationConstants.READ_ACCESS,
					AuthorizationConstants.CHANGE_ACCESS,
					AuthorizationConstants.SHARE_ACCESS }));
			tx.commit();
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
		}
		return g;
	}

	public JDOUserGroup getIndividualGroup(PersistenceManager pm) {
		if (null == userId)
			return null;
		Query query = pm.newQuery(JDOUserGroup.class);
		query
				.setFilter("isSystemGroup==true && name==pName && isIndividual==true");
		query.declareParameters(String.class.getName() + " pName");
		@SuppressWarnings("unchecked")
		Collection<JDOUserGroup> ans = (Collection<JDOUserGroup>) query
				.execute(userId);
		if (ans.size() > 1)
			throw new IllegalStateException("Expected 0-1 but found "
					+ ans.size());
		if (ans.size() == 0)
			return null;
		return ans.iterator().next();
	}

	public JDOUserGroup getOrCreateIndividualGroup(PersistenceManager pm) {
		if (null == userId)
			throw new NullPointerException();
		// get the individual group
		JDOUserGroup group = getIndividualGroup(pm);
		if (/* individual group doesn't exist */null == group) {
			// create an Individual group
			JDOUser user = (new JDOUserDAOImpl(userId)).getUser(pm);
			group = createIndividualGroup(pm, user);
		}
		return group;
	}

	protected UserGroup newDTO() {
		return new UserGroup();
	}

	protected JDOUserGroup newJDO() {
		JDOUserGroup g = new JDOUserGroup();
		g.setUsers(new HashSet<Long>());
		g.setResourceAccess(new HashSet<JDOResourceAccess>());
		g.setCreatableTypes(new HashSet<String>());
		return g;
	}

	protected void copyToDto(JDOUserGroup jdo, UserGroup dto)
			throws DatastoreException {
		dto.setId(jdo.getId() == null ? null : KeyFactory.keyToString(jdo
				.getId()));
		dto.setCreationDate(jdo.getCreationDate());
		dto.setName(jdo.getName());
	}

	protected void copyFromDto(UserGroup dto, JDOUserGroup jdo)
			throws InvalidModelException {
		jdo.setName(dto.getName());
		jdo.setCreationDate(dto.getCreationDate());
	}

	protected Class getJdoClass() {
		return JDOUserGroup.class;
	}

	public Collection<String> getPrimaryFields() {
		return Arrays.asList(new String[] { "name" });
	}

	public void addUser(UserGroup userGroup, User user)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		PersistenceManager pm = PMF.get();
		if (!hasAccessIntern(pm, KeyFactory.stringToKey(userGroup.getId()),
				AuthorizationConstants.CHANGE_ACCESS))
			throw new UnauthorizedException();
		Transaction tx = null;
		try {
			Long userKey = KeyFactory.stringToKey(user.getId());
			// this is done simply to make check that the user exists
			JDOUser jdoUser = (JDOUser) pm.getObjectById(
					JDOUser.class, userKey);
			Long groupKey = KeyFactory.stringToKey(userGroup.getId());
			JDOUserGroup jdoGroup = (JDOUserGroup) pm.getObjectById(
					JDOUserGroup.class, groupKey);
			tx = pm.currentTransaction();
			tx.begin();
			jdoGroup.getUsers().add(userKey);
			tx.commit();
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}

	public void removeUser(UserGroup userGroup, User user)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			Long userKey = KeyFactory.stringToKey(user.getId());
			// this is done simply to make check that the user exists
			JDOUser jdoUser = (JDOUser) pm.getObjectById(
					JDOUser.class, userKey);
			Long groupKey = KeyFactory.stringToKey(userGroup.getId());
			JDOUserGroup jdoGroup = (JDOUserGroup) pm.getObjectById(
					JDOUserGroup.class, groupKey);
			if (!hasAccessIntern(pm, jdoGroup.getId(),
					AuthorizationConstants.CHANGE_ACCESS))
				throw new UnauthorizedException();
			jdoGroup.getUsers().remove(userKey);
			tx.commit();
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (UnauthorizedException e) {
			throw e;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}

	public Collection<User> getUsers(UserGroup userGroup)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		PersistenceManager pm = PMF.get();
		try {
			Long groupKey = KeyFactory.stringToKey(userGroup.getId());
			JDOUserGroup jdoGroup = (JDOUserGroup) pm.getObjectById(
					JDOUserGroup.class, groupKey);
			if (!hasAccessIntern(pm, jdoGroup.getId(),
					AuthorizationConstants.READ_ACCESS))
				throw new UnauthorizedException();
			Collection<Long> userKeys = jdoGroup.getUsers();
			JDOUserDAOImpl userDAO = new JDOUserDAOImpl(userId);
			Collection<User> ans = new HashSet<User>();
			for (Long userKey : userKeys) {
				ans.add(userDAO.get(KeyFactory.keyToString(userKey)));
			}
			return ans;
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (UnauthorizedException e) {
			throw e;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

	public void addResource(UserGroup userGroup, String resourceId,
			String accessType) throws NotFoundException, DatastoreException,
			UnauthorizedException {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			Long resourceKey = KeyFactory.stringToKey(resourceId);
			if (!hasAccessIntern(pm, resourceKey,
					AuthorizationConstants.SHARE_ACCESS))
				throw new UnauthorizedException();
			Long groupKey = KeyFactory.stringToKey(userGroup.getId());
			JDOUserGroup jdoGroup = (JDOUserGroup) pm.getObjectById(
					JDOUserGroup.class, groupKey);
			if (!hasAccessIntern(pm, jdoGroup.getId(),
					AuthorizationConstants.CHANGE_ACCESS))
				throw new UnauthorizedException();
			tx = pm.currentTransaction();
			tx.begin();
			JDOResourceAccess ra = new JDOResourceAccess();
			ra.setResource(resourceKey);
			ra.setAccessType(accessType);
			// TODO make sure it's not a duplicate
			jdoGroup.getResourceAccess().add(ra);
			tx.commit();
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (UnauthorizedException e) {
			throw e;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}

	public void removeResource(UserGroup userGroup, String resourceId,
			String accessType) throws NotFoundException, DatastoreException,
			UnauthorizedException {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			Long resourceKey = KeyFactory.stringToKey(resourceId);
			Long groupKey = KeyFactory.stringToKey(userGroup.getId());
			JDOUserGroup jdoGroup = (JDOUserGroup) pm.getObjectById(
					JDOUserGroup.class, groupKey);
			if (!hasAccessIntern(pm, jdoGroup.getId(),
					AuthorizationConstants.CHANGE_ACCESS))
				throw new UnauthorizedException();
			Collection<JDOResourceAccess> ras = jdoGroup.getResourceAccess();
			tx = pm.currentTransaction();
			tx.begin();
			for (JDOResourceAccess ra : ras) {
				if (ra.getResource().equals(resourceKey)
						&& ra.getAccessType().equals(accessType)) {
					ras.remove(ra);
				}
			}
			tx.commit();
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (UnauthorizedException e) {
			throw e;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}

	public Collection<String> getResources(UserGroup userGroup)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// TODO
		throw new RuntimeException("Not yet implemented");
	}

	public Collection<String> getResources(UserGroup userGroup,
			String accessType) throws NotFoundException, DatastoreException,
			UnauthorizedException {
		// TODO
		throw new RuntimeException("Not yet implemented");
	}

	public Collection<String> getAccessTypes(UserGroup userGroup,
			String resourceId) throws NotFoundException, DatastoreException,
			UnauthorizedException {
		// TODO
		throw new RuntimeException("Not yet implemented");
	}

}