package de.metas.ui.web.window.descriptor.factory.standard;

import java.util.Set;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Check;
import org.compiere.util.DisplayType;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet;

import de.metas.logging.LogManager;
import de.metas.ui.web.window.WindowConstants;
import de.metas.ui.web.window.descriptor.DocumentFieldWidgetType;
import de.metas.ui.web.window.descriptor.DocumentLayoutElementFieldDescriptor;
import de.metas.ui.web.window.descriptor.DocumentLayoutElementFieldDescriptor.LookupSource;
import de.metas.ui.web.window.descriptor.LookupDescriptor;
import de.metas.ui.web.window.exceptions.DocumentLayoutBuildException;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2016 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

/**
 * Miscellaneous descriptors building helpers.
 *
 * @author metas-dev <dev@metasfresh.com>
 *
 */
public final class DescriptorsFactoryHelper
{
	private static final Logger logger = LogManager.getLogger(DescriptorsFactoryHelper.class);

	/** Column names where we shall use {@link DocumentFieldWidgetType#Switch} instead of {@link DocumentFieldWidgetType#YesNo} */
	private static final Set<String> COLUMNNAMES_Switch = ImmutableSet.of(WindowConstants.FIELDNAME_IsActive); // FIXME: hardcoded

	private DescriptorsFactoryHelper()
	{
		super();
	}

	public static Class<?> getValueClass(final DocumentFieldWidgetType widgetType, final LookupDescriptor lookupDescriptor)
	{
		final Class<?> widgetValueClass = widgetType.getValueClassOrNull();

		//
		// Try fetching the valueClass from lookup
		if (lookupDescriptor != null)
		{
			final Class<?> lookupValueClass = lookupDescriptor.getValueClass();
			Check.assumeNotNull(lookupValueClass, "Parameter lookupValueClass is not null for {}", lookupDescriptor); // shall not happen

			if (widgetValueClass == null)
			{
				return lookupValueClass;
			}
			else if (lookupValueClass.equals(widgetValueClass))
			{
				return lookupValueClass;
			}
			else
			{
				throw new IllegalArgumentException("WidgetType's class is not compatible with LookupDescriptor's class"
						+ "\n WidgetType: " + widgetType
						+ "\n WidgetType value class: " + widgetValueClass
						+ "\n LookupDescriptor: " + lookupDescriptor
						+ "\n LookupDescriptor value class: " + lookupValueClass);
			}
		}

		//
		// Use the standard widget's valueClass, if any
		if (widgetValueClass != null)
		{
			return widgetValueClass;
		}

		//
		// HARDCODED case: if Button, return String
		if (widgetType == DocumentFieldWidgetType.Button)
		{
			return String.class;
		}

		//
		// Fallback
		throw new IllegalArgumentException("No value class found for widgetType=" + widgetType);
	}

