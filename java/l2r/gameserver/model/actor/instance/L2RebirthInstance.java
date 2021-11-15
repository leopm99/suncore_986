package l2r.gameserver.model.actor.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;

import l2r.L2DatabaseFactory;
import l2r.RebirthEngineConfigs;
import l2r.gameserver.data.xml.impl.ExperienceData;
import l2r.gameserver.model.actor.templates.L2NpcTemplate;
import l2r.gameserver.model.items.instance.L2ItemInstance;
import l2r.gameserver.model.skills.L2Skill;
import l2r.gameserver.network.SystemMessageId;
import l2r.gameserver.network.serverpackets.ActionFailed;
import l2r.gameserver.network.serverpackets.ExShowScreenMessage;
import l2r.gameserver.network.serverpackets.MagicSkillUse;
import l2r.gameserver.network.serverpackets.NpcHtmlMessage;
import l2r.gameserver.network.serverpackets.SocialAction;
import l2r.gameserver.network.serverpackets.StatusUpdate;
import l2r.gameserver.network.serverpackets.SystemMessage;

/**
 * @author vGodFather
 */
public final class L2RebirthInstance extends L2NpcInstance
{
	private final String HTML_LOC = "data/html/sunrise/Rebirth/";
	private static HashMap<Integer, Integer> _rebirthInfo = new HashMap<>();
	
	public L2RebirthInstance(L2NpcTemplate template)
	{
		super(template);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("performRebirth"))
		{
			// Maximum rebirth count. Return the player's current Rebirth Level.
			int currBirth = getRebirthLevel(player);
			if (currBirth >= RebirthEngineConfigs.REBIRTH_MAX)
			{
				// Send a Server->Client NpcHtmlMessage containing the text of the L2Npc to the L2PcInstance
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, player.getHtmlPrefix(), HTML_LOC + "rebirth-max.htm");
				player.sendPacket(html);
				return;
			}
			
