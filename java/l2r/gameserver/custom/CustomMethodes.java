
package l2r.gameserver.custom;

import l2r.gameserver.model.actor.instance.L2PcInstance;
import l2r.gameserver.model.itemcontainer.Inventory;
import l2r.gameserver.model.items.instance.L2ItemInstance;

import gr.sr.dressmeEngine.DressMeHandler;

public class CustomMethodes
{
	public static void checkForOldVisuals(L2PcInstance player)
	{
		Inventory inv = player.getInventory();
		for (L2ItemInstance item : inv.getItems())
		{
			if (item.getOldVisualItemId() != 0)
			{
				DressMeHandler.visuality(player, item, item.getOldVisualItemId());
			}
		}
	}
}