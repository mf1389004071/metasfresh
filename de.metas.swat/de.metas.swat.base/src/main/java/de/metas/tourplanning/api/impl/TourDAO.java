/**
 * 
 */
package de.metas.tourplanning.api.impl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.ad.dao.IQueryBuilder;
import org.adempiere.ad.dao.IQueryOrderBy.Direction;
import org.adempiere.ad.dao.IQueryOrderBy.Nulls;
import org.adempiere.ad.dao.impl.CompareQueryFilter.Operator;
import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.adempiere.util.proxy.Cached;
import org.adempiere.util.time.generator.DateSequenceGenerator;
import org.adempiere.util.time.generator.Frequency;
import org.adempiere.util.time.generator.FrequencyType;
import org.adempiere.util.time.generator.IDateShifter;
import org.compiere.util.TimeUtil;

import de.metas.adempiere.service.IBusinessDayMatcher;
import de.metas.adempiere.service.ICalendarBL;
import de.metas.adempiere.util.CacheCtx;
import de.metas.adempiere.util.CacheTrx;
import de.metas.tourplanning.api.ITourDAO;
import de.metas.tourplanning.api.ITourVersionRange;
import de.metas.tourplanning.api.impl.TourVersionDeliveryDateShifter.OnNonBussinessDay;
import de.metas.tourplanning.model.I_M_Tour;
import de.metas.tourplanning.model.I_M_TourVersion;
import de.metas.tourplanning.model.I_M_TourVersionLine;
import lombok.NonNull;

/**
 * @author cg
 * 
 */
public class TourDAO implements ITourDAO
{
	@Override
	public List<I_M_Tour> retriveAllTours(final Properties ctx)
	{
		final IQueryBuilder<I_M_Tour> queryBuilder = Services.get(IQueryBL.class)
				.createQueryBuilder(I_M_Tour.class, ctx, ITrx.TRXNAME_None)
				.addOnlyActiveRecordsFilter();

		queryBuilder.orderBy()
				.addColumn(I_M_Tour.COLUMNNAME_M_Tour_ID);

		return queryBuilder
				.create()
				.list();
	}

	@Override
	public List<I_M_TourVersion> retrieveTourVersions(final I_M_Tour tour)
	{
		final Date validFrom = null;
		final Date validTo = null;
		return retrieveTourVersions(tour, validFrom, validTo);
	}

	private IQueryBuilder<I_M_TourVersion> createTourVersionQueryBuilder(final I_M_Tour tour, final Date validFrom, final Date validTo)
	{
		Check.assumeNotNull(tour, "tour not null");

		final IQueryBuilder<I_M_TourVersion> queryBuilder = Services.get(IQueryBL.class)
				.createQueryBuilder(I_M_TourVersion.class, tour)
				.addOnlyActiveRecordsFilter()
				.addEqualsFilter(I_M_TourVersion.COLUMN_M_Tour_ID, tour.getM_Tour_ID())
				.addOnlyActiveRecordsFilter()
				.addOnlyContextClient();

		if (validFrom != null)
		{
			queryBuilder.addCompareFilter(I_M_TourVersion.COLUMNNAME_ValidFrom, Operator.GREATER_OR_EQUAL, validFrom);
		}
		if (validTo != null)
		{
			queryBuilder.addCompareFilter(I_M_TourVersion.COLUMNNAME_ValidFrom, Operator.LESS_OR_EQUAL, validTo);
		}

		return queryBuilder;
	}

	private List<I_M_TourVersion> retrieveTourVersions(final I_M_Tour tour, final Date validFrom, final Date validTo)
	{
		Check.assumeNotNull(tour, "tour not null");

		final IQueryBuilder<I_M_TourVersion> queryBuilder = createTourVersionQueryBuilder(tour, validFrom, validTo);

		queryBuilder.orderBy()
				.clear()
				.addColumn(I_M_TourVersion.COLUMN_ValidFrom, Direction.Ascending, Nulls.First);

		return queryBuilder
				.create()
				.list();
	}

