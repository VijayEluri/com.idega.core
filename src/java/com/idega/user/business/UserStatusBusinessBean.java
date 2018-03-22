/*
 * $Id$
 *
 * Copyright (C) 2000-2003 Idega Software. All Rights Reserved.
 *
 * This software is the proprietary information of Idega Software.
 * Use is subject to license terms.
 */
package com.idega.user.business;

import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;

import com.idega.business.IBOLookup;
import com.idega.business.IBOServiceBean;
import com.idega.user.data.Status;
import com.idega.user.data.StatusHome;
import com.idega.user.data.User;
import com.idega.user.data.UserStatus;
import com.idega.user.data.UserStatusHome;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;

/**
 * @author palli,eiki
 *Used to manipulate a users status.
 */
public class UserStatusBusinessBean extends IBOServiceBean implements UserStatusBusiness {

	public final static String STATUS_DECEASED = "deceased";

	@Override
	public boolean removeUserFromGroup(int user_id, int group_id) {
		return setUserGroupStatus(user_id,group_id,-1,-1);
	}

	@Override
	public boolean setUserGroupStatus(int user_id, int group_id, int status_id){
	    return setUserGroupStatus(user_id,group_id,status_id,-1);
	}

	@Override
	public boolean setUserGroupStatus(int userId, int groupId, int statusId,int doneByUserId) {
		try {
			Collection obj = getUserStatusHome().findAllByUserIdAndGroupId(userId,groupId);
			boolean alreadyIsSet = false;
			IWTimestamp now = new IWTimestamp();
			if (obj != null && !obj.isEmpty()) {
				//set all older statuses to inactive (that are not the same as the one we are activating)
				Iterator it = obj.iterator();
				while (it.hasNext()) {
					UserStatus uStatus = (UserStatus)it.next();
					if ( (uStatus.getDateTo()==null && uStatus.getStatusId()!=statusId) || statusId<0 ) {
						uStatus.setDateTo(now.getTimestamp());
						uStatus.store();
					}
					else if(uStatus.getDateTo()==null && uStatus.getStatusId()==statusId ){
						alreadyIsSet = true;
					}
				}
			}

			if (statusId > 0 && !alreadyIsSet) {
				UserStatus uStatus = getUserStatusHome().create();
				uStatus.setUserId(userId);
				uStatus.setGroupId(groupId);
				uStatus.setDateFrom(now.getTimestamp());
				uStatus.setStatusId(statusId);
				if(doneByUserId>0){
				   uStatus.setCreatedBy(doneByUserId);
				}
				uStatus.store();
			}
		}
		catch(Exception e) {
			e.printStackTrace();

			return false;
		}


		return true;
	}

	@Override
	public int getUserGroupStatus(int user_id, int group_id) {
		try {
			Collection obj = getUserStatusHome().findAllActiveByUserIdAndGroupId(user_id,group_id);
			int ret = -1;

			if (obj != null && obj.size() > 0) {
				UserStatus uStatus = (UserStatus)obj.toArray()[obj.size()-1];
				ret = uStatus.getStatusId();
			}

			return ret;
		}
		catch(Exception e) {
			e.printStackTrace();

			return -1;
		}
	}

	@Override
	public Collection getAllUserStatuses(int userId) throws RemoteException {
		try {
			return getUserStatusHome().findAllActiveByUserId(userId);
		}
		catch (FinderException e) {
			e.printStackTrace();
		}

		return ListUtil.getEmptyList();
	}

	@Override
	public UserStatusHome getUserStatusHome() throws RemoteException{
		return (UserStatusHome) getIDOHome(UserStatus.class);
	}

	@Override
	public String getDeceasedStatusKey(){
		return STATUS_DECEASED;
	}

	@Override
	public StatusHome getStatusHome() throws RemoteException{
		return (StatusHome) getIDOHome(Status.class);
	}

	@Override
	public Status getDeceasedStatus() throws RemoteException{
		try {
			return getStatusHome().findByStatusKey(STATUS_DECEASED);
		}
		catch (FinderException e) {

		}
		return null;
	}

