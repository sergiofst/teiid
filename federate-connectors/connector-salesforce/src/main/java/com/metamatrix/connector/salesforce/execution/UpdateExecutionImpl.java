/*
 * JBoss, Home of Professional Open Source.
 * Copyright (C) 2008 Red Hat, Inc.
 * Copyright (C) 2000-2007 MetaMatrix, Inc.
 * Licensed to Red Hat, Inc. under one or more contributor 
 * license agreements.  See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package com.metamatrix.connector.salesforce.execution;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axis.message.MessageElement;

import com.metamatrix.connector.salesforce.Util;
import com.metamatrix.connector.salesforce.execution.visitors.UpdateVisitor;
import com.metamatrix.data.exception.ConnectorException;
import com.metamatrix.data.language.IElement;
import com.metamatrix.data.language.ILiteral;
import com.metamatrix.data.language.ISetClause;
import com.metamatrix.data.language.IUpdate;
import com.metamatrix.data.metadata.runtime.Element;

public class UpdateExecutionImpl {

	public int execute(IUpdate update, UpdateExecutionParent parent)
			throws ConnectorException {
		int result = 0;
		UpdateVisitor visitor = new UpdateVisitor(parent
				.getMetadata());
		visitor.visit(update);
		String[] Ids = parent.getIDs(update.getCriteria(), visitor);

		if (null != Ids && Ids.length > 0) {
			List<MessageElement> elements = new ArrayList<MessageElement>();
			for (ISetClause clause : update.getChanges().getClauses()) {
				IElement element = clause.getSymbol();
				Element column = (Element) parent.getMetadata().getObject(
						element.getMetadataID());
				String val = ((ILiteral) clause.getValue())
						.toString();
				MessageElement messageElem = new MessageElement(new QName(
						column.getNameInSource()), Util.stripQutes(val));
				elements.add(messageElem);
			}

			List<DataPayload> updateDataList = new ArrayList<DataPayload>();
			for (int i = 0; i < Ids.length; i++) {
				DataPayload data = new DataPayload();
				data.setType(visitor.getTableName());
				data.setID(Ids[i]);
				data.setMessageElements(elements
						.toArray(new MessageElement[] {}));
				updateDataList.add(data);
			}

			result = parent.getConnection().update(updateDataList);
		}
		return result;
	}

}
