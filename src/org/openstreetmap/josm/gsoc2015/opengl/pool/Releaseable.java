package org.openstreetmap.josm.gsoc2015.opengl.pool;

import java.nio.FloatBuffer;

public interface Releaseable {

	public FloatBuffer getBuffer();
	public void release();
}
