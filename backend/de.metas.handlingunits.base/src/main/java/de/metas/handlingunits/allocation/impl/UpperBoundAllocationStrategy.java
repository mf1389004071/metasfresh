package de.metas.handlingunits.allocation.impl;

import javax.annotation.Nullable;

import org.adempiere.exceptions.AdempiereException;

import de.metas.handlingunits.allocation.IAllocationRequest;
import de.metas.handlingunits.allocation.IAllocationResult;
import de.metas.handlingunits.allocation.IAllocationStrategyFactory;
import de.metas.handlingunits.model.I_M_HU_Item;
import de.metas.handlingunits.model.I_M_HU_PI_Item_Product;
import de.metas.handlingunits.model.X_M_HU_PI_Item;
import de.metas.handlingunits.storage.IHUItemStorage;
import de.metas.quantity.Capacity;
import lombok.NonNull;
import lombok.ToString;

/**
 * This classe's {@link #getHUItemStorage(I_M_HU_Item, IAllocationRequest)} can return a storage with an
 * "artificial" uppper bound that is different from the capacity defined in {@link I_M_HU_PI_Item_Product}.
 *
 * @author metas-dev <dev@metasfresh.com>
 *
 */
@ToString
public class UpperBoundAllocationStrategy extends AbstractFIFOStrategy
{
	@Nullable
	private final Capacity capacityOverride;

	/**
	 * @param capacityOverride optional capacity that can override the one from the packing instructions.
	 */
	public UpperBoundAllocationStrategy(
			@Nullable final Capacity capacityOverride,
			@NonNull final AllocationStrategySupportingServicesFacade services,
			@Nullable final IAllocationStrategyFactory allocationStrategyFactory)
	{
		super(AllocationDirection.INBOUND_ALLOCATION,
				services,
				allocationStrategyFactory);

		this.capacityOverride = capacityOverride;
	}

	@Override
	protected IHUItemStorage getHUItemStorage(final I_M_HU_Item item, final IAllocationRequest request)
	{
		final IHUItemStorage storage = super.getHUItemStorage(item, request);

		// make sure that the capacity is forced by the user, not the system
		// If capacityOverride is null it means that we were asked to take the defaults
		if (capacityOverride != null && !storage.isPureVirtual())
		{
			storage.setCustomCapacity(capacityOverride);
		}

		return storage;
	}

	@Override
	protected IAllocationResult allocateOnIncludedHUItem(
			@NonNull final I_M_HU_Item item,
			@NonNull final IAllocationRequest request)
	{
		// Prevent allocating on a included HU item
		final String itemType = services.getItemType(item);
		if (X_M_HU_PI_Item.ITEMTYPE_HandlingUnit.equals(itemType))
		{
			if (services.isDeveloperMode())
			{
				throw new AdempiereException("HUs which are used in " + this + " shall not have included HUs. They shall be pure TUs."
						+ "\n Item: " + item
						// + "\n PI: " + handlingUnitsBL.getPI(item.getM_HU()).getName()
						+ "\n Request: " + request);
			}
			return AllocationUtils.nullResult();
		}

		//
		// We are about to allocate to Virtual HUs linked to given "item" (which shall be of type Material).
		// In this case we rely on the standard logic.
		return super.allocateOnIncludedHUItem(item, request);
	}

	/**
	 * Does nothing, returns null result.
	 */
	@Override
	protected IAllocationResult allocateRemainingOnIncludedHUItem(final I_M_HU_Item item, final IAllocationRequest request)
	{
		return AllocationUtils.nullResult();
	}
}
