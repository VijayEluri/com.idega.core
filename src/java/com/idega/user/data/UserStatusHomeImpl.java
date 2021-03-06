/*
 * $Id$
 * Created on Jun 27, 2005
 *
 * Copyright (C) 2005 Idega Software hf. All Rights Reserved.
 *
 * This software is the proprietary information of Idega hf.
 * Use is subject to license terms.
 */
package com.idega.user.data;

import java.util.Collection;
import javax.ejb.FinderException;
import com.idega.data.IDOFactory;


/**
 * 
 *  Last modified: $Date$ by $Author$
 * 
 * @author <a href="mailto:eiki@idega.com">eiki</a>
 * @version $Revision$
 */
public class UserStatusHomeImpl extends IDOFactory implements UserStatusHome {

	protected Class getEntityInterfaceClass() {
		return UserStatus.class;
	}

	public UserStatus create() throws javax.ejb.CreateException {
		return (UserStatus) super.createIDO();
	}

	public UserStatus findByPrimaryKey(Object pk) throws javax.ejb.FinderException {
		return (UserStatus) super.findByPrimaryKeyIDO(pk);
	}

	public Collection findAll() throws FinderException {
		com.idega.data.IDOEntity entity = this.idoCheckOutPooledEntity();
		java.util.Collection ids = ((UserStatusBMPBean) entity).ejbFindAll();
		this.idoCheckInPooledEntity(entity);
		return this.getEntityCollectionForPrimaryKeys(ids);
	}

	public Collection findAllByUserId(int id) throws FinderException {
		com.idega.data.IDOEntity entity = this.idoCheckOutPooledEntity();
		java.util.Collection ids = ((UserStatusBMPBean) entity).ejbFindAllByUserId(id);
		this.idoCheckInPooledEntity(entity);
		return this.getEntityCollectionForPrimaryKeys(ids);
	}

	public Collection findAllActiveByUserId(int id) throws FinderException {
		com.idega.data.IDOEntity entity = this.idoCheckOutPooledEntity();
		java.util.Collection ids = ((UserStatusBMPBean) entity).ejbFindAllActiveByUserId(id);
		this.idoCheckInPooledEntity(entity);
		return this.getEntityCollectionForPrimaryKeys(ids);
	}

	public Collection findAllByStatusId(int id) throws FinderException {
		com.idega.data.IDOEntity entity = this.idoCheckOutPooledEntity();
		java.util.Collection ids = ((UserStatusBMPBean) entity).ejbFindAllByStatusId(id);
		this.idoCheckInPooledEntity(entity);
		return this.getEntityCollectionForPrimaryKeys(ids);
	}

	public Collection findAllActiveByStatusId(int id) throws FinderException {
		com.idega.data.IDOEntity entity = this.idoCheckOutPooledEntity();
		java.util.Collection ids = ((UserStatusBMPBean) entity).ejbFindAllActiveByStatusId(id);
		this.idoCheckInPooledEntity(entity);
		return this.getEntityCollectionForPrimaryKeys(ids);
	}

	public Collection findAllActiveByGroupId(int id) throws FinderException {
		com.idega.data.IDOEntity entity = this.idoCheckOutPooledEntity();
		java.util.Collection ids = ((UserStatusBMPBean) entity).ejbFindAllActiveByGroupId(id);
		this.idoCheckInPooledEntity(entity);
		return this.getEntityCollectionForPrimaryKeys(ids);
	}

	public Collection findAllByGroupId(int id) throws FinderException {
		com.idega.data.IDOEntity entity = this.idoCheckOutPooledEntity();
		java.util.Collection ids = ((UserStatusBMPBean) entity).ejbFindAllByGroupId(id);
		this.idoCheckInPooledEntity(entity);
		return this.getEntityCollectionForPrimaryKeys(ids);
	}

	public Collection findAllByUserIdAndGroupId(int user_id, int group_id) throws FinderException {
		com.idega.data.IDOEntity entity = this.idoCheckOutPooledEntity();
		java.util.Collection ids = ((UserStatusBMPBean) entity).ejbFindAllByUserIdAndGroupId(user_id, group_id);
		this.idoCheckInPooledEntity(entity);
		return this.getEntityCollectionForPrimaryKeys(ids);
	}

	public Collection findAllActiveByUserIdAndGroupId(int user_id, int group_id) throws FinderException {
		com.idega.data.IDOEntity entity = this.idoCheckOutPooledEntity();
		java.util.Collection ids = ((UserStatusBMPBean) entity).ejbFindAllActiveByUserIdAndGroupId(user_id, group_id);
		this.idoCheckInPooledEntity(entity);
		return this.getEntityCollectionForPrimaryKeys(ids);
	}

	public Collection findAllByUserIDAndStatusID(Integer userID, Integer statusID) throws FinderException {
		com.idega.data.IDOEntity entity = this.idoCheckOutPooledEntity();
		java.util.Collection ids = ((UserStatusBMPBean) entity).ejbFindAllByUserIDAndStatusID(userID, statusID);
		this.idoCheckInPooledEntity(entity);
		return this.getEntityCollectionForPrimaryKeys(ids);
	}

	public Collection findAllActiveByUserIDAndStatusID(Integer userID, Integer statusID) throws FinderException {
		com.idega.data.IDOEntity entity = this.idoCheckOutPooledEntity();
		java.util.Collection ids = ((UserStatusBMPBean) entity).ejbFindAllActiveByUserIDAndStatusID(userID, statusID);
		this.idoCheckInPooledEntity(entity);
		return this.getEntityCollectionForPrimaryKeys(ids);
	}
}
