package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

/**
 * This class allows you to do a delayed deletion of VBOs
 * 
 * @author Michael Zangl
 */
public interface GeometryDisposer {
	/**
	 * Marks an VBO to be deleted. Can be called on any thread.
	 * 
	 * @param vboId
	 *            The id of the vbo.
	 */
	void disposeVBO(int vboId);
}
