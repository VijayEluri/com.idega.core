/*
 * $Id: TableColumn.java,v 1.3 2005/09/19 15:00:22 laddi Exp $
 * Created on Aug 6, 2005
 *
 * Copyright (C) 2005 Idega Software hf. All Rights Reserved.
 *
 * This software is the proprietary information of Idega hf.
 * Use is subject to license terms.
 */
package com.idega.presentation;

import java.io.IOException;
import javax.faces.context.FacesContext;



/**
 * Last modified: $Date: 2005/09/19 15:00:22 $ by $Author: laddi $
 * 
 * @author <a href="mailto:laddi@idega.com">laddi</a>
 * @version $Revision: 1.3 $
 */
public class TableColumn extends PresentationObject {

	private static final String MARKUP_ATTRIBUTE_SPAN = "span";
	private static final String MARKUP_ATTRIBUTE_WIDTH = "width";

	/**
	 * Sets the alignment of data and the justification of text in the cells contained.
	 * 
	 * @param alignment		The alignment to set.
	 */
	public void setCellHorizontalAlignment(String alignment) {
		setMarkupAttribute(Table2.MARKUP_ATTRIBUTE_CELL_HORIZONTAL_ALIGNMENT, alignment);
	}
	
	/**
	 * Gets the horizontal alignment set on the cells contained.
	 * 
	 * @return	The alignment set for the cell.  Returns the default value (HORIZONTAL_ALIGNMENT_LEFT) if not set.
	 */
	public String getCellHorizontalAlignment() {
		String alignment = getMarkupAttribute(Table2.MARKUP_ATTRIBUTE_CELL_HORIZONTAL_ALIGNMENT);
		if (alignment != null) {
			return alignment;
		}
		return Table2.HORIZONTAL_ALIGNMENT_LEFT;
	}
	
	/**
	 * Sets the vertical position of data within contained cells.
	 * 
	 * @param alignment		The alignment to set.
	 */
	public void setCellVerticalAlignment(String alignment) {
		setMarkupAttribute(Table2.MARKUP_ATTRIBUTE_CELL_VERTICAL_ALIGNMENT, alignment);
	}
	
	/**
	 * Gets the vertical alignment set on the cells contained.
	 * 
	 * @return	The alignment set for the cell.  Returns the default value (VERTICAL_ALIGNMENT_MIDDLE) if not set.
	 */
	public String getCellVerticalAlignment() {
		String alignment = getMarkupAttribute(Table2.MARKUP_ATTRIBUTE_CELL_VERTICAL_ALIGNMENT);
		if (alignment != null) {
			return alignment;
		}
		return Table2.VERTICAL_ALIGNMENT_MIDDLE;
	}
	
	/**
	 * Sets the number of columns "spanned" by the <code>TableColumn</code> element; the <code>TableColumn</code> element shares its attributes with all the columns it spans.
	 * 
	 * @param span	The span value to set, must be an integer > 0.
	 */
	public void setSpan(int span) {
		if (span < 1) {
			throw new IllegalArgumentException("Span value must be greater than 0.");
		}
		setMarkupAttribute(MARKUP_ATTRIBUTE_SPAN, span);
	}
	
	/**
	 * Gets the span set on the column.
	 * 
	 * @return	The span set for the column.  Returns the default value (1) if not set.
	 */
	public int getSpan() {
		String span = getMarkupAttribute(MARKUP_ATTRIBUTE_SPAN);
		if (span != null) {
			return Integer.parseInt(span);
		}
		return 1;
	}
	
	/**
	 * Sets the default width for each column spanned by the current <code>TableColumn</code> element.
	 * It has the same meaning as the width attribute for the <code>TableColumnGroup</code> element and overrides it.
	 * 
	 * @param width	The width to set.
	 */
	public void setWidth(String width) {
		setMarkupAttribute(MARKUP_ATTRIBUTE_WIDTH, width);
	}
	
	/**
	 * Gets the width set on the column.  The value must be either a number (150) or a percentage (66%).
	 * 
	 * @return	The width set for the column.  Returns null if not set.
	 */
	public String getWidth() {
		return getMarkupAttribute(MARKUP_ATTRIBUTE_WIDTH);
	}
	
	public void print(IWContext iwc) throws Exception {
		if (getMarkupLanguage().equals("HTML")) {
			println("<col" + getMarkupAttributesString() + " />");
		}
	}

	public void encodeBegin(FacesContext context) throws IOException {
	}

	public void encodeEnd(FacesContext arg0) throws IOException {
		println("<col" + getMarkupAttributesString() + " />");
	}

}
