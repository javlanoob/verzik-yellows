package com.verzikyellows;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.Perspective;
import net.runelite.api.TextureProvider;
import net.runelite.api.WorldView;

/**
 * Draws a model onto the canvas the way the game would have drawn it, a face at a time.
 *
 * The game draws the scene and then hands the canvas to the overlays, so anything drawn here lands on
 * top of everything in the scene whatever it is standing behind. That is the whole point of doing this
 * rather than colouring in the tile: what comes out is the model itself, in its own shape and its own
 * colours, at whatever frame of its animation the game has it on.
 */
class ModelRenderer
{
	/**
	 * A face with this for its third colour is drawn flat, in the first colour, rather than shaded across
	 * its corners.
	 */
	private static final int FLAT = -1;

	/**
	 * A face with this for its third colour is not drawn at all.
	 */
	private static final int HIDDEN = -2;

	private final Client client;

	/**
	 * Where each corner of the model lands on the canvas. Kept and grown rather than made for each model,
	 * since this runs for every pool of every frame.
	 */
	private int[] canvasX = new int[0];
	private int[] canvasY = new int[0];

	/**
	 * The one triangle every face is drawn with, filled in again for each of them, since a model is
	 * hundreds of faces and a shape for every one of them is hundreds of objects a frame.
	 */
	private final Polygon face = new Polygon(new int[3], new int[3], 3);

	/**
	 * The colours of the brightness the player is on, built again if they change it.
	 */
	private Palette palette;

	ModelRenderer(Client client)
	{
		this.client = client;
	}

	/**
	 * Draws the model at a point in the world, where any of it lands on the canvas at all.
	 */
	void draw(Graphics2D graphics, WorldView worldView, Model model, int x, int y, int z)
	{
		int vertices = model.getVerticesCount();
		int faces = model.getFaceCount();

		if (vertices < 3 || faces < 1)
		{
			return;
		}

		project(worldView, model, vertices, x, y, z);

		int[] a = model.getFaceIndices1();
		int[] b = model.getFaceIndices2();
		int[] c = model.getFaceIndices3();
		int[] colors1 = model.getFaceColors1();
		int[] colors2 = model.getFaceColors2();
		int[] colors3 = model.getFaceColors3();
		byte[] transparencies = model.getFaceTransparencies();
		short[] textures = model.getFaceTextures();

		for (int i = 0; i < faces; i++)
		{
			if (colors3 != null && colors3[i] == HIDDEN)
			{
				continue;
			}

			if (!shape(a[i], b[i], c[i], vertices))
			{
				continue;
			}

			Color color = color(i, colors1, colors2, colors3, transparencies, textures);

			if (color == null)
			{
				continue;
			}

			graphics.setColor(color);
			graphics.fill(face);
		}
	}

	/**
	 * Where every corner of the model lands on the canvas. The game keeps a model standing up the other
	 * way round from the way it projects one, so the up and the along arrays go in swapped.
	 */
	private void project(WorldView worldView, Model model, int vertices, int x, int y, int z)
	{
		if (canvasX.length < vertices)
		{
			canvasX = new int[vertices];
			canvasY = new int[vertices];
		}

		Perspective.modelToCanvas(
			client,
			worldView,
			vertices,
			x,
			y,
			z,
			0,
			model.getVerticesX(),
			model.getVerticesZ(),
			model.getVerticesY(),
			canvasX,
			canvasY);
	}

	/**
	 * Puts the three corners of a face into the triangle, or says no where the face has nothing to draw.
	 * A corner the game could not place comes back as a long way off the canvas, so a face with one of
	 * those is behind the camera rather than small.
	 */
	private boolean shape(int a, int b, int c, int vertices)
	{
		if (a < 0 || b < 0 || c < 0 || a >= vertices || b >= vertices || c >= vertices)
		{
			return false;
		}

		int ax = canvasX[a];
		int bx = canvasX[b];
		int cx = canvasX[c];

		if (ax == Integer.MIN_VALUE || bx == Integer.MIN_VALUE || cx == Integer.MIN_VALUE)
		{
			return false;
		}

		int ay = canvasY[a];
		int by = canvasY[b];
		int cy = canvasY[c];

		// Nothing between the three of them, so there are no pixels in it to fill
		if ((bx - ax) * (cy - ay) - (by - ay) * (cx - ax) == 0)
		{
			return false;
		}

		face.xpoints[0] = ax;
		face.xpoints[1] = bx;
		face.xpoints[2] = cx;
		face.ypoints[0] = ay;
		face.ypoints[1] = by;
		face.ypoints[2] = cy;
		face.invalidate();

		return true;
	}

	/**
	 * What colour a face is drawn in.
	 *
	 * The game shades a face across its three corners, and a single fill can only be the one colour, so
	 * the three are averaged. A model of this sort is made of small faces, so the step from one to the
	 * next is where the shading would have been anyway.
	 */
	private Color color(
		int i, int[] colors1, int[] colors2, int[] colors3, byte[] transparencies, short[] textures)
	{
		int alpha = 255;

		if (transparencies != null)
		{
			// Kept as how much of what is behind shows through, which is the other way up from alpha
			alpha = 255 - (transparencies[i] & 0xff);

			if (alpha < 1)
			{
				return null;
			}
		}

		Palette colors = palette();

		if (textures != null && textures[i] != -1)
		{
			// Drawn flat in what the game calls the texture's own colour, rather than with the picture
			// itself wrapped over it
			return new Color(texture(textures[i]) & 0xffffff | (alpha << 24), true);
		}

		if (colors1 == null)
		{
			return null;
		}

		int rgb = colors.rgb(colors1[i]);

		if (colors3 != null && colors3[i] != FLAT && colors2 != null)
		{
			rgb = average(rgb, colors.rgb(colors2[i]), colors.rgb(colors3[i]));
		}

		return new Color(rgb | (alpha << 24), true);
	}

	private static int average(int first, int second, int third)
	{
		int red = (((first >> 16) & 0xff) + ((second >> 16) & 0xff) + ((third >> 16) & 0xff)) / 3;
		int green = (((first >> 8) & 0xff) + ((second >> 8) & 0xff) + ((third >> 8) & 0xff)) / 3;
		int blue = ((first & 0xff) + (second & 0xff) + (third & 0xff)) / 3;

		return (red << 16) | (green << 8) | blue;
	}

	private int texture(int texture)
	{
		TextureProvider provider = client.getTextureProvider();

		return provider == null ? 0 : provider.getDefaultColor(texture);
	}

	/**
	 * The colours for the brightness the player is on now, built again where they have changed it.
	 */
	private Palette palette()
	{
		TextureProvider provider = client.getTextureProvider();
		double brightness = provider == null ? 1 : provider.getBrightness();

		if (palette == null || palette.getBrightness() != brightness)
		{
			palette = new Palette(brightness);
		}

		return palette;
	}
}