	@Override
	public Status createDeceasedStatus() throws RemoteException,CreateException{
		Status status = getStatusHome().create();
		status.setStatusKey(STATUS_DECEASED);
		status.store();
		return status;
	}

	@Override
	public Status getDeceasedStatusCreateIfNone() throws RemoteException{
		Status status = getDeceasedStatus();
		if(status!=null) {
			return status;
		}
		else{
			try {
				status = createDeceasedStatus();
				return status;
			}
			catch (CreateException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	@Override
	public Status getStatusByStatusId(int statusId) throws RemoteException{
		Status status = null;
		try {
			status = getStatusHome().findByPrimaryKey(new Integer(statusId));
		}
		catch (RemoteException e) {
			e.printStackTrace();
		}
		catch (FinderException e) {
			e.printStackTrace();
		}
		return status;
	}
	@Override
	public UserStatus getDeceasedUserStatus(Integer userID) throws RemoteException{
		try {
			Status deceasedStatus = getDeceasedStatusCreateIfNone();
			Collection coll = getUserStatusHome().findAllByUserIDAndStatusID(userID,(Integer) deceasedStatus.getPrimaryKey());
			if(coll !=null && !coll.isEmpty()) {
				return (UserStatus) coll.iterator().next();
			}
		}
		catch (EJBException e) {
			e.printStackTrace();
		}
		catch (FinderException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public void setUserAsDeceased(Integer userID, Date deceasedDate) throws RemoteException {
		if (userID == null) {
			return;
		}

		try {
			UserBusiness userBusiness = IBOLookup.getServiceInstance(getIWApplicationContext(), UserBusiness.class);
			User user = userBusiness.getUser(userID);
			setUserAsDeceased(user, deceasedDate);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error marking user (ID: " + userID + ") as deceased", e);
		}
	}

	@Override
	public void setUserAsDeceased(User user, Date deceasedDate) throws RemoteException {
		try {
			if (user == null || user.isDeceased()) {
				return;
			}

			Integer userID = (Integer) user.getPrimaryKey();
			Status deceasedStatus = getDeceasedStatusCreateIfNone();
			UserStatus dUserStatus = getDeceasedUserStatus(userID);
			if (dUserStatus == null) {
				dUserStatus = getUserStatusHome().create();
				dUserStatus.setUserId(userID.intValue());
				dUserStatus.setDateFrom(new Timestamp(deceasedDate.getTime()));
				dUserStatus.setStatusId(((Integer)deceasedStatus.getPrimaryKey()).intValue());
				dUserStatus.store();
			}
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error marking " + user + " (personal ID: " + user.getPersonalID() + ") as deceased", e);
		}
	}

	@Override
	public Collection<User> getAllUsersWithStatus(int statusId){
		try {
			Collection<UserStatus> userStatuses = getUserStatusHome().findAllActiveByStatusId(statusId);
			List<User> users = new ArrayList<User>();
			Iterator<UserStatus> iter = userStatuses.iterator();
			while (iter.hasNext()) {
				UserStatus userStatus = iter.next();
				users.add(userStatus.getUser());
			}

			return users;
		}
		catch (FinderException e) {
		//nothing found...just return an empty list
		}
		catch (RemoteException e) {
			e.printStackTrace();
		}

		return ListUtil.getEmptyList();
	}

	@Override
	public boolean isDeceased(User user) {
		return user == null ? false : user.isDeceased();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<UserStatus> findAllActiveBySearchCriteria(List<Integer> groupIds, List<String> roles) {
		Collection<UserStatus> userStatuses = null;
		try {
			userStatuses = getUserStatusHome().findAllActiveBySearchCriteria(groupIds, roles);
		} catch(Exception e) {
			getLogger().log(Level.WARNING, "Could not get the user statuses for groups: " + groupIds + " and roles: " + roles, e);
		}
		return userStatuses;
	}


}