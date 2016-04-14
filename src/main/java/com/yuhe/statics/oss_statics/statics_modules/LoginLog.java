package com.yuhe.statics.oss_statics.statics_modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.yuhe.statics.oss_statics.db.ServerDB;
import com.yuhe.statics.oss_statics.db.UserInfoDB;
import com.yuhe.statics.oss_statics.db.log.LoginLogDB;
import com.yuhe.statics.oss_statics.utils.RegUtils;

import net.sf.json.JSONObject;

/**
 * 解析登陆日志
 * 
 * @author xiongyunkun
 *
 */
public class LoginLog extends AbstractStaticsModule {
	// 需要提取的列，注意要跟数据库中对应，前面的HostID和后面的时间都不要在这里填
	private static final String[] LOG_COLS = { "Uid", "Name", "Urs", "Level", "Ip", "PhoneInfo", "SDKInfo" };
	public static Logger logger = Logger.getLogger(LoginLog.class);

	@Override
	public boolean execute(List<String> logList) {
		Map<String, List<Map<String, String>>> platformResults = new HashMap<String, List<Map<String, String>>>();
		Map<String, String> staticsHosts = ServerDB.getStaticsServers();
		for (String logStr : logList) {
			JSONObject json = JSONObject.fromObject(logStr);
			if (json != null) {
				String message = json.getString("message");
				String hostID = json.getString("hostid");
				if (!message.isEmpty() && !message.equals(" ") && staticsHosts.containsKey(hostID)) {
					Map<String, String> map = new HashMap<String, String>();
					map.put("HostID", hostID);
					String time = RegUtils.getLogTime(message);
					map.put("Time", time);
					String sdkInfo = "";
					for (String col : LOG_COLS) {
						String value = RegUtils.getLogValue(message, col, "");
						map.put(col, value);
						if (col.equals("SDKInfo")) {
							sdkInfo = value;
						}
					}
					String platformID = ServerDB.getPlatformIDBySDKID(sdkInfo);
					List<Map<String, String>> platformResult = platformResults.get(platformID);
					if (platformResult == null)
						platformResult = new ArrayList<Map<String, String>>();
					platformResult.add(map);
					platformResults.put(platformID, platformResult);
				}
			}
		}
		// 插入数据库
		Iterator<String> it = platformResults.keySet().iterator();
		while (it.hasNext()) {
			String platformID = it.next();
			List<Map<String, String>> platformResult = platformResults.get(platformID);
			LoginLogDB db = new LoginLogDB();
			db.batchInsert(platformID, platformResult);
			UserInfoDB.batchInsert(platformID, platformResult);
		}
		return true;
	}

}