			// Level requirement for a rebirth.
			if (player.getLevel() < RebirthEngineConfigs.REBIRTH_MIN_LEVEL)
			{
				// Send a Server->Client NpcHtmlMessage containing the text of the L2Npc to the L2PcInstance
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, player.getHtmlPrefix(), HTML_LOC + "rebirth-level.htm");
				player.sendPacket(html);
				return;
			}
			
			int itemId = 0;
			int itemCount = 0;
			int loopBirth = 0;
			for (String readItems : RebirthEngineConfigs.REBIRTH_ITEMS)
			{
				String[] currItem = readItems.split(",");
				if (loopBirth == currBirth)
				{
					itemId = Integer.parseInt(currItem[0]);
					itemCount = Integer.parseInt(currItem[1]);
					break;
				}
				loopBirth++;
			}
			
			// Rewards the player with an item.
			rebirthItemReward(player, itemId, itemCount);
			
			// Check and see if its the player's first rebirth calling.
			boolean firstBirth = currBirth == 0;
			
			// Player meets requirements and starts Rebirth process.
			rebirthRequirements(player, (currBirth + 1), firstBirth);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	@Override
	public void showChatWindow(L2PcInstance player)
	{
		// Send a Server->Client NpcHtmlMessage containing the text of the L2Npc to the L2PcInstance
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		
		// Html contents.
		html.setFile(player, player.getHtmlPrefix(), HTML_LOC + "rebirth.htm");
		html.replace("%objectId%", getObjectId());
		html.replace("%level%", +RebirthEngineConfigs.REBIRTH_RETURN_TO_LEVEL);
		
		// Send a packet to the L2PcInstance.
		player.sendPacket(html);
		
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Physically rewards player and resets status to nothing.
	 * @param player the player
	 * @param newBirthCount the new birth count
	 * @param firstBirth the first birth
	 */
	public void rebirthRequirements(L2PcInstance player, int newBirthCount, boolean firstBirth)
	{
		try
		{
			// Delevel
			int lvl = RebirthEngineConfigs.REBIRTH_RETURN_TO_LEVEL;
			if ((lvl >= 1) && (lvl <= ExperienceData.getInstance().getMaxLevel()))
			{
				long pXp = player.getExp();
				long tXp = ExperienceData.getInstance().getExpForLevel(lvl);
				
				player.removeExpAndSp(pXp - tXp, 0);
				
				if (firstBirth)
				{
					storePlayerBirth(player);
				}
				else
				{
					updatePlayerBirth(player, newBirthCount);
				}
				
				// Remove the player's current skills.
				for (L2Skill skill : player.getAllSkills())
				{
					player.removeSkill(skill);
				}
				
				// Give all available skills to the player.
				player.giveAvailableAutoGetSkills();
				
				// Updates the player's information in the Character Database.
				player.store();
				player.broadcastStatusUpdate();
				player.broadcastUserInfo();
				
				// Send a Server->Client NpcHtmlMessage containing the text of the L2Npc to the L2PcInstance
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				
				// Html contents.
				html.setFile(player, player.getHtmlPrefix(), HTML_LOC + "rebirth-successfully.htm");
				html.replace("%level%", +RebirthEngineConfigs.REBIRTH_RETURN_TO_LEVEL);
				
				// Send a packet to the L2PcInstance.
				player.sendPacket(html);
				
				displayCongrats(player);// Displays a congratulation message to the player.
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Rewards the player with an item.
	 * @param player the player
	 * @param itemId : Identifier of the item.
	 * @param itemCount : Quantity of items to add.
	 */
	public static void rebirthItemReward(L2PcInstance player, int itemId, int itemCount)
	{
		// Incorrect amount.
		if (itemCount <= 0)
		{
			return;
		}
		
		final L2ItemInstance item = player.getInventory().addItem("Rebirth", itemId, itemCount, player, player);
		if (item == null)
		{
			return;
		}
		
		// Send message to the client.
		SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1);
		smsg.addItemName(itemId);
		player.sendPacket(smsg);
		
		// Send status update packet.
		StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
	}
	
	/**
	 * Return the player's current Rebirth Level.
	 * @param player the player
	 * @return the rebirth level
	 */
	public static int getRebirthLevel(L2PcInstance player)
	{
		int playerId = player.getObjectId();
		if (_rebirthInfo.get(playerId) == null)
		{
			loadRebirthInfo(player);
		}
		
		return _rebirthInfo.get(playerId);
	}
	
	/**
	 * Special effects when the player levels.
	 * @param player
	 */
	public void displayCongrats(L2PcInstance player)
	{
		player.broadcastPacket(new SocialAction(player.getObjectId(), 3));// Victory Social Action.
		MagicSkillUse MSU = new MagicSkillUse(player, player, 2024, 1, 1, 0);// Fireworks Display
		player.broadcastPacket(MSU);
		ExShowScreenMessage screen = new ExShowScreenMessage("Congratulations " + player.getName() + "! You have been Reborn!", 15000);
		player.sendPacket(screen);
	}
	
	/**
	 * Database caller to retrieve player's current Rebirth Level.
	 * @param player the player
	 */
	public static void loadRebirthInfo(L2PcInstance player)
	{
		int playerId = player.getObjectId();
		int rebirthCount = 0;
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM `character_rebirths` WHERE playerId = ?"))
		{
			ps.setInt(1, playerId);
			
			try (ResultSet rset = ps.executeQuery())
			{
				while (rset.next())
				{
					rebirthCount = rset.getInt("rebirthCount");
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		_rebirthInfo.put(playerId, rebirthCount);
	}
	
	/**
	 * Stores the player's information in the DB.
	 * @param player the player
	 */
	public static void storePlayerBirth(L2PcInstance player)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO `character_rebirths` (playerId,rebirthCount) VALUES (?,1)"))
		{
			ps.setInt(1, player.getObjectId());
			ps.execute();
			
			_rebirthInfo.put(player.getObjectId(), 1);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Updates the player's information in the DB.
	 * @param player the player
	 * @param newRebirthCount the new rebirth count
	 */
	public static void updatePlayerBirth(L2PcInstance player, int newRebirthCount)
	{
		int playerId = player.getObjectId();
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE `character_rebirths` SET rebirthCount = ? WHERE playerId = ?"))
		{
			ps.setInt(1, newRebirthCount);
			ps.setInt(2, playerId);
			ps.execute();
			
			_rebirthInfo.put(playerId, newRebirthCount);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
