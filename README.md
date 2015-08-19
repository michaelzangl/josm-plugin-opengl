# josm-plugin-opengl

This is a josm plugin that replaces the default, Java2D map view with a version rendered using OpenGL.

## Installing

See [Releases](https://github.com/michaelzangl/josm-plugin-opengl/releases)

## Compiling

For this plugin, you need a special version of JOSM and of GLG2D. For the ease of compiling, GLG2D is just added to the plugin by a symbilic link, so you need Linux to compile this.

Check out those repositories in the same directory:
* https://github.com/michaelzangl/josm-plugin-opengl
* https://github.com/openstreetmap/josm (JOSM with changes for this plugin).
* https://github.com/michaelzangl/glg2d (Bug fixes for GLG2D)

Then run:
* ant josm/build.xml
* ant josm-plugin-opengl/build.xml

## Developing

I use eclipse for developing. Simply add josm and josm-plugin-opengl as eclipse projects.
