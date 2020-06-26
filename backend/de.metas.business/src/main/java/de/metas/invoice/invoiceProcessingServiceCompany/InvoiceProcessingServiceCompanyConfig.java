/*
 * #%L
 * de.metas.business
 * %%
 * Copyright (C) 2020 metas GmbH
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

package de.metas.invoice.invoiceProcessingServiceCompany;

import com.google.common.collect.ImmutableList;
import de.metas.bpartner.BPartnerId;
import de.metas.document.DocTypeId;
import de.metas.product.ProductId;
import de.metas.util.lang.Percent;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.Optional;

@Value
@Builder
public class InvoiceProcessingServiceCompanyConfig
{
	@NonNull
	BPartnerId serviceCompanyBPartnerId;

	@NonNull
	DocTypeId serviceInvoiceDocTypeId;

	@NonNull
	ProductId serviceFeeProductId;

	@NonNull ZonedDateTime validFrom;

	@Getter(AccessLevel.NONE)
	@NonNull ImmutableList<InvoiceProcessingServiceCompanyConfigBPartnerDetails> bpartnerDetails;

	public ImmutableList<BPartnerId> getBPartnerIds()
	{
		return bpartnerDetails.stream()
				.map(InvoiceProcessingServiceCompanyConfigBPartnerDetails::getBpartnerId)
				.collect(ImmutableList.toImmutableList());
	}

	public Optional<Percent> getFeePercentageOfGrandTotalByBpartner(@NonNull final BPartnerId bpartnerId)
	{
		return bpartnerDetails.stream()
				.filter(it -> it.getBpartnerId().equals(bpartnerId))
				.map(InvoiceProcessingServiceCompanyConfigBPartnerDetails::getPercent)
				.findFirst();
	}
}

@Builder
@Value
		/* package */ class InvoiceProcessingServiceCompanyConfigBPartnerDetails
{
	@NonNull
	BPartnerId bpartnerId;

	@NonNull
	Percent percent;
}
