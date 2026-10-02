package com.verzikyellows;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class VerzikYellowsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(VerzikYellowsPlugin.class);
		RuneLite.main(args);
	}
}
