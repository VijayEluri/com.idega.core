/*
 * $Id: ExternalLink.java,v 1.6 2009/05/15 15:08:06 valdas Exp $
 * Created on 12.1.2005
 *
 * Copyright (C) 2005 Idega Software hf. All Rights Reserved.
 *
 * This software is the proprietary information of Idega hf.
 * Use is subject to license terms.
 */
package com.idega.idegaweb.include;

import java.io.InputStream;
import java.io.Serializable;

import com.idega.util.StringUtil;
import com.idega.util.reflect.Property;

/**
 * 	<p>
 *  Class to represent an External Link to a Resource that is represented in html as 
 *  a <code>&lt;link&gt;</code> tag used to define resources such ass CSS links.
 *  This class is used by the GlobalIncludeManager for managing resources included in all pages.
 *  </p>
 *  Last modified: $Date: 2009/05/15 15:08:06 $ by $Author: valdas $
 * 
 * @author <a href="mailto:tryggvil@idega.com">tryggvil</a>
 * @version $Revision: 1.6 $
 */
public abstract class ExternalLink implements Serializable {
	
	private static final long serialVersionUID = -3223586255509071468L;
	
	//static contants
	public final static String MEDIA_SCREEN="screen";
	public final static String MEDIA_TTY="tty";
	public final static String MEDIA_TV="tv";
	public final static String MEDIA_PROJECTION="projection";
	public final static String MEDIA_HANDHELD="handheld";
	public final static String MEDIA_PRINT="print";
	public final static String MEDIA_BRAILLE="braille";
	public final static String MEDIA_AURAL="aural";
	public final static String MEDIA_ALL="all";
	
	protected final static String TYPE_CSS="text/css";
	protected final static String TYPE_RSS="application/rss+xml";
	
	protected final static String RELATIONSHIP_STYLESHEET="stylesheet";
	protected final static String RELATIONSHIP_ALTERNATE="alternate";
	
	//href attribute
	private String url;
	//charset attribute
	private String characterset;
	//hreflang attribute
	private String urllanguage;
	private String type;
	//rel attribute
	private String relationship;
	//rev attribute
	private String reverseRelationship;

	private String content;
	private InputStream contentStream;
	
	/**
	 * @return Returns the characterset.
	 */
	public String getCharacterset() {
		return this.characterset;
	}
	/**
	 * @param characterset The characterset to set.
	 */
	public void setCharacterset(String characterset) {
		this.characterset = characterset;
	}
	/**
	 * @return Returns the relationship.
	 */
	public String getRelationship() {
		return this.relationship;
	}
	/**
	 * @param relationship The relationship to set.
	 */
	public void setRelationship(String relationship) {
		this.relationship = relationship;
	}
	/**
	 * @return Returns the reverseRelationship.
	 */
	public String getReverseRelationship() {
		return this.reverseRelationship;
	}
	/**
	 * @param reverseRelationship The reverseRelationship to set.
	 */
	public void setReverseRelationship(String reverseRelationship) {
		this.reverseRelationship = reverseRelationship;
	}
	/**
	 * @return Returns the type.
	 */
	public String getType() {
		return this.type;
	}
	/**
	 * @param type The type to set.
	 */
	public void setType(String type) {
		this.type = type;
	}
	/**
	 * @return Returns the url.
	 */
	public String getUrl() {
		return this.url;
	}
	/**
	 * @param url The url to set.
	 */
	public void setUrl(String url) {
		String urlFromExpression = Property.getValueFromExpression(url, String.class);
		if (!StringUtil.isEmpty(urlFromExpression)) {
			url = urlFromExpression;
		}
		this.url = url;
	}
	/**
	 * @return Returns the urllanguage.
	 */
	public String getUrllanguage() {
		return this.urllanguage;
	}
	/**
	 * @param urllanguage The urllanguage to set.
	 */
	public void setUrllanguage(String urllanguage) {
		this.urllanguage = urllanguage;
	}
	
	@Override
	public String toString() {
		return url;
	}
	
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	
	public InputStream getContentStream() {
		return contentStream;
	}
	public void setContentStream(InputStream contentStream) {
		this.contentStream = contentStream;
	}
	
}
