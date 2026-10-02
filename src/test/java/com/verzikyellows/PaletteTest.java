package com.verzikyellows;

import net.runelite.api.JagexColor;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PaletteTest
{
	private static final double PLAIN = 1.0;

	@Test
	public void turnsTheRedEndOfTheWheelIntoRed()
	{
		int rgb = new Palette(PLAIN).rgb(hsl(0, JagexColor.SATURATION_MAX, 64));

		assertTrue(red(rgb) > green(rgb));
		assertTrue(red(rgb) > blue(rgb));
	}

	@Test
	public void turnsTheGreenEndOfTheWheelIntoGreen()
	{
		int rgb = new Palette(PLAIN).rgb(hsl(21, JagexColor.SATURATION_MAX, 64));

		assertTrue(green(rgb) > red(rgb));
		assertTrue(green(rgb) > blue(rgb));
	}

	/**
	 * The game keeps nothing at a flat zero, since a face colour of zero is what it reads as a face with
	 * no colour at all, so the darkest a colour comes out is one off black.
	 */
	@Test
	public void keepsTheDarkestColourOffZero()
	{
		assertEquals(1, new Palette(PLAIN).rgb(hsl(0, JagexColor.SATURATION_MAX, 0)));
	}

	/**
	 * The brightness setting is a power rather than a multiplier, so a lower one lifts a colour instead of
	 * dimming it, and lifts the dark parts of a model further than the light parts.
	 */
	@Test
	public void liftsAColourAsTheBrightnessSettingDrops()
	{
		int color = hsl(0, JagexColor.SATURATION_MAX, 64);
		int plain = new Palette(PLAIN).rgb(color);
		int lifted = new Palette(0.6).rgb(color);

		assertTrue(red(lifted) >= red(plain));
		assertTrue(green(lifted) > green(plain));
		assertTrue(blue(lifted) > blue(plain));
	}

	@Test
	public void givesTheSameColourTheSecondTimeItIsAsked()
	{
		Palette palette = new Palette(PLAIN);
		int color = hsl(30, 4, 90);

		assertEquals(palette.rgb(color), palette.rgb(color));
	}

	@Test
	public void hasNoColourForANumberThatIsNotOne()
	{
		assertEquals(0, new Palette(PLAIN).rgb(-1));
		assertEquals(0, new Palette(PLAIN).rgb(1 << 16));
	}

	/**
	 * A face colour arrives as a number rather than as the short the game packs one into, and the top
	 * half of the wheel packs into a negative short, so the sign has to come off it first.
	 */
	private static int hsl(int hue, int saturation, int luminance)
	{
		return JagexColor.packHSL(hue, saturation, luminance) & 0xffff;
	}

	private static int red(int rgb)
	{
		return (rgb >> 16) & 0xff;
	}

	private static int green(int rgb)
	{
		return (rgb >> 8) & 0xff;
	}

	private static int blue(int rgb)
	{
		return rgb & 0xff;
	}
}
