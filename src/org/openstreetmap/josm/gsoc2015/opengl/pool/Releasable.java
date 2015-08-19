package org.openstreetmap.josm.gsoc2015.opengl.pool;

import java.nio.FloatBuffer;

/**
 * This is a generic, releasable buffer.
 * @author Michael Zangl
 *
 */
public interface Releasable {

	public FloatBuffer getBuffer();
	public void release();
}