	@Override
	public List<ITourVersionRange> retrieveTourVersionRanges(final I_M_Tour tour, final LocalDate dateFrom, final LocalDate dateTo)
	{
		Check.assumeNotNull(dateFrom, "dateFrom not null");
		Check.assumeNotNull(dateTo, "dateTo not null");
		Check.assume(dateFrom.compareTo(dateTo) <= 0, "dateFrom({}) <= dateTo({})", dateFrom, dateTo);

		//
		// Retrieve all tour versions in our scope
		// NOTE: we assume they are already ordered by ValidFrom
		// NOTE2: we are not restricting the dateFrom because we want to also get the tour version which is currently active at the beginning of our interval
		final List<I_M_TourVersion> tourVersions = retrieveTourVersions(tour, null, TimeUtil.asTimestamp(dateTo));
		if (tourVersions.isEmpty())
		{
			return Collections.emptyList();
		}

		//
		// Continue iterating the tour versions and create Tour Version Ranges
		List<ITourVersionRange> tourVersionRanges = new ArrayList<>();
		boolean previousTourVersionValid = false;
		I_M_TourVersion previousTourVersion = null;
		LocalDate previousTourVersionValidFrom = null;

		final Iterator<I_M_TourVersion> tourVersionsIterator = tourVersions.iterator();
		while (tourVersionsIterator.hasNext())
		{
			final I_M_TourVersion tourVersion = tourVersionsIterator.next();

			final LocalDate tourVersionValidFrom = TimeUtil.asLocalDate(tourVersion.getValidFrom());
			Check.assumeNotNull(tourVersionValidFrom, "tourVersionValidFrom not null");

			//
			// Guard: tour version's ValidFrom shall be before "dateTo"
			if (tourVersionValidFrom.compareTo(dateTo) > 0)
			{
				// shall not happen because we retrieved until dateTo, but just to make sure
				break;
			}

			//
			// Case: We are still searching for first tour version to consider
			if (!previousTourVersionValid)
			{
				// Case: our current tour version is before given dateFrom
				if (tourVersionValidFrom.compareTo(dateFrom) < 0)
				{
					if (tourVersionsIterator.hasNext())
					{
						// do nothing, let's see what we get next
					}
					else
					{
						// there is no other next, so we need to consider this one
						previousTourVersion = tourVersion;
						previousTourVersionValidFrom = dateFrom;
						previousTourVersionValid = true;
						continue;
					}
				}
				// Case: our current tour version starts exactly on our given dateFrom
				else if (tourVersionValidFrom.compareTo(dateFrom) == 0)
				{
					previousTourVersion = tourVersion;
					previousTourVersionValidFrom = dateFrom;
					previousTourVersionValid = true;
					continue;
				}
				// Case: our current tour version start after our given dateFrom
				else
				{
					// Check if we have a previous tour version, because if we have, that shall be the first tour version to consider
					if (previousTourVersion != null)
					{
						// NOTE: we consider dateFrom as first date because tour version's ValidFrom is before dateFrom
						previousTourVersionValidFrom = dateFrom;
						previousTourVersionValid = true;
						// don't continue: we got it right now
						// continue;
					}
					// ... else it seems this is the first tour version which actually starts after our dateFrom
					else
					{
						previousTourVersion = tourVersion;
						previousTourVersionValidFrom = tourVersionValidFrom;
						previousTourVersionValid = true;
						continue;
					}
				}

			}

			//
			// Case: we do have a previous valid tour version to consider so we can generate tour ranges
			if (previousTourVersionValid)
			{
				final LocalDate previousTourVersionValidTo = tourVersionValidFrom.minusDays(1);
				final ITourVersionRange previousTourVersionRange = createTourVersionRange(previousTourVersion, previousTourVersionValidFrom, previousTourVersionValidTo);
				tourVersionRanges.add(previousTourVersionRange);
			}

			//
			// Set current as previous and move on
			previousTourVersion = tourVersion;
			previousTourVersionValidFrom = tourVersionValidFrom;
		}

		//
		// Create Tour Version Range for last version
		if (previousTourVersionValid)
		{
			final ITourVersionRange lastTourVersionRange = createTourVersionRange(previousTourVersion, previousTourVersionValidFrom, dateTo);
			tourVersionRanges.add(lastTourVersionRange);
		}

		return tourVersionRanges;
	}

	@Override
	public List<I_M_TourVersionLine> retrieveTourVersionLines(final I_M_TourVersion tourVersion)
	{
		Check.assumeNotNull(tourVersion, "tourVersion not null");
		final Properties ctx = InterfaceWrapperHelper.getCtx(tourVersion);
		final String trxName = InterfaceWrapperHelper.getTrxName(tourVersion);
		final int tourVersionId = tourVersion.getM_TourVersion_ID();

		final List<I_M_TourVersionLine> tourVersionLines = retrieveTourVersionLines(ctx, tourVersionId, trxName);

		// Optimization: set M_TourVersion model in case somebody want to get it, it shall retrieve the cached version
		for (I_M_TourVersionLine tourVersionLine : tourVersionLines)
		{
			tourVersionLine.setM_TourVersion(tourVersion);
		}

		return tourVersionLines;
	}

	@Cached(cacheName = I_M_TourVersionLine.Table_Name
			+ "#by"
			+ "#" + I_M_TourVersionLine.COLUMNNAME_M_TourVersion_ID)
	List<I_M_TourVersionLine> retrieveTourVersionLines(@CacheCtx final Properties ctx, final int tourVersionId, @CacheTrx final String trxName)
	{
		final IQueryBuilder<I_M_TourVersionLine> queryBuilder = Services.get(IQueryBL.class)
				.createQueryBuilder(I_M_TourVersionLine.class, ctx, trxName)
				.addOnlyActiveRecordsFilter()
				.addEqualsFilter(I_M_TourVersionLine.COLUMN_M_TourVersion_ID, tourVersionId)
				.addOnlyActiveRecordsFilter()
				.addOnlyContextClient();

		queryBuilder.orderBy()
				.addColumn(I_M_TourVersionLine.COLUMN_SeqNo, Direction.Ascending, Nulls.First);

		final List<I_M_TourVersionLine> tourVersionLines = queryBuilder
				.create()
				.list();

		return tourVersionLines;
	}

