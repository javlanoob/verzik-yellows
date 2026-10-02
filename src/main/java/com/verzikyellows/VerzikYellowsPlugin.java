package com.verzikyellows;

import javax.inject.Inject;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Verzik Yellows",
	description = "Renders yellows at Verzik P3 above everything else",
	tags = {"verzik", "yellow", "yellows", "tob", "theatre", "blood", "raid"}
)
public class VerzikYellowsPlugin extends Plugin
{
	@Inject
	private OverlayManager overlayManager;

	@Inject
	private VerzikYellowsOverlay overlay;

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
	}
}
