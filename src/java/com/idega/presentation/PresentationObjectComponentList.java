/*
 * $Id: PresentationObjectComponentList.java,v 1.8 2008/04/24 23:44:14 laddi Exp $ Created on
 * 14.11.2004
 *
 * Copyright (C) 2004 Idega Software hf. All Rights Reserved.
 *
 * This software is the proprietary information of Idega hf. Use is subject to
 * license terms.
 */
package com.idega.presentation;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIComponent;

import com.idega.util.CoreConstants;

/**
 * Overrided from JSFs standard Children because of the clone() issue.
 *
 * Last modified: $Date: 2008/04/24 23:44:14 $ by $Author: laddi $
 *
 * @author <a href="mailto:tryggvil@idega.com">Tryggvi Larusson </a>
 * @version $Revision: 1.8 $
 */
class PresentationObjectComponentList extends AbstractList<UIComponent> implements Serializable,Cloneable {

	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = -1682244512314682987L;

	private UIComponent _component;

	private List<UIComponent> _list = new ArrayList<UIComponent>();

	PresentationObjectComponentList(UIComponent component) {
		this._component = component;
	}

	@Override
	public UIComponent get(int index) {
		try{
			return this._list.get(index);
		}
		catch(ArrayIndexOutOfBoundsException e){
			throw new RuntimeException(e);
		}
	}

	@Override
	public int size() {
		return this._list.size();
	}

	@Override
	public UIComponent set(int index, UIComponent value) {
		checkValue(value);
		setNewParent(value);
		UIComponent child = this._list.set(index, value);
		if (child != null) {
			child.setParent(null);
		}
		return child;
	}

	@Override
	public boolean add(UIComponent value) {
		checkValue(value);
		setNewParent(value);
		return this._list.add(value);
	}

	@Override
	public void add(int index, UIComponent value) {
		checkValue(value);
		setNewParent(value);
		this._list.add(index, value);
	}

	@Override
	public UIComponent remove(int index) {
		UIComponent child = this._list.remove(index);
		if (child != null) {
			child.setParent(null);
		}
		return child;
	}

	private void setNewParent(UIComponent child) {
		//UIComponent oldParent = child.getParent();
		//if (oldParent != null) {
		//	oldParent.getChildren().remove(child);
		//}
		child.setParent(this._component);
	}

	private void checkValue(Object value) {
		if (!(value instanceof UIComponent)) {
			throw new ClassCastException(value + (value == null ? CoreConstants.EMPTY : " (" + value.getClass().getName() + ")") + " is not a UIComponent");
		}
	}

	@Override
	public Object clone(){
		Object newObject = null;
		try {
			newObject = super.clone();
			PresentationObjectComponentList componentList = (PresentationObjectComponentList) newObject;
			Object clone = ((ArrayList<UIComponent>)this._list).clone();
			if (clone instanceof List) {
				componentList._list = (List<UIComponent>) clone;
			}
		}
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return newObject;
	}


	/**
	 * @return Returns the _component.
	 */
	UIComponent getComponent() {
		return this._component;
	}
	/**
	 * @param _component The _component to set.
	 */
	void setComponent(UIComponent _component) {
		this._component = _component;
	}
}