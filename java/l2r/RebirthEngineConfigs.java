package l2r;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import l2r.gameserver.util.Util;

import gr.sr.configsEngine.AbstractConfigs;
import gr.sr.utils.L2Properties;

/**
 * @author vGodFather
 */
public class RebirthEngineConfigs extends AbstractConfigs
{
	private static final String CONFIG_FILE = "./config/extra/Rebirth.ini";
	
	public static int REBIRTH_MIN_LEVEL;
	public static int REBIRTH_MAX;
	public static int REBIRTH_RETURN_TO_LEVEL;
	public static List<Integer> REBIRTH_SKILL_IDS;
	public static int REBIRTH_SKILL_SELLER_ID;
	public static String[] REBIRTH_ITEMS;
	
	@Override
	public void loadConfigs()
	{
		// Load Server L2Properties file (if exists)
		L2Properties Rebirth = new L2Properties();
		final File file = new File(CONFIG_FILE);
		try (InputStream is = new FileInputStream(file))
		{
			Rebirth.load(is);
		}
		catch (Exception e)
		{
			_log.error("Error while loading rebirth system settings!", e);
		}
		
		REBIRTH_SKILL_SELLER_ID = Integer.parseInt(Rebirth.getProperty("RebirthSkillSellerId", "500"));
		REBIRTH_MIN_LEVEL = Integer.parseInt(Rebirth.getProperty("RebirthMinLevel", "78"));
		REBIRTH_MAX = Integer.parseInt(Rebirth.getProperty("RebirthMaxAllowed", "3"));
		REBIRTH_RETURN_TO_LEVEL = Integer.parseInt(Rebirth.getProperty("RebirthReturnToLevel", "1"));
		REBIRTH_ITEMS = Rebirth.getProperty("RebirthItems", "").split(";");
		
		String[] ids = Rebirth.getProperty("RebirthSkillIds", "5200,35201,35202,35203,35203,35204,35205,35206,35207,35208,35209,35210,35211").split(",");
		REBIRTH_SKILL_IDS = new ArrayList<>(ids.length);
		for (String id : ids)
		{
			if (Util.isDigit(id))
			{
				REBIRTH_SKILL_IDS.add(Integer.parseInt(id));
			}
			else
			{
				_log.warn("Wrong skill id format (RebirthSkillIds) rebirth.ini");
			}
		}
	}
	
	public static RebirthEngineConfigs getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final RebirthEngineConfigs _instance = new RebirthEngineConfigs();
	}
}