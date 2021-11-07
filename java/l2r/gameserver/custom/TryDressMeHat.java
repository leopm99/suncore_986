package l2r.gameserver.custom;

import l2r.gameserver.model.actor.instance.L2PcInstance;
import l2r.gameserver.model.items.instance.L2ItemInstance;
import l2r.gameserver.network.SystemMessageId;

import gr.sr.dressmeEngine.DressMeHandler;

public class TryDressMeHat implements Runnable
{
	private final L2ItemInstance itemToRemove;
	private final L2PcInstance p;
	
	public TryDressMeHat(L2PcInstance player, L2ItemInstance item)
	{
		itemToRemove = item;
		p = player;
	}
	
	@Override
	public void run()
	{
		DressMeHandler.visualityHat(p, itemToRemove, itemToRemove.getOldVisualItemId());
		itemToRemove.setOldVisualItemId(0);
		p.sendPacket(SystemMessageId.NO_LONGER_TRYING_ON);
		p.broadcastUserInfo();
	}
}