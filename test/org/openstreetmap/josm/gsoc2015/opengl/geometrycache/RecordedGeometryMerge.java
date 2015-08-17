package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.FloatBuffer;

import javax.media.opengl.GL2;

import org.jogamp.glg2d.VertexBuffer;
import org.junit.Test;
import org.openstreetmap.josm.gsoc2015.opengl.pool.VertexBufferPool;
import org.openstreetmap.josm.gsoc2015.opengl.pool.VertexBufferPool.PooledVertexBuffer;

public class RecordedGeometryMerge {
	private class AccesibleRecordedGeometry extends RecordedGeometry {
		public AccesibleRecordedGeometry(int drawMode,
				PooledVertexBuffer vBuffer, int color) {
			super(drawMode, vBuffer, color);
		}

		int getPoints() {
			return points;
		}

		FloatBuffer getCoordinates() {
			return coordinates.getBuffer();
		}
	}

	@Test
	public void simpleMerge() {
		AccesibleRecordedGeometry g1 = new AccesibleRecordedGeometry(
				GL2.GL_TRIANGLES, genVBuffer(0, 20), 0xffffffff);
		AccesibleRecordedGeometry g2 = new AccesibleRecordedGeometry(
				GL2.GL_TRIANGLES, genVBuffer(20, 20), 0xffffffff);
		assertTrue(g1.attemptCombineWith(g2));
		assertEquals((long) 40, (long) g1.getPoints());
		assertBufferEquals(genVBuffer(0, 40).getBuffer(), g1.getCoordinates(),
				g1.getPoints() * 2);
	}

	private void assertBufferEquals(FloatBuffer expected, FloatBuffer actual,
			int len) {
		expected.rewind();
		actual.rewind();
		assertTrue(expected.remaining() >= len);
		assertTrue(actual.remaining() >= len);
		float[] expectedA = new float[len];
		float[] actualA = new float[len];
		expected.get(expectedA);
		actual.get(actualA);
		assertArrayEquals(expectedA, actualA, 0.001f);

		// for (int i = 0; i < len; i++) {
		// assertEquals("Buffer at " + i, expected.get(i), actual.get(i),
		// 0.0001);
		// }
	}

	@Test
	public void triangleStripMerge() {
		AccesibleRecordedGeometry g1 = new AccesibleRecordedGeometry(
				GL2.GL_TRIANGLE_STRIP, genVBuffer(0, 5), 0xffffffff);
		AccesibleRecordedGeometry g2 = new AccesibleRecordedGeometry(
				GL2.GL_TRIANGLES, genVBuffer(20, 20), 0xffffffff);
		assertTrue(g1.attemptCombineWith(g2));
		assertEquals((long) 3 * 3 + 20, (long) g1.getPoints());
		VertexBuffer res = genVBufferTriangleStrip(0, 5);
		res.addVertices((FloatBuffer) genVBuffer(20, 20).getBuffer()
				.position(0).limit(40));
		assertBufferEquals(res.getBuffer(), g1.getCoordinates(),
				g1.getPoints() * 2);
	}

	@Test
	public void triangleFanMerge() {
		AccesibleRecordedGeometry g1 = new AccesibleRecordedGeometry(
				GL2.GL_TRIANGLE_FAN, genVBuffer(0, 5), 0xffffffff);
		AccesibleRecordedGeometry g2 = new AccesibleRecordedGeometry(
				GL2.GL_TRIANGLES, genVBuffer(20, 20), 0xffffffff);
		assertTrue(g1.attemptCombineWith(g2));
		assertEquals((long) 3 * 3 + 20, (long) g1.getPoints());
		VertexBuffer res = genVBufferTriangleFan(0, 5);
		res.addVertices((FloatBuffer) genVBuffer(20, 20).getBuffer()
				.position(0).limit(40));
		assertBufferEquals(res.getBuffer(), g1.getCoordinates(),
				g1.getPoints() * 2);
	}

	private PooledVertexBuffer genVBuffer(int start, int length) {
		PooledVertexBuffer b = VertexBufferPool.DEFAULT_POOL.getVertexBuffer(length);
		for (int i = start; i < start + length; i++) {
			b.addVertex(i, i);
		}
		return b;
	}

	private VertexBuffer genVBufferTriangleStrip(int start, int stripLength) {
		int triangles = stripLength - 2;
		VertexBuffer b = new VertexBuffer(triangles * 3);
		for (int i = start; i < start + triangles; i++) {
			if ((i - start) % 2 == 0) {
				b.addVertex(i, i);
				b.addVertex(i + 1, i + 1);
			} else {
				b.addVertex(i + 1, i + 1);
				b.addVertex(i, i);
			}
			b.addVertex(i + 2, i + 2);
		}
		return b;
	}

	private VertexBuffer genVBufferTriangleFan(int start, int stripLength) {
		int triangles = stripLength - 2;
		VertexBuffer b = new VertexBuffer(triangles * 3);
		for (int i = start; i < start + triangles; i++) {
			b.addVertex(start, start);
			b.addVertex(i + 1, i + 1);
			b.addVertex(i + 2, i + 2);
		}
		return b;
	}

}