	public static DocumentFieldWidgetType extractWidgetType(final String columnName, final int displayType)
	{
		if (displayType == DisplayType.List)
		{
			return DocumentFieldWidgetType.List;
		}
		else if (displayType == DisplayType.Location)
		{
			return DocumentFieldWidgetType.Address;
		}
		else if (displayType == DisplayType.PAttribute)
		{
			return DocumentFieldWidgetType.ProductAttributes;
		}
		else if (displayType == DisplayType.Table)
		{
			return DocumentFieldWidgetType.List;
		}
		else if (displayType == DisplayType.TableDir)
		{
			return DocumentFieldWidgetType.List;
		}
		else if (displayType == DisplayType.Search)
		{
			return DocumentFieldWidgetType.Lookup;
		}
		else if (DisplayType.isAnyLookup(displayType))
		{
			return DocumentFieldWidgetType.Lookup;
		}
		else if (displayType == DisplayType.ID)
		{
			return DocumentFieldWidgetType.Integer;
		}
		//
		//
		else if (displayType == DisplayType.Date)
		{
			return DocumentFieldWidgetType.Date;
		}
		else if (displayType == DisplayType.Time)
		{
			return DocumentFieldWidgetType.Time;
		}
		else if (displayType == DisplayType.DateTime)
		{
			return DocumentFieldWidgetType.DateTime;
		}
		//
		//
		else if (displayType == DisplayType.TextLong || displayType == DisplayType.Memo || displayType == DisplayType.Text)
		{
			return DocumentFieldWidgetType.LongText;
		}
		else if (DisplayType.isText(displayType))
		{
			return DocumentFieldWidgetType.Text;
		}
		//
		//
		else if (DisplayType.Integer == displayType)
		{
			return DocumentFieldWidgetType.Integer;
		}
		else if (displayType == DisplayType.Amount)
		{
			return DocumentFieldWidgetType.Amount;
		}
		else if (displayType == DisplayType.Number)
		{
			return DocumentFieldWidgetType.Number;
		}
		else if (displayType == DisplayType.CostPrice)
		{
			return DocumentFieldWidgetType.CostPrice;
		}
		else if (displayType == DisplayType.Quantity)
		{
			return DocumentFieldWidgetType.Quantity;
		}
		//
		//
		else if (displayType == DisplayType.YesNo)
		{
			if (COLUMNNAMES_Switch.contains(columnName))
			{
				return DocumentFieldWidgetType.Switch;
			}
			return DocumentFieldWidgetType.YesNo;
		}
		else if (displayType == DisplayType.Button)
		{
			return DocumentFieldWidgetType.Button;
		}
		else if (displayType == DisplayType.Image)
		{
			return DocumentFieldWidgetType.Image;
		}
		//
		//
		else
		{
			throw new DocumentLayoutBuildException("Unknown displayType=" + displayType + " of columnName=" + columnName);
		}
	}

	public static DocumentFieldWidgetType extractWidgetType(final String columnName, final int displayType, final LookupDescriptor lookupDescriptor)
	{
		final DocumentFieldWidgetType widgetType = extractWidgetType(columnName, displayType);
		if (lookupDescriptor != null
				&& (widgetType == DocumentFieldWidgetType.List || widgetType == DocumentFieldWidgetType.Lookup))
		{
			final LookupSource lookupSourceType = lookupDescriptor.getLookupSourceType();
			final DocumentFieldWidgetType lookupWidgetType = extractWidgetType(lookupSourceType);
			if (lookupWidgetType != widgetType)
			{
				logger.warn("Inconsistent '{}/{}'({}) vs '{}'({}). Considering the widgetType provided by lookupSourceType." //
						, columnName, displayType, widgetType //
						, lookupSourceType, lookupWidgetType);
			}
			
			return lookupWidgetType;
		}
		
		return widgetType;
	}

	public static final DocumentFieldWidgetType extractWidgetType(final DocumentLayoutElementFieldDescriptor.LookupSource lookupSource)
	{
		Check.assumeNotNull(lookupSource, "Parameter lookupSource is not null");
		switch (lookupSource)
		{
			case list:
				return DocumentFieldWidgetType.List;
			case lookup:
				return DocumentFieldWidgetType.Lookup;
			default:
				throw new AdempiereException("LookupSource " + lookupSource + " is not supported");
		}
	}

	public static DocumentLayoutElementFieldDescriptor.LookupSource extractLookupSource(final int displayType, final int adReferenceValueId)
	{
		if (DisplayType.Search == displayType)
		{
			return DocumentLayoutElementFieldDescriptor.LookupSource.lookup;
		}
		else if (DisplayType.List == displayType)
		{
			return DocumentLayoutElementFieldDescriptor.LookupSource.list;
		}
		else if (DisplayType.TableDir == displayType)
		{
			return DocumentLayoutElementFieldDescriptor.LookupSource.list;
		}
		else if (DisplayType.Table == displayType)
		{
			return DocumentLayoutElementFieldDescriptor.LookupSource.list;
		}
		else if (DisplayType.isAnyLookup(displayType))
		{
			return DocumentLayoutElementFieldDescriptor.LookupSource.lookup;
		}
		else if (DisplayType.Button == displayType && adReferenceValueId > 0)
		{
			return DocumentLayoutElementFieldDescriptor.LookupSource.list;
		}
		else
		{
			return null;
		}
	}
}