	private static TourVersionRange createTourVersionRange(final I_M_TourVersion tourVersion, final LocalDate validFrom, final LocalDate validTo)
	{
		return TourVersionRange.builder()
				.tourVersion(tourVersion)
				.validFrom(validFrom)
				.validTo(validTo)
				.dateSequenceGenerator(createDateSequenceGenerator(tourVersion, validFrom, validTo))
				.build();
	}

	private static DateSequenceGenerator createDateSequenceGenerator(final I_M_TourVersion tourVersion, final LocalDate validFrom, final LocalDate validTo)
	{
		final Frequency frequency = extractFrequency(tourVersion);
		if (frequency == null)
		{
			return null;
		}

		final OnNonBussinessDay onNonBusinessDay = extractOnNonBussinessDayOrNull(tourVersion);

		return DateSequenceGenerator.builder()
				.dateFrom(validFrom)
				.dateTo(validTo)
				.shifter(createDateShifter(frequency, onNonBusinessDay))
				// task 08252: don't shift beyond getValidTo(), because there will probably be another version to create it's own delivery days at that date
				.enforceDateToAfterShift(true)
				.frequency(frequency)
				.build();
	}

	private static IDateShifter createDateShifter(final Frequency frequency, final OnNonBussinessDay onNonBusinessDay)
	{
		final ICalendarBL calendarBL = Services.get(ICalendarBL.class);
		IBusinessDayMatcher businessDayMatcher = calendarBL.createBusinessDayMatcher();

		//
		// If user explicitly asked for a set of week days, don't consider them non-business days by default
		if (frequency.isWeekly()
				&& frequency.isOnlySomeDaysOfTheWeek()
				&& onNonBusinessDay == null)
		{
			businessDayMatcher = businessDayMatcher.removeWeekendDays(frequency.getOnlyDaysOfWeek());
		}

		return TourVersionDeliveryDateShifter.builder()
				.businessDayMatcher(businessDayMatcher)
				.onNonBussinessDay(onNonBusinessDay != null ? onNonBusinessDay : OnNonBussinessDay.Cancel)
				.build();
	}

	private static OnNonBussinessDay extractOnNonBussinessDayOrNull(final I_M_TourVersion tourVersion)
	{
		if (tourVersion.isCancelDeliveryDay())
		{
			return OnNonBussinessDay.Cancel;
		}
		else if (tourVersion.isMoveDeliveryDay())
		{
			return OnNonBussinessDay.MoveToNextBusinessDay;
		}
		else
		{
			return null; // N/A
		}
	}

	private static Frequency extractFrequency(final I_M_TourVersion tourVersion)
	{
		//
		// Get and adjust the parameters
		boolean isWeekly = tourVersion.isWeekly();
		int everyWeek = tourVersion.getEveryWeek();
		if (isWeekly)
		{
			everyWeek = 1;
		}
		else if (!isWeekly && everyWeek > 0)
		{
			isWeekly = true;
		}

		boolean isMonthly = tourVersion.isMonthly();
		int everyMonth = tourVersion.getEveryMonth();
		final int monthDay = tourVersion.getMonthDay();
		if (isMonthly)
		{
			everyMonth = 1;
		}
		else if (!isMonthly && everyMonth > 0)
		{
			isMonthly = true;
		}

		if (isWeekly)
		{
			return Frequency.builder()
					.type(FrequencyType.Weekly)
					.everyNthWeek(everyWeek)
					.onlyDaysOfWeek(extractWeekDays(tourVersion))
					.build();
		}
		else if (isMonthly)
		{
			return Frequency.builder()
					.type(FrequencyType.Monthly)
					.everyNthMonth(1)
					.onlyDayOfMonth(monthDay)
					.build();
		}
		else
		{
			return null;
		}
	}

	private static Set<DayOfWeek> extractWeekDays(@NonNull final I_M_TourVersion tourVersion)
	{
		final Set<DayOfWeek> weekDays = new HashSet<>();
		if (tourVersion.isOnSunday())
		{
			weekDays.add(DayOfWeek.SUNDAY);
		}
		if (tourVersion.isOnMonday())
		{
			weekDays.add(DayOfWeek.MONDAY);
		}
		if (tourVersion.isOnTuesday())
		{
			weekDays.add(DayOfWeek.TUESDAY);
		}
		if (tourVersion.isOnWednesday())
		{
			weekDays.add(DayOfWeek.WEDNESDAY);
		}
		if (tourVersion.isOnThursday())
		{
			weekDays.add(DayOfWeek.THURSDAY);
		}
		if (tourVersion.isOnFriday())
		{
			weekDays.add(DayOfWeek.FRIDAY);
		}
		if (tourVersion.isOnSaturday())
		{
			weekDays.add(DayOfWeek.SATURDAY);
		}

		return weekDays;
	}
}
