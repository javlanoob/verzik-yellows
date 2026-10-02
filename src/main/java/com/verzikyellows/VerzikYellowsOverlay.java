package com.verzikyellows;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.Model;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class VerzikYellowsOverlay extends Overlay
{
	private final Client client;
	private final ModelRenderer renderer;

	/**
	 * The pools as of this frame. Kept rather than made each time, since this is walked for every frame
	 * of the fight and most of those frames have nothing in it.
	 */
	private final List<GraphicsObject> pools = new ArrayList<>();

	@Inject
	VerzikYellowsOverlay(Client client)
	{
		setPosition(OverlayPosition.DYNAMIC);

		// The game has finished with the scene by the time an overlay is drawn, so a pool drawn here lands
		// over Verzik however far behind her it stands. Under the game's own panels, so it does not come
		// out over the inventory either
		setLayer(OverlayLayer.ABOVE_SCENE);
		this.client = client;
		this.renderer = new ModelRenderer(client);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		WorldView worldView = client.getTopLevelWorldView();

		find(worldView);

		if (pools.isEmpty())
		{
			return null;
		}

		Object antialias = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		Shape clip = graphics.getClip();

		// The game draws a face with hard edges, and faces that meet along an edge would each be smoothed
		// into the other, leaving a seam down the middle of the model where there is none
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

		keepPlayerInFront(graphics, clip);

		for (GraphicsObject pool : pools)
		{
			draw(graphics, worldView, pool);
		}

		// Put back afterwards, since the graphics goes on to the overlays after this one
		graphics.setClip(clip);
		graphics.setRenderingHint(
			RenderingHints.KEY_ANTIALIASING,
			antialias == null ? RenderingHints.VALUE_ANTIALIAS_DEFAULT : antialias);

		pools.clear();

		return null;
	}

	private void draw(Graphics2D graphics, WorldView worldView, GraphicsObject pool)
	{
		LocalPoint location = pool.getLocation();
		Model model = pool.getModel();

		if (location == null || model == null)
		{
			// Still on its way in, so the game has nothing to draw for it either
			return;
		}

		WorldView own = pool.getWorldView();

		renderer.draw(
			graphics,
			own == null ? worldView : own,
			model,
			location.getX(),
			location.getY(),
			// Where the game has the model standing, which an animation can lift off the floor
			pool.getZ() - pool.getAnimationHeightOffset());
	}

	/**
	 * Where the pools are, as of this frame. Read out of the scene rather than remembered from when they
	 * were created, so a pool that ends early, or a scene thrown away on the way out of the room, takes
	 * its own drawing with it and there is no list of them to go stale.
	 */
	private void find(WorldView worldView)
	{
		pools.clear();

		if (worldView == null)
		{
			return;
		}

		for (GraphicsObject object : worldView.getGraphicsObjects())
		{
			if (isPool(object.getId()) && !object.finished())
			{
				pools.add(object);
			}
		}
	}

	/**
	 * Whether a spotanim is one of the pools. The game has two of them, the second being the one her
	 * quicker attack puts down, and both are drawn the same way.
	 */
	private static boolean isPool(int spotanim)
	{
		return spotanim == SpotanimID.VERZIK_POWERBLAST_SAFEZONE
			|| spotanim == SpotanimID.VERZIK_POWERBLAST_SAFEZONE_QUICK;
	}

	/**
	 * Keeps the drawing off the player, so that what the game drew where they are stays showing. Without
	 * this a pool they are standing in is drawn over the top of them, since the whole point of drawing it
	 * here is that it goes over whatever the game had put there.
	 *
	 * What is kept clear is the envelope the game keeps around the player rather than their outline, so
	 * the gap is a little wider than they are, and anything the game drew nearer the camera than them
	 * shows through it as well.
	 */
	private void keepPlayerInFront(Graphics2D graphics, Shape clip)
	{
		Player player = client.getLocalPlayer();
		Shape hull = player == null ? null : player.getConvexHull();

		if (hull == null || clip == null)
		{
			return;
		}

		Rectangle bounds = clip.getBounds();

		if (bounds.isEmpty() || !hull.intersects(bounds))
		{
			return;
		}

		Area area = new Area(bounds);
		area.subtract(new Area(hull));
		graphics.setClip(area);
	}
}
