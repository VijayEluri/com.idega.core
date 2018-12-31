package com.idega.core.accesscontrol.data;

import com.idega.user.data.GroupTypeBMPBean;

/**
 * Title:        AccessControl
 * Description:
 * Copyright:    Copyright (c) 2001 idega.is All Rights Reserved
 * Company:      idega margmiðlun
 * @author
 * @version 1.1
 * @deprecated All hardcoded group "type" classes should be avoided. Just use a regular com.idega.user.data.Group and set its grouptype.
 */

@Deprecated
public class PermissionGroupBMPBean extends com.idega.user.data.GroupBMPBean implements com.idega.core.accesscontrol.data.PermissionGroup {

  @Override
public String getGroupTypeValue(){

    return GroupTypeBMPBean.TYPE_PERMISSION_GROUP;

  }

  public static String getClassName(){
	  return PermissionGroup.class.getName();
  }

  /**
   * ONLY FOR BACKWARD COMPATABILTY ISSUES WITH ACCESSCONTROL
   */
  @Override
public void setID(int id) {
	setColumn(getIDColumnName(), new Integer(id));
  }

  /**
   * ONLY FOR BACKWARD COMPATABILTY ISSUES WITH ACCESSCONTROL
   */
  @Override
public int getID() {
	return getIntColumnValue(getIDColumnName());
  }


   public static PermissionGroup getStaticPermissionGroupInstance(){
    return (PermissionGroup)getStaticInstance(getClassName());
  }

} // Class PermissionGroup
