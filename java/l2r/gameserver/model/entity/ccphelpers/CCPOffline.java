package l2r.gameserver.model.entity.ccphelpers;

import l2r.Config;
import l2r.gameserver.enums.ZoneIdType;
import l2r.gameserver.model.actor.instance.L2PcInstance;
import l2r.gameserver.model.entity.olympiad.OlympiadManager;

public class CCPOffline
{
	public static boolean setOfflineStore(L2PcInstance activeChar)
	{
		if (!Config.OFFLINE_TRADE_ENABLE)
		{
			activeChar.sendMessage("This option is currently disabled!");
			return false;
		}
		
		if (activeChar.isInOlympiadMode() || activeChar.inObserverMode() || OlympiadManager.getInstance().isRegistered(activeChar) || (activeChar.getKarma() > 0))
		{
			activeChar.sendMessage("You cannot do it right now!");
			return false;
		}
		
		if (!activeChar.isInsideZone(ZoneIdType.PEACE) && Config.OFFLINE_MODE_IN_PEACE_ZONE)
		{
			activeChar.sendMessage("You cannot set offline store in this area!");
			return false;
		}
		
		if (!activeChar.isInStoreMode())
		{
			activeChar.sendMessage("You need to place Private Store first!");
			return false;
		}
		
		activeChar.logout();
		return true;
	}
}
