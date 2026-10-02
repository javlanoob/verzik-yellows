# Verzik Yellows

Renders yellows at Verzik P3 above everything else, so the ones that land underneath her are still
somewhere you can see.

## The problem

In the last phase of the Theatre of Blood, Verzik puts down one yellow pool per player and then fires at
everyone. Standing on a pool protects you, two people cannot share one, and being off yours is up to 80
damage. The pools are drawn on the floor, so Verzik's own model covers any that land underneath her.

## What it draws

The pool itself, not a marker for it. The game draws the scene and then hands the canvas over, so the
pool's own model is drawn again at that point, which puts it over Verzik however far behind her it is. It
comes out in its own shape, at whatever frame of its animation the game has it on, in the colours the
game would have used, down to the brightness setting.

Your own character is kept in front of it, since a pool drawn over everything would otherwise be drawn
over you as well. The gap left for you is the envelope the game keeps around your model rather than your
outline, so it is a little wider than you are, and anything the game drew nearer the camera than you shows
through it too.

There is nothing to configure.

## How exact it is

The geometry, the position, the animation frame, the per-face transparency and the colours are the
game's own. Three things are not:

- **Shading across a face.** The game shades each face between its three corners. A single fill can only
  be one colour, so the three are averaged. A model of this sort is made of small faces, so this lands
  close to where the shading would have been.
- **Textured faces.** A face with a texture on it is filled with the colour the game keeps as that
  texture's own, rather than with the picture wrapped over it.
- **Face order.** The game sorts a model's faces before drawing them. These are drawn in the order the
  model lists them, which matters for something with depth to it and not for something lying flat.

## How it finds them

The pools are two spotanims, `VERZIK_POWERBLAST_SAFEZONE` and the one her quicker attack uses. They are
read out of the scene for each frame rather than remembered from when they appeared, so a pool that ends
early, or a scene thrown away on the way out of the room, takes its own drawing with it and there is
nothing kept between frames to go stale. A frame with no pools in it costs the walk over the scene and
nothing else.
