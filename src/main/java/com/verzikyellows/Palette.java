package com.verzikyellows;

import net.runelite.api.JagexColor;

/**
 * The colours the game draws models in, worked out the same way the game works them out.
 *
 * A model face does not carry a colour. It carries a number the game looks up in a table it builds once
 * from the brightness the player has chosen, so the same face comes out lighter or darker depending on
 * that setting. The table is not handed out, so it is built again here, by the same arithmetic and from
 * the same brightness, and a face drawn from it comes out the colour the game would have drawn it.
 */
class Palette
{
	/**
	 * How many colours there are: six bits of hue, three of saturation and seven of brightness, which is
	 * what a face colour is packed into.
	 */
	private static final int SIZE = 1 << 16;

	/**
	 * Hue runs around the wheel, so the first and last of the six bits stand for nearly the same colour.
	 * Half a step in puts each one at the middle of what it covers rather than at the edge of it.
	 */
	private static final double HUE_STEP = 1.0 / ((JagexColor.HUE_MAX + 1) * 2);

	private static final double SATURATION_STEP = 1.0 / ((JagexColor.SATURATION_MAX + 1) * 2);

	private final double brightness;

	/**
	 * Worked out as they are asked for rather than all at once, since a pool is drawn in a handful of
	 * colours and working out all of them means a power for every one of sixty five thousand.
	 */
	private final int[] colors = new int[SIZE];

	Palette(double brightness)
	{
		this.brightness = brightness;
	}

	double getBrightness()
	{
		return brightness;
	}

	/**
	 * The colour a face number stands for, without an alpha of its own, or black where the number is not
	 * one of the colours at all.
	 */
	int rgb(int color)
	{
		if (color < 0 || color >= SIZE)
		{
			return 0;
		}

		int known = colors[color];

		if (known != 0)
		{
			return known & 0xffffff;
		}

		int rgb = mix((short) color);

		// Marked as worked out, so that a colour that comes to black is not worked out again every frame
		colors[color] = rgb | 0xff000000;

		return rgb;
	}

	/**
	 * Hue, saturation and brightness turned into red, green and blue, then bent by the brightness
	 * setting. The setting is a power rather than a multiplier, so it lifts the dark parts of a model
	 * further than the light parts, which is why a model drawn without it looks flat.
	 */
	private int mix(short color)
	{
		double hue = JagexColor.unpackHue(color) / (double) (JagexColor.HUE_MAX + 1) + HUE_STEP;
		double saturation =
			JagexColor.unpackSaturation(color) / (double) (JagexColor.SATURATION_MAX + 1) + SATURATION_STEP;
		double light = JagexColor.unpackLuminance(color) / (double) (JagexColor.LUMINANCE_MAX + 1);

		double red = light;
		double green = light;
		double blue = light;

		if (saturation != 0)
		{
			double high = light < 0.5
				? light * (1 + saturation)
				: light + saturation - light * saturation;
			double low = 2 * light - high;

			red = component(low, high, hue + 1 / 3.0);
			green = component(low, high, hue);
			blue = component(low, high, hue - 1 / 3.0);
		}

		int rgb = (channel(red) << 16) | (channel(green) << 8) | channel(blue);

		// The game keeps nothing at a flat zero, since that is what it reads as no colour at all
		return rgb == 0 ? 1 : rgb;
	}

	/**
	 * One of the three channels, from how far around the hue wheel it sits between the darkest and
	 * lightest the saturation allows.
	 */
	private static double component(double low, double high, double hue)
	{
		if (hue < 0)
		{
			hue += 1;
		}
		else if (hue > 1)
		{
			hue -= 1;
		}

		if (6 * hue < 1)
		{
			return low + (high - low) * 6 * hue;
		}

		if (2 * hue < 1)
		{
			return high;
		}

		if (3 * hue < 2)
		{
			return low + (high - low) * (2 / 3.0 - hue) * 6;
		}

		return low;
	}

	private int channel(double amount)
	{
		int value = (int) (Math.pow(amount, brightness) * 256);

		return Math.max(0, Math.min(255, value));
	}
}